package us.tastybento.bskyblock.commands;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.AbstractCommand;
import us.tastybento.bskyblock.api.commands.ArgumentHandler;
import us.tastybento.bskyblock.api.commands.CanUseResp;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.managers.island.NewIsland;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.panels.LanguagePanel;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

/**
 * "/island" command
 *
 * @author Tastybento
 * @author Poslovitch
 */
public class IslandCommand extends AbstractCommand {
    protected static final boolean DEBUG = false;
    private BSkyBlock plugin;
    /**
     * Invite list - invited player name string (key), inviter name string
     * (value)
     */
    private final BiMap<UUID, UUID> inviteList = HashBiMap.create();
    // The time a player has to wait until they can reset their island again
    private HashMap<UUID, Long> resetWaitTime = new HashMap<>();
    protected Set<UUID> leavingPlayers = new HashSet<>();
    protected Set<UUID> kickingPlayers = new HashSet<>();

    public IslandCommand(BSkyBlock plugin) {
        super(Settings.ISLANDCOMMAND, new String[]{"is"}, true);
        plugin.getCommand(Settings.ISLANDCOMMAND).setExecutor(this);
        plugin.getCommand(Settings.ISLANDCOMMAND).setTabCompleter(this);
        this.plugin = plugin;
    }

    @Override
    public CanUseResp canUse(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return new CanUseResp(getLocale(sender).get("general.errors.use-in-game"));
        }

