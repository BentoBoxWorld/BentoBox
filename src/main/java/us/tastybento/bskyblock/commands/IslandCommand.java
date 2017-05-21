package us.tastybento.bskyblock.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

/**
 * "/island" command
 * 
 * @author Tastybento
 * @author Poslovitch
 */
public class IslandCommand extends BSBCommand{
    private BSkyBlock plugin;
    
    public IslandCommand(BSkyBlock plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public boolean canExecute(CommandSender sender, String label) {
        if(!(sender instanceof Player)){
            Util.sendMessage(sender, plugin.getLocale(sender).errorUseInGame);
            return false;
        }
        
        Player player = (Player) sender;
        // Basic permission check to even use /island
        if(!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.create")){
            Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).errorNoPermission);
            return false;
        }
        
        return true;
    }

    @Override
    public void onExecuteDefault(CommandSender sender, String label, String[] args) {
        // TODO Auto-generated method stub
    }
    
    @Override
    public void setup() {
        /* /is about - Display plugin's info (license, version, authors) */
        registerArgument(new String[] {"about"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                return true;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                Util.sendMessage(sender, ChatColor.GOLD + "About " + ChatColor.GREEN + plugin.getDescription().getName() + ChatColor.GOLD + " v" + ChatColor.AQUA + plugin.getDescription().getVersion() + ChatColor.GOLD + ":");
                Util.sendMessage(sender, ChatColor.GOLD + "This plugin is free software: you can redistribute");
                Util.sendMessage(sender, ChatColor.GOLD + "it and/or modify it under the terms of the GNU");
                Util.sendMessage(sender, ChatColor.GOLD + "General Public License as published by the Free");
                Util.sendMessage(sender, ChatColor.GOLD + "Software Foundation, either version 3 of the License,");
                Util.sendMessage(sender, ChatColor.GOLD + "or (at your option) any later version.");
                Util.sendMessage(sender, ChatColor.GOLD + "This plugin is distributed in the hope that it");
                Util.sendMessage(sender, ChatColor.GOLD + "will be useful, but WITHOUT ANY WARRANTY; without");
                Util.sendMessage(sender, ChatColor.GOLD + "even the implied warranty of MERCHANTABILITY or");
                Util.sendMessage(sender, ChatColor.GOLD + "FITNESS FOR A PARTICULAR PURPOSE.  See the");
                Util.sendMessage(sender, ChatColor.GOLD + "GNU General Public License for more details.");
                Util.sendMessage(sender, ChatColor.GOLD + "You should have received a copy of the GNU");
                Util.sendMessage(sender, ChatColor.GOLD + "General Public License along with this plugin.");
                Util.sendMessage(sender, ChatColor.GOLD + "If not, see <http://www.gnu.org/licenses/>.");
                Util.sendMessage(sender, ChatColor.GOLD + "Souce code is available on GitHub.");
                Util.sendMessage(sender, ChatColor.GOLD + "(c) 2014 - 2017 by Tastybento & Poslovitch");
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return null;
            }
        });
        
        /* /is go [<1-x>] - Teleport player to his island or the specified home */
        registerArgument(new String[] {"go", "home", "h"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                // TODO check if multiple homes
                if(VaultHelper.hasPerm((Player) sender, "todo")) return new String[] {"[1-x]", plugin.getLocale(sender).islandHelpGoHomes};
                return new String[] {null, plugin.getLocale(sender).islandHelpGo};
            }
        });
        
        /* /is spawn - Teleport player to spawn */
        registerArgument(new String[] {"spawn"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpSpawn};
            }
        });
        
        /* /is create - Create an island for this player (show the schematic selection panel if enabled) */
        registerArgument(new String[] {"create", "auto"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"[schematic]", plugin.getLocale(sender).islandHelpCreate};
            }
        });
        
        /* /is info [player] - Display info about (specified) player's island*/
        registerArgument(new String[] {"info"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"[player]", plugin.getLocale(sender).islandHelpInfo};
            }
        });
        
        /* /is cp [<on/off>] - Open Control Panel or toggle it */
        registerArgument(new String[] {"controlpanel", "cp"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"[on/off]", plugin.getLocale(sender).islandHelpControlPanel};
            }
        });
        
        /* /is reset - Reset the island */
        registerArgument(new String[] {"reset", "restart"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpReset};
            }
        });
        
        /* /is sethome - Set a home where the player is located */
        registerArgument(new String[] {"sethome"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpSetHome};
            }
        });
        
        /* /is name <name> - Set island display name */
        registerArgument(new String[] {"name"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                Player player = (Player) sender;
                
                if(!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.name")){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).errorNoPermission);
                    return false;
                }
                
                if(!plugin.getIslands().hasIsland(player.getUniqueId())){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).errorNoIsland);
                    return false;
                }
                
                if(!plugin.getIslands().isOwner(player.getUniqueId())){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).errorNotLeader);
                    return false;
                }
                
                return true;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                Player player = (Player) sender;
                
                // Explain command
                if(args.length == 1){
                    Util.sendMessage(player, getHelpMessage(player, label, args[0], getHelp(sender, label)));
                    return;
                }
                
                // Naming the island
                String name = args[1];
                for(int i = 2; i < args.length; i++){
                    name += " " + args[i];
                }
                
                // Check if the name isn't too short or too long
                if(name.length() < Settings.nameMinLength){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).errorTooShort.replace("[length]", String.valueOf(Settings.nameMinLength)));
                    return;
                }
                if(name.length() > Settings.nameMaxLength){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).errorTooLong.replace("[length]", String.valueOf(Settings.nameMaxLength)));
                    return;
                }
                
                // Set the name
                if(VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.name.format")) plugin.getIslands().getIsland(player.getUniqueId()).setName(ChatColor.translateAlternateColorCodes('&', name));
                else plugin.getIslands().getIsland(player.getUniqueId()).setName(name);
                
                Util.sendMessage(player, ChatColor.GREEN + plugin.getLocale(player).generalSuccess);
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"<name>", plugin.getLocale(sender).islandHelpName};
            }
        });
        
        /* /is resetname - Reset island display name */
        registerArgument(new String[] {"resetname"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                Player player = (Player) sender;
                
                if(!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.name")){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).errorNoPermission);
                    return false;
                }
                
                if(!plugin.getIslands().hasIsland(player.getUniqueId())){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).errorNoIsland);
                    return false;
                }
                
                if(!plugin.getIslands().isOwner(player.getUniqueId())){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).errorNotLeader);
                    return false;
                }
                
                return true;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                Player player = (Player) sender;
                
                // Resets the island name
                plugin.getIslands().getIsland(player.getUniqueId()).setName(null);
                Util.sendMessage(player, ChatColor.GREEN + plugin.getLocale(player).generalSuccess);
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpResetName};
            }
        });
        
        /* /is limits - Show the (tile) entities limits */
        registerArgument(new String[] {"limits"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpLimits};
            }
        });
        
        /* /is team - Display island team info */
        registerArgument(new String[] {"team"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpTeam};
            }
        });
        
        /* /is invite <player> - Invite a player to join the island */
        registerArgument(new String[] {"invite"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"<player>", plugin.getLocale(sender).islandHelpInvite};
            }
        });
        
        /* /is uninvite <player> - Deletes the invite to join the island */
        registerArgument(new String[] {"uninvite"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"<player>", plugin.getLocale(sender).islandHelpUninvite};
            }
        });
        
        /* /is leave - Leave the island */
        registerArgument(new String[] {"leave"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpLeave};
            }
        });
        
        /* /is kick <player> - Kick the specified player from island team */
        registerArgument(new String[] {"kick"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"<player>", plugin.getLocale(sender).islandHelpKick};
            }
        });
        
        /* /is accept [player] - Accept invite */
        registerArgument(new String[] {"accept"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"[player]", plugin.getLocale(sender).islandHelpAccept};
            }
        });
        
        /* /is reject [player] - Reject invite */
        registerArgument(new String[] {"reject"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"[player]", plugin.getLocale(sender).islandHelpReject};
            }
        });
        
        /* /is makeleader <player> - Set the specified player as leader/owner of the island */
        registerArgument(new String[] {"makeleader", "transfer"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"<player>", plugin.getLocale(sender).islandHelpMakeleader};
            }
        });
        
        /* /is teamchat - Toggle TeamChat */
        registerArgument(new String[] {"teamchat", "tc"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpTeamchat};
            }
        });
        
        /* /is biomes - Change island biome */
        registerArgument(new String[] {"biomes"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpBiomes};
            }
        });
        
        /* /is expel <player> - Expel a visitor/coop from the island */
        registerArgument(new String[] {"expel"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"<player>", plugin.getLocale(sender).islandHelpExpel};
            }
        });
        
        /* /is expel - Expel every visitor/coop from the island */
        registerArgument(new String[] {"expelall", "expel!"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpExpelall};
            }
        });
        
        /* /is ban <player> - Ban a player from the island */
        registerArgument(new String[] {"ban"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"<player>", plugin.getLocale(sender).islandHelpBan};
            }
        });
        
        /* /is unban <player> - Unban player from the island */
        registerArgument(new String[] {"unban"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"<player>", plugin.getLocale(sender).islandHelpUnban};
            }
        });
        
        /* /is banlist - Display island banned players */
        registerArgument(new String[] {"banlist", "bl"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpBanlist};
            }
        });
        
        /* /is trust <player> - Trust a player */
        registerArgument(new String[] {"trust"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"<player>", plugin.getLocale(sender).islandHelpTrust};
            }
        });
        
        /* /is untrust <player> - Untrust a player */
        registerArgument(new String[] {"untrust"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"<player>", plugin.getLocale(sender).islandHelpUntrust};
            }
        });
        
        /* /is trustlist - Display trust players */
        registerArgument(new String[] {"trustlist", "tl"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpTrustlist};
            }
        });
        
        /* /is coop <player> - Coop a player */
        registerArgument(new String[] {"coop"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"<player>", plugin.getLocale(sender).islandHelpCoop};
            }
        });
        
        /* /is uncoop <player> - Uncoop a player */
        registerArgument(new String[] {"uncoop"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"<player>", plugin.getLocale(sender).islandHelpUncoop};
            }
        });
        
        /* /is cooplist - Display coop players */
        registerArgument(new String[] {"cooplist", "cl"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpCooplist};
            }
        });
        
        /* /is lock - Toggle island lock */
        registerArgument(new String[] {"lock", "unlock"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                Player player = (Player) sender;
                
                if(!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.lock")){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).errorNoPermission);
                    return false;
                }
                
                if(!plugin.getIslands().hasIsland(player.getUniqueId())){
                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).errorNoIsland);
                    return false;
                }
                
                return true;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                Player player = (Player) sender;
                Island island = plugin.getIslands().getIsland(player.getUniqueId());
                
                if(!island.isLocked()){
                    // TODO: Expel all visitors
                    // TODO: send offline messages
                    island.setLocked(false);
                } else {
                    Util.sendMessage(player, ChatColor.GREEN + plugin.getLocale(player).lockUnlocking);
                    // TODO: send offline messages
                    island.setLocked(false);
                }
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpLock};
            }
        });
        
        /* /is settings - Display Settings menu */
        registerArgument(new String[] {"settings"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).islandHelpSettings};
            }
        });
        
        /* /is language <id> - Set the language */
        registerArgument(new String[] {"language", "lang"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"<id>", plugin.getLocale(sender).islandHelpLanguage};
            }
        });
    }

}
