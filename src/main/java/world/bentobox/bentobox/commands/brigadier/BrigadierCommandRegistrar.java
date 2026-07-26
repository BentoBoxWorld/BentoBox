package world.bentobox.bentobox.commands.brigadier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.commands.CompositeCommand;

/**
 * Registers BentoBox {@link CompositeCommand}s with Paper's Brigadier command
 * system, so clients get native subcommand completion and highlighting
 * <i>while</i> the player types rather than only after they press enter.
 * <p>
 * <b>Why this class captures the dispatcher.</b> Paper only permits Brigadier
 * registration inside the {@code LifecycleEvents.COMMANDS} callback, and that
 * event fires after {@code onEnable} returns but <i>before</i> the first server
 * tick. BentoBox enables its addons on that first tick (see
 * {@code BentoBox#completeSetup}), so game mode commands such as
 * {@code /island} do not exist yet while the lifecycle window is open. Using the
 * {@code Commands} registrar after the window closes throws
 * {@code "No lifecycle owner context is set"}, and calling {@code getDispatcher()}
 * on it then throws {@code "cannot access the dispatcher in this context"}.
 * <p>
 * The dispatcher instance itself is the live one the server dispatches against,
 * so this class grabs it <i>while</i> the window is open and registers nodes into
 * it afterwards as addons come up. Nodes added that way survive
 * {@code syncCommands()} and are dispatchable by players and console alike.
 * <p>
 * Nodes never capture a {@link CompositeCommand} reference; every lookup goes
 * through the label. That matters because Brigadier has no API for removing a
 * node once added: a {@code /bentobox reload} that swaps the command objects
 * therefore leaves no stale references behind, and a label that disappears
 * entirely is hidden from clients because its {@code requires} predicate stops
 * matching.
 *
 * @author tastybento
 * @since 3.22.0
 */
public class BrigadierCommandRegistrar {

    /**
     * Maximum depth of the literal subcommand tree handed to clients. Deeper
     * subcommands still work - they fall through to the greedy argument - they
     * are simply not pre-advertised in the command tree packet.
     */
    private static final int MAX_TREE_DEPTH = 4;

    private static final String ARGS = "args";

    private final BentoBox plugin;

    /**
     * The live dispatcher, captured inside the COMMANDS lifecycle window. Null
     * until that window has opened at least once.
     */
    @Nullable
    private CommandDispatcher<CommandSourceStack> dispatcher;

    /** Top level labels and aliases already given a node in the dispatcher. */
    private final Set<String> registeredLabels = new HashSet<>();

