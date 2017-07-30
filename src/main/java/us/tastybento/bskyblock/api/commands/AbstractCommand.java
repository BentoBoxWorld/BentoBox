package us.tastybento.bskyblock.api.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Poslovitch
 */
public abstract class AbstractCommand implements CommandExecutor, TabCompleter {

    private Map<String, ArgumentHandler> argumentsMap;
    private Map<String, String> aliasesMap;

    private final String name;

    private final boolean help;
    private static final int MAX_PER_PAGE = 7;

    protected AbstractCommand(String name, boolean help) {
        this.name = name;
        this.help = help;

        // Register the help argument if needed
        if (help) {

        }

        // Register the other arguments
        setup();
    }

    /**
     *
     */
    public interface ArgumentHandler {
        boolean canUse(CommandSender sender);
        void execute(CommandSender sender, String[] args);
        List<String> tabComplete(CommandSender sender, String[] args);
        String[] getHelp(CommandSender sender);
    }

    public abstract void setup();

    public abstract boolean canUse(CommandSender sender);
    public abstract void execute(CommandSender sender, String[] args);

    public void addArgument(String[] names, ArgumentHandler handler) {

    }

    public ArgumentHandler getHandler(String argument) {
        if (isAlias(argument)) return argumentsMap.get(getParent(argument));
        else return argumentsMap.get(argument);
    }

    public void setHandler(String argument, ArgumentHandler handler) {
        if (argumentsMap.containsKey(argument)) argumentsMap.put(argument, handler);
    }

    public boolean isAlias(String argument) {
        if (aliasesMap.containsValue(argument)) return true;
        return false;
    }

    public void addAliases(String parent, String... aliases) {
        if (argumentsMap.containsKey(parent)) {
            for (String alias : aliases) {
                if (!aliasesMap.containsKey(alias) && !aliasesMap.containsValue(alias)) aliasesMap.put(parent, alias);
            }
        }
    }

    public void removeAliases(String... aliases) {
        for (String alias : aliases) {
            if (aliasesMap.containsValue(alias)) aliasesMap.remove(getParent(alias));
        }
    }

    public String getParent(String alias) {
        if (isAlias(alias)) {
            for(String parent : aliasesMap.keySet()) {
                if (aliasesMap.get(parent).equals(alias)) return parent;
            }
            return null;
        }
        else return alias;
    }

    public List<String> getAliases(String argument) {
        return null; //TODO
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (this.canUse(sender)) {
            if(args.length >= 1) {
                ArgumentHandler handler = getHandler(args[0]); // Store the handler to save some calculations
                if (handler != null && handler.canUse(sender)) {
                    handler.execute(sender, args);
                } else if (help) {
                    argumentsMap.get("help").execute(sender, args);
                } else {
                    this.execute(sender, args);
                }
            } else {
                this.execute(sender, args);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args){
        List<String> options = new ArrayList<String>();
        if (canUse(sender)) {
            if (args.length <= 1) {
                // Go through every argument, check if player can use it and if so, add it in tab options
                for(String argument : argumentsMap.keySet()) {
                    if (getHandler(argument).canUse(sender)) options.add(argument);
                }
            } else {
                // If player can execute the argument, get its tab-completer options
                ArgumentHandler handler = getHandler(args[0]);
                if (handler != null && handler.canUse(sender)) {
                    List<String> tabOptions = handler.tabComplete(sender, args);
                    if (tabOptions != null) options.addAll(tabOptions);
                }
            }
        }
        return options;
    }
}
