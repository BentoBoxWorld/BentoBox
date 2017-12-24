package us.tastybento.bskyblock.api.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.managers.PlayersManager;
import us.tastybento.bskyblock.database.managers.island.IslandsManager;
import us.tastybento.bskyblock.util.Util;

/**
 * BSB composite command
 * @author ben, poslovich
 *
 */
public abstract class CompositeCommand extends Command implements PluginIdentifiableCommand, BSBCommand {

    private static final boolean DEBUG = true;
    private boolean onlyPlayer = false;
    private final CompositeCommand parent;
    private final int level;
    private String permission = "";
    public BSkyBlock plugin = BSkyBlock.getPlugin();

    private Map<String, CompositeCommand> subCommands;

    /**
     * Sub-command constructor
     * @param parent - the parent composite command
     * @param label - string label for this subcommand
     * @param aliases - aliases for this subcommand
     */
    public CompositeCommand(CompositeCommand parent, String label, String... aliases) {
        super(label);
        this.parent = parent;
        this.level = parent.getLevel() + 1;
        // Add this sub-command to the parent
        parent.getSubCommands().put(label, this);
        this.setAliases(new ArrayList<>(Arrays.asList(aliases)));
        this.subCommands = new LinkedHashMap<>();

        this.setup();
        if (DEBUG)
            Bukkit.getLogger().info("DEBUG: registering command " + label);
    }


    /**
     * @return this command's sub-level. Top level is 0.
     * Every time a command registers with a parent, their level will be set.
     */
    protected int getLevel() {
        return level;
    }


    /**
     * This is the top-level command constructor for commands that have no parent.
     * @param label - string for this command
     * @param aliases - aliases for this command
     */
    public CompositeCommand(String label, String... aliases) {
        super(label);
        this.setDescription(description);
        this.setAliases(new ArrayList<>(Arrays.asList(aliases)));
        this.parent = null;
        this.level = 0; // Top level
        this.subCommands = new LinkedHashMap<>();

        this.setup();
    }

