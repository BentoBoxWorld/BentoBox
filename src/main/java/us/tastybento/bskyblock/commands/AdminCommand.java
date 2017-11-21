package us.tastybento.bskyblock.commands;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.AbstractCommand;
import us.tastybento.bskyblock.api.commands.ArgumentHandler;
import us.tastybento.bskyblock.api.commands.CanUseResp;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

public class AdminCommand extends AbstractCommand {

    protected static final boolean DEBUG = false;
    BSkyBlock plugin;

    /**
     * Handles admin commands
     * @param plugin
     */
    public AdminCommand(BSkyBlock plugin) {
        super(plugin, Settings.ADMINCOMMAND, new String[0], true);
        plugin.getCommand(Settings.ADMINCOMMAND).setExecutor(this);
        plugin.getCommand(Settings.ADMINCOMMAND).setTabCompleter(this);
        this.plugin = plugin;
    }

    @Override
    public void setup() {
        /* /asadmin delete <name> - delete name's island */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(!(sender instanceof Player)
                        || VaultHelper.hasPerm(player, Settings.PERMPREFIX + "admin.delete"));
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                if (args.length > 0) {
                    // Convert name to a UUID
                    UUID targetPlayer = getPlayers().getUUID(args[0]);
                    if (targetPlayer == null) {
                        Util.sendMessage(sender, ChatColor.RED + getLocale(sender).get("general.errors.unknown-player"));
                        return;
                    }

                    Util.sendMessage(sender, ChatColor.YELLOW + getLocale(sender).get("delete.removing").replace("[name]", args[0]));
                    getIslands().deleteIsland(targetPlayer, true);
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                Set<String> result = new HashSet<>();
                if (args.length == 1) {
                    result.addAll(Util.getOnlinePlayerList(player));
                }
                return result;
            }

            @Override
            public String[] usage(CommandSender sender){
                return new String[] {null, getLocale(sender).get("help.admin.delete")};
            }
        }.alias("delete"));


        /* /asadmin unregister <name> - unregister name's island */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(!(sender instanceof Player)
                        || VaultHelper.hasPerm(player, Settings.PERMPREFIX + "admin.unregister"));
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                if (args.length > 0) {
                    // Convert name to a UUID
                    UUID targetPlayer = getPlayers().getUUID(args[0]);
                    if (targetPlayer == null) {
                        Util.sendMessage(sender, ChatColor.RED + getLocale(sender).get("general.errors.unknown-player"));
                        return;
                    }
                    Island island = getIslands().getIsland(targetPlayer);
                    if (island != null) {
                        Util.sendMessage(sender, ChatColor.YELLOW + getLocale(sender).get("adminUnregister.keepBlocks").replace("[location]", island.getCenter().toString()));
                        getIslands().deleteIsland(targetPlayer, false);
                    } else {
                        // Error
                    }
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                Set<String> result = new HashSet<>();
                if (args.length == 1) {
                    result.addAll(Util.getOnlinePlayerList(player));
                }
                return result;
            }

            @Override
            public String[] usage(CommandSender sender){
                return new String[] {null, getLocale(sender).get("adminHelp.unregister")};
            }
        }.alias("unregister"));

        /* /asadmin info - show info on island */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(!(sender instanceof Player)
                        || VaultHelper.hasPerm(player, Settings.PERMPREFIX + "admin.info"));
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                if (args.length > 0) {
                    // Convert name to a UUID
                    UUID targetPlayer = getPlayers().getUUID(args[0]);
                    if (targetPlayer == null) {
                        Util.sendMessage(sender, ChatColor.RED + getLocale(sender).get("general.errors.unknown-player"));
                        return;
                    }
                    showInfo(targetPlayer, sender);
                } else {
                    if (!(sender instanceof Player)) {
                        Util.sendMessage(sender, ChatColor.RED + getLocale(sender).get("general.errors.use-in-game"));
                        return;
                    }
                    Island island = getIslands().getIslandAt(((Player)sender).getLocation());
                    if (island == null) {
                        Util.sendMessage(sender, ChatColor.RED + "Sorry, could not find an island. Move closer?");
                        return;
                    }
                    if (island.isSpawn()) {
                        Util.sendMessage(sender, ChatColor.GREEN + getLocale(sender).get("adminInfo.title"));
                        Util.sendMessage(sender, ChatColor.YELLOW + getLocale(sender).get("adminSetSpawncenter").replace("[location]", island.getCenter().getBlockX() + "," + island.getCenter().getBlockZ()));
                        Util.sendMessage(sender, ChatColor.YELLOW + (getLocale(sender).get("adminSetSpawnlimits").replace("[min]", island.getMinX() + "," + island.getMinZ())).replace("[max]",
                                (island.getMinX() + island.getRange() - 1) + "," + (island.getMinZ() + island.getRange() - 1)));
                        Util.sendMessage(sender, ChatColor.YELLOW + getLocale(sender).get("adminSetSpawnrange").replace("[number]",String.valueOf(island.getProtectionRange())));
                        Util.sendMessage(sender, ChatColor.YELLOW + (getLocale(sender).get("adminSetSpawncoords").replace("[min]",  island.getMinProtectedX() + ", " + island.getMinProtectedZ())).replace("[max]",
                                + (island.getMinProtectedX() + island.getProtectionRange() - 1) + ", "
                                        + (island.getMinProtectedZ() + island.getProtectionRange() - 1)));
                        if (island.isLocked()) {
                            Util.sendMessage(sender, ChatColor.RED + getLocale(sender).get("adminSetSpawnlocked"));
                        }
                        return;
                    }
                    showInfo(island.getOwner(), sender);
                    return;
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                Set<String> result = new HashSet<>();
                if (args.length == 1) {
                    result.addAll(Util.getOnlinePlayerList(player));
                }
                return result;
            }

            @Override
            public String[] usage(CommandSender sender){
                return new String[] {null, getLocale(sender).get("adminHelp.infoisland")};
            }
        }.alias("info"));

        /* /asadmin team - manage teams */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(!(sender instanceof Player)
                        || VaultHelper.hasPerm(player, Settings.PERMPREFIX + "admin.team"));
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                /*
                 * Commands are:
                 * team info <player> - lists info on the player's team
                 * team add <player1> <player2> - adds player1 to player2's team. If player1 has an island, the island will become unowned.
                 * team kick <player> - removes player from the team.
                 * team makeleader <player> - makes the player the team leader. The old leader will become a team member.
                 * team delete <player> - kicks all the players from the team player is in. The leader remains as the island owner.
                 * 
                 * Note that you do not have to specify the team leader.
                 * 
                 * 
                 */
                if (args.length == 2) {
                    // see if arg 1 is a player
                    UUID targetPlayer = getPlayers().getUUID(args[1]);
                    if (targetPlayer == null) {
                        Util.sendMessage(sender, ChatColor.RED + getLocale(playerUUID).get("general.errors.unknown-player"));
                        return;
                    }
                    // Check if player is in a team
                    if (!getPlayers().inTeam(targetPlayer)) {
                        Util.sendMessage(sender, ChatColor.RED + getLocale(playerUUID).get("general.errors.no-team"));
                        return;
                    }
                    switch (args[0].toLowerCase()) {
                    case "info":
                        // Fire event so add-ons can run commands, etc.
                        TeamEvent event = TeamEvent.builder().island(getIslands().getIsland(targetPlayer))
                        .admin(true)
                        .reason(TeamReason.INFO)
                        .involvedPlayer(targetPlayer).build();
                        plugin.getServer().getPluginManager().callEvent(event);
                        if (event.isCancelled()) return;
                        // Display info
                        Util.sendMessage(sender, getLocale(sender).get("team.listingMembers"));
                        // Display members in the list
                        for (UUID m : getIslands().getMembers(targetPlayer)) {
                            if (DEBUG)
                                plugin.getLogger().info("DEBUG: member " + m);
                            if (getIslands().getTeamLeader(targetPlayer).equals(m)) {
                                Util.sendMessage(sender, getLocale(sender).get("team.leader-color") + getPlayers().getName(m) + getLocale(sender).get("team.leader"));
                            } else {
                                Util.sendMessage(sender, getLocale(sender).get("team.color") + getPlayers().getName(m));
                            }
                        }
                        return;
                    case "makeleader":
                        // Check if already leader
                        if (getIslands().getTeamLeader(targetPlayer).equals(targetPlayer)) {
                            Util.sendMessage(sender, ChatColor.RED + "'" + args[1] + "' - " + getLocale(playerUUID).get("admin.team.error.already-a-leader"));
                            return;
                        }
                        // Fire event so add-ons can run commands, etc.
                        TeamEvent event2 = TeamEvent.builder().island(getIslands().getIsland(targetPlayer))
                                .admin(true)
                                .reason(TeamReason.MAKELEADER)
                                .involvedPlayer(targetPlayer).build();
                        plugin.getServer().getPluginManager().callEvent(event2);
                        if (event2.isCancelled()) return;
                        // Display info
                        getIslands().getIsland(targetPlayer).setOwner(targetPlayer);
                        Util.sendMessage(sender, ChatColor.GREEN
                                + getLocale(sender).get("makeleader.nameIsNowTheOwner").replace("[name]", getPlayers().getName(targetPlayer)));

                        // Check if online
                        Player target = plugin.getServer().getPlayer(targetPlayer);
                        if (target == null) {
                            // TODO offline messaging
                            //plugin.getMessages().setMessage(targetPlayer, getLocale(playerUUID).get("makeleader.youAreNowTheOwner"));

                        } else {
                            // Online
                            Util.sendMessage(plugin.getServer().getPlayer(targetPlayer), ChatColor.GREEN + getLocale(targetPlayer).get("makeleader.youAreNowTheOwner"));
                            // Check if new leader has a lower range permission than the island size
                            boolean hasARangePerm = false;
                            int range = Settings.islandProtectionRange;
                            // Check for zero protection range
                            Island islandByOwner = getIslands().getIsland(targetPlayer);
                            if (islandByOwner.getProtectionRange() == 0) {
                                plugin.getLogger().warning("Player " + player.getName() + "'s island had a protection range of 0. Setting to default " + range);
                                islandByOwner.setProtectionRange(range);
                            }
                            for (PermissionAttachmentInfo perms : target.getEffectivePermissions()) {
                                if (perms.getPermission().startsWith(Settings.PERMPREFIX + "island.range.")) {
                                    if (perms.getPermission().contains(Settings.PERMPREFIX + "island.range.*")) {
                                        // Ignore
                                        break;
                                    } else {
                                        String[] spl = perms.getPermission().split(Settings.PERMPREFIX + "island.range.");
                                        if (spl.length > 1) {
                                            if (!NumberUtils.isDigits(spl[1])) {
                                                plugin.getLogger().severe("Player " + player.getName() + " has permission: " + perms.getPermission() + " <-- the last part MUST be a number! Ignoring...");

                                            } else {
                                                hasARangePerm = true;
                                                range = Math.max(range, Integer.valueOf(spl[1]));
                                            }
                                        }
                                    }
                                }
                            }
                            // Only set the island range if the player has a perm to override the default
                            if (hasARangePerm) {
                                // Do some sanity checking
                                if (range % 2 != 0) {
                                    range--;
                                }
                                // Get island range

                                // Range can go up or down
                                if (range != islandByOwner.getProtectionRange()) {
                                    Util.sendMessage(sender, getLocale(targetPlayer).get("admin.SetRangeUpdated").replace("[number]", String.valueOf(range)));
                                    Util.sendMessage(target, getLocale(targetPlayer).get("admin.SetRangeUpdated").replace("[number]", String.valueOf(range)));
                                    plugin.getLogger().info(
                                            "Makeleader: Island protection range changed from " + islandByOwner.getProtectionRange() + " to "
                                                    + range + " for " + player.getName() + " due to permission.");
                                }
                                islandByOwner.setProtectionRange(range);
                            }
                        }
                        getIslands().save(true);
                        return;
                    case "kick":
                        if (getIslands().getTeamLeader(targetPlayer).equals(targetPlayer)) {
                            Util.sendMessage(sender, ChatColor.RED + "'" + args[1] + "' - " + getLocale(playerUUID).get("admin.team.error.player-is-a-team-leader"));
                            return;
                        }
                        // Fire event so add-ons can run commands, etc.
                        TeamEvent event3 = TeamEvent.builder().island(getIslands().getIsland(targetPlayer))
                                .admin(true)
                                .reason(TeamReason.KICK)
                                .involvedPlayer(targetPlayer).build();
                        plugin.getServer().getPluginManager().callEvent(event3);
                        if (event3.isCancelled()) return;
                        // Display info
                        // Remove from team
                        getIslands().setLeaveTeam(targetPlayer);
                        // Tell the player they kicked okay
                        Util.sendMessage(sender, ChatColor.GREEN + getLocale(sender).get("kick.nameRemoved").replace("[name]", getPlayers().getName(targetPlayer)));
                        // Tell the target if they are online
                        if (plugin.getServer().getPlayer(targetPlayer) != null) {
                            Player t = plugin.getServer().getPlayer(targetPlayer);
                            Util.sendMessage(t, ChatColor.RED + getLocale(targetPlayer).get("kick.nameRemovedYou").replace("[name]", player.getName()));
                        } else {
                            // TODO: Leave them an offline message
                        }
                        getIslands().save(true);
                        return;
                    case "delete":
                        // Fire event so add-ons can run commands, etc.
                        TeamEvent event4 = TeamEvent.builder().island(getIslands().getIsland(targetPlayer))
                        .admin(true)
                        .reason(TeamReason.DELETE)
                        .involvedPlayer(targetPlayer).build();
                        plugin.getServer().getPluginManager().callEvent(event4);
                        if (event4.isCancelled()) return;
                        UUID teamLeader = getIslands().getTeamLeader(targetPlayer);
                        for (UUID m : getIslands().getMembers(targetPlayer)) {
                            if (DEBUG)
                                plugin.getLogger().info("DEBUG: member " + m);
                            // Only the team leader gets to stay
                            if (!teamLeader.equals(m)) {
                                getIslands().setLeaveTeam(targetPlayer);
                            } else {
                                // Tell the player they kicked okay
                                Util.sendMessage(sender, ChatColor.GREEN + getLocale(sender).get("kick.nameRemoved").replace("[name]", getPlayers().getName(targetPlayer)));
                                // Tell the target if they are online
                                if (plugin.getServer().getPlayer(targetPlayer) != null) {
                                    Player t = plugin.getServer().getPlayer(targetPlayer);
                                    Util.sendMessage(t, ChatColor.RED + getLocale(targetPlayer).get("kick.nameRemovedYou").replace("[name]", player.getName()));
                                } else {
                                    // TODO: Leave them an offline message
                                }
                            }
                        }
                        Util.sendMessage(sender, ChatColor.GREEN + getLocale(sender).get("general.success"));
                        getIslands().save(true);
                        return;
                    }
                }
                else if (args.length == 3) {
                    if (args[0].equalsIgnoreCase("add")) {
                        UUID targetPlayer = getPlayers().getUUID(args[1]);
                        if (targetPlayer == null) {
                            Util.sendMessage(sender, ChatColor.RED + "'" + args[1] + "' - " + getLocale(playerUUID).get("general.errors.unknown-player"));
                            return;
                        }
                        UUID targetPlayer2 = getPlayers().getUUID(args[2]);
                        if (targetPlayer2 == null) {
                            Util.sendMessage(sender, ChatColor.RED + "'" + args[2] + "' - " + getLocale(playerUUID).get("general.errors.unknown-player"));
                            return;
                        }
                        // Check if player2 has an island, if not, error
                        if (!getIslands().hasIsland(targetPlayer2)) {
                            Util.sendMessage(sender, ChatColor.RED + "'" + args[2] + "' - " + getLocale(playerUUID).get("admin.error.no-island"));
                            return; 
                        }
                        // If player1 and 2 are already on the same team, error
                        if (getIslands().getMembers(targetPlayer2).contains(targetPlayer)) {
                            Util.sendMessage(sender, ChatColor.RED + getLocale(playerUUID).get("admin.team.error.alreadyInTeam"));
                            return;
                        }
                        // If player1 is in a team and the leader, error
                        if (getPlayers().inTeam(targetPlayer) && getIslands().getTeamLeader(targetPlayer).equals(targetPlayer)) {
                            Util.sendMessage(sender, ChatColor.RED + "'" + args[1] + "' - " + getLocale(playerUUID).get("admin.team.error.player-is-a-team-leader"));
                            return;
                        }
                        // Get the team's island
                        Island teamIsland = getIslands().getIsland(targetPlayer2);
                        // Fire event so add-ons can run commands, etc.
                        TeamEvent event = TeamEvent.builder().island(teamIsland)
                                .admin(true)
                                .reason(TeamReason.JOIN)
                                .involvedPlayer(targetPlayer).build();
                        plugin.getServer().getPluginManager().callEvent(event);
                        if (event.isCancelled()) return;
                        // Move player to team's island
                        Location newHome = getIslands().getSafeHomeLocation(targetPlayer2, 1);
                        if (newHome != null) {
                            // A safe location could not be found...
                            getPlayers().setHomeLocation(targetPlayer, teamIsland.getCenter());
                            Util.sendMessage(sender, ChatColor.RED + getLocale(sender).get("general.errors.no-safe-location"));
                        } else {
                            // Set the player's home
                            getPlayers().setHomeLocation(targetPlayer, newHome);
                        }
                        // Check if player1 is online or not
                        Player player1 = plugin.getServer().getPlayer(targetPlayer);
                        if (player1 != null) {
                            // Put player into Spectator mode
                            player1.setGameMode(GameMode.SPECTATOR);
                            if (newHome != null)
                                player1.teleport(newHome);
                            // Put player back into normal mode
                            player1.setGameMode(GameMode.SURVIVAL);
                            Util.sendMessage(player1, ChatColor.GREEN + getLocale(sender).get("invite.youHaveJoinedAnIsland").replace("[label]", Settings.ISLANDCOMMAND));
                        }
                        // Remove player from any islands
                        getIslands().removePlayer(targetPlayer);
                        // Add the player as a team member of the new island
                        getIslands().setJoinTeam(teamIsland, targetPlayer);
                        // Reset deaths
                        if (Settings.teamJoinDeathReset) {
                            getPlayers().setDeaths(targetPlayer, 0);
                        }
                        // Tell island members
                        for (UUID member: teamIsland.getMembers()) {
                            // If online tell them
                            if (!member.equals(targetPlayer) && plugin.getServer().getPlayer(member) != null) {
                                Util.sendMessage(plugin.getServer().getPlayer(member),
                                        ChatColor.GREEN + getLocale(sender).get("invite.hasJoinedYourIsland").replace("[name]", player.getName()));
                            }
                        }
                        getIslands().save(false);
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: After save " + getIslands().getIsland(targetPlayer2).getMembers().toString());
                        Util.sendMessage(sender, ChatColor.GREEN + getLocale(sender).get("general.success"));
                        return;  
                    }
                }
                // Wrong command syntax or arguments, show help
                Util.sendMessage(sender, getLocale(sender).get("help.admin.team.command"));
                Util.sendMessage(sender, getLocale(sender).get("help.admin.team.info"));
                Util.sendMessage(sender, getLocale(sender).get("help.admin.team.makeleader"));
                Util.sendMessage(sender, getLocale(sender).get("help.admin.team.add"));
                Util.sendMessage(sender, getLocale(sender).get("help.admin.team.kick"));
                Util.sendMessage(sender, getLocale(sender).get("help.admin.team.delete"));
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                Set<String> result = new HashSet<>();
                if (args.length == 1) {
                    result.add("info");
                    result.add("makeleader");
                    result.add("kick");
                    result.add("delete");
                    result.add("add");
                } else if (args.length == 1) {
                    result.addAll(Util.getOnlinePlayerList(player));
                }
                return result;
            }

            @Override
            public String[] usage(CommandSender sender){
                return new String[] {null, getLocale(sender).get("help.admin.team.command")};
            }
        }.alias("team"));

    }

    /**
     * Shows info on a player
     *
     * @param playerUUID
     * @param sender
     */
    private void showInfo(UUID playerUUID, CommandSender sender) {
        // Show info on player
        Util.sendMessage(sender, getLocale(sender).get("adminInfo.player") + ": " + ChatColor.GREEN + getPlayers().getName(playerUUID));
        Util.sendMessage(sender, ChatColor.WHITE + "UUID: " + playerUUID.toString());
        // Last login
        try {
            Date d = new Date(plugin.getServer().getOfflinePlayer(playerUUID).getLastPlayed());
            Util.sendMessage(sender, ChatColor.GOLD + getLocale(sender).get("adminInfo.lastLogin") + ": " + d.toString());
        } catch (Exception e) {
        }
        Util.sendMessage(sender, ChatColor.GREEN + getLocale(sender).get("general.deaths") + ": " + getPlayers().getDeaths(playerUUID));
        String resetsLeft = getLocale(sender).get("general.unlimited");
        if (getPlayers().getResetsLeft(playerUUID) >= 0) {
            resetsLeft = String.valueOf(getPlayers().getResetsLeft(playerUUID)) + " / " + String.valueOf(Settings.resetLimit);
        }
        Util.sendMessage(sender, ChatColor.GREEN + getLocale(sender).get("adminInfo.resetsLeft") + ": " + resetsLeft);
        // Teams
        if (getPlayers().inTeam(playerUUID)) {
            final UUID leader = getIslands().getTeamLeader(playerUUID);
            final Set<UUID> pList = getIslands().getMembers(leader);
            Util.sendMessage(sender, ChatColor.GREEN + getLocale(sender).get("adminInfo.teamLeader") + ": " + getPlayers().getName(leader));
            Util.sendMessage(sender, ChatColor.GREEN + getLocale(sender).get("adminInfo.teamMembers") + ":");
            for (UUID member : pList) {
                if (!member.equals(leader)) {
                    Util.sendMessage(sender, ChatColor.WHITE + " - " + getPlayers().getName(member));
                }
            }
        } else {
            Util.sendMessage(sender, ChatColor.YELLOW + getLocale(sender).get("error.noTeam"));
            if (getIslands().getMembers(playerUUID).size() > 1) {
                Util.sendMessage(sender, ChatColor.RED + getLocale(sender).get("adminInfo.errorTeamMembersExist"));
            }
        }
        // Island info
        Island island = getIslands().getIsland(playerUUID);
        if (island != null) {
            Util.sendMessage(sender, ChatColor.YELLOW + getLocale(sender).get("adminInfo.islandLocation") + ":" + ChatColor.WHITE + " (" + island.getCenter().getBlockX() + ","
                    + island.getCenter().getBlockY() + "," + island.getCenter().getBlockZ() + ")");
            Util.sendMessage(sender, ChatColor.YELLOW + getLocale(sender).get("adminSetSpawn.center").replace("[location]", island.getCenter().getBlockX() + "," + island.getCenter().getBlockZ()));
            Util.sendMessage(sender, ChatColor.YELLOW + (getLocale(sender).get("adminSetSpawn.limits").replace("[min]", island.getMinX() + "," + island.getMinZ())).replace("[max]",
                    (island.getMinX() + island.getRange()*2 - 1) + "," + (island.getMinZ() + island.getRange()*2 - 1)));
            Util.sendMessage(sender, ChatColor.YELLOW + getLocale(sender).get("adminSetSpawn.range").replace("[number]",String.valueOf(island.getProtectionRange())));
            Util.sendMessage(sender, ChatColor.YELLOW + (getLocale(sender).get("adminSetSpawn.coords").replace("[min]",  island.getMinProtectedX() + ", " + island.getMinProtectedZ())).replace("[max]",
                    + (island.getMinProtectedX() + island.getProtectionRange()*2 - 1) + ", "
                            + (island.getMinProtectedZ() + island.getProtectionRange()*2 - 1)));
            if (island.isSpawn()) {
                Util.sendMessage(sender, ChatColor.YELLOW + getLocale(sender).get("adminInfo.isSpawn"));
            }
            if (island.isLocked()) {
                Util.sendMessage(sender, ChatColor.YELLOW + getLocale(sender).get("adminInfo.isLocked"));
            } else {
                Util.sendMessage(sender, ChatColor.YELLOW + getLocale(sender).get("adminInfo.isUnlocked"));
            }
            /*
            if (island.isPurgeProtected()) {
                Util.sendMessage(sender, ChatColor.GREEN + getLocale(sender).get("adminInfoIsProtected"));
            } else {
                Util.sendMessage(sender, ChatColor.GREEN + getLocale(sender).get("adminInfoIsUnprotected"));
            }*/
            Set<UUID> banList = getIslands().getBanList(playerUUID);
            if (!banList.isEmpty()) {
                Util.sendMessage(sender, ChatColor.YELLOW + getLocale(sender).get("adminInfo.bannedPlayers") + ":");
                String list = "";
                for (UUID uuid : banList) {
                    Player target = plugin.getServer().getPlayer(uuid);
                    if (target != null) {
                        //online
                        list += target.getName() + ", ";
                    } else {
                        list += getPlayers().getName(uuid) + ", ";
                    }
                }
                if (!list.isEmpty()) {
                    Util.sendMessage(sender, ChatColor.RED + list.substring(0, list.length()-2));
                }
            }
            // Number of hoppers
            // Util.sendMessage(sender, ChatColor.YELLOW + getLocale(sender).get("adminInfoHoppers").replace("[number]", String.valueOf(island.getHopperCount())));
        } else {
            Util.sendMessage(sender, ChatColor.RED + getLocale(sender).get("error.noIslandOther"));
        }
    }

    @Override
    public CanUseResp canUse(CommandSender sender) {
        return new CanUseResp(true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // TODO: Show help...

    }
}
