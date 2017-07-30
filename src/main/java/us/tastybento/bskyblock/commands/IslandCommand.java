package us.tastybento.bskyblock.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.AbstractCommand;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.schematics.Schematic;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

import java.util.List;

/**
 * "/island" command
 * 
 * @author Tastybento
 * @author Poslovitch
 */
public class IslandCommand extends AbstractCommand {
    private BSkyBlock plugin;

    public IslandCommand(BSkyBlock plugin) {
        super(Settings.ISLANDCOMMAND, true);
        plugin.getCommand(Settings.ISLANDCOMMAND).setExecutor(this);
        plugin.getCommand(Settings.ISLANDCOMMAND).setTabCompleter(this);
        this.plugin = plugin;
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if(!(sender instanceof Player)){
            Util.sendMessage(sender, plugin.getLocale(sender).get("general.errors.use-in-game"));
            return false;
        }

        Player player = (Player) sender;
        // Basic permission check to even use /island
        if(!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.create")){
            Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).get("general.errors.no-permission"));
            return false;
        }

        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player)sender;
            if (plugin.getIslands().hasIsland(player.getUniqueId())) {
                // Has island
                plugin.getIslands().homeTeleport(player);
            } else {
                // Create island
                createIsland(player);
            }
        } else {
            Util.sendMessage(sender, plugin.getLocale().get("general.errors.use-in-game"));
        }
    }

    @Override
    public void setup() {
        /* /is about - Display plugin's info (license, version, authors) */
        addArgument(new String[] {"about"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                return true;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                Util.sendMessage(sender, ChatColor.GOLD + "About " + ChatColor.GREEN + plugin.getDescription().getName() + ChatColor.GOLD + " v" + ChatColor.AQUA + plugin.getDescription().getVersion() + ChatColor.GOLD + ":");
                Util.sendMessage(sender, ChatColor.GOLD + "Copyright (c) 2017 tastybento, Poslovitch");
                Util.sendMessage(sender, ChatColor.GOLD + "All rights reserved.");
                Util.sendMessage(sender, ChatColor.GOLD + "");
                Util.sendMessage(sender, ChatColor.GOLD + "Redistribution and use in source and binary forms, with or without");
                Util.sendMessage(sender, ChatColor.GOLD + "modification, are permitted provided that the following conditions are met:");

                Util.sendMessage(sender, ChatColor.GOLD + "    * Redistributions of source code must retain the above copyright notice,");
                Util.sendMessage(sender, ChatColor.GOLD + "      this list of conditions and the following disclaimer.");
      
                Util.sendMessage(sender, ChatColor.GOLD + "    * Redistributions in binary form must reproduce the above copyright");
                Util.sendMessage(sender, ChatColor.GOLD + "      notice, this list of conditions and the following disclaimer in the");
                Util.sendMessage(sender, ChatColor.GOLD + "      documentation and/or other materials provided with the distribution.");
      
                Util.sendMessage(sender, ChatColor.GOLD + "    * Neither the name of the BSkyBlock team nor the names of its");
                Util.sendMessage(sender, ChatColor.GOLD + "      contributors may be used to endorse or promote products derived from");
                Util.sendMessage(sender, ChatColor.GOLD + "      this software without specific prior written permission.");

                Util.sendMessage(sender, ChatColor.GOLD + "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"");
                Util.sendMessage(sender, ChatColor.GOLD + "AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE");
                Util.sendMessage(sender, ChatColor.GOLD + "IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE");
                Util.sendMessage(sender, ChatColor.GOLD + "ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE");
                Util.sendMessage(sender, ChatColor.GOLD + "LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR");
                Util.sendMessage(sender, ChatColor.GOLD + "CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF");
                Util.sendMessage(sender, ChatColor.GOLD + "SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS");
                Util.sendMessage(sender, ChatColor.GOLD + "INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN");
                Util.sendMessage(sender, ChatColor.GOLD + "CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)");
                Util.sendMessage(sender, ChatColor.GOLD + "ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE");
                Util.sendMessage(sender, ChatColor.GOLD + "POSSIBILITY OF SUCH DAMAGE. ");
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return null;
            }
        });

        /* /is go [<1-x>] - Teleport player to his island or the specified home */
        addArgument(new String[] {"go", "home", "h"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return true;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                if (sender instanceof Player) {
                    Player player = (Player)sender;
                    if (plugin.getIslands().hasIsland(player.getUniqueId())) {
                        plugin.getIslands().homeTeleport(player);
                    }
                }
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                // TODO check if multiple homes
                if(VaultHelper.hasPerm((Player) sender, "todo")) return new String[] {"[1-x]", plugin.getLocale(sender).get("help.island.go-homes")};
                return new String[] {null, plugin.getLocale(sender).get("help.island.go")};
            }
        });

        /* /is spawn - Teleport player to spawn */
        addArgument(new String[] {"spawn"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.spawn")};
            }
        });

        /* /is create - Create an island for this player (show the schematic selection panel if enabled) */
        addArgument(new String[] {"create", "auto"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return true;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                if (sender instanceof Player) {
                    Player player = (Player)sender;
                    if (!plugin.getIslands().hasIsland(player.getUniqueId())) {
                        createIsland(player);
                    } else {
                        Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).get("island.error.YouAlreadyHaveAnIsland"));
                    }
                }
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"[schematic]", plugin.getLocale(sender).get("help.island.create")};
            }
        });

        /* /is info [player] - Display info about (specified) player's island*/
        addArgument(new String[] {"info"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"[player]", plugin.getLocale(sender).get("help.island.info")};
            }
        });

        /* /is cp [<on/off>] - Open Control Panel or toggle it */
        addArgument(new String[] {"controlpanel", "cp"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"[on/off]", plugin.getLocale(sender).get("help.island.control-panel")};
            }
        });

        /* /is reset - Reset the island */
        addArgument(new String[] {"reset", "restart"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return true;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                
                if (!(sender instanceof Player)) {
                    Util.sendMessage(sender, plugin.getLocale().get("error.useInGame"));
                }
                Player player = (Player)sender;
                if (plugin.getIslands().hasIsland(player.getUniqueId())) {
                    // Get the player's old island
                    Island oldIsland = plugin.getIslands().getIsland(player.getUniqueId());
                    plugin.getLogger().info("DEBUG: old island is at " + oldIsland.getCenter().getBlockX() + "," + oldIsland.getCenter().getBlockZ());
                    // Remove them from this island (it still exists and will be deleted later)
                    plugin.getIslands().removePlayer(player.getUniqueId());
                    plugin.getLogger().info("DEBUG: old island's owner is " + oldIsland.getOwner());
                    // Create new island and then delete the old one
                    plugin.getLogger().info("DEBUG: making new island ");
                    Schematic schematic = plugin.getSchematics().getSchematic("default");
                    plugin.getIslands().newIsland(player, schematic, oldIsland); 
                    
                } else {
                    Util.sendMessage(player, plugin.getLocale(player.getUniqueId()).get("error.noIsland")); 
                }
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.reset")};
            }
        });

        /* /is sethome - Set a home where the player is located */
        addArgument(new String[] {"sethome"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.sethome")};
            }
        });

        /* /is name <name> - Set island display name */
        addArgument(new String[] {"name"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                Player player = (Player) sender;

                if(!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.name")){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).get("general.errors.no-permission"));
                    return false;
                }

                if(!plugin.getIslands().hasIsland(player.getUniqueId())){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).get("general.errors.no-island"));
                    return false;
                }

                if(!plugin.getIslands().isOwner(player.getUniqueId())){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).get("general.errors.not-leader"));
                    return false;
                }

                return true;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                Player player = (Player) sender;

                // Explain command
                if(args.length == 1){
                    //TODO Util.sendMessage(player, getHelpMessage(player, label, args[0], getHelp(sender, label)));
                    return;
                }

                // Naming the island
                String name = args[1];
                for(int i = 2; i < args.length; i++){
                    name += " " + args[i];
                }

                // Check if the name isn't too short or too long
                if(name.length() < Settings.nameMinLength){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).get("general.errors.too-short").replace("[length]", String.valueOf(Settings.nameMinLength)));
                    return;
                }
                if(name.length() > Settings.nameMaxLength){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).get("general.errors.too-long").replace("[length]", String.valueOf(Settings.nameMaxLength)));
                    return;
                }

                // Set the name
                if(VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.name.format")) plugin.getIslands().getIsland(player.getUniqueId()).setName(ChatColor.translateAlternateColorCodes('&', name));
                else plugin.getIslands().getIsland(player.getUniqueId()).setName(name);

                Util.sendMessage(player, ChatColor.GREEN + plugin.getLocale(player).get("general.success"));
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"<name>", plugin.getLocale(sender).get("help.island.name")};
            }
        });

        /* /is resetname - Reset island display name */
        addArgument(new String[] {"resetname"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                Player player = (Player) sender;

                if(!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.name")){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).get("general.errors.no-permission"));
                    return false;
                }

                if(!plugin.getIslands().hasIsland(player.getUniqueId())){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).get("general.errors.no-island"));
                    return false;
                }

                if(!plugin.getIslands().isOwner(player.getUniqueId())){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).get("general.errors.not-leader"));
                    return false;
                }

                return true;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                Player player = (Player) sender;

                // Resets the island name
                plugin.getIslands().getIsland(player.getUniqueId()).setName(null);
                Util.sendMessage(player, ChatColor.GREEN + plugin.getLocale(player).get("general.success"));
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.resetname")};
            }
        });

        /* /is limits - Show the (tile) entities limits */
        addArgument(new String[] {"limits"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.limits")};
            }
        });

        /* /is team - Display island team info */
        addArgument(new String[] {"team"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.team")};
            }
        });

        /* /is invite <player> - Invite a player to join the island */
        addArgument(new String[] {"invite"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.invite")};
            }
        });

        /* /is uninvite <player> - Deletes the invite to join the island */
        addArgument(new String[] {"uninvite"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.uninvite")};
            }
        });

        /* /is leave - Leave the island */
        addArgument(new String[] {"leave"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.leave")};
            }
        });

        /* /is kick <player> - Kick the specified player from island team */
        addArgument(new String[] {"kick"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.kick")};
            }
        });

        /* /is accept [player] - Accept invite */
        addArgument(new String[] {"accept"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"[player]", plugin.getLocale(sender).get("help.island.accept")};
            }
        });

        /* /is reject [player] - Reject invite */
        addArgument(new String[] {"reject"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"[player]", plugin.getLocale(sender).get("help.island.reject")};
            }
        });

        /* /is makeleader <player> - Set the specified player as leader/owner of the island */
        addArgument(new String[] {"makeleader", "transfer"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.makeleader")};
            }
        });

        /* /is teamchat - Toggle TeamChat */
        addArgument(new String[] {"teamchat", "tc"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.teamchat")};
            }
        });

        /* /is biomes - Change island biome */
        addArgument(new String[] {"biomes"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.biomes")};
            }
        });

        /* /is expel <player> - Expel a visitor/coop from the island */
        addArgument(new String[] {"expel"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.expel")};
            }
        });

        /* /is expel - Expel every visitor/coop from the island */
        addArgument(new String[] {"expelall", "expel!"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.expelall")};
            }
        });

        /* /is ban <player> - Ban a player from the island */
        addArgument(new String[] {"ban"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.ban")};
            }
        });

        /* /is unban <player> - Unban player from the island */
        addArgument(new String[] {"unban"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.unban")};
            }
        });

        /* /is banlist - Display island banned players */
        addArgument(new String[] {"banlist", "bl"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.banlist")};
            }
        });

        /* /is trust <player> - Trust a player */
        addArgument(new String[] {"trust"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.trust")};
            }
        });

        /* /is untrust <player> - Untrust a player */
        addArgument(new String[] {"untrust"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.untrust")};
            }
        });

        /* /is trustlist - Display trust players */
        addArgument(new String[] {"trustlist", "tl"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.trustlist")};
            }
        });

        /* /is coop <player> - Coop a player */
        addArgument(new String[] {"coop"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.coop")};
            }
        });

        /* /is uncoop <player> - Uncoop a player */
        addArgument(new String[] {"uncoop"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.uncoop")};
            }
        });

        /* /is cooplist - Display coop players */
        addArgument(new String[] {"cooplist", "cl"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.cooplist")};
            }
        });

        /* /is lock - Toggle island lock */
        addArgument(new String[] {"lock", "unlock"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                Player player = (Player) sender;

                if(!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.lock")){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).get("general.errors.no-permission"));
                    return false;
                }

                if(!plugin.getIslands().hasIsland(player.getUniqueId())){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).get("general.errors.no-island"));
                    return false;
                }

                return true;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                Player player = (Player) sender;
                Island island = plugin.getIslands().getIsland(player.getUniqueId());

                if(!island.getLocked()){
                    // TODO: Expel all visitors
                    // TODO: send offline messages
                    island.setLocked(true);
                } else {
                    Util.sendMessage(player, ChatColor.GREEN + plugin.getLocale(player).get("island.lock.unlocking"));
                    // TODO: send offline messages
                    island.setLocked(false);
                }
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.lock")};
            }
        });

        /* /is settings - Display Settings menu */
        addArgument(new String[] {"settings"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.island.settings")};
            }
        });

        /* /is language <id> - Set the language */
        addArgument(new String[] {"language", "lang"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                
                return false;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {"<id>", plugin.getLocale(sender).get("help.island.language")};
            }
        });
    }

    /**
     * Creates an island for player
     * @param player
     */
    protected void createIsland(Player player) {
        //TODO: Add panels, make a selection.
        Schematic schematic = plugin.getSchematics().getSchematic("default");
        plugin.getIslands().newIsland(player, schematic);        
    }

}
