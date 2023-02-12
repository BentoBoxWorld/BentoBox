package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Mode;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.builders.TabbedPanelBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.panels.settings.SettingsTab;
import world.bentobox.bentobox.panels.settings.WorldDefaultSettingsTab;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 * @since 1.6.0
 */
public class AdminSettingsCommand extends CompositeCommand {

    private static final String SPAWN_ISLAND = "spawn-island";
    private List<String> protectionFlagNames;
    private Island island;
    private List<String> settingFlagNames;
    private List<String> worldSettingFlagNames;
    private @NonNull Optional<Flag> flag = Optional.empty();
    private boolean activeState;
    private int rank;
    private final GameModeAddon gameMode;

    public AdminSettingsCommand(CompositeCommand islandCommand) {
        super(islandCommand, "settings", "flags", "options");
        gameMode = getPlugin().getIWM().getAddon(getWorld()).orElse(null);
    }

    private void makeLists() {
        protectionFlagNames = getPlugin().getFlagsManager().getFlags().stream()
                .filter(f -> f.getType().equals(Type.PROTECTION))
                .filter(f -> f.getGameModes().isEmpty() || gameMode == null || f.getGameModes().contains(gameMode))
                .map(Flag::getID)
                .toList();
        settingFlagNames = getPlugin().getFlagsManager().getFlags().stream()
                .filter(f -> f.getType().equals(Type.SETTING))
                .filter(f -> f.getGameModes().isEmpty() || gameMode == null || f.getGameModes().contains(gameMode))
                .map(Flag::getID)
                .toList();
        worldSettingFlagNames = getPlugin().getFlagsManager().getFlags().stream()
                .filter(f -> f.getType().equals(Type.WORLD_SETTING))
                .filter(f -> f.getGameModes().isEmpty() || gameMode == null || f.getGameModes().contains(gameMode))
                .map(Flag::getID)
                .toList();
    }

    @Override
    public void setup() {
        setPermission("admin.settings");
        setParametersHelp("commands.admin.settings.parameters");
        setDescription("commands.admin.settings.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            // World settings
            return true;
        }
        if (args.size() > 1) {
            // Command
            return checkSyntax(user, args);
        }
        return getIsland(user, args);
    }

