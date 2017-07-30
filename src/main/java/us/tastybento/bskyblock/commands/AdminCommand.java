package us.tastybento.bskyblock.commands;

import org.bukkit.command.CommandSender;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;

import java.util.List;

public class AdminCommand extends BSBCommand{
    
    BSkyBlock plugin;

    public AdminCommand(BSkyBlock plugin) {
        super(plugin, Settings.ADMINCOMMAND, true);
        plugin.getCommand(Settings.ADMINCOMMAND).setExecutor(this);
        plugin.getCommand(Settings.ADMINCOMMAND).setTabCompleter(this);
        this.plugin = plugin;
    }

    @Override
    public void setup() {
        /* /asadmin delete <name> - delete name's island */
        registerArgument(new String[] {"delete"}, new CommandArgumentHandler() {

            @Override
            public boolean canExecute(CommandSender sender, String[] args) {
                // TODO Auto-generated method stub
                return true;
            }

            @Override
            public void onExecute(CommandSender sender, String[] args) {

            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.admin.delete")};
            }
        });
        
    }

    @Override
    public boolean canExecute(CommandSender sender) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void onExecuteDefault(CommandSender sender, String[] args) {

        
    }

}
