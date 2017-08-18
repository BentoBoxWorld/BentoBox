package us.tastybento.bskyblock.commands;

import java.util.Set;

import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.AbstractCommand;
import us.tastybento.bskyblock.config.Settings;

public class AdminCommand extends AbstractCommand {

    BSkyBlock plugin;

    public AdminCommand(BSkyBlock plugin) {
        super(plugin, Settings.ADMINCOMMAND, new String[0], true);
        plugin.getCommand(Settings.ADMINCOMMAND).setExecutor(this);
        plugin.getCommand(Settings.ADMINCOMMAND).setTabCompleter(this);
        this.plugin = plugin;
    }

    @Override
    public void setup() {
        /* /asadmin delete <name> - delete name's island */
        addArgument(new String[] {"delete"}, new ArgumentHandler() {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {

            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender){
                return new String[] {null, plugin.getLocale(sender).get("help.admin.delete")};
            }
        });

    }

    @Override
    public CanUseResp canUse(CommandSender sender) {
        return new CanUseResp(true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {


    }

}
