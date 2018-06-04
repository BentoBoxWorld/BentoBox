package us.tastybento.bskyblock.api.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.addons.Addon;
import us.tastybento.bskyblock.api.events.command.CommandEvent;
import us.tastybento.bskyblock.api.localization.TextVariables;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.managers.IslandWorldManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.PlayersManager;
import us.tastybento.bskyblock.util.Util;

/**
 * BSB composite command
 * @author tastybento
 * @author Poslovitch
 */
public abstract class CompositeCommand extends Command implements PluginIdentifiableCommand, BSBCommand {

    private final BSkyBlock plugin;

    /**
     * True if the command is for the player only (not for the console)
     */
    private boolean onlyPlayer = false;
    /**
     * The parameters string for this command. It is the commands followed by a locale reference.
     */
    private String parameters = "";
    /**
     * The parent command to this one. If this is a top-level command it will be empty.
     */
    protected final CompositeCommand parent;
    /**
     * The permission required to execute this command
     */
    private String permission = "";
    /**
     * This is the command level. 0 is the top, 1 is the first level sub command.
     */
    private final int subCommandLevel;
    /**
     * Map of sub commands
     */
    private Map<String, CompositeCommand> subCommands;

    /**
     * Map of aliases for subcommands
     */
    private Map<String, CompositeCommand> subCommandAliases;
    /**
     * The command chain from the very top, e.g., island team promote
     */
    private String usage;

    /**
     * The prefix to be used in this command
     */
    private String permissionPrefix = "";

    /**
     * The world that this command operates in. This is an overworld and will cover any associated nether or end
     * If the world value does not exist, then the command is general across worlds
     */
    private World world;

    /**
     * The addon creating this command, if any
     */
    private Addon addon;

    /**
     * The top level label
     */
    private String topLabel = "";

    private static Map<User, Confirmer> toBeConfirmed = new HashMap<>();

    /**
     * Used only for testing....
     */
    public CompositeCommand(BSkyBlock plugin, String label, String... string) {
        super(label);
        this.plugin = plugin;
        setAliases(new ArrayList<>(Arrays.asList(string)));
        parent = null;
        setUsage("");
        subCommandLevel = 0; // Top level
        subCommands = new LinkedHashMap<>();
        subCommandAliases = new LinkedHashMap<>();
        setup();
        if (!getSubCommand("help").isPresent() && !label.equals("help")) {
            new DefaultHelpCommand(this);
        }
    }

    /**
     * Top level command
     * @param addon - addon creating the command
     * @param label - string for this command
     * @param aliases - aliases
     */
    public CompositeCommand(Addon addon, String label, String... aliases) {
        super(label);
        this.topLabel = label;
        this.addon = addon;
        this.plugin = BSkyBlock.getInstance();
        setAliases(new ArrayList<>(Arrays.asList(aliases)));
        parent = null;
        setUsage("");
        subCommandLevel = 0; // Top level
        subCommands = new LinkedHashMap<>();
        subCommandAliases = new LinkedHashMap<>();
        // Register command if it is not already registered
        if (plugin.getCommand(label) == null) {
            plugin.getCommandsManager().registerCommand(this);
        }
        setup();
        if (!getSubCommand("help").isPresent() && !label.equals("help")) {
            new DefaultHelpCommand(this);
        }
    }

    /**
     * This is the top-level command constructor for commands that have no parent.
     * @param label - string for this command
     * @param aliases - aliases for this command
     */
    public CompositeCommand(String label, String... aliases) {
        super(label);
        this.topLabel = label;
        this.plugin = BSkyBlock.getInstance();
        setAliases(new ArrayList<>(Arrays.asList(aliases)));
        parent = null;
        setUsage("");
        subCommandLevel = 0; // Top level
        subCommands = new LinkedHashMap<>();
        subCommandAliases = new LinkedHashMap<>();
        // Register command if it is not already registered
        if (plugin.getCommand(label) == null) {
            plugin.getCommandsManager().registerCommand(this);
        }
        setup();
        if (!getSubCommand("help").isPresent() && !label.equals("help")) {
            new DefaultHelpCommand(this);
        }
    }

    /**
     * Sub-command constructor
     * @param parent - the parent composite command
     * @param label - string label for this subcommand
     * @param aliases - aliases for this subcommand
     */
    public CompositeCommand(CompositeCommand parent, String label, String... aliases) {
        super(label);
        this.topLabel = parent.getTopLabel();
        this.plugin = BSkyBlock.getInstance();
        this.parent = parent;
        subCommandLevel = parent.getLevel() + 1;
        // Add this sub-command to the parent
        parent.getSubCommands().put(label, this);
        setAliases(new ArrayList<>(Arrays.asList(aliases)));
        subCommands = new LinkedHashMap<>();
        subCommandAliases = new LinkedHashMap<>();
        // Add aliases to the parent for this command
        for (String alias : aliases) {
            parent.getSubCommandAliases().put(alias, this);
        }
        setUsage("");
        // Inherit permission prefix
        this.permissionPrefix = parent.getPermissionPrefix();
        // Inherit world
        this.world = parent.getWorld();
        setup();
        // If this command does not define its own help class, then use the default help command
        if (!getSubCommand("help").isPresent() && !label.equals("help")) {
            new DefaultHelpCommand(this);
        }

    }
    /*
     * This method deals with the command execution. It traverses the tree of
     * subcommands until it finds the right object and then runs execute on it.
     */
    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        // Get the User instance for this sender
        User user = User.getInstance(sender);
        CompositeCommand cmd = getCommandFromArgs(args);
        // Check for console and permissions
        if (cmd.onlyPlayer && !(sender instanceof Player)) {
            user.sendMessage("general.errors.use-in-game");
            return true;
        }
        // Check perms, but only if this isn't the console
        if ((sender instanceof Player) && !sender.isOp() && !cmd.getPermission().isEmpty() && !sender.hasPermission(cmd.getPermission())) {
            user.sendMessage("general.errors.no-permission");
            user.sendMessage("general.errors.you-need", TextVariables.PERMISSION, cmd.getPermission());
            return true;
        }
        // Fire an event to see if this command should be cancelled
        CommandEvent event = CommandEvent.builder()
                .setCommand(this)
                .setLabel(label)
                .setSender(sender)
                .setArgs(args)
                .build();
        if (event.isCancelled()) {
            return true;
        }
        // Execute and trim args
        return cmd.execute(user, Arrays.asList(args).subList(cmd.subCommandLevel, args.length));
    }

    /**
     * Get the current composite command based on the arguments
     * @param args - arguments
     * @return the current composite command based on the arguments
     */
    private CompositeCommand getCommandFromArgs(String[] args) {
        CompositeCommand subCommand = this;
        // Run through any arguments
        for (String arg : args) {
            // get the subcommand corresponding to the arg
            if (subCommand.hasSubCommands()) {
                Optional<CompositeCommand> sub = subCommand.getSubCommand(arg);
                if (!sub.isPresent()) {
                    return subCommand;
                }
                // Step down one
                subCommand = sub.orElse(subCommand);
                // Set the label
                subCommand.setLabel(arg);
            } else {
                // We are at the end of the walk
                return subCommand;
            }
            // else continue the loop
        }
        return subCommand;
    }

    /**
     * Convenience method to get the island manager
     * @return IslandsManager
     */
    protected IslandsManager getIslands() {
        return plugin.getIslands();
    }

    /**
     * @return this command's sub-level. Top level is 0.
     * Every time a command registers with a parent, their level will be set.
     */
    protected int getLevel() {
        return subCommandLevel;
    }

    /**
     * @return Logger
     */
    public Logger getLogger() {
        return plugin.getLogger();
    }

    /**
     * Convenience method to obtain team members
     * @param world - world to check
     * @param user - the User
     * @return set of UUIDs of all team members
     */
    protected Set<UUID> getMembers(World world, User user) {
        return plugin.getIslands().getMembers(world, user.getUniqueId());
    }

    public String getParameters() {
        return parameters;
    }

    /**
     * @return the parent command object
     */
    public CompositeCommand getParent() {
        return parent;
    }

    @Override
    public String getPermission() {
        return permission;
    }

    /**
     * Convenience method to get the player manager
     * @return PlayersManager
     */
    protected PlayersManager getPlayers() {
        return plugin.getPlayers();
    }

    @Override
    public BSkyBlock getPlugin() {
        return plugin;
    }

    /**
     * Get the island worlds manager
     * @return island worlds manager
     */
    public IslandWorldManager getIWM() {
        return plugin.getIWM();
    }
    /**
     * @return Settings object
     */
    public Settings getSettings() {
        return plugin.getSettings();
    }

    /**
     * Returns the CompositeCommand object referring to this command label
     * @param label - command label or alias
     * @return CompositeCommand or null if none found
     */
    public Optional<CompositeCommand> getSubCommand(String label) {
        if (subCommands.containsKey(label.toLowerCase())) {
            return Optional.ofNullable(subCommands.get(label));
        }
        // Try aliases
        if (subCommandAliases.containsKey(label.toLowerCase())) {
            return Optional.ofNullable(subCommandAliases.get(label)); 
        }
        return Optional.empty();
    }

    /**
     * @return Map of sub commands for this command
     */
    public Map<String, CompositeCommand> getSubCommands() {
        return subCommands;
    }

    /**
     * Convenience method to obtain the user's team leader
     * @param world - world to check
     * @param user - the User
     * @return UUID of player's team leader or null if user has no island
     */
    protected UUID getTeamLeader(World world, User user) {
        return plugin.getIslands().getTeamLeader(world, user.getUniqueId());
    }

    @Override
    public String getUsage() {
        return "/" + usage;
    }


    /**
     * Check if this command has a specific sub command
     * @param subCommand - sub command
     * @return true if this command has this sub command
     */
    protected boolean hasSubCommand(String subCommand) {
        return subCommands.containsKey(subCommand) || subCommandAliases.containsKey(subCommand);
    }

    /**
     * Check if this command has any sub commands
     * @return true if this command has subcommands
     */
    protected boolean hasSubCommands() {
        return !subCommands.isEmpty();
    }

    /**
     * Convenience method to check if a user has a team
     * @param world - the world to check
     * @param user - the User
     * @return true if player is in a team
     */
    protected boolean inTeam(World world, User user) {
        return plugin.getIslands().inTeam(world, user.getUniqueId());
    }

    /**
     * Check if this command is only for players
     * @return true or false
     */
    public boolean isOnlyPlayer() {
        return onlyPlayer;
    }

    /**
     * Convenience method to check if a user is a player
     * @param user - the User
     * @return true if sender is a player
     */
    protected boolean isPlayer(User user) {
        return user.getPlayer() != null;
    }

    /**
     * Set whether this command is only for players
     * @param onlyPlayer - true if command only for players
     */
    public void setOnlyPlayer(boolean onlyPlayer) {
        this.onlyPlayer = onlyPlayer;
    }

    /**
     * Sets the command parameters to be shown in help
     * @param parameters - string of parameters
     */
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    /* (non-Javadoc)
     * @see org.bukkit.command.Command#setPermission(java.lang.String)
     */
    @Override
    public void setPermission(String permission) {
        this.permission = permissionPrefix + permission;
    }

    /**
     * Inherits the permission from parent command
     */
    public void inheritPermission() {
        this.permission = parent.getPermission();
    } 

    /**
     * This creates the full linking chain of commands
     */
    @Override
    public Command setUsage(String usage) {
        // Go up the chain
        CompositeCommand parentCommand = getParent();
        StringBuilder u = new StringBuilder().append(getLabel()).append(" ").append(usage);
        while (parentCommand != null) {
            u.insert(0, " ");
            u.insert(0, parentCommand.getLabel());
            parentCommand = parentCommand.getParent();
        }
        this.usage = u.toString().trim();
        return this;
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String alias, final String[] args) {
        List<String> options = new ArrayList<>();
        // Get command object based on args entered so far
        CompositeCommand cmd = getCommandFromArgs(args);
        // Check for console and permissions
        if (cmd.onlyPlayer && !(sender instanceof Player)) {
            return options;
        }
        if (!cmd.getPermission().isEmpty() && !sender.hasPermission(cmd.getPermission())) {
            return options;
        }
        // Add any tab completion from the subcommand
        options.addAll(cmd.tabComplete(User.getInstance(sender), alias, new LinkedList<>(Arrays.asList(args))).orElse(new ArrayList<>()));
        // Add any sub-commands automatically
        if (cmd.hasSubCommands()) {
            // Check if subcommands are visible to this sender
            for (CompositeCommand subCommand: cmd.getSubCommands().values()) {
                if (sender instanceof Player) {
                    // Player
                    if (subCommand.getPermission().isEmpty() || sender.hasPermission(subCommand.getPermission())) {
                        // Permission is okay
                        options.add(subCommand.getLabel());
                    }
                } else {
                    // Console
                    if (!subCommand.onlyPlayer) {
                        // Not a player command
                        options.add(subCommand.getLabel());
                    }
                }
            }
        }

        String lastArg = args.length != 0 ? args[args.length - 1] : "";

        return Util.tabLimit(options, lastArg);
    }

    /**
     * Show help
     * @param command - command that this help is for
     * @param user - the User
     * @return result of help command or false if no help defined
     */
    protected boolean showHelp(CompositeCommand command, User user) {
        return command.getSubCommand("help").map(helpCommand -> helpCommand.execute(user, new ArrayList<>())).orElse(false);
    }

    /**
     * @return the subCommandAliases
     */
    public Map<String, CompositeCommand> getSubCommandAliases() {
        return subCommandAliases;
    }

    /**
     * If the permission prefix has been set, will return the prefix plus a trailing dot.
     * @return the permissionPrefix
     */
    public String getPermissionPrefix() {
        return permissionPrefix;
    }

    /**
     * Set the permission prefix. This will be added automatically to the permission 
     * and will apply to any sub commands too.
     * Do not put a dot on the end of it.
     * @param permissionPrefix the permissionPrefix to set
     */
    public void setPermissionPrefix(String permissionPrefix) {
        this.permissionPrefix = permissionPrefix + ".";
    }

    /**
     * The the world that this command applies to.
     * @return the world
     */
    public World getWorld() {
        if (world == null) {
            plugin.logError(getLabel() + " did not setWorld in setup!");
        }
        return world;
    }

    /**
     * @param world the world to set
     */
    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * @return the addon
     */
    public Addon getAddon() {
        return addon;
    }

    /**
     * @return top level label, e.g., island
     */
    public String getTopLabel() {
        return topLabel;
    }

    /**
     * Tells user to confirm command by retyping
     * @param user - user
     * @param confirmed - runnable to be executed if confirmed
     */
    public void askConfirmation(User user, Runnable confirmed) {
        // Check for pending confirmations
        if (toBeConfirmed.containsKey(user)) {
            if (toBeConfirmed.get(user).getTopLabel().equals(getTopLabel()) && toBeConfirmed.get(user).getLabel().equalsIgnoreCase(getLabel())) {
                toBeConfirmed.get(user).getTask().cancel();
                Bukkit.getScheduler().runTask(getPlugin(), toBeConfirmed.get(user).getRunnable());
                toBeConfirmed.remove(user);
                return;
            } else {
                // Player has another outstanding confirmation request that will now be cancelled
                user.sendMessage("general.previous-request-cancelled");
            }
        }
        // Tell user that they need to confirm
        user.sendMessage("general.confirm", "[seconds]", String.valueOf(getSettings().getConfirmationTime()));
        // Set up a cancellation task
        BukkitTask task = Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            user.sendMessage("general.request-cancelled");
            toBeConfirmed.remove(user);
        }, getPlugin().getSettings().getConfirmationTime() * 20L);
        
        // Add to the global confirmation map
        toBeConfirmed.put(user, new Confirmer(getTopLabel(), getLabel(), confirmed, task));
    }

    private class Confirmer {
        private final String topLabel;
        private final String label;
        private final Runnable runnable;
        private final BukkitTask task;

        /**
         * @param label - command label
         * @param runnable - runnable to run when confirmed
         * @param task - task ID to cancel when confirmed
         */
        Confirmer(String topLabel, String label, Runnable runnable, BukkitTask task) {
            this.topLabel = topLabel;
            this.label = label;
            this.runnable = runnable;
            this.task = task;
        }
        /**
         * @return the topLabel
         */
        public String getTopLabel() {
            return topLabel;
        }
        /**
         * @return the label
         */
        public String getLabel() {
            return label;
        }
        /**
         * @return the runnable
         */
        public Runnable getRunnable() {
            return runnable;
        }
        /**
         * @return the task
         */
        public BukkitTask getTask() {
            return task;
        }

    }
}