    public BrigadierCommandRegistrar(@NonNull BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Hooks the Paper COMMANDS lifecycle event. Must be called during
     * {@code onEnable}, before the lifecycle window opens.
     */
    public void hookLifecycle() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            dispatcher = event.registrar().getDispatcher();
            // A reload rebuilds the tree, so anything registered before has gone.
            registeredLabels.clear();
            // Register everything already known. Commands created later come
            // through registerCommand() directly.
            new ArrayList<>(plugin.getCommandsManager().getCommands().values()).forEach(this::registerCommand);
        });
    }

    /**
     * @return {@code true} once the COMMANDS lifecycle window has opened and the
     *         dispatcher has been captured.
     */
    public boolean isAvailable() {
        return dispatcher != null;
    }

    /**
     * Registers a top level command and all of its aliases with Brigadier. If the
     * lifecycle window has not opened yet the command is a no-op here - it will
     * be swept up by {@link #hookLifecycle()} when the window opens.
     *
     * @param command top level composite command
     */
    public void registerCommand(@NonNull CompositeCommand command) {
        if (dispatcher == null) {
            return;
        }
        String label = command.getLabel().toLowerCase(Locale.ENGLISH);
        boolean added = false;
        LiteralCommandNode<CommandSourceStack> main = null;
        if (registeredLabels.add(label)) {
            main = dispatcher.register(buildTopLevel(label));
            added = true;
        }
        if (main != null) {
            // Aliases and the namespaced form redirect rather than duplicating the
            // tree - a game mode admin tree runs to dozens of nodes and every one
            // of them is sent to every client.
            for (String alias : command.getAliases()) {
                added |= registerRedirect(alias, main, label);
            }
            added |= registerRedirect(namespace(command) + ":" + label, main, label);
        }
        if (added) {
            // Players already online - an addon enabled at runtime, or a
            // /bentobox reload - need the new tree pushed to them.
            Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
        }
    }

    /**
     * Rebuilds the literal tree of every registered top level command, picking up
     * sub-commands added since the node was first built.
     * <p>
     * This is needed because a top level command's children are baked in when its
     * node is created, and addons add their sub-commands to game mode commands
     * later - {@code enableAddons()} runs a tick after the lifecycle window has
     * closed. Without this the late sub-commands still run, because the greedy
     * argument catches them, but the client is never told they exist.
     * <p>
     * Brigadier merges same-named literals rather than replacing them
     * ({@code CommandNode#addChild} copies the incoming command across and
     * recursively merges its children), so re-registering a freshly built tree
     * grafts the new children onto the live node and leaves the existing ones
     * alone. Nodes are never removed, but a sub-command that has gone away
     * resolves to null and its {@code requires} predicate stops matching, so it
     * disappears from clients anyway.
     */
    public void refreshTrees() {
        if (dispatcher == null) {
            return;
        }
        boolean changed = false;
        for (CompositeCommand command : new ArrayList<>(plugin.getCommandsManager().getCommands().values())) {
            String label = command.getLabel().toLowerCase(Locale.ENGLISH);
            if (!registeredLabels.contains(label)) {
                // Never registered at all - the normal path builds it and pushes it.
                registerCommand(command);
                continue;
            }
            int before = childCount(label);
            dispatcher.register(buildTopLevel(label));
            // Merging is a no-op when nothing new turned up, and pushing the tree to
            // every online player for no reason is not free.
            changed |= childCount(label) != before;
        }
        if (changed) {
            refreshClients();
        }
    }

    /**
     * Total number of nodes in a top level command's tree, used to tell whether a
     * rebuild actually added anything.
     *
     * @param label top level label
     * @return node count, 0 if the label has no node
     */
    private int childCount(@NonNull String label) {
        CommandNode<CommandSourceStack> node = dispatcher == null ? null : dispatcher.getRoot().getChild(label);
        return node == null ? 0 : countNodes(node);
    }

    private static int countNodes(@NonNull CommandNode<CommandSourceStack> node) {
        int count = 1;
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            count += countNodes(child);
        }
        return count;
    }

    /**
     * Pushes the tree for online players. Used after commands are unregistered,
     * so clients stop offering commands that have gone away.
     */
    public void refreshClients() {
        if (dispatcher != null) {
            Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
        }
    }

    /**
     * Registers {@code alias} as a Brigadier redirect onto the command's main
     * node.
     *
     * @param alias    alias or namespaced label
     * @param main     node to redirect to
     * @param topLabel main label, used to resolve the command at runtime
     * @return {@code true} if a node was added
     */
    private boolean registerRedirect(@NonNull String alias, @NonNull LiteralCommandNode<CommandSourceStack> main,
            @NonNull String topLabel) {
        String key = alias.toLowerCase(Locale.ENGLISH);
        if (!registeredLabels.add(key)) {
            return false;
        }
        dispatcher.register(Commands.literal(key).requires(source -> canUse(resolve(topLabel), source))
                .executes(ctx -> run(topLabel, ctx)).redirect(main));
        return true;
    }

    /**
     * The command map prefix BentoBox has always used for the {@code plugin:label}
     * form of a command.
     */
    private static String namespace(@NonNull CompositeCommand command) {
        return command.getAddon() == null ? "bentobox"
                : ((Addon) command.getAddon()).getDescription().getName().toLowerCase(Locale.ENGLISH);
    }

    /*
     * ------------------------------------------------------------------
     * Node building
     * ------------------------------------------------------------------
     */

    /**
     * Builds the literal node for a top level label. The node resolves its
     * command by label at execution time rather than capturing it, so it stays
     * correct across reloads.
     */
    private LiteralArgumentBuilder<CommandSourceStack> buildTopLevel(@NonNull String label) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(label)
                .requires(source -> canUse(resolve(label), source)).executes(ctx -> run(label, ctx));
        addChildren(root, label, () -> resolve(label), 0);
        return root;
    }

    /**
     * Adds the literal subcommand children plus the catch-all greedy argument to
     * {@code parent}.
     *
     * @param parent   builder being populated
     * @param topLabel top level label, used to re-resolve the tree at runtime
     * @param supplier resolves the command this node represents
     * @param depth    current depth, guards against absurdly deep trees
     */
    private void addChildren(LiteralArgumentBuilder<CommandSourceStack> parent, @NonNull String topLabel,
            @NonNull CommandSupplier supplier, int depth) {
        CompositeCommand command = supplier.get();
        if (command != null && depth < MAX_TREE_DEPTH) {
            for (CompositeCommand sub : command.getSubCommands().values()) {
                if (sub.isHidden()) {
                    // Hidden commands still run, they are just not advertised.
                    continue;
                }
                String subLabel = sub.getLabel().toLowerCase(Locale.ENGLISH);
                CommandSupplier subSupplier = () -> {
                    CompositeCommand p = supplier.get();
                    return p == null ? null : p.getSubCommands().get(subLabel);
                };
                LiteralArgumentBuilder<CommandSourceStack> child = Commands.literal(subLabel)
                        .requires(source -> canUse(subSupplier.get(), source)).executes(ctx -> run(topLabel, ctx));
                addChildren(child, topLabel, subSupplier, depth + 1);
                parent.then(child);
            }
        }
        // Catch-all: anything the literals do not match is handed to the existing
        // dispatch, preserving today's behaviour exactly.
        parent.then(Commands.argument(ARGS, StringArgumentType.greedyString())
                .suggests((ctx, builder) -> suggest(topLabel, ctx.getSource().getSender(), builder))
                .executes(ctx -> run(topLabel, ctx)));
    }

    /*
     * ------------------------------------------------------------------
     * Execution and suggestions
     * ------------------------------------------------------------------
     */

    /**
     * Runs the command. Arguments are taken from the raw input rather than from
     * Brigadier's parsed nodes, so dispatch is identical to the legacy command
     * map path however the parse happened to split.
     */
    private int run(@NonNull String topLabel, CommandContext<CommandSourceStack> ctx) {
        CompositeCommand command = resolve(topLabel);
        if (command == null) {
            return 0;
        }
        String[] tokens = tokenize(ctx.getInput());
        String label = tokens.length > 0 ? tokens[0] : topLabel;
        command.execute(ctx.getSource().getSender(), label, argsOf(tokens, false));
        // Always report success. BentoBox commands report their own failures to
        // the user; a Brigadier failure result would print a second, generic
        // error on top of that.
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    /**
     * Maps {@link CompositeCommand#tabComplete} onto Brigadier's suggestion
     * mechanism, which the client queries on every keystroke rather than only on
     * TAB.
     */
    private CompletableFuture<Suggestions> suggest(@NonNull String topLabel, CommandSender sender,
            SuggestionsBuilder builder) {
        CompositeCommand command = resolve(topLabel);
        if (command == null) {
            return builder.buildFuture();
        }
        String[] tokens = tokenize(builder.getInput());
        String label = tokens.length > 0 ? tokens[0] : topLabel;
        String[] args = argsOf(tokens, builder.getInput().endsWith(" "));
        List<String> options;
        try {
            options = new ArrayList<>(command.tabComplete(sender, label, args));
        } catch (Exception e) {
            plugin.logError("Brigadier suggestions failed for /" + topLabel + ": " + e.getMessage());
            return builder.buildFuture();
        }
        // Literal sibling nodes already suggest these client-side, and Brigadier
        // does not deduplicate across nodes, so drop them to avoid doubles.
        options.removeAll(literalSiblings(builder, sender));

        SuggestionsBuilder offset = atCurrentToken(builder);
        options.forEach(offset::suggest);
        return offset.buildFuture();
    }

    /**
     * The visible literal subcommands at the position the greedy argument starts,
     * i.e. the ones Brigadier itself will already be suggesting.
     * <p>
     * A sub-command only counts if it actually has a literal node in the
     * dispatcher. The command tree and the live {@link CompositeCommand} tree can
     * disagree - a sub-command registered after this node was built has no literal
     * of its own - and dropping such a sub-command here would remove it from the
     * greedy argument's suggestions as well, leaving no way for the client to
     * learn about it at all.
     */
    private Set<String> literalSiblings(SuggestionsBuilder builder, CommandSender sender) {
        // Everything before the greedy argument was consumed by literal nodes.
        String[] consumed = tokenize(builder.getInput().substring(0, builder.getStart()));
        CompositeCommand current = consumed.length > 0 ? resolve(consumed[0]) : null;
        for (int i = 1; i < consumed.length && current != null; i++) {
            current = current.getSubCommands().get(consumed[i].toLowerCase(Locale.ENGLISH));
        }
        if (current == null || consumed.length - 1 >= MAX_TREE_DEPTH) {
            return Set.of();
        }
        Set<String> advertised = advertisedChildren(consumed);
        Set<String> labels = new HashSet<>();
        for (CompositeCommand sub : current.getSubCommands().values()) {
            if (!sub.isHidden() && canUse(sub, sender)
                    && advertised.contains(sub.getLabel().toLowerCase(Locale.ENGLISH))) {
                labels.add(sub.getLabel());
            }
        }
        return labels;
    }

    /**
     * The names of the literal children Brigadier holds at the node the given
     * tokens lead to, which are exactly the ones it will suggest by itself.
     *
     * @param consumed tokens consumed by literal nodes, label first
     * @return literal child names, lower case, or an empty set if the path does
     *         not exist in the dispatcher
     */
    private Set<String> advertisedChildren(String[] consumed) {
        if (dispatcher == null || consumed.length == 0) {
            return Set.of();
        }
        CommandNode<CommandSourceStack> node = dispatcher.getRoot()
                .getChild(consumed[0].toLowerCase(Locale.ENGLISH));
        if (node != null && node.getRedirect() != null) {
            // An alias or the namespaced form - the children live on the main node.
            node = node.getRedirect();
        }
        for (int i = 1; i < consumed.length && node != null; i++) {
            node = node.getChild(consumed[i].toLowerCase(Locale.ENGLISH));
        }
        if (node == null) {
            return Set.of();
        }
        Set<String> names = new HashSet<>();
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            if (child instanceof LiteralCommandNode) {
                names.add(child.getName().toLowerCase(Locale.ENGLISH));
            }
        }
        return names;
    }

    /*
     * ------------------------------------------------------------------
     * Helpers
     * ------------------------------------------------------------------
     */

    /**
     * Splits a raw command line into label and arguments, dropping any leading
     * slash and Paper's {@code plugin:} namespace prefix.
     */
    static String[] tokenize(@NonNull String input) {
        String line = input.startsWith("/") ? input.substring(1) : input;
        String[] tokens = line.split(" ");
        if (tokens.length > 0) {
            int colon = tokens[0].indexOf(':');
            if (colon >= 0) {
                tokens[0] = tokens[0].substring(colon + 1);
            }
        }
        return tokens;
    }

    /**
     * The argument array to hand to {@link CompositeCommand}, i.e. everything
     * after the label.
     *
     * @param tokens        output of {@link #tokenize(String)}
     * @param trailingSpace whether the raw input ended in a space, in which case
     *                      CompositeCommand expects an extra empty element so that
     *                      it completes the next argument rather than the last one
     * @return arguments, never null
     */
    static String[] argsOf(String @NonNull [] tokens, boolean trailingSpace) {
        String[] args = tokens.length > 1 ? Arrays.copyOfRange(tokens, 1, tokens.length) : new String[0];
        if (trailingSpace) {
            args = Arrays.copyOf(args, args.length + 1);
            args[args.length - 1] = "";
        }
        return args;
    }

    /**
     * Start index for suggestions inside the greedy argument. The argument covers
     * everything from {@code builder.getStart()} to the cursor, but a suggestion
     * must replace only the token currently being typed.
     *
     * @param builder Brigadier's builder for the greedy argument
     * @return builder positioned at the start of the token being typed
     */
    static SuggestionsBuilder atCurrentToken(@NonNull SuggestionsBuilder builder) {
        int lastSpace = builder.getRemaining().lastIndexOf(' ');
        return lastSpace < 0 ? builder : builder.createOffset(builder.getStart() + lastSpace + 1);
    }

    /**
     * Resolves a top level command by its label or by one of its aliases.
     */
    @Nullable
    private CompositeCommand resolve(@NonNull String label) {
        CompositeCommand command = plugin.getCommandsManager().getCommand(label);
        if (command != null) {
            return command;
        }
        for (CompositeCommand c : plugin.getCommandsManager().getCommands().values()) {
            if (c.getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(label))) {
                return c;
            }
        }
        return null;
    }

    /**
     * Mirrors the visibility rules {@link CompositeCommand} applies at execution
     * time, so the client is only shown what it could actually run.
     */
    private static boolean canUse(@Nullable CompositeCommand command, CommandSourceStack source) {
        return command != null && canUse(command, source.getSender());
    }

    private static boolean canUse(@NonNull CompositeCommand command, CommandSender sender) {
        if (!(sender instanceof Player)) {
            // Hiding a node from the console would turn "that command is only
            // available in game" into Brigadier's generic "unknown command", so
            // the console keeps seeing everything and the command reports for
            // itself, exactly as it did under the command map.
            return true;
        }
        if (command.isOnlyConsole()) {
            return false;
        }
        String permission = command.getPermission();
        return permission == null || permission.isEmpty() || sender.isOp() || sender.hasPermission(permission);
    }

    /** Resolves a command lazily by label, so nodes never hold stale references. */
    @FunctionalInterface
    private interface CommandSupplier {
        @Nullable
        CompositeCommand get();
    }
}
