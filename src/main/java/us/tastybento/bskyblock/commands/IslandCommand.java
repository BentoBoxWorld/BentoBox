package us.tastybento.bskyblock.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.schematics.Schematic;
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
        super(plugin, true);
        this.plugin = plugin;
    }

    @Override
    public boolean canExecute(CommandSender sender, String label) {
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
                return true;
            }

            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                if (sender instanceof Player) {
                    Player player = (Player)sender;
                    if (plugin.getIslands().hasIsland(player.getUniqueId())) {
                        plugin.getIslands().homeTeleport(player);
                    }
                }
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender, String label){
                // TODO check if multiple homes
                if(VaultHelper.hasPerm((Player) sender, "todo")) return new String[] {"[1-x]", plugin.getLocale(sender).get("help.island.go-homes")};
                return new String[] {null, plugin.getLocale(sender).get("help.island.go")};
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
                return new String[] {null, plugin.getLocale(sender).get("help.island.spawn")};
            }
        });

        /* /is create - Create an island for this player (show the schematic selection panel if enabled) */
        registerArgument(new String[] {"create", "auto"}, new CommandArgumentHandler() {

            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return true;
            }

            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                if (sender instanceof Player) {
                    Player player = (Player)sender;
                    if (!plugin.getIslands().hasIsland(player.getUniqueId())) {
                        Schematic schematic = plugin.getSchematics().getSchematic("default");
                        plugin.getIslands().newIsland(player, schematic);
                    } else {
                        Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player).get("island.error.YouAlreadyHaveAnIsland"));
                    }
                }
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"[schematic]", plugin.getLocale(sender).get("help.island.create")};
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
                return new String[] {"[player]", plugin.getLocale(sender).get("help.island.info")};
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
                return new String[] {"[on/off]", plugin.getLocale(sender).get("help.island.control-panel")};
            }
        });

        /* /is reset - Reset the island */
        registerArgument(new String[] {"reset", "restart"}, new CommandArgumentHandler() {

            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return true;
            }

            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
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
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).get("help.island.reset")};
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
                return new String[] {null, plugin.getLocale(sender).get("help.island.sethome")};
            }
        });

        /* /is name <name> - Set island display name */
        registerArgument(new String[] {"name"}, new CommandArgumentHandler() {

            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
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
            public void onExecute(CommandSender sender, String label, String[] args) {
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
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {"<name>", plugin.getLocale(sender).get("help.island.name")};
            }
        });

        /* /is resetname - Reset island display name */
        registerArgument(new String[] {"resetname"}, new CommandArgumentHandler() {

            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
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
            public void onExecute(CommandSender sender, String label, String[] args) {
                Player player = (Player) sender;

                // Resets the island name
                plugin.getIslands().getIsland(player.getUniqueId()).setName(null);
                Util.sendMessage(player, ChatColor.GREEN + plugin.getLocale(player).get("general.success"));
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).get("help.island.resetname")};
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
                return new String[] {null, plugin.getLocale(sender).get("help.island.limits")};
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
                return new String[] {null, plugin.getLocale(sender).get("help.island.team")};
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
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.invite")};
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
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.uninvite")};
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
                return new String[] {null, plugin.getLocale(sender).get("help.island.leave")};
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
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.kick")};
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
                return new String[] {"[player]", plugin.getLocale(sender).get("help.island.accept")};
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
                return new String[] {"[player]", plugin.getLocale(sender).get("help.island.reject")};
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
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.makeleader")};
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
                return new String[] {null, plugin.getLocale(sender).get("help.island.teamchat")};
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
                return new String[] {null, plugin.getLocale(sender).get("help.island.biomes")};
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
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.expel")};
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
                return new String[] {null, plugin.getLocale(sender).get("help.island.expelall")};
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
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.ban")};
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
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.unban")};
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
                return new String[] {null, plugin.getLocale(sender).get("help.island.banlist")};
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
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.trust")};
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
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.untrust")};
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
                return new String[] {null, plugin.getLocale(sender).get("help.island.trustlist")};
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
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.coop")};
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
                return new String[] {"<player>", plugin.getLocale(sender).get("help.island.uncoop")};
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
                return new String[] {null, plugin.getLocale(sender).get("help.island.cooplist")};
            }
        });

        /* /is lock - Toggle island lock */
        registerArgument(new String[] {"lock", "unlock"}, new CommandArgumentHandler() {

            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
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
            public void onExecute(CommandSender sender, String label, String[] args) {
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
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).get("help.island.lock")};
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
                return new String[] {null, plugin.getLocale(sender).get("help.island.settings")};
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
                return new String[] {"<id>", plugin.getLocale(sender).get("help.island.language")};
            }
        });
    }

}
