package us.tastybento.bskyblock.api.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.BSBLocale;
import us.tastybento.bskyblock.database.managers.PlayersManager;
import us.tastybento.bskyblock.database.managers.island.IslandsManager;

public abstract class CommandArgument {

    public BSkyBlock plugin = BSkyBlock.getPlugin();
    private String label;
    private List<String> aliases;
    private Map<String, CommandArgument> subCommands;

    public CommandArgument(String label, String... aliases) {
        this.label = label;
        this.aliases = new ArrayList<>(Arrays.asList(aliases));
        this.subCommands = new LinkedHashMap<>();
    }

    public CommandArgument() {}

    public abstract boolean execute(CommandSender sender, String[] args);
    public abstract Set<String> tabComplete(CommandSender sender, String[] args);

    public String getLabel() {
        return label;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public boolean hasSubCommmands() {
        return !subCommands.isEmpty();
    }

    public Map<String, CommandArgument> getSubCommands() {
        return subCommands;
    }

    public CommandArgument getSubCommand(String label) {
        for (Map.Entry<String, CommandArgument> entry : subCommands.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(label)) return subCommands.get(label);
            else if (entry.getValue().getAliases().contains(label)) return subCommands.get(entry.getValue().getLabel());
        }
        return null;
    }

    public void addSubCommand(CommandArgument subCommand) {
        subCommands.putIfAbsent(subCommand.getLabel(), subCommand);
    }

    public void removeSubCommand(String label) {
        subCommands.remove(label);
    }

    public void replaceSubCommand(CommandArgument subCommand) {
        subCommands.put(subCommand.getLabel(), subCommand);
    }
    
    // These methods below just neaten up the code in the commands so "plugin." isn't always used
    
    /**
     * @param sender
     * @return true if sender is a player
     */
    protected boolean isPlayer(CommandSender sender) {
        return (sender instanceof Player);
    }
    
    /**
     * @param player
     * @return true if player is in a team
     */
    protected boolean inTeam(Player player) {
        return plugin.getPlayers().inTeam(player.getUniqueId());
    }
    
    /**
     * @param player
     * @return UUID of player's team leader
     */
    protected UUID getTeamLeader(Player player) {
        return plugin.getIslands().getTeamLeader(player.getUniqueId());
    }
    
    /**
     * @param player
     * @return set of UUIDs of all team members
     */
    protected Set<UUID> getMembers(Player player) {
        return plugin.getIslands().getMembers(player.getUniqueId());
    }
    
    /**
     * @return PlayersManager
     */
    protected PlayersManager getPlayers() {
        return plugin.getPlayers();
    }
    /**
     * @return IslandsManager
     */
    protected IslandsManager getIslands() {
        return plugin.getIslands();
    }
    /**
     * @param sender
     * @return Locale for sender
     */
    protected BSBLocale getLocale(CommandSender sender) {
        return plugin.getLocale(sender);
    }
    /**
     * @param uuid
     * @return Locale for UUID
     */
    protected BSBLocale getLocale(UUID uuid) {
        return plugin.getLocale(uuid);
    }
}
