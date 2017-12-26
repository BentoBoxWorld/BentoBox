package us.tastybento.bskyblock.api.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.events.command.CommandEvent;
import us.tastybento.bskyblock.database.managers.PlayersManager;
import us.tastybento.bskyblock.database.managers.island.IslandsManager;
import us.tastybento.bskyblock.util.Util;

/**
 * BSB composite command
 * @author ben, poslovich
 *
 */
public abstract class CompositeCommand extends Command implements PluginIdentifiableCommand, BSBCommand {

    private static final boolean DEBUG = false;
    /**
     * This is the command level. 0 is the top, 1 is the first level sub command.
     */
    private final int subCommandLevel;
    /**
     * True if the command is for the player only (not for the console)
     */
    private boolean onlyPlayer = false;
    /**
     * The parent command to this one. If this is a top-level command it will be empty.
     */
    private final Optional<CompositeCommand> parent;
    /**
     * The permission required to execute this command
     */
    private String permission = "";
    /**
     * Map of sub commands
     */
    private Map<String, CompositeCommand> subCommands;
    /**
     * The usage string for this command. It is the commands followed by a locale reference.
     */
    private String usage;
    private BSkyBlock bsb;

    /**
     * Sub-command constructor
     * @param parent - the parent composite command
     * @param label - string label for this subcommand
     * @param aliases - aliases for this subcommand
     */
    public CompositeCommand(CompositeCommand parent, String label, String... aliases) {
        super(label);
        this.parent = Optional.of(parent);
        this.subCommandLevel = parent.getLevel() + 1;
        // Add this sub-command to the parent
        parent.getSubCommands().put(label, this);
        this.setAliases(new ArrayList<>(Arrays.asList(aliases)));
        this.subCommands = new LinkedHashMap<>();
        setUsage("");
        setDescription("");
        this.setup();
        // If this command does not define its own help class, then use the default help command
        if (!this.getSubCommand("help").isPresent() && !label.equals("help"))
            new DefaultHelpCommand(this);

        if (DEBUG)
            Bukkit.getLogger().info("DEBUG: registering command " + label);
    }


    /**
     * This is the top-level command constructor for commands that have no parent.
     * @param label - string for this command
     * @param string - aliases for this command
     */
    public CompositeCommand(String label, String... string) {
        super(label);
        this.setAliases(new ArrayList<>(Arrays.asList(string)));
        this.parent = Optional.empty();
        setUsage("");
        this.subCommandLevel = 0; // Top level
        this.subCommands = new LinkedHashMap<>();
        if (!label.equals("help"))
            new DefaultHelpCommand(this);
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
     * @param args
     * @return the current composite command based on the arguments
     */
    private CompositeCommand getCommandFromArgs(String[] args) {
        CompositeCommand subCommand = this;
        // Run through any arguments
        if (DEBUG)
            Bukkit.getLogger().info("DEBUG: Running through args: " + args.toString());
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (DEBUG)
                    Bukkit.getLogger().info("DEBUG: Argument " + i);
                // get the subcommand corresponding to the arg
                if (subCommand.hasSubCommmands()) {
                    if (DEBUG)
                        Bukkit.getLogger().info("DEBUG: This command has subcommands");
                    if (subCommand.hasSubCommand(args[i])) {
                        // Step down one
                        subCommand = subCommand.getSubCommand(args[i]).get();
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

    /**
     * Convenience method to get the island manager
     * @return IslandsManager
     */
    protected IslandsManager getIslands() {
        return bsb.getIslands();
    }

    /**
     * @return this command's sub-level. Top level is 0.
     * Every time a command registers with a parent, their level will be set.
     */
    protected int getLevel() {
        return subCommandLevel;
    }

    /**
     * Convenience method to obtain team members
     * @param user
     * @return set of UUIDs of all team members
     */
    protected Set<UUID> getMembers(User user) {
        return bsb.getIslands().getMembers(user.getUniqueId());
    }

    /**
     * @return the parent command object
     */
    public Optional<CompositeCommand> getParent() {
        return parent;
    }

    @Override
    public String getPermission() {
        return this.permission;
    }

    /**
     * Convenience method to get the player manager
     * @return PlayersManager
     */
    protected PlayersManager getPlayers() {
        return bsb.getPlayers();
    }

    @Override
    public BSkyBlock getPlugin() {
        this.bsb = BSkyBlock.getPlugin();
        return this.bsb;
    }
    
    /**
     * Returns the CompositeCommand object refering to this command label
     * @param label - command label or alias
     * @return CompositeCommand or null if none found
     */
    public Optional<CompositeCommand> getSubCommand(String label) {
        for (Map.Entry<String, CompositeCommand> entry : subCommands.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(label)) return Optional.of(subCommands.get(label));
            else if (entry.getValue().getAliases().contains(label)) return Optional.of(subCommands.get(entry.getValue().getLabel()));
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
     * @param user
     * @return UUID of player's team leader
     */
    protected UUID getTeamLeader(User user) {
        return bsb.getIslands().getTeamLeader(user.getUniqueId());
    }

    @Override
    public String getUsage() {
        return usage;
    }
    
    @Override
    public Command setUsage(String usage) {
        // Go up the chain
        Optional<CompositeCommand> parent = this.getParent();
        this.usage = this.getLabel() + " " + usage;
        while (parent.isPresent()) {
            this.usage = parent.get().getLabel() + " " + this.usage;
            parent = parent.get().getParent();
        }
        this.usage = "/" + this.usage;
        this.usage = this.usage.trim();
        return this;
    }
    
    /**
     * Get usage for sub commands
     * @param subCommands
     * @return
     */
    public String getUsage(String... subCommands) {
        CompositeCommand subCommand = this.getCommandFromArgs(subCommands);
        return subCommand.getUsage();
    }

    /**
     * Check if this command has a specific sub command
     * @param subCommand
     * @return true if this command has this sub command
     */
    private boolean hasSubCommand(String subCommand) {
        return subCommands.containsKey(subCommand);
    }

    /**
     * Check if this command has any sub commands
     * @return true if this command has subcommands
     */
    protected boolean hasSubCommmands() {
        return !subCommands.isEmpty();
    }

    /**
     * Convenience method to check if a user has a team
     * @param user
     * @return true if player is in a team
     */
    protected boolean inTeam(User user) {
        return bsb.getPlayers().inTeam(user.getUniqueId());
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
     * @param user
     * @return true if sender is a player
     */
    protected boolean isPlayer(User user) {
        return (user.getPlayer() instanceof Player);
    }


    /**
     * Set whether this command is only for players
     * @param onlyPlayer
     */
    public void setOnlyPlayer(boolean onlyPlayer) {
        this.onlyPlayer = onlyPlayer;
    }

    @Override
    public void setPermission(String permission) {
        this.permission = permission;
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
}
