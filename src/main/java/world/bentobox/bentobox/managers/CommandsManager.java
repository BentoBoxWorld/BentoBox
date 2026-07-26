package world.bentobox.bentobox.managers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
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

    public void registerCommand(@NonNull CompositeCommand command) {
        commands.put(command.getLabel(), command);
        if (brigadier != null) {
            // Commands created before the lifecycle window opens are picked up by
            // the registrar when it opens, so nothing more is needed here.
            brigadier.registerCommand(command);
            return;
        }
        registerWithCommandMap(command);
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
     * Unregisters all BentoBox registered commands with Bukkit
     */
    public void unregisterCommands() {
        if (brigadier != null) {
            // Brigadier has no API for removing a node, so the nodes stay put and
            // instead resolve to nothing: their requires predicate hides them from
            // clients and their executor becomes a no-op. Any command re-registered
            // after this reuses the node it already has.
            commands.clear();
            brigadier.refreshClients();
            return;
        }
        // Use reflection to obtain the knownCommands in the commandMap
        try {
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) commandMap.getClass().getMethod("getKnownCommands").invoke(commandMap);
            //noinspection SuspiciousMethodCalls
            knownCommands.values().removeIf(commands.values()::contains);
            // Not sure if this is needed, but it clears out all references
            commands.values().forEach(c -> c.unregister(commandMap));
            // Zap everything
            commands.clear();
        } catch(Exception e){
            BentoBox.getInstance().logError("Known commands reflection was not possible, BentoBox is now unstable, so restart server!");
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