    /* 
     * This method deals with the command execution. It traverses the tree of 
     * subcommands until it finds the right object and then runs execute on it.
     */
    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (DEBUG)
            Bukkit.getLogger().info("DEBUG: executing command " + label);
        // Get the User instance for this sender
        User user = User.getInstance(sender);
        CompositeCommand cmd = getCommandFromArgs(args);
        if (DEBUG) {
            Bukkit.getLogger().info("DEBUG: Command = " + cmd.getLabel() + " onlyplayer = " + cmd.isOnlyPlayer() + " permission = " + cmd.getPermission());
        }
        // Check for console and permissions
        if (cmd.onlyPlayer && !(sender instanceof Player)) {
            user.sendMessage("general.errors.use-in-game");
            return true;
        }
        if (!cmd.getPermission().isEmpty() && !sender.hasPermission(cmd.getPermission())) {
            user.sendMessage("general.errors.no-permission");
            return true;
        }
        // Execute and trim args
        return cmd.execute(user, Arrays.asList(args).subList(cmd.level, args.length));
    }

    /**
     * @return IslandsManager
     */
    protected IslandsManager getIslands() {
        return plugin.getIslands();
    }

    /**
     * @param user
     * @return set of UUIDs of all team members
     */
    protected Set<UUID> getMembers(User user) {
        return plugin.getIslands().getMembers(user.getUniqueId());
    }

    /**
     * @return the parent command objectx
     */
    public CompositeCommand getParent() {
        return parent;
    }

    @Override
    public String getPermission() {
        return this.permission;
    }

    /**
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
     * Returns the CompositeCommand object refering to this command label
     * @param label - command label or alias
     * @return CompositeCommand or null if none found
     */
    public CompositeCommand getSubCommand(String label) {
        for (Map.Entry<String, CompositeCommand> entry : subCommands.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(label)) return subCommands.get(label);
            else if (entry.getValue().getAliases().contains(label)) return subCommands.get(entry.getValue().getLabel());
        }
        return null;
    }
    /**
     * Recursively obtain a list of all sub command help references
     * TODO: use this in conjunction with a user's request for help
     * @return a list of this command and all sub commands help references
     */
    public List<String> getSubCommandHelp() {
        return getSubCommandHelp("");
    }

    private List<String> getSubCommandHelp(String helpRef) {
        CompositeCommand subCommand = this;
        List<String> result = new ArrayList<>();
        result.add(helpRef + " " + getDescription());
        while (subCommand.hasSubCommmands()) {
            result.addAll(subCommand.getSubCommandList(getDescription()));
        }
        return result;
    }
    /**
     * Recursively obtain a list of all sub commands
     * @return a list of this command and all sub commands
     */
    public List<String> getSubCommandList() {
        return getSubCommandList("");
    }

    private List<String> getSubCommandList(String label) {
        CompositeCommand subCommand = this;
        List<String> result = new ArrayList<>();
        result.add(label + " " + getLabel());
        while (subCommand.hasSubCommmands()) {
            result.addAll(subCommand.getSubCommandList(getLabel()));
        }
        return result;
    }


    public Map<String, CompositeCommand> getSubCommands() {
        return subCommands;
    }


    /**
     * @param user
     * @return UUID of player's team leader
     */
    protected UUID getTeamLeader(User user) {
        return plugin.getIslands().getTeamLeader(user.getUniqueId());
    }


    /**
     * @param subCommand
     * @return true if this command has this sub command
     */
    private boolean hasSubCommand(String subCommand) {
        return subCommands.containsKey(subCommand);
    }


    /**
     * @return true if this command has subcommands
     */
    protected boolean hasSubCommmands() {
        return !subCommands.isEmpty();
    }

    /**
     * @param player
     * @return true if player is in a team
     */
    protected boolean inTeam(Player player) {
        return plugin.getPlayers().inTeam(player.getUniqueId());
    }

    public boolean isOnlyPlayer() {
        return onlyPlayer;
    }

    /**
     * @param user
     * @return true if sender is a player
     */
    protected boolean isPlayer(User user) {
        return (user.getPlayer() instanceof Player);
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String alias, final String[] args) {
        List<String> options = new ArrayList<>();
        // Get command object based on args entered so far
        CompositeCommand cmd = getCommandFromArgs(args);
        if (DEBUG) {
            Bukkit.getLogger().info("DEBUG: subCommand = " + cmd.getLabel() + " onlyplayer = " + cmd.isOnlyPlayer() + " permission = " + cmd.getPermission());
        }
        // Check for console and permissions
        if (cmd.onlyPlayer && !(sender instanceof Player)) {
            if (DEBUG)
                Bukkit.getLogger().info("DEBUG: returning, only for player");
            return options;
        }
        if (!cmd.getPermission().isEmpty() && !sender.hasPermission(cmd.getPermission())) {
            if (DEBUG)
                Bukkit.getLogger().info("DEBUG: failed perm check");
            return options;
        }
        // Add any tab completion from the subcommand
        options.addAll(cmd.tabComplete(User.getInstance(sender), alias, new LinkedList<String>(Arrays.asList(args))).orElse(new ArrayList<>()));
        // Add any sub-commands automatically
        if (cmd.hasSubCommmands()) {
            // Check if subcommands are visible to this sender
            for (CompositeCommand subCommand: cmd.getSubCommands().values()) {
                if ((sender instanceof Player)) {
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
        String lastArg = (args.length != 0 ? args[args.length - 1] : "");

        if (DEBUG) {
            String arguments = "";
            for (String arg : args) {
                arguments += "'" + arg + "' ";
            }
            Bukkit.getLogger().info("DEBUG: tab complete for " + arguments);
            Bukkit.getLogger().info("DEBUG: result = " + Util.tabLimit(options, lastArg));
        }
        return Util.tabLimit(options, lastArg);
    }

    /**
     * Get the current composite command based on the arguments
     * @param args
     * @return the current composite command based on the arguments
     */
    private CompositeCommand getCommandFromArgs(String[] args) {
        CompositeCommand subCommand = this;
        // Run through any arguments
        if (DEBUG)
            Bukkit.getLogger().info("DEBUG: Running through args: " + args.toString());
        if (args.length > 0) {
            for (int i = 0; i <= args.length; i++) {
                if (DEBUG)
                    Bukkit.getLogger().info("DEBUG: Argument " + i);
                // get the subcommand corresponding to the arg
                if (subCommand.hasSubCommmands()) {
                    if (DEBUG)
                        Bukkit.getLogger().info("DEBUG: This command has subcommands");
                    if (subCommand.hasSubCommand(args[i])) {
                        // Step down one
                        subCommand = subCommand.getSubCommand(args[i]);
                        if (DEBUG)
                            Bukkit.getLogger().info("DEBUG: Moved to " + subCommand.getLabel());
                    } else {
                        return subCommand;
                    }
                } else {
                    // We are at the end of the walk
                    if (DEBUG)
                        Bukkit.getLogger().info("DEBUG: End of traversal");
                    return subCommand;
                }
                // else continue the loop
            }
        }
        return subCommand;
    }


    public void setOnlyPlayer(boolean onlyPlayer) {
        this.onlyPlayer = onlyPlayer;
    }

    @Override
    public void setPermission(String permission) {
        this.permission = permission;
    }
    
}
