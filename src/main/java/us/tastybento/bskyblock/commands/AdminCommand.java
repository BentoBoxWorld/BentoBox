package us.tastybento.bskyblock.commands;

import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.BSkyBlock;

public class AdminCommand extends BSBCommand{

    public AdminCommand(BSkyBlock plugin) {
        super(plugin, true);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void setup() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean canExecute(CommandSender sender, String label) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onExecuteDefault(CommandSender sender, String label, String[] args) {
        // TODO Auto-generated method stub
        
    }

}
