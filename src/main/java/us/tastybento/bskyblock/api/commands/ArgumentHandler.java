package us.tastybento.bskyblock.api.commands;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.BSkyBlock;

/**
 *
 */
public abstract class ArgumentHandler {
    private BSkyBlock plugin;
    public Set<String> aliasSet;
    public final String label;

    public abstract CanUseResp canUse(CommandSender sender);
    public abstract void execute(CommandSender sender, String[] args);
    public abstract Set<String> tabComplete(CommandSender sender, String[] args);
    public abstract String[] usage(CommandSender sender);

    public ArgumentHandler(String label) {
        super();
        this.plugin = BSkyBlock.getPlugin();
        this.label = label; // The original command this is relating to, e.g., /island
        this.aliasSet = new HashSet<>(); // The sub-command and aliases, if any. The first one in the list is the main sub-command
    }

    public String getShortDescription(CommandSender sender) {
        // syntax: "  &7/&b[label] &c[command] &a[args] &7: &e[info]"
        String msg = plugin.getLocale(sender).get("help.syntax");
        msg = msg.replace("[label]", label);

        String cmds = "";
        for(String alias : aliasSet) {
            if (cmds.isEmpty()) {
                cmds = alias;
            } else {
                cmds += plugin.getLocale(sender).get("help.syntax-alias-separator") + alias;
            }
        }

        msg = msg.replace("[command]", cmds);

        String[] usage = usage(sender);
        if (usage == null) usage = new String[2];

        msg = msg.replace("[args]", (usage[0] != null) ? usage[0] : "")
                .replace("[info]", (usage[1] != null) ? usage[1] : "");

        return msg;
    }

    public Set<String> getAliases() {
        return aliasSet;
    }

    public ArgumentHandler alias(String alias) {
        aliasSet.add(alias);
        return this;
    }

}

