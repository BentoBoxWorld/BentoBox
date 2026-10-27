package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.builders.TabbedPanelBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.panels.customizable.SettingsPanel;
import world.bentobox.bentobox.panels.settings.IslandDefaultSettingsTab;
import world.bentobox.bentobox.panels.settings.SettingsTab;
import world.bentobox.bentobox.panels.settings.WorldDefaultSettingsTab;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 * @since 1.6.0
 */
public class AdminSettingsCommand extends CompositeCommand {

    private static final String SPAWN_ISLAND = "spawn-island";
    private record SettingChange(Island island, Flag flag, int value) { }
    private final GameModeAddon gameMode;

    public AdminSettingsCommand(CompositeCommand islandCommand) {
        super(islandCommand, "settings", "flags", "options");
        gameMode = getPlugin().getIWM().getAddon(getWorld()).orElse(null);
    }

    private List<String> flagNames(Type type) {
        return getPlugin().getFlagsManager().getFlags().stream()
                .filter(f -> f.getType().equals(type))
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
        return resolveChange(user, args) != null;
    }

    private @Nullable SettingChange resolveChange(User user, List<String> args) {
        if (args.isEmpty()) {
            // World settings
            return new SettingChange(null, null, 0);
        }
        if (args.size() == 1) {
            Island island = getIsland(user, args);
            return island == null ? null : new SettingChange(island, null, 0);
        }
        return args.size() == 2 ? checkWorldSetting(user, args) : checkIslandSetting(user, args);
    }

