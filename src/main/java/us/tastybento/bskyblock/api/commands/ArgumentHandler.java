package us.tastybento.bskyblock.api.commands;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.BSkyBlock;

/**
 *
 */
public abstract class ArgumentHandler {
    private BSkyBlock plugin;
    private Map<String, ArgumentHandler> argumentsMap;
    private Map<String, String> aliasesMap;
    public final String[] aliases;
    public final String label;

    public abstract CanUseResp canUse(CommandSender sender);
    public abstract void execute(CommandSender sender, String[] args);
    public abstract Set<String> tabComplete(CommandSender sender, String[] args);
    public abstract String[] usage(CommandSender sender);

    public ArgumentHandler(BSkyBlock plugin, String label, String[] aliases, Map<String, ArgumentHandler> argumentsMap) {
        super();
        this.plugin = plugin;
        this.argumentsMap = new LinkedHashMap<>();
        this.aliasesMap = new HashMap<>();
        this.label = label;
        this.aliases = aliases;
        this.argumentsMap = argumentsMap;
    }

    public String getShortDescription(CommandSender sender) {
        String msg = BSkyBlock.getPlugin().getLocale(sender).get("help.syntax");
        msg = msg.replace("[label]", (aliases[0] != null) ? aliases[0] : label);

        String command = "";
        for(Map.Entry<String, ArgumentHandler> entry : argumentsMap.entrySet()) {
            if (entry.getValue().equals(this)) {
                command = entry.getKey();
                break;
            }
        }

        String cmds = command;
        for(String alias : getAliases(command)) {
            cmds += plugin.getLocale(sender).get("help.syntax-alias-separator") + alias;
        }

        msg = msg.replace("[command]", cmds);

        String[] usage = argumentsMap.get(command).usage(sender);
        if (usage == null) usage = new String[2];

        msg = msg.replace("[args]", (usage[0] != null) ? usage[0] : "")
                .replace("[info]", (usage[1] != null) ? usage[1] : "");

        return msg;
    }
    
    public Set<String> getAliases(String argument) {
        Set<String> aliases = new HashSet<>();

        for (Map.Entry<String, String> entry : aliasesMap.entrySet()) {
            if (entry.getKey().equals(argument)) aliases.add(entry.getValue());
        }

        return aliases;
    }
}