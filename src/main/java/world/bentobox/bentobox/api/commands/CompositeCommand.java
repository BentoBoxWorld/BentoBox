package world.bentobox.bentobox.api.commands;

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
import java.util.stream.Collectors;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.events.command.CommandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * BSB composite command
 * @author tastybento
 * @author Poslovitch
 */
public abstract class CompositeCommand extends Command implements PluginIdentifiableCommand, BentoBoxCommand {

    private static final String COMMANDS = "commands.";

    private final BentoBox plugin;

    /**
     * True if the command is for the player only (not for the console)
     */
    private boolean onlyPlayer = false;

    /**
     * True if command is a configurable rank
     */
    private boolean configurableRankCommand = false;

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

    /**
     * Cool down tracker
     */
    private Map<UUID, Map<UUID, Long>> cooldowns = new HashMap<>();

    /**
     * Top level command
     * @param addon - addon creating the command
     * @param label - string for this command
     * @param aliases - aliases
     */
    public CompositeCommand(Addon addon, String label, String... aliases) {
        super(label, "", "", Arrays.asList(aliases));
        this.addon = addon;
        this.topLabel = label;
        this.plugin = BentoBox.getInstance();
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
        // Default references to description and parameters
        setDescription(COMMANDS + label + ".description");
        setParametersHelp(COMMANDS + label + ".parameters");
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
        this((Addon)null, label, aliases);
    }

    /**
     * Sub-command constructor
     * @param parent - the parent composite command
     * @param label - string label for this subcommand
     * @param aliases - aliases for this subcommand
     */
    public CompositeCommand(CompositeCommand parent, String label, String... aliases) {
        this(parent.getAddon(), parent, label, aliases);
    }