    private @Nullable Island getIsland(User user, List<String> args) {
        if (args.getFirst().equalsIgnoreCase(SPAWN_ISLAND)) {
            Optional<Island> spawn = getIslands().getSpawn(getWorld());
            if (spawn.isPresent()) return spawn.get();
        }
        // Get target player
        @Nullable UUID targetUUID = Util.getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return null;
        }
        Island island = getOwnedIsland(targetUUID);
        if (island == null) {
            user.sendMessage("general.errors.player-has-no-island");
            return null;
        }
        return island;
    }

    /**
     * Resolve the island the target actually owns. getIsland(World, UUID) returns the island the
     * player is standing on, which may be someone else's team island.
     *
     * @param targetUUID target player
     * @return the owned island, or null if the player owns none
     */
    private @Nullable Island getOwnedIsland(UUID targetUUID) {
        List<Island> owned = getIslands().getIslands(getWorld(), targetUUID).stream()
                .filter(i -> targetUUID.equals(i.getOwner())).toList();
        if (owned.isEmpty()) {
            // No island is attributed to this player: keep the previous resolution so that
            // islands whose owner field is missing are still reachable
            return getIslands().hasIsland(getWorld(), targetUUID) ? getIslands().getIsland(getWorld(), targetUUID)
                    : null;
        }
        if (owned.size() == 1) {
            return owned.getFirst();
        }
        // More than one island: prefer the one the player is standing on
        User target = User.getInstance(targetUUID);
        return target.isOnline() && target.getLocation() != null
                ? getIslands().getIslandAt(target.getLocation()).filter(owned::contains).orElse(owned.getFirst())
                : owned.getFirst();
    }

    private @Nullable SettingChange checkWorldSetting(User user, List<String> args) {
        String arg0 = args.getFirst().toUpperCase(Locale.ENGLISH);

        if (!flagNames(Type.WORLD_SETTING).contains(arg0)) {
            this.showHelp(this, user);
            return null;
        }
        Flag flag = getPlugin().getFlagsManager().getFlag(arg0).orElse(null);
        Boolean active = checkActiveDisabled(user, args.get(1));
        return flag == null || active == null ? null : new SettingChange(null, flag, active ? 1 : 0);
    }

    private @Nullable SettingChange checkIslandSetting(User user, List<String> args) {
        // Get island
        Island island = getIsland(user, args);
        if (island == null) return null;

        String arg1 = args.get(1).toUpperCase(Locale.ENGLISH);

        if (!flagNames(Type.SETTING).contains(arg1) && !flagNames(Type.PROTECTION).contains(arg1)) {
            user.sendMessage("commands.admin.settings.unknown-flag", TextVariables.NAME, args.get(1));
            return null;
        }
        // Set flag
        Optional<Flag> flag = getPlugin().getFlagsManager().getFlag(arg1);
        // Check settings
        if (flag.isPresent()) {
            if (flag.get().getType().equals(Type.SETTING)) {
                Boolean active = checkActiveDisabled(user, args.get(2));
                return active == null ? null : new SettingChange(island, flag.get(), active ? 1 : 0);
            } else {
                // Protection flag
                Integer rank = checkRank(user, String.join(" ", args.subList(2, args.size())));
                return rank == null ? null : new SettingChange(island, flag.get(), rank);
            }
        }

        return null;
    }

    /**
     * Check the rank given.
     * @param user - user
     * @param string - the rank given in the command line
     * @return the rank, or null if invalid
     */
    private @Nullable Integer checkRank(User user, String string) {
        for (Entry<String, Integer> en : RanksManager.getInstance().getRanks().entrySet()) {
            if (en.getValue() > RanksManager.BANNED_RANK && en.getValue() <= RanksManager.OWNER_RANK
                    && string.equalsIgnoreCase(Util.stripColor(user.getTranslation(en.getKey())))) {
                // We have a winner
                return en.getValue();
            }
        }
        user.sendMessage("commands.admin.setrank.unknown-rank");
        return null;
    }

    private @Nullable Boolean checkActiveDisabled(User user, String string) {
        String active = Util.stripColor(user.getTranslation("protection.panel.flag-item.setting-active"));
        String disabled = Util.stripColor(user.getTranslation("protection.panel.flag-item.setting-disabled"));
        if (!string.equalsIgnoreCase(active) && !string.equalsIgnoreCase(disabled)) {
            user.sendMessage("commands.admin.settings.unknown-setting", TextVariables.NAME, string);
            return null;
        }
        return string.equalsIgnoreCase(active);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        SettingChange change = resolveChange(user, args);
        if (change == null) return false;
        if (args.size() > 1) {
            // Command line setting
            Flag f = change.flag();
            switch (f.getType()) {
            case PROTECTION -> change.island().setFlag(f, change.value());
            case SETTING -> change.island().setSettingsFlag(f, change.value() != 0);
            case WORLD_SETTING -> f.setSetting(getWorld(), change.value() != 0);
            default -> {
                // Do nothing
            }
            }
            user.sendMessage("general.success");
            return true;
        }
        // GUI requires in-game
        if (!user.isPlayer()) {
            user.sendMessage("general.errors.use-in-game");
            return false;
        }
        // World settings without a player argument, otherwise the player's island settings
        Island target = change.island();
        if (!SettingsPanel.openAdminPanel(this, user, target)) {
            getPlugin().logError("Could not load the admin settings panel template; showing the built-in panel");
            openLegacyPanel(user, target);
        }
        return true;
    }

    /**
     * Opens the legacy tabbed panel, used when the admin settings panel template cannot be loaded.
     * @param user the admin
     * @param target the island to edit, or null for the world
     * @since 3.23.1
     */
    protected void openLegacyPanel(User user, @Nullable Island target) {
        if (target == null) {
            new TabbedPanelBuilder()
            .user(user)
            .world(getWorld())
                    .tab(1, new SettingsTab(getWorld(), user, Flag.Type.WORLD_SETTING, Flag.Mode.EXPERT))
            .tab(2, new WorldDefaultSettingsTab(getWorld(), user))
            .tab(3, new IslandDefaultSettingsTab(getWorld(), user))
            .startingSlot(1)
            .size(54)
            .build().openPanel();
            return;
        }
        // Player settings
        new TabbedPanelBuilder()
        .user(user)
        .world(target.getWorld())
                .island(target).tab(1, new SettingsTab(getWorld(), user, Flag.Type.PROTECTION, Flag.Mode.EXPERT))
                .tab(2, new SettingsTab(getWorld(), user, Flag.Type.SETTING, Flag.Mode.EXPERT))
        .startingSlot(1)
        .size(54)
        .build().openPanel();
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        // Update with the latest lists
        List<String> protectionFlagNames = flagNames(Type.PROTECTION);
        List<String> settingFlagNames = flagNames(Type.SETTING);
        List<String> worldSettingFlagNames = flagNames(Type.WORLD_SETTING);
        String active = Util.stripColor(user.getTranslation("protection.panel.flag-item.setting-active"));
        String disabled = Util.stripColor(user.getTranslation("protection.panel.flag-item.setting-disabled"));
        List<String> options = new ArrayList<>();
        String lastArg = !args.isEmpty() ? args.getLast() : "";
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
            case PROTECTION -> RanksManager.getInstance().getRanks().entrySet().stream()
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
