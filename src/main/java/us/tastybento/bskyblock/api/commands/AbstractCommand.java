package us.tastybento.bskyblock.api.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.events.command.CommandEvent;
import us.tastybento.bskyblock.config.BSBLocale;
import us.tastybento.bskyblock.database.managers.PlayersManager;
import us.tastybento.bskyblock.database.managers.island.IslandsManager;
import us.tastybento.bskyblock.util.Util;

/**
 *
 * @author Poslovitch
 */
public abstract class AbstractCommand implements CommandExecutor, TabCompleter {

    private BSkyBlock plugin;

    public final Map<String, ArgumentHandler> argumentsMap;
    public final Set<ArgumentHandler> handlers;
    public final String label;
    public final String[] aliases;
    public boolean isPlayer;
    public boolean inTeam;
    public UUID teamLeaderUUID;
    public Set<UUID> teamMembers;
    public Player player;
    public UUID playerUUID;

    private final boolean help;
    //private static final int MAX_PER_PAGE = 7;

    private static final boolean DEBUG = false;

    protected AbstractCommand(BSkyBlock plugin, String label, String[] aliases, boolean help) {
        this.plugin = plugin;
        this.argumentsMap = new LinkedHashMap<>();
        this.handlers = new HashSet<>();
        this.label = label;
        this.aliases = aliases;
        this.help = help;
        this.teamMembers = new HashSet<>();

        // Register the help argument if needed
        if (help) {
            addArgument(new ArgumentHandler(label) {
                @Override
                public CanUseResp canUse(CommandSender sender) {
                    return new CanUseResp(true); // If the player has access to this command, he can get help
                }

                @Override
                public void execute(CommandSender sender, String[] args) {
                    Util.sendMessage(sender, plugin.getLocale(sender).get("help.header"));
                    for(ArgumentHandler handler: handlers) {
                        if (handler.canUse(sender).isAllowed()) Util.sendMessage(sender, handler.getShortDescription(sender));
                    }
                    Util.sendMessage(sender, plugin.getLocale(sender).get("help.end"));
                }

                @Override
                public Set<String> tabComplete(CommandSender sender, String[] args) {
                    return null; // No tab options for this one
                }

                @Override
                public String[] usage(CommandSender sender) {
                    return new String[] {"", plugin.getLocale(sender).get("help.this")};
                }
            }.alias("help").alias("?"));
        }

        // Register the other arguments
        setup();
    }



    public abstract void setup();

    public abstract CanUseResp canUse(CommandSender sender);
    public abstract void execute(CommandSender sender, String[] args);

    public void addArgument(ArgumentHandler handler) {
        for (String argument : handler.getAliases()) {
            argumentsMap.put(argument, handler);
        }
        handlers.add(handler);
    }

    public ArgumentHandler getHandler(String argument) {
        return argumentsMap.get(argument);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        checkForPlayer(sender);

        // Fire command event
        CommandEvent event = CommandEvent.builder().setSender(sender).setCommand(command).setLabel(label).setArgs(args).build();
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return true;

        CanUseResp canUse = this.canUse(sender);
        if (canUse.isAllowed()) {
            if(args.length >= 1) {
                ArgumentHandler handler = getHandler(args[0]); // Store the handler to save some calculations
                if (handler != null && handler.canUse(sender).isAllowed()) {
                    handler.execute(sender, clean(Arrays.copyOfRange(args, 1, args.length)));
                } else if (handler != null && !handler.canUse(sender).isAllowed() && !handler.canUse(sender).getErrorResponse().isEmpty()) {
                    Util.sendMessage(sender, handler.canUse(sender).errorResponse);
                } else if (help) {
                    if (argumentsMap.containsKey("help")) {
                        argumentsMap.get("help").execute(sender, clean(Arrays.copyOfRange(args, 1, args.length)));
                    }
                } else {
                    // Unknown handler
                    this.execute(sender, args);
                }
            } else {
                // No args
                this.execute(sender, args);
            }
        } else {
            // Sender cannot use this command - tell them why
            Util.sendMessage(sender, canUse.errorResponse);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args){
        List<String> options = new ArrayList<>();
        checkForPlayer(sender);
        String lastArg = (args.length != 0 ? args[args.length - 1] : "");
        if (canUse(sender).isAllowed()) {
            if (args.length <= 1) {
                // Go through every argument, check if player can use it and if so, add it in tab options
                for(ArgumentHandler handler: handlers) {
                    if (handler.canUse(sender).isAllowed()) options.addAll(handler.aliasSet);
                }
            } else {
                // If player can execute the argument, get its tab-completer options
                ArgumentHandler handler = getHandler(args[0]);
                if (handler != null && handler.canUse(sender).isAllowed()) {
                    // We remove the 1st arg - and remove any blank args caused by hitting space before the tab
                    Set<String> tabOptions = handler.tabComplete(sender, clean(Arrays.copyOfRange(args, 1, args.length)));
                    if (tabOptions != null) options.addAll(tabOptions);
                }
            }
        }
        return Util.tabLimit(options, lastArg);
    }

    private static String[] clean(final String[] v) {
        List<String> list = new ArrayList<>(Arrays.asList(v));
        list.removeAll(Collections.singleton(""));
        return list.toArray(new String[list.size()]);
    }

    /**
     * Sets some variables and flags if this is a player
     * @param sender
     */
    private void checkForPlayer(CommandSender sender) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: checkForPlayer");
        // Check if the command sender is a player or not
        if (sender instanceof Player) {
            isPlayer = true;
            player = (Player)sender;
            playerUUID = player.getUniqueId();
        } else {
            isPlayer = false;
        }
        // Check if the player is in a team or not and if so, grab the team leader's UUID
        if (plugin.getPlayers().inTeam(playerUUID)) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: player in team");
            inTeam = true;
            teamLeaderUUID = plugin.getIslands().getTeamLeader(playerUUID);
            if (DEBUG)
                plugin.getLogger().info("DEBUG: team leader UUID = " + teamLeaderUUID);
            teamMembers = plugin.getIslands().getMembers(teamLeaderUUID);
            if (DEBUG) {
                plugin.getLogger().info("DEBUG: teammembers = ");
                for (UUID member: teamMembers) {
                    plugin.getLogger().info("DEBUG: " + member);
                }
            }
        } else {
            inTeam = false;
        }

    }

    // These methods below just neaten up the code in the commands so "plugin." isn't always used
    /**
     * @return PlayersManager
     */
    protected PlayersManager getPlayers() {
        return plugin.getPlayers();
    }
    /**
     * @return IslandsManager
     */
    protected IslandsManager getIslands() {
        return plugin.getIslands();
    }
    /**
     * @param sender
     * @return Locale for sender
     */
    protected BSBLocale getLocale(CommandSender sender) {
        return plugin.getLocale(sender);
    }
    /**
     * @param uuid
     * @return Locale for UUID
     */
    protected BSBLocale getLocale(UUID uuid) {
        return plugin.getLocale(uuid);
    }



    public Map<String, ArgumentHandler> getArgumentsMap() {
        return argumentsMap;
    }

}
