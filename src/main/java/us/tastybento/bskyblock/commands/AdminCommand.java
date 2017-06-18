package us.tastybento.bskyblock.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.util.VaultHelper;

public class AdminCommand extends BSBCommand{
    
    BSkyBlock plugin;

    public AdminCommand(BSkyBlock plugin) {
        super(plugin, true);
    }

    @Override
    public void setup() {
        /* /asadmin delete <name> - delete name's island */
        registerArgument(new String[] {"delete"}, new CommandArgumentHandler() {

            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return true;
            }

            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {

            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String[] getHelp(CommandSender sender, String label){
                return new String[] {null, plugin.getLocale(sender).get("help.admin.delete")};
            }
        });
        
    }

    @Override
    public boolean canExecute(CommandSender sender, String label) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void onExecuteDefault(CommandSender sender, String label, String[] args) {

        
    }

}
