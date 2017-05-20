package us.tastybento.bskyblock.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.util.Util;

/**
 * Abstract class that handles commands and tabs. It makes the commands code modular
 * and allow addons to add their own arguments.
 * 
 * @author Poslovitch
 */
public abstract class ASBCommand implements CommandExecutor, TabCompleter{    
    private Map<String, CommandArgumentHandler> arguments;
        
    protected ASBCommand(BSkyBlock plugin){
        arguments = new HashMap<String, CommandArgumentHandler>();
        
        // Automatically register the help argument
        registerArgument(new String[] {"help", "?"}, new CommandArgumentHandler() {
            
            @Override
            public boolean canExecute(CommandSender sender, String label, String[] args) {
                return true; // If the player can execute the command, he can receive help
            }
            
            @Override
            public void onExecute(CommandSender sender, String label, String[] args) {
                // Generate help
                String help = plugin.getLocale(sender).helpHeader + "\n";
                
                for(String argument : arguments.keySet()){
                    CommandArgumentHandler cah = getArgumentHandler(argument);
                    if(cah.canExecute(sender, label, args) && cah.getHelp(sender, label) != null) {
                        help += getHelpMessage(sender, label, argument, cah.getHelp(sender, label)) + "\n";
                    }
                }
                
                //TODO: multiple pages
                
                Util.sendMessage(sender, help);
            }
            
            @Override
            public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
                return null; // Doesn't have options for tab-completion
            }
            
            @Override
            public String[] getHelp(CommandSender sender, String label) {
                return null; // Obviously, don't send any help message.
            }
            
        });
        
        // Register other arguments
        setup();
    }
    
    /**
     * Setup the command arguments
     */
    public abstract void setup();
    
    /**
     * Check if the sender can use the command
     * @param sender
     * @param label
     * @return if the sender can use the command
     */
    public abstract boolean canExecute(CommandSender sender, String label);
    
    /**
     * This code is executed when no arguments is specified for the command
     * @param sender
     * @param label
     * @param args
     */
    public abstract void onExecuteDefault(CommandSender sender, String label, String[] args);
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(this.canExecute(sender, label)){
            if(args.length >= 1){
                if(arguments.get(args[0]) != null){
                    if(arguments.get(args[0]).canExecute(sender, label, args)){
                        arguments.get(args[0]).onExecute(sender, label, args);
                    }
                } else {
                    arguments.get("help").onExecute(sender, label, args);
                }
            } else {
                this.onExecuteDefault(sender, label, args);
            }
        }
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args){
        List<String> options = new ArrayList<String>();
        if(this.canExecute(sender, label)){
            if(args.length <= 1){
                // Go through every argument, check if player can use it and if so, add it in tab options
                for(Entry<String, CommandArgumentHandler> entry : arguments.entrySet()){
                    if(entry.getValue().canExecute(sender, label, args)){
                        options.add(entry.getKey());
                    }
                }
            } else {
                // If player can execute the argument, get its tab-completer options
                if(getArgumentHandler(args[0]) != null && getArgumentHandler(args[0]).canExecute(sender, label, args)
                        && getArgumentHandler(args[0]).onTabComplete(sender, label, args) != null){
                    options.addAll(getArgumentHandler(args[0]).onTabComplete(sender, label, args));
                }
            }
        }
        return options;
    }
    
    public abstract class CommandArgumentHandler{
        /**
         * Check if the sender can use the argument
         * @param sender
         * @param label
         * @param args
         * @return if the sender can use the argument
         */
        public abstract boolean canExecute(CommandSender sender, String label, String[] args);
        
        /**
         * Code to execute for this argument
         * @param sender
         * @param label
         * @param args
         */
        public abstract void onExecute(CommandSender sender, String label, String[] args);
        
        /**
         * Request a list of tab-completion options with the argument
         * @param sender
         * @param label
         * @param args
         * @return the list of options
         */
        public abstract List<String> onTabComplete(CommandSender sender, String label, String[] args);
        
        /**
         * Get help information
         * <code>new String[] {arguments, description};</code>
         * @param sender
         * @param label
         * @return the help information
         */
        public abstract String[] getHelp(CommandSender sender, String label);
    }
    
    public void registerArgument(String[] args, CommandArgumentHandler handler){
        Arrays.asList(args).forEach(arg -> arguments.put(arg, handler));
    }
    
    public Map<String, CommandArgumentHandler> getArguments(){
        return arguments;
    }
    
    public CommandArgumentHandler getArgumentHandler(String argument){
        return arguments.get(argument);
    }
    
    public String getHelpMessage(CommandSender sender, String label, String argument, String[] helpData){
        return ""; //TODO help
    }
}
