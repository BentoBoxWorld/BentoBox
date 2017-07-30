package us.tastybento.bskyblock.commands;

import org.bukkit.command.CommandSender;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.AbstractCommand;
import us.tastybento.bskyblock.config.Settings;

import java.util.List;

public class AdminCommand extends AbstractCommand {
    
    BSkyBlock plugin;

    public AdminCommand(BSkyBlock plugin) {
        super(Settings.ADMINCOMMAND, true);
        plugin.getCommand(Settings.ADMINCOMMAND).setExecutor(this);
        plugin.getCommand(Settings.ADMINCOMMAND).setTabCompleter(this);
        this.plugin = plugin;
    }

    @Override
    public void setup() {
        /* /asadmin delete <name> - delete name's island */
        addArgument(new String[] {"delete"}, new ArgumentHandler() {

            @Override
            public boolean canUse(CommandSender sender) {
                // TODO Auto-generated method stub
                return true;
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
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
    public boolean canUse(CommandSender sender) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        
    }

}
