package world.bentobox.bentobox.managers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.commands.BentoBoxCommand;
import world.bentobox.bentobox.commands.brigadier.BrigadierCommandRegistrar;

public class CommandsManager {

    @NonNull
    private final Map<@NonNull String, @NonNull CompositeCommand> commands = new HashMap<>();
    private SimpleCommandMap commandMap;

    /**
     * Brigadier registrar, or {@code null} when Brigadier registration is turned
     * off in the config or is unavailable on this server, in which case the
     * legacy command map path is used instead.
     */
    @Nullable
    private final BrigadierCommandRegistrar brigadier;

    public CommandsManager() {
        BentoBox plugin = BentoBox.getInstance();
        BrigadierCommandRegistrar registrar = null;
        if (plugin.getSettings().isUseBrigadierCommands()) {
            try {
                registrar = new BrigadierCommandRegistrar(plugin);
                // Must be hooked before Paper opens the COMMANDS lifecycle window,
                // which happens as soon as onEnable returns.
                registrar.hookLifecycle();
            } catch (Exception | LinkageError e) {
                plugin.logWarning(
                        "Brigadier command registration is unavailable, falling back to the legacy command map: "
                                + e.getMessage());
                registrar = null;
            }
        }
        this.brigadier = registrar;
    }

    /**
     * Registers a top level command with the server.
     * <p>
     * The command goes into the Bukkit command map whatever Brigadier does with it
     * afterwards. The command map is what every other plugin dispatches BentoBox
     * commands through - NPC plugins, command signs, GUI plugins - and Paper builds
     * its command map entries from whatever it finds in the Brigadier dispatcher. A
     * node put there outside Paper's own command API carries no {@code
     * APICommandMeta}, so Paper wraps it as a {@code VanillaCommandWrapper}, which
     * demands {@code minecraft.command.<label>} and answers with the server's
     * no-permission message before the command ever runs (#3050).
     * <p>
     * Order matters: Paper's command map removes whatever node already holds a
     * label before installing its own, so the command map has to go first and
     * Brigadier merges the sub-command tree onto Paper's node afterwards.
     *
     * @param command top level command
     */
    public void registerCommand(@NonNull CompositeCommand command) {
        commands.put(command.getLabel(), command);
        registerWithCommandMap(command);
        if (brigadier != null) {
            // Commands created before the lifecycle window opens are picked up by
            // the registrar when it opens, so nothing more is needed here.
            brigadier.registerCommand(command);
        }
    }

    // Reflection is required to access Bukkit's internal command map for dynamic command registration
    @SuppressWarnings("java:S3011")
    private void registerWithCommandMap(@NonNull CompositeCommand command) {
        // Use reflection to obtain the commandMap method in Bukkit's server.
        try{
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (SimpleCommandMap) commandMapField.get(Bukkit.getServer());

            String commandPrefix = "bentobox";
            if (command.getAddon() != null) {
                commandPrefix = command.getAddon().getDescription().getName().toLowerCase(Locale.ENGLISH);
            }
            if (!commandMap.register(commandPrefix, command)) {
                BentoBox.getInstance().logError("Failed to register command " + commandPrefix + " " + command.getLabel());
            }
        }
        catch(Exception exception){
            BentoBox.getInstance().logError("Bukkit server commandMap method is not there! This means no commands can be registered!");
        }
    }

    /**
     * Rebuilds the Brigadier command tree so that sub-commands registered after a
     * top level command was first advertised are included.
     * <p>
     * Addons add their sub-commands to game mode commands as they enable, which
     * happens a tick after Paper's command lifecycle window has closed and the
     * tree was built. Those sub-commands run either way, but without this the
     * client is never told about them, so they are missing from tab completion.
     * <p>
     * Does nothing when Brigadier registration is off - the legacy command map
     * resolves completions dynamically and has never had this problem.
     *
     * @since 3.22.0
     */
    public void refreshCommandTrees() {
        if (brigadier != null) {
            brigadier.refreshTrees();
        }
    }

