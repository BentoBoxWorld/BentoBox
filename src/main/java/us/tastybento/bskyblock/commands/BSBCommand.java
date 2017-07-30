package us.tastybento.bskyblock.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import us.tastybento.bskyblock.BSkyBlock;

import java.util.*;
import java.util.Map.Entry;

/**
 * Abstract class that handles commands and tab completion.
 *
 * It makes the commands code modular and allow addons to add their own arguments or even remove/edit existing ones.
 * 
 * @author Poslovitch
 */
public abstract class BSBCommand implements CommandExecutor, TabCompleter{    
    private Map<String, CommandArgumentHandler> arguments;

    private String command;

    /** Whether the command has an help list. */
    private boolean help = true;
    /** Max subcommands per help page. */
    private static final int MAX_PER_PAGE = 7; // 10 seems to be the maximum acceptable, 7 is a good number.

    protected BSBCommand(BSkyBlock plugin, String command, boolean help){
        this.command = command;

        arguments = new HashMap<String, CommandArgumentHandler>();
        this.help = help;

        // Register the help argument if needed
        if(help) {
            registerArgument(new String[] {"help", "?"}, new CommandArgumentHandler() {

                @Override
                public boolean canExecute(CommandSender sender, String[] args) {
                    return true; // If the player can execute the command, he can receive help
                }

                @Override
                public void onExecute(CommandSender sender, String[] args) {
                    sender.sendMessage(plugin.getLocale(sender).get("commands." + command + ".help-header"));
                }

                @Override
                public List<String> onTabComplete(CommandSender sender, String[] args) {
                    return null; // Doesn't have options for tab-completion
                }

                @Override
                public String[] getHelp(CommandSender sender) {
                    return null; // Obviously, don't send any help message.
                }

            });
        }

        // Register other arguments
        setup();
    }

    /**
     * Registers the command-specific arguments.
     *
     * This method is called when BSBCommand has been successfully constructed.
     */
    public abstract void setup();

    /**
     * Asks if the sender can use the command
     *
     * @param sender
     * @return if the sender can use the command
     */
    public abstract boolean canExecute(CommandSender sender);

    /**
     * This code is executed when no arguments is specified for the command
     * @param sender
     * @param args
     */
    public abstract void onExecuteDefault(CommandSender sender, String[] args);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(this.canExecute(sender)){
            if(args.length >= 1){
                if(arguments.containsKey(args[0]) && arguments.get(args[0]).canExecute(sender, args)){
                    arguments.get(args[0]).onExecute(sender, args);
                } else if(help) {
                    arguments.get("?").onExecute(sender, args);
                } else {
                    this.onExecuteDefault(sender, args);
                }
            } else {
                this.onExecuteDefault(sender, args);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args){
        List<String> options = new ArrayList<String>();
        if(this.canExecute(sender)){
            if(args.length <= 1){
                // Go through every argument, check if player can use it and if so, add it in tab options
                for(Entry<String, CommandArgumentHandler> entry : arguments.entrySet()){
                    if(entry.getValue().canExecute(sender, args)){
                        options.add(entry.getKey());
                    }
                }
            } else {
                // If player can execute the argument, get its tab-completer options
                if(getArgumentHandler(args[0]) != null && getArgumentHandler(args[0]).canExecute(sender, args)
                        && getArgumentHandler(args[0]).onTabComplete(sender, args) != null){
                    options.addAll(getArgumentHandler(args[0]).onTabComplete(sender, args));
                }
            }
        }
        return options;
    }

    /**
     * Defines the behavior of an argument and its aliases.
     */
    public abstract class CommandArgumentHandler{
        /**
         * Check if the sender can use the argument
         * @param sender
         * @param args
         * @return if the sender can use the argument
         */
        public abstract boolean canExecute(CommandSender sender, String[] args);

        /**
         * Code to execute for this argument
         * @param sender
         * @param args
         */
        public abstract void onExecute(CommandSender sender, String[] args);

        /**
         * Request a list of tab-completion options with the argument
         * @param sender
         * @param args
         * @return the list of options
         */
        public abstract List<String> onTabComplete(CommandSender sender, String[] args);

        /**
         * Get help information
         * <code>new String[] {arguments, description};</code>
         * @param sender
         * @return the help information
         */
        public abstract String[] getHelp(CommandSender sender);
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
}