        // Basic permission check to even use /island
        if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.create")) {
            return new CanUseResp(getLocale(sender).get("general.errors.no-permission"));
        }

        return new CanUseResp(true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (getPlayers().inTeam(playerUUID) || getIslands().hasIsland(playerUUID)) {
            // Has island
            getIslands().homeTeleport(player);
        } else {
            // Create island
            createIsland(player);
        }
    }

    @Override
    public void setup() {
        /* /is about - Display plugin's info (license, version, authors) */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                Util.sendMessage(sender, "About " + plugin.getDescription().getName() + " v" + plugin.getDescription().getVersion() + ":");
                Util.sendMessage(sender, "Copyright (c) 2017 tastybento, Poslovitch");
                Util.sendMessage(sender, "All rights reserved.");
                Util.sendMessage(sender, "");
                Util.sendMessage(sender, "Redistribution and use in source and binary forms, with or without");
                Util.sendMessage(sender, "modification, are permitted provided that the following conditions are met:");

                Util.sendMessage(sender, "    * Redistributions of source code must retain the above copyright notice,");
                Util.sendMessage(sender, "      this list of conditions and the following disclaimer.");

                Util.sendMessage(sender, "    * Redistributions in binary form must reproduce the above copyright");
                Util.sendMessage(sender, "      notice, this list of conditions and the following disclaimer in the");
                Util.sendMessage(sender, "      documentation and/or other materials provided with the distribution.");

                Util.sendMessage(sender, "    * Neither the name of the BSkyBlock team nor the names of its");
                Util.sendMessage(sender, "      contributors may be used to endorse or promote products derived from");
                Util.sendMessage(sender, "      this software without specific prior written permission.");

                Util.sendMessage(sender, "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"");
                Util.sendMessage(sender, "AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE");
                Util.sendMessage(sender, "IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE");
                Util.sendMessage(sender, "ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE");
                Util.sendMessage(sender, "LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR");
                Util.sendMessage(sender, "CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF");
                Util.sendMessage(sender, "SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS");
                Util.sendMessage(sender, "INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN");
                Util.sendMessage(sender, "CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)");
                Util.sendMessage(sender, "ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE");
                Util.sendMessage(sender, "POSSIBILITY OF SUCH DAMAGE. ");
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[] {null, getLocale(sender).get("help.island.about")};
            }
        }.alias("about"));

        /* /is go [<1-x>] - Teleport player to his island or the specified home */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.home")) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-permission"));
                }
                if (!getIslands().hasIsland(playerUUID)) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-island"));
                }

                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                if (isPlayer) {
                    if (args.length == 1 && NumberUtils.isDigits(args[0])) {
                        int homeValue = Integer.valueOf(args[0]);
                        int maxHomes = Util.getPermValue(player, Settings.PERMPREFIX + "island.maxhomes", Settings.maxHomes);
                        if (homeValue > 1  && homeValue <= maxHomes) {
                            getIslands().homeTeleport(player, homeValue);
                            return;
                        }
                    }
                    getIslands().homeTeleport(player);
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                int maxHomes = Util.getPermValue((Player)sender, Settings.PERMPREFIX + "island.maxhomes", Settings.maxHomes);
                if (isPlayer && maxHomes > 1) {
                    return new String[]{"[1 - " + maxHomes + "]", getLocale(player).get("help.island.go-homes")};
                }
                return new String[]{null, getLocale(sender).get("help.island.go")};
            }
        }.alias("go").alias("home").alias("h"));

        /* /is spawn - Teleport player to spawn */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.spawn")) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-permission"));
                }
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{null, getLocale(sender).get("help.island.spawn")};
            }
        }.alias("spawn"));

        /* /is create - Create an island for this player (show the schematic selection panel if enabled) */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.create")) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-permission"));
                }
                if (getIslands().hasIsland(playerUUID)) {
                    return new CanUseResp(getLocale(sender).get("general.errors.already-have-island"));
                }
                if (inTeam) {
                    new CanUseResp(false); 
                }
                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                createIsland(player);
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"[schematic]", getLocale(sender).get("help.island.create")};
            }
        }.alias("create").alias("auto"));

        /* /is info [player] - Display info about (specified) player's island*/
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.info")) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-permission"));
                }
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"[player]", getLocale(sender).get("help.island.info")};
            }
        }.alias("info"));

        /* /is cp [<on/off>] - Open Control Panel or toggle it */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.cp")) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-permission"));
                }
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"[on/off]", getLocale(sender).get("help.island.controlpanel")};
            }
        }.alias("cp").alias("controlpanel"));

        /* /is reset - Reset the island */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.reset")) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-permission"));
                }
                if (!getIslands().hasIsland(playerUUID)) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-island"));
                }
                if (!getIslands().isOwner(playerUUID)) {
                    return new CanUseResp(false); 
                }
                if (inTeam) {
                    return new CanUseResp(getLocale(sender).get("island.reset.MustRemovePlayers"));
                }
                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                player.setGameMode(GameMode.SPECTATOR);
                // Get the player's old island
                Island oldIsland = getIslands().getIsland(playerUUID);
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: old island is at " + oldIsland.getCenter().getBlockX() + "," + oldIsland.getCenter().getBlockZ());
                // Remove them from this island (it still exists and will be deleted later)
                getIslands().removePlayer(playerUUID);
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: old island's owner is " + oldIsland.getOwner());
                // Create new island and then delete the old one
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: making new island ");
                try {
                    NewIsland.builder()
                    .player(player)
                    .reason(Reason.RESET)
                    .oldIsland(oldIsland)
                    .build();
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not create island for player.");
                    Util.sendMessage(sender, ChatColor.RED + plugin.getLocale(sender).get("general.errors.general"));
                    e.printStackTrace();
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{null, getLocale(sender).get("help.island.reset")};
            }
        }.alias("reset").alias("restart"));

        /* /is sethome - Set a home where the player is located */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.sethome")) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-permission"));
                }
                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                // Check island
                if (plugin.getIslands().getIsland(player.getUniqueId()) == null) {
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(playerUUID).get("general.errors.no-island"));
                    return;
                }
                if (!plugin.getIslands().playerIsOnIsland(player)) {
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(playerUUID).get("sethome.error.NotOnIsland"));
                    return; 
                }
                if (args.length == 0) {
                    // island sethome
                    plugin.getPlayers().setHomeLocation(playerUUID, player.getLocation());
                    Util.sendMessage(player, ChatColor.GREEN + plugin.getLocale(playerUUID).get("sethome.homeSet"));
                    return;
                } else if (args.length == 1) {
                    // Dynamic home sizes with permissions
                    int maxHomes = Util.getPermValue(player, Settings.PERMPREFIX + "island.maxhomes", Settings.maxHomes);
                    if (maxHomes > 1) {
                        // Check the number given is a number
                        int number = 0;
                        try {
                            number = Integer.valueOf(args[0]);
                            if (number < 1 || number > maxHomes) {
                                Util.sendMessage(player, ChatColor.RED + plugin.getLocale(playerUUID).get("sethome.error.NumHomes").replace("[max]",String.valueOf(maxHomes)));
                            } else {
                                plugin.getPlayers().setHomeLocation(playerUUID, player.getLocation(), number);
                                Util.sendMessage(player, ChatColor.GREEN + plugin.getLocale(playerUUID).get("sethome.homeSet"));
                            }
                        } catch (Exception e) {
                            Util.sendMessage(player, ChatColor.RED + plugin.getLocale(playerUUID).get("sethome.error.NumHomes").replace("[max]",String.valueOf(maxHomes)));
                        }
                    } else {
                        Util.sendMessage(player, ChatColor.RED + plugin.getLocale(playerUUID).get("general.errors.no-permission"));
                    }
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                int maxHomes = Util.getPermValue((Player)sender, Settings.PERMPREFIX + "island.maxhomes", Settings.maxHomes);
                if (isPlayer && maxHomes > 1) {
                    return new String[]{"[1 - " + maxHomes + "]", getLocale(sender).get("help.island.sethome")};
                }
                return new String[]{null, getLocale(sender).get("help.island.sethome")};
            }
        }.alias("sethome"));

        /* /is name <name> - Set island display name */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.name")) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-permission"));
                }

                if (!getIslands().hasIsland(playerUUID)) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-island"));
                }

                if (!getIslands().isOwner(playerUUID)) {
                    return new CanUseResp(getLocale(sender).get("general.errors.not-leader"));
                }

                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                // Explain command
                if (args.length == 1) {
                    //TODO Util.sendMessage(player, getHelpMessage(player, label, args[0], usage(sender, label)));
                    return;
                }

                // Naming the island
                String name = args[1];
                for (int i = 2; i < args.length; i++) name += " " + args[i];

                // Check if the name isn't too short or too long
                if (name.length() < Settings.nameMinLength) {
                    Util.sendMessage(player, getLocale(sender).get("general.errors.too-short").replace("[length]", String.valueOf(Settings.nameMinLength)));
                    return;
                }
                if (name.length() > Settings.nameMaxLength) {
                    Util.sendMessage(player, getLocale(sender).get("general.errors.too-long").replace("[length]", String.valueOf(Settings.nameMaxLength)));
                    return;
                }

                // Set the name
                if (VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.name.format"))
                    getIslands().getIsland(player.getUniqueId()).setName(ChatColor.translateAlternateColorCodes('&', name));
                else getIslands().getIsland(playerUUID).setName(name);

                Util.sendMessage(player, getLocale(sender).get("general.success"));
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"<name>", getLocale(sender).get("help.island.name")};
            }
        }.alias("name"));

        /* /is resetname - Reset island display name */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.name")) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-permission"));
                }

                if (!getIslands().hasIsland(playerUUID)) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-island"));
                }

                if (!getIslands().isOwner(playerUUID)) {
                    return new CanUseResp(getLocale(sender).get("general.errors.not-leader"));
                }

                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                // Resets the island name
                getIslands().getIsland(playerUUID).setName(null);
                Util.sendMessage(player, getLocale(sender).get("general.success"));
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{null, getLocale(sender).get("help.island.resetname")};
            }
        }.alias("resetname"));

        /* /is team - Display island team info */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "team")) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-permission"));
                }
                return new CanUseResp(inTeam);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: executing team command for " + playerUUID);
                // Fire event so add-ons can run commands, etc.
                TeamEvent event = TeamEvent.builder().island(getIslands().getIsland(playerUUID)).reason(TeamReason.INFO).involvedPlayer(playerUUID).build();
                plugin.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) return;
                if (teamLeaderUUID.equals(playerUUID)) {
                    int maxSize = Settings.maxTeamSize;
                    for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
                        if (perms.getPermission().startsWith(Settings.PERMPREFIX + "team.maxsize.")) {
                            if (perms.getPermission().contains(Settings.PERMPREFIX + "team.maxsize.*")) {
                                maxSize = Settings.maxTeamSize;
                                break;
                            } else {
                                // Get the max value should there be more than one
                                String[] spl = perms.getPermission().split(Settings.PERMPREFIX + "team.maxsize.");
                                if (spl.length > 1) {
                                    if (!NumberUtils.isDigits(spl[1])) {
                                        plugin.getLogger().severe("Player " + player.getName() + " has permission: " + perms.getPermission() + " <-- the last part MUST be a number! Ignoring...");
                                    } else {
                                        maxSize = Math.max(maxSize, Integer.valueOf(spl[1]));
                                    }
                                }
                            }
                        }
                        // Do some sanity checking
                        if (maxSize < 1) maxSize = 1;
                    }
                    if (teamMembers.size() < maxSize) {
                        Util.sendMessage(player, getLocale(sender).get("invite.youCanInvite").replace("[number]", String.valueOf(maxSize - teamMembers.size())));
                    } else {
                        Util.sendMessage(player, getLocale(sender).get("invite.error.YourIslandIsFull"));
                    }
                }
                Util.sendMessage(player, getLocale(sender).get("team.listingMembers"));
                // Display members in the list
                for (UUID m : teamMembers) {
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: member " + m);
                    if (teamLeaderUUID.equals(m)) {
                        Util.sendMessage(player, getLocale(sender).get("team.leader-color") + getPlayers().getName(m) + getLocale(sender).get("team.leader"));
                    } else {
                        Util.sendMessage(player, getLocale(sender).get("team.color") + getPlayers().getName(m));
                    }
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: executing team help");

                return new String[]{null, getLocale(sender).get("help.island.team")};
            }
        }.alias("team"));

        /* /is invite <player> - Invite a player to join the island */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "team")) {
                    return new CanUseResp(ChatColor.RED + getLocale(sender).get("general.errors.no-permission"));
                }
                // Player issuing the command must have an island
                if (!getPlayers().hasIsland(playerUUID)) {
                    // If the player is in a team, they are not the leader
                    if (getPlayers().inTeam(playerUUID)) {
                        return new CanUseResp(ChatColor.RED + getLocale(sender).get("general.errors.not-leader"));
                    }
                    return new CanUseResp(ChatColor.RED + getLocale(sender).get("invite.error.YouMustHaveIslandToInvite"));
                }
                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                if (args.length == 0 || args.length > 1) {
                    // Invite label with no name, i.e., /island invite - tells the player who has invited them so far
                    //TODO
                    if (inviteList.containsKey(playerUUID)) {
                        OfflinePlayer inviter = plugin.getServer().getOfflinePlayer(inviteList.get(playerUUID));
                        Util.sendMessage(player, ChatColor.GOLD + getLocale(sender).get("invite.nameHasInvitedYou").replace("[name]", inviter.getName()));
                    } else {
                        Util.sendMessage(player, ChatColor.GOLD + getLocale(sender).get("help.island.invite"));
                    }
                    return;
                }
                if (args.length == 1) {
                    // Only online players can be invited
                    UUID invitedPlayerUUID = getPlayers().getUUID(args[0]);
                    if (invitedPlayerUUID == null) {
                        Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("general.errors.offline-player"));
                        return;
                    }
                    Player invitedPlayer = plugin.getServer().getPlayer(invitedPlayerUUID);
                    if (invitedPlayer == null) {
                        Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("general.errors.offline-player"));
                        return;
                    }
                    // Player cannot invite themselves
                    if (playerUUID.equals(invitedPlayerUUID)) {
                        Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("invite.error.YouCannotInviteYourself"));
                        return;
                    }
                    // Check if this player can be invited to this island, or
                    // whether they are still on cooldown
                    long time = getPlayers().getInviteCoolDownTime(invitedPlayerUUID, getIslands().getIslandLocation(playerUUID));
                    if (time > 0 && !player.isOp()) {
                        Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("invite.error.CoolDown").replace("[time]", String.valueOf(time)));
                        return;
                    }
                    // Player cannot invite someone already on a team
                    if (getPlayers().inTeam(invitedPlayerUUID)) {
                        Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("invite.error.ThatPlayerIsAlreadyInATeam"));
                        return;
                    }
                    // Check if player has space on their team
                    int maxSize = Settings.maxTeamSize;
                    // Dynamic team sizes with permissions
                    for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
                        if (perms.getPermission().startsWith(Settings.PERMPREFIX + "team.maxsize.")) {
                            if (perms.getPermission().contains(Settings.PERMPREFIX + "team.maxsize.*")) {
                                maxSize = Settings.maxTeamSize;
                                break;
                            } else {
                                // Get the max value should there be more than one
                                String[] spl = perms.getPermission().split(Settings.PERMPREFIX + "team.maxsize.");
                                if (spl.length > 1) {
                                    if (!NumberUtils.isDigits(spl[1])) {
                                        plugin.getLogger().severe("Player " + player.getName() + " has permission: " + perms.getPermission() + " <-- the last part MUST be a number! Ignoring...");
                                    } else {
                                        maxSize = Math.max(maxSize, Integer.valueOf(spl[1]));
                                    }
                                }
                            }
                        }
                        // Do some sanity checking
                        if (maxSize < 1) maxSize = 1;
                    }
                    if (teamMembers.size() < maxSize) {
                        // If that player already has an invite out then retract it.
                        // Players can only have one invite one at a time - interesting
                        if (inviteList.containsValue(playerUUID)) {
                            inviteList.inverse().remove(playerUUID);
                            Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("invite.removingInvite"));
                        }
                        // Fire event so add-ons can run commands, etc.
                        TeamEvent event = TeamEvent.builder().island(getIslands().getIsland(playerUUID)).reason(TeamReason.INVITE).involvedPlayer(invitedPlayerUUID).build();
                        plugin.getServer().getPluginManager().callEvent(event);
                        if (event.isCancelled()) return;
                        // Put the invited player (key) onto the list with inviter (value)
                        // If someone else has invited a player, then this invite will overwrite the previous invite!
                        inviteList.put(invitedPlayerUUID, playerUUID);
                        Util.sendMessage(player, getLocale(sender).get("invite.inviteSentTo").replace("[name]", args[0]));
                        // Send message to online player
                        Util.sendMessage(Bukkit.getPlayer(invitedPlayerUUID), ChatColor.GOLD + getLocale(invitedPlayerUUID).get("invite.nameHasInvitedYou").replace("[name]", player.getName()));
                        Util.sendMessage(Bukkit.getPlayer(invitedPlayerUUID),ChatColor.GOLD + 
                                "/" + label + " [accept/reject]" + " " + getLocale(invitedPlayerUUID).get("invite.toAcceptOrReject"));
                        if (getPlayers().hasIsland(invitedPlayerUUID)) {
                            Util.sendMessage(Bukkit.getPlayer(invitedPlayerUUID), ChatColor.RED + getLocale(invitedPlayerUUID).get("invite.warningYouWillLoseIsland"));
                        }
                    } else {
                        Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("invite.error.YourIslandIsFull"));
                    }
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    // Don't show every player on the server. Require at least the first letter
                    return null;
                }
                return new HashSet<>(Util.getOnlinePlayerList(player));
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"<player>", getLocale(sender).get("help.island.invite")};
            }
        }.alias("invite"));

        /* /is uninvite <player> - Deletes the invite to join the island */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "team")) {
                    return new CanUseResp(ChatColor.RED + getLocale(sender).get("general.errors.no-permission"));
                }
                // Can only use if you have an invite out there
                return new CanUseResp(inviteList.inverse().containsKey(playerUUID));
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                // Fire event so add-ons can run commands, etc.
                TeamEvent event = TeamEvent.builder().island(getIslands().getIsland(playerUUID)).reason(TeamReason.UNINVITE).involvedPlayer(playerUUID).build();
                plugin.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) return;
                if (inviteList.inverse().containsKey(playerUUID)) {
                    Player invitee = plugin.getServer().getPlayer(inviteList.inverse().get(playerUUID));
                    if (invitee != null) {
                        inviteList.inverse().remove(playerUUID);
                        Util.sendMessage(invitee, ChatColor.RED + getLocale(invitee.getUniqueId()).get("invite.nameHasUninvitedYou").replace("[name]", player.getName()));
                        Util.sendMessage(player, ChatColor.GREEN + getLocale(sender).get("general.success"));
                    }
                } else {
                    Util.sendMessage(player, ChatColor.YELLOW + getLocale(sender).get("help.island.invite"));
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"", getLocale(sender).get("help.island.uninvite")};
            }
        }.alias("uninvite"));

        /* /is leave - Leave the island */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "team")) {
                    return new CanUseResp(ChatColor.RED + getLocale(sender).get("general.errors.no-permission"));
                }
                // Can only leave if you are not the leader
                return new CanUseResp(inTeam && !teamLeaderUUID.equals(playerUUID));
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                if (getPlayers().inTeam(playerUUID)) {
                    // Team leaders cannot leave
                    if (teamLeaderUUID != null && teamLeaderUUID.equals(playerUUID)) {
                        Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("leave.errorYouAreTheLeader"));
                        return;
                    }
                    // Fire event so add-ons can run commands, etc.
                    TeamEvent event = TeamEvent.builder().island(getIslands().getIsland(playerUUID)).reason(TeamReason.LEAVE).involvedPlayer(playerUUID).build();
                    plugin.getServer().getPluginManager().callEvent(event);
                    if (event.isCancelled()) return;
                    // Check for confirmation
                    if (Settings.leaveConfirmation && !leavingPlayers.contains(playerUUID)) {
                        leavingPlayers.add(playerUUID);
                        Util.sendMessage(player, ChatColor.GOLD + getLocale(sender).get("leave.warning"));

                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            // If the player is still on the list, remove them and cancel the leave
                            if (leavingPlayers.contains(playerUUID)) {
                                leavingPlayers.remove(playerUUID);
                                Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("leave.canceled"));
                            }
                        }, Settings.leaveConfirmWait * 20L);

                        return;
                    }
                    // Remove from confirmation list
                    leavingPlayers.remove(playerUUID);
                    // Remove from team
                    getIslands().setLeaveTeam(playerUUID);
                    // Log the location that this player left so they
                    // cannot join again before the cool down ends
                    getPlayers().startInviteCoolDownTimer(playerUUID, getIslands().getIslandLocation(teamLeaderUUID));

                    Util.sendMessage(player, ChatColor.GREEN + getLocale(sender).get("leave.youHaveLeftTheIsland"));
                    // Tell the leader if they are online
                    if (plugin.getServer().getPlayer(teamLeaderUUID) != null) {
                        Player leader = plugin.getServer().getPlayer(teamLeaderUUID);
                        Util.sendMessage(leader, ChatColor.RED + getLocale(teamLeaderUUID).get("leave.nameHasLeftYourIsland").replace("[name]", player.getName()));
                    } else {
                        // TODO: Leave them a message
                        //plugin.getMessages().setMessage(teamLeader, plugin.myLocale(teamLeader).leavenameHasLeftYourIsland.replace("[name]", player.getName()));
                    }

                    // Clear all player variables and save
                    getPlayers().resetPlayer(player);
                    if (!player.performCommand(Settings.SPAWNCOMMAND)) {
                        player.teleport(player.getWorld().getSpawnLocation());
                    }
                } else {
                    Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("leave.errorYouCannotLeaveIsland"));
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{null, getLocale(sender).get("help.island.leave")};
            }
        }.alias("leave"));

        /* /is kick <player> - Kick the specified player from island team */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "team")) {
                    return new CanUseResp(ChatColor.RED + getLocale(sender).get("general.errors.no-permission"));
                }

                return new CanUseResp(inTeam && teamLeaderUUID.equals(playerUUID));
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                if (args.length != 1) {
                    Util.sendMessage(player, ChatColor.RED + getShortDescription(sender));
                    return;  
                }
                // Only team members can be kicked
                UUID targetPlayerUUID = getPlayers().getUUID(args[0]);
                if (targetPlayerUUID == null || !getIslands().getMembers(playerUUID).contains(targetPlayerUUID)) {
                    Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("kick.error.notPartOfTeam"));
                    return;
                }
                // Player cannot kick themselves
                if (playerUUID.equals(targetPlayerUUID)) {
                    Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("kick.error.youCannotKickYourself"));
                    return;
                }
                // Fire event so add-ons can run commands, etc.
                TeamEvent event = TeamEvent.builder().island(getIslands().getIsland(playerUUID)).reason(TeamReason.KICK).involvedPlayer(targetPlayerUUID).build();
                plugin.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) return;
                // Check for confirmation
                if (Settings.confirmKick && !kickingPlayers.contains(playerUUID)) {
                    kickingPlayers.add(playerUUID);
                    Util.sendMessage(player, ChatColor.GOLD + getLocale(sender).get("kick.warning"));

                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        // If the player is still on the list, remove them and cancel the leave
                        if (kickingPlayers.contains(playerUUID)) {
                            kickingPlayers.remove(playerUUID);
                            Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("kick.canceled"));
                        }
                    }, Settings.confirmKickWait * 20L);

                    return;
                }
                // Remove from confirmation list
                kickingPlayers.remove(playerUUID);
                // Remove from team
                getIslands().setLeaveTeam(targetPlayerUUID);
                // Log the location that this player left so they
                // cannot join again before the cool down ends
                getPlayers().startInviteCoolDownTimer(targetPlayerUUID, getIslands().getIslandLocation(teamLeaderUUID));
                // Tell the player they kicked okay
                Util.sendMessage(player, ChatColor.GREEN + getLocale(sender).get("kick.nameRemoved").replace("[name]", getPlayers().getName(targetPlayerUUID)));
                // Tell the target if they are online
                if (plugin.getServer().getPlayer(targetPlayerUUID) != null) {
                    Player target = plugin.getServer().getPlayer(targetPlayerUUID);
                    Util.sendMessage(target, ChatColor.RED + getLocale(targetPlayerUUID).get("kick.nameRemovedYou").replace("[name]", player.getName()));
                } else {
                    // TODO: Leave them an offline message
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                Set<String> result = new HashSet<>();
                for (UUID members : getIslands().getMembers(teamLeaderUUID)) {
                    if (!members.equals(teamLeaderUUID)) {
                        result.add(getPlayers().getName(members));
                    }
                }
                return result;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"<player>", getLocale(sender).get("help.island.kick")};
            }
        }.alias("kick"));

        /* /is accept [player] - Accept invite */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "team")) {
                    return new CanUseResp(getLocale(sender).get("general.errors.no-permission"));
                }

                return new CanUseResp(inviteList.containsKey(player.getUniqueId()));
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                // Check if player has been invited
                if (!inviteList.containsKey(playerUUID)) {
                    Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("invite.error.NoOneInvitedYou"));
                    return;
                }
                // Check if player is already in a team
                if (getPlayers().inTeam(playerUUID)) {
                    Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("invite.error.YouAreAlreadyOnATeam"));
                    return;
                }
                // Get the team leader
                UUID prospectiveTeamLeaderUUID = inviteList.get(playerUUID);
                if (!getIslands().hasIsland(prospectiveTeamLeaderUUID)) {
                    Util.sendMessage(player, ChatColor.RED + getLocale(sender).get("invite.error.InvalidInvite"));
                    inviteList.remove(playerUUID);
                    return;
                }
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Invite is valid");
                // Fire event so add-ons can run commands, etc.
                TeamEvent event = TeamEvent.builder().island(getIslands().getIsland(prospectiveTeamLeaderUUID)).reason(TeamReason.JOIN).involvedPlayer(playerUUID).build();
                plugin.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) return;
                // Remove the invite
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Removing player from invite list");
                inviteList.remove(playerUUID);
                // Put player into Spectator mode
                player.setGameMode(GameMode.SPECTATOR);
                // Get the player's island - may be null if the player has no island
                Island island = getIslands().getIsland(playerUUID);
                // Get the team's island
                Island teamIsland = getIslands().getIsland(prospectiveTeamLeaderUUID);
                // Clear the player's inventory
                player.getInventory().clear();
                // Move player to team's island
                Location newHome = getIslands().getSafeHomeLocation(prospectiveTeamLeaderUUID, 1);
                player.teleport(newHome);
                // Remove player as owner of the old island
                getIslands().removePlayer(playerUUID);
                // Add the player as a team member of the new island
                getIslands().setJoinTeam(teamIsland, playerUUID);
                // Set the player's home
                getPlayers().setHomeLocation(playerUUID, player.getLocation());
                // Delete the old island
                getIslands().deleteIsland(island, true);
                // Set the cooldown
                setResetWaitTime(player);
                // Reset deaths
                if (Settings.teamJoinDeathReset) {
                    getPlayers().setDeaths(playerUUID, 0);
                }
                // Put player back into normal mode
                player.setGameMode(GameMode.SURVIVAL);

                Util.sendMessage(player, ChatColor.GREEN + getLocale(sender).get("invite.youHaveJoinedAnIsland").replace("[label]", Settings.ISLANDCOMMAND));

                if (plugin.getServer().getPlayer(inviteList.get(playerUUID)) != null) {
                    Util.sendMessage(plugin.getServer().getPlayer(inviteList.get(playerUUID)),
                            ChatColor.GREEN + getLocale(sender).get("invite.hasJoinedYourIsland").replace("[name]", player.getName()));
                }
                getIslands().save(false);
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: After save " + getIslands().getIsland(prospectiveTeamLeaderUUID).getMembers().toString());
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"[player]", getLocale(sender).get("help.island.accept")};
            }
        }.alias("accept"));

        /* /is reject [player] - Reject invite */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "team")) {
                    return new CanUseResp(ChatColor.RED + getLocale(sender).get("general.errors.no-permission"));
                }
                // Can use if invited
                return new CanUseResp(inviteList.containsKey(player.getUniqueId()));
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                // Reject /island reject
                if (inviteList.containsKey(playerUUID)) {
                    // Fire event so add-ons can run commands, etc.
                    TeamEvent event = TeamEvent.builder().island(getIslands().getIsland(inviteList.get(playerUUID))).reason(TeamReason.REJECT).involvedPlayer(playerUUID).build();
                    plugin.getServer().getPluginManager().callEvent(event);
                    if (event.isCancelled()) return;

                    // Remove this player from the global invite list
                    inviteList.remove(player.getUniqueId());
                    Util.sendMessage(player, ChatColor.GREEN + getLocale(playerUUID).get("reject.youHaveRejectedInvitation"));
                    // If the leader is online tell them directly
                    // about the rejection
                    if (Bukkit.getPlayer(inviteList.get(playerUUID)) != null) {
                        Util.sendMessage(Bukkit.getPlayer(inviteList.get(playerUUID)),
                                ChatColor.RED + getLocale(playerUUID).get("reject.nameHasRejectedInvite").replace("[name]", player.getName()));
                    }

                } else {
                    // Someone typed /island reject and had not been invited
                    Util.sendMessage(player, ChatColor.RED + getLocale(playerUUID).get("reject.youHaveNotBeenInvited"));
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"[player]", getLocale(sender).get("help.island.reject")};
            }
        }.alias("reject"));

        /* /is makeleader <player> - Set the specified player as leader/owner of the island */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "team")) {
                    return new CanUseResp(ChatColor.RED + getLocale(sender).get("general.errors.no-permission"));
                }
                // Can use if in a team
                return new CanUseResp(inTeam && teamLeaderUUID.equals(playerUUID));
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                plugin.getLogger().info("DEBUG: arg[0] = " + args[0]);
                UUID targetUUID = getPlayers().getUUID(args[0]);
                if (targetUUID == null) {
                    Util.sendMessage(player, ChatColor.RED + getLocale(playerUUID).get("general.errors.unknown-player"));
                    return;
                }
                if (!getPlayers().inTeam(playerUUID)) {
                    Util.sendMessage(player, ChatColor.RED + getLocale(playerUUID).get("makeleader.errorYouMustBeInTeam"));
                    return;
                }
                if (!teamLeaderUUID.equals(playerUUID)) {
                    Util.sendMessage(player, ChatColor.RED + getLocale(playerUUID).get("makeleader.errorNotYourIsland"));
                    return;
                }
                if (targetUUID.equals(playerUUID)) {
                    Util.sendMessage(player, ChatColor.RED + getLocale(playerUUID).get("makeleader.errorGeneralError"));
                    return;
                }
                if (!teamMembers.contains(targetUUID)) {
                    Util.sendMessage(player, ChatColor.RED + getLocale(playerUUID).get("makeleader.errorThatPlayerIsNotInTeam"));
                    return;
                }
                // Fire event so add-ons can run commands, etc.
                TeamEvent event = TeamEvent.builder().island(getIslands().getIsland(playerUUID)).reason(TeamReason.MAKELEADER).involvedPlayer(targetUUID).build();
                plugin.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) return;

                // target is the new leader
                getIslands().getIsland(playerUUID).setOwner(targetUUID);
                Util.sendMessage(player, ChatColor.GREEN
                        + getLocale(playerUUID).get("makeleader.nameIsNowTheOwner").replace("[name]", getPlayers().getName(targetUUID)));

                // Check if online
                Player target = plugin.getServer().getPlayer(targetUUID);
                if (target == null) {
                    // TODO offline messaging
                    //plugin.getMessages().setMessage(targetPlayer, getLocale(playerUUID).get("makeleader.youAreNowTheOwner"));

                } else {
                    // Online
                    Util.sendMessage(plugin.getServer().getPlayer(targetUUID), ChatColor.GREEN + getLocale(targetUUID).get("makeleader.youAreNowTheOwner"));
                    // Check if new leader has a lower range permission than the island size
                    boolean hasARangePerm = false;
                    int range = Settings.islandProtectionRange;
                    // Check for zero protection range
                    Island islandByOwner = getIslands().getIsland(targetUUID);
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
                            Util.sendMessage(player, getLocale(targetUUID).get("admin.SetRangeUpdated").replace("[number]", String.valueOf(range)));
                            Util.sendMessage(target, getLocale(targetUUID).get("admin.SetRangeUpdated").replace("[number]", String.valueOf(range)));
                            plugin.getLogger().info(
                                    "Makeleader: Island protection range changed from " + islandByOwner.getProtectionRange() + " to "
                                            + range + " for " + player.getName() + " due to permission.");
                        }
                        islandByOwner.setProtectionRange(range);
                    }
                }
                getIslands().save(true);
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return new HashSet<>(Util.getOnlinePlayerList(player));
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"<player>", getLocale(sender).get("help.island.makeleader")};
            }
        }.alias("makeleader").alias("transfer"));

        /* /is expel <player> - Expel a visitor/coop from the island */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"<player>", getLocale(sender).get("help.island.expel")};
            }
        }.alias("expel"));

        /* /is ban <player> - Ban a player from the island */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"<player>", getLocale(sender).get("help.island.ban")};
            }
        }.alias("ban"));

        /* /is unban <player> - Unban player from the island */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"<player>", getLocale(sender).get("help.island.unban")};
            }
        }.alias("unban"));

        /* /is banlist - Display island banned players */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {


            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{null, getLocale(sender).get("help.island.banlist")};
            }
        }.alias("banlist").alias("bl"));

        /* /is trust <player> - Trust a player */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"<player>", getLocale(sender).get("help.island.trust")};
            }
        }.alias("trust"));

        /* /is untrust <player> - Untrust a player */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"<player>", getLocale(sender).get("help.island.untrust")};
            }
        }.alias("untrust"));

        /* /is trustlist - Display trust players */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{null, getLocale(sender).get("help.island.trustlist")};
            }
        }.alias("trustlist").alias("tl"));

        /* /is coop <player> - Coop a player */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"<player>", getLocale(sender).get("help.island.coop")};
            }
        }.alias("coop"));

        /* /is uncoop <player> - Uncoop a player */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"<player>", getLocale(sender).get("help.island.uncoop")};
            }
        }.alias("uncoop"));

        /* /is cooplist - Display coop players */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{null, getLocale(sender).get("help.island.cooplist")};
            }
        }.alias("cooplist").alias("cl"));

        /* /is lock - Toggle island lock */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.lock")) {
                    return new CanUseResp(ChatColor.RED + getLocale(sender).get("general.errors.no-permission"));
                }

                if (!getIslands().hasIsland(playerUUID)) {
                    return new CanUseResp(ChatColor.RED + getLocale(sender).get("general.errors.no-island"));
                }

                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                Island island = getIslands().getIsland(playerUUID);

                if (!island.getLocked()) {
                    // TODO: Expel all visitors
                    // TODO: send offline messages
                    island.setLocked(true);
                } else {
                    Util.sendMessage(player, getLocale(sender).get("island.lock.unlocking"));
                    // TODO: send offline messages
                    island.setLocked(false);
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{null, getLocale(sender).get("help.island.lock")};
            }
        }.alias("lock").alias("unlock"));

        /* /is settings - Display Settings menu */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{null, getLocale(sender).get("help.island.settings")};
            }
        }.alias("settings"));

        /* /is language <id> - Set the language */
        addArgument(new ArgumentHandler(label) {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                LanguagePanel.openPanel((Player) sender);
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"<id>", getLocale(sender).get("help.island.language")};
            }
        }.alias("language").alias("lang"));
    }

    /**
     * Sets a timeout for player into the Hashmap resetWaitTime
     *
     * @param player
     */
    private void setResetWaitTime(final Player player) {
        resetWaitTime.put(player.getUniqueId(), Calendar.getInstance().getTimeInMillis() + Settings.resetWait * 1000);
    }

    /**
     * Creates an island for player
     *
     * @param player
     */
    protected void createIsland(Player player) {
        //TODO: Add panels, make a selection.
        try {
            NewIsland.builder()
            .player(player)
            .reason(Reason.CREATE)
            .build();
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create island for player.");
            Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).get("general.errors.general"));
            e.printStackTrace();
        }
    }

}