    /**
     * Unregisters the top level commands owned by an addon.
     * <p>
     * Addons commonly build their commands in {@code onLoad()}, and a
     * {@link CompositeCommand} registers itself the moment it is constructed. An
     * addon that never gets as far as being enabled - a missing dependency, an
     * incompatibility, an exception on the way up - therefore leaves live commands
     * behind whose world was never set, and a game mode command with a null world
     * throws as soon as anyone runs it (#3059). This takes them back out again.
     * <p>
     * Brigadier keeps its nodes, because it has no API for removing one, but a
     * node whose label no longer resolves to a command is refused by its
     * {@code requires} predicate and does nothing if it is run anyway, so it is
     * invisible to clients from here on.
     *
     * @param addon addon whose commands should be removed
     * @since 3.22.2
     */
    public void unregisterCommands(@NonNull Addon addon) {
        List<CompositeCommand> owned = commands.values().stream().filter(c -> addon.equals(c.getAddon())).toList();
        if (owned.isEmpty()) {
            return;
        }
        owned.forEach(c -> commands.remove(c.getLabel()));
        if (commandMap != null) {
            // Use reflection to obtain the knownCommands in the commandMap
            try {
                @SuppressWarnings("unchecked")
                Map<String, Command> knownCommands = (Map<String, Command>) commandMap.getClass()
                        .getMethod("getKnownCommands").invoke(commandMap);
                // Remove by key rather than through the values() view - see
                // unregisterCommands() for why.
                List<String> labels = knownCommands.entrySet().stream().filter(e -> owned.contains(e.getValue()))
                        .map(Entry::getKey).toList();
                labels.forEach(knownCommands::remove);
                owned.forEach(c -> c.unregister(commandMap));
            } catch (Exception e) {
                BentoBox.getInstance()
                        .logError("Could not unregister commands for " + addon.getDescription().getName() + ": " + e);
            }
        }
        if (brigadier != null) {
            // Clients are still offering the commands that have just gone away
            brigadier.refreshClients();
        }
    }

    /**
     * Unregisters all BentoBox registered commands with Bukkit
     */
    public void unregisterCommands() {
        if (brigadier != null) {
            // The nodes go with the command map entries below - on Paper the two are
            // the same thing. Forget what was registered so that a command coming
            // back gets its sub-command tree grafted on again rather than being
            // skipped as already known.
            brigadier.reset();
        }
        if (commandMap == null) {
            // Nothing was ever registered, so there is nothing to take out
            commands.clear();
            return;
        }
        // Use reflection to obtain the knownCommands in the commandMap
        try {
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) commandMap.getClass().getMethod("getKnownCommands").invoke(commandMap);
            // Remove by key rather than through the values() view. On Paper the known
            // commands are a facade over the Brigadier dispatcher rather than a real
            // map, and removing a key is the only operation that reliably takes the
            // node out with it.
            List<String> labels = knownCommands.entrySet().stream().filter(e -> commands.containsValue(e.getValue()))
                    .map(Entry::getKey).toList();
            labels.forEach(knownCommands::remove);
            // Not sure if this is needed, but it clears out all references
            commands.values().forEach(c -> c.unregister(commandMap));
            // Zap everything
            commands.clear();
        } catch(Exception e){
            BentoBox.getInstance().logError("Known commands reflection was not possible, BentoBox is now unstable, so restart server!");
        }
        if (brigadier != null) {
            // Clients are still offering the commands that have just gone away
            brigadier.refreshClients();
        }
    }

    /**
     * Try to get a registered command.
     * @param command - command string
     * @return CompositeCommand or null if it does not exist
     */
    @Nullable
    public CompositeCommand getCommand(@NonNull String command) {
        return commands.get(command);
    }

    /**
     * Resolves a top level command from the first token of something that was
     * typed: its label, one of its aliases, or either of those in the
     * {@code addon:label} form, in any case.
     *
     * @param label first token of a command line, without the leading slash
     * @return the command, or {@code null} if BentoBox does not own that label
     * @since 3.22.0
     */
    @Nullable
    public CompositeCommand resolveCommand(@NonNull String label) {
        int colon = label.indexOf(':');
        String name = colon >= 0 ? label.substring(colon + 1) : label;
        CompositeCommand command = commands.get(name);
        if (command != null) {
            return command;
        }
        for (CompositeCommand c : commands.values()) {
            if (c.getLabel().equalsIgnoreCase(name)
                    || c.getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(name))) {
                return c;
            }
        }
        return null;
    }

    /**
     * Get a map of every command registered in BentoBox
     * @return the commands
     */
    @NonNull
    public Map<String, CompositeCommand> getCommands() {
        return commands;
    }

    /**
     * List all commands registered so far
     * @return set of commands
     */
    @NonNull
    public Set<String> listCommands() {
        return commands.keySet();
    }

    /**
     * Registers BentoBox's built-in top-level commands.
     */
    public void registerDefaultCommands() {
        new BentoBoxCommand();
    }
}