    /**
     * Command to register a command from an addon under a parent command (that could be from another addon)
     * @param addon - this command's addon
     * @param parent - parent command
     * @param aliases - aliases for this command
     */
    public CompositeCommand(Addon addon, CompositeCommand parent, String label, String... aliases ) {
        super(label, "", "", Arrays.asList(aliases));
        this.topLabel = parent.getTopLabel();
        this.plugin = BentoBox.getInstance();
        this.addon = addon;
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

        // Default references to description and parameters
        StringBuilder reference = new StringBuilder();
        reference.append(label);
        CompositeCommand p = this;
        int index = 0;
        while (p.getParent() != null && index < 20) {
            reference.insert(0, p.getParent().getLabel() + ".");
            p = p.getParent();
            index++;
        }
        setDescription(COMMANDS + reference.toString() + ".description");
        setParametersHelp(COMMANDS + reference.toString() + ".parameters");
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
            return false;
        }
        // Check perms, but only if this isn't the console
        if ((sender instanceof Player) && !sender.isOp() && !cmd.getPermission().isEmpty() && !sender.hasPermission(cmd.getPermission())) {
            user.sendMessage("general.errors.no-permission", TextVariables.PERMISSION, cmd.getPermission());
            return false;
        }
        // Fire an event to see if this command should be cancelled
        CommandEvent event = CommandEvent.builder()
                .setCommand(this)
                .setLabel(label)
                .setSender(sender)
                .setArgs(args)
                .build();
        if (event.isCancelled()) {
            return false;
        }
        // Execute and trim args
        return cmd.execute(user, (cmd.subCommandLevel > 0) ? args[cmd.subCommandLevel-1] : label, Arrays.asList(args).subList(cmd.subCommandLevel, args.length));
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
    public BentoBox getPlugin() {
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
        label = label.toLowerCase(java.util.Locale.ENGLISH);
        if (subCommands.containsKey(label)) {
            return Optional.ofNullable(subCommands.get(label));
        }
        // Try aliases
        if (subCommandAliases.containsKey(label)) {
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
     * Returns a map of sub commands for this command.
     * As it needs more calculations to handle the Help subcommand, it is preferable to use {@link #getSubCommands()} when no such distinction is needed.
     * @param ignoreHelp Whether the Help subcommand should not be returned in the map or not.
     * @return Map of sub commands for this command
     * @see #hasSubCommands(boolean)
     */
    public Map<String, CompositeCommand> getSubCommands(boolean ignoreHelp) {
        if (ignoreHelp && getSubCommand("help").isPresent()) {
            Map<String, CompositeCommand> result = subCommands;
            result.remove("help");
            return result;
        }
        return getSubCommands();
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
     * Check if this command has a specific sub command.
     * @param subCommand - sub command
     * @return true if this command has this sub command
     */
    protected boolean hasSubCommand(String subCommand) {
        return subCommands.containsKey(subCommand) || subCommandAliases.containsKey(subCommand);
    }

    /**
     * Check if this command has any sub commands.
     * @return true if this command has subcommands
     */
    protected boolean hasSubCommands() {
        return !subCommands.isEmpty();
    }

    /**
     * Check if this command has any sub commands.
     * As it needs more calculations to handle the Help subcommand, it is preferable to use {@link #hasSubCommands()} when no such distinction is needed.
     * @param ignoreHelp Whether the Help subcommand should not be taken into account or not.
     * @return true if this command has subcommands
     * @see #getSubCommands(boolean)
     */
    protected boolean hasSubCommands(boolean ignoreHelp) {
        return !getSubCommands(ignoreHelp).isEmpty();
    }

    /**
     * Convenience method to check if a user has a team.
     * @param world - the world to check
     * @param user - the User
     * @return true if player is in a team
     */
    protected boolean inTeam(World world, User user) {
        return plugin.getIslands().inTeam(world, user.getUniqueId());
    }

    /**
     * Check if this command is only for players.
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
     * Sets whether this command should only be run by players.
     * If this is set to {@code true}, this command will only be runnable by objects implementing {@link Player}.
     * <br/><br/>
     * The default value provided when instantiating this CompositeCommand is {@code false}.
     * Therefore, this method should only be used in case you want to explicitly edit the value.
     * @param onlyPlayer {@code true} if this command should only be run by players.
     */
    public void setOnlyPlayer(boolean onlyPlayer) {
        this.onlyPlayer = onlyPlayer;
    }

    /**
     * Sets locale reference to this command's description.
     * It is used to display the help of this command.
     *
     * <br/><br/>
     *
     * A default value is provided when instantiating this CompositeCommand:
     *
     * <ul>
     *     <li>{@code "commands." + getLabel() + ".description"} if this is a top-level command;</li>
     *     <li>{@code "commands." + getParent.getLabel() + getLabel() + ".description"} if this is a sub-command.
     *     <br/>
     *     Note that it can have up to 20 parent commands' labels being inserted before this sub-command's label.
     *     Here are a few examples :
     *     <ul>
     *         <li>/bentobox info : {@code "commands.bentobox.info.description"};</li>
     *         <li>/bsbadmin range set : {@code "commands.bsbadmin.range.set.description"};</li>
     *         <li>/mycommand sub1 sub2 sub3 [...] sub22 : {@code "commands.sub3.[...].sub20.sub21.sub22.description"}.</li>
     *     </ul>
     *     </li>
     * </ul>
     *
     * This method should therefore only be used in case you want to provide a different value than the default one.
     *
     * @param description The locale command's description reference to set.
     * @return The instance of this {@link Command}.
     */
    @Override
    public Command setDescription(String description) {
        super.setDescription(description);
        return this;
    }

    /**
     * Sets locale reference to this command's parameters.
     * It is used to display the help of this command.
     *
     * <br/><br/>
     *
     * A default value is provided when instantiating this CompositeCommand:
     *
     * <ul>
     *     <li>{@code "commands." + getLabel() + ".parameters"} if this is a top-level command;</li>
     *     <li>{@code "commands." + getParent.getLabel() + getLabel() + ".parameters"} if this is a sub-command.
     *     <br/>
     *     Note that it can have up to 20 parent commands' labels being inserted before this sub-command's label.
     *     Here are a few examples :
     *     <ul>
     *         <li>/bentobox info : {@code "commands.bentobox.info.parameters"};</li>
     *         <li>/bsbadmin range set : {@code "commands.bsbadmin.range.set.parameters"};</li>
     *         <li>/mycommand sub1 sub2 sub3 [...] sub22 : {@code "commands.sub3.[...].sub20.sub21.sub22.parameters"}.</li>
     *     </ul>
     *     </li>
     * </ul>
     *
     * This method should therefore only be used in case you want to provide a different value than the default one.
     *
     * @param parametersHelp The locale command's paramaters reference to set.
     */
    public void setParametersHelp(String parametersHelp) {
        this.parameters = parametersHelp;
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
        if ((cmd.onlyPlayer && !(sender instanceof Player))) {
            return options;
        }
        if (!cmd.getPermission().isEmpty() && !sender.hasPermission(cmd.getPermission()) && !sender.isOp()) {
            return options;
        }
        // Add any tab completion from the subcommand
        options.addAll(cmd.tabComplete(User.getInstance(sender), alias, new LinkedList<>(Arrays.asList(args))).orElse(new ArrayList<>()));
        if (cmd.hasSubCommands()) {
            options.addAll(getSubCommandLabels(sender, cmd));
        }
        String lastArg = args.length != 0 ? args[args.length - 1] : "";
        return Util.tabLimit(options, lastArg).stream().sorted().collect(Collectors.toList());
    }

    private List<String> getSubCommandLabels(CommandSender sender, CompositeCommand cmd) {
        return cmd.getSubCommands().values().stream().filter(c -> !c.isOnlyPlayer() || sender.isOp()
                || (sender instanceof Player && (c.getPermission().isEmpty() || sender.hasPermission(c.getPermission()))) )
                .map(CompositeCommand::getLabel).collect(Collectors.toList());
    }

    /**
     * Show help
     * @param command - command that this help is for
     * @param user - the User
     * @return result of help command or false if no help defined
     */
    protected boolean showHelp(CompositeCommand command, User user) {
        return command.getSubCommand("help").map(helpCommand -> helpCommand.execute(user, helpCommand.getLabel(), new ArrayList<>())).orElse(false);
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
     * Set a cool down - can be set by other commands on this one
     * @param uniqueId - the caller
     * @param targetUUID - the target (if any)
     * @param timeInSeconds - time in seconds to cool down
     */
    public void setCooldown(UUID uniqueId, UUID targetUUID, int timeInSeconds) {
        cooldowns.putIfAbsent(uniqueId, new HashMap<>());
        cooldowns.get(uniqueId).put(targetUUID, System.currentTimeMillis() + timeInSeconds * 1000);
    }

    /**
     * Check if cool down is in progress
     * @param user - the caller of the command
     * @param targetUUID - the target (if any)
     * @return true if cool down in place, false if not
     */
    protected boolean checkCooldown(User user, UUID targetUUID) {
        if (!cooldowns.containsKey(user.getUniqueId()) || user.isOp() || user.hasPermission(getPermissionPrefix() + ".mod.bypasscooldowns")) {
            return false;
        }
        cooldowns.putIfAbsent(user.getUniqueId(), new HashMap<>());
        if (cooldowns.get(user.getUniqueId()).getOrDefault(targetUUID, 0L) - System.currentTimeMillis() <= 0) {
            // Cool down is done
            cooldowns.get(user.getUniqueId()).remove(targetUUID);
            return false;
        }
        int timeToGo = (int) ((cooldowns.get(user.getUniqueId()).getOrDefault(targetUUID, 0L) - System.currentTimeMillis()) / 1000);
        user.sendMessage("general.errors.you-must-wait", TextVariables.NUMBER, String.valueOf(timeToGo));
        return true;
    }

    /**
     * @return the configurableRankCommand
     */
    public boolean isConfigurableRankCommand() {
        return configurableRankCommand;
    }

    /**
     * This command can be configured for use by different ranks
     */
    public void setConfigurableRankCommand() {
        this.configurableRankCommand = true;
    }

}