    private boolean getIsland(User user, List<String> args) {
        if (args.get(0).equalsIgnoreCase(SPAWN_ISLAND) && getIslands().getSpawn(getWorld()).isPresent()) {
            island = getIslands().getSpawn(getWorld()).get();
            return true;
        }
        // Get target player
        @Nullable UUID targetUUID = Util.getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        island = getIslands().getIsland(getWorld(), targetUUID);
        if (island == null || !getPlugin().getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        return true;
    }

    /**
     * Check that this command is correct to set a setting
     * @param user - user
     * @param args - args
     * @return true if the syntax is correct
     */
    private boolean checkSyntax(User user, List<String> args) {
        // Update the flag lists
        this.makeLists();
        if (args.size() == 2) {
            // Should be a world setting
            // If world settings, then active/disabled, otherwise player flags
            if (worldSettingFlagNames.contains(args.get(0).toUpperCase(Locale.ENGLISH))) {
                if (checkActiveDisabled(user, args.get(1))) {
                    flag = getPlugin().getFlagsManager().getFlag(args.get(0).toUpperCase(Locale.ENGLISH));
                    return true;
                }
            } else {
                this.showHelp(this, user);
                return false;
            }
        } else if (args.size() > 2) {
            // Get island
            if (!getIsland(user, args)) {
                return false;
            }

            if (!settingFlagNames.contains(args.get(1).toUpperCase(Locale.ENGLISH))
                    && !protectionFlagNames.contains(args.get(1).toUpperCase(Locale.ENGLISH))) {
                user.sendMessage("commands.admin.settings.unknown-flag", TextVariables.NAME, args.get(2));
                return false;
            }
            // Set flag
            flag = getPlugin().getFlagsManager().getFlag(args.get(1).toUpperCase(Locale.ENGLISH));
            // Check settings
            if (flag.isPresent()) {
                if (flag.get().getType().equals(Type.SETTING)) {
                    return checkActiveDisabled(user, args.get(2));
                } else {
                    // Protection flag
                    return checkRank(user, String.join(" ", args.subList(2, args.size())));
                }
            }
        }
        return false;
    }


    /**
     * Check the rank given.
     * @param user - user
     * @param string - the rank given in the command line
     * @return true if rank is valid
     */
    private boolean checkRank(User user, String string) {
        for (Entry<String, Integer> en : getPlugin().getRanksManager().getRanks().entrySet()) {
            if (en.getValue() > RanksManager.BANNED_RANK && en.getValue() <= RanksManager.OWNER_RANK
                    && string.equalsIgnoreCase(ChatColor.stripColor(user.getTranslation(en.getKey())))) {
                // We have a winner
                rank = en.getValue();
                return true;
            }
        }
        user.sendMessage("commands.admin.setrank.unknown-rank");
        return false;
    }

    private boolean checkActiveDisabled(User user, String string) {
        String active = ChatColor.stripColor(user.getTranslation("protection.panel.flag-item.setting-active"));
        String disabled = ChatColor.stripColor(user.getTranslation("protection.panel.flag-item.setting-disabled"));
        if (!string.equalsIgnoreCase(active) && !string.equalsIgnoreCase(disabled)) {
            user.sendMessage("commands.admin.settings.unknown-setting", TextVariables.NAME, string);
            return false;
        }
        activeState = string.equalsIgnoreCase(active);
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() > 1) {
            // Command line setting
            flag.ifPresent(f -> {
                switch (f.getType()) {
                case PROTECTION -> {
                    island.setFlag(f, rank);
                    getIslands().save(island);
                }
                case SETTING -> {
                    island.setSettingsFlag(f, activeState);
                    getIslands().save(island);
                }
                case WORLD_SETTING -> f.setSetting(getWorld(), activeState);
                default -> {
                    // Do nothing
                }
                }
            });
            user.sendMessage("general.success");
            return true;
        }
        // GUI requires in-game
        if (!user.isPlayer()) {
            user.sendMessage("general.errors.use-in-game");
            return false;
        }
        getPlayers().setFlagsDisplayMode(user.getUniqueId(), Mode.EXPERT);
        if (args.isEmpty()) {
            new TabbedPanelBuilder()
            .user(user)
            .world(getWorld())
            .tab(1, new SettingsTab(getWorld(), user, Flag.Type.WORLD_SETTING))
            .tab(2, new WorldDefaultSettingsTab(getWorld(), user))
            .startingSlot(1)
            .size(54)
            .build().openPanel();
            return true;
        }
        // Player settings
        new TabbedPanelBuilder()
        .user(user)
        .world(island.getWorld())
        .tab(1, new SettingsTab(user, island, Flag.Type.PROTECTION))
        .tab(2, new SettingsTab(user, island, Flag.Type.SETTING))
        .startingSlot(1)
        .size(54)
        .build().openPanel();
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        // Update with the latest lists
        this.makeLists();
        String active = ChatColor.stripColor(user.getTranslation("protection.panel.flag-item.setting-active"));
        String disabled = ChatColor.stripColor(user.getTranslation("protection.panel.flag-item.setting-disabled"));
        List<String> options = new ArrayList<>();
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        if (args.size() == 2) {
            // Player names or world settings
            options = Util.tabLimit(Util.getOnlinePlayerList(user), lastArg);
            options.addAll(worldSettingFlagNames);
            if (getIslands().getSpawn(getWorld()).isPresent()) {
                options.add(SPAWN_ISLAND);
            }
        } else if (args.size() == 3) {
            // If world settings, then active/disabled, otherwise player flags
            if (worldSettingFlagNames.contains(args.get(1).toUpperCase(Locale.ENGLISH))) {
                options = Arrays.asList(active, disabled);
            } else {
                // Flag IDs
                options.addAll(protectionFlagNames);
                options.addAll(settingFlagNames);
            }
        } else if (args.size() == 4) {
            // Get flag in previous argument
            options = getPlugin().getFlagsManager().getFlag(args.get(2).toUpperCase(Locale.ENGLISH)).map(f -> switch (f.getType()) {
            case PROTECTION -> getPlugin().getRanksManager()
            .getRanks().entrySet().stream()
            .filter(en -> en.getValue() > RanksManager.BANNED_RANK && en.getValue() <= RanksManager.OWNER_RANK)
            .map(Entry::getKey)
            .map(user::getTranslation).toList();
            case SETTING -> Arrays.asList(active, disabled);
            default -> Collections.<String>emptyList();
            }).orElse(Collections.emptyList());
        }
        return Optional.of(Util.tabLimit(options, lastArg));

    }
}
