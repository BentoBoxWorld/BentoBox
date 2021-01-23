package world.bentobox.bentobox;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.Flag;

/**
 * Class for tests that require world settings
 * @author tastybento
 *
 */
public class TestWorldSettings implements WorldSettings {

    private long epoch;

    @Override
    public GameMode getDefaultGameMode() {

        return GameMode.SURVIVAL;
    }

    @Override
    public Map<Flag, Integer> getDefaultIslandFlags() {

        return Collections.emptyMap();
    }

    @Override
    public Map<Flag, Integer> getDefaultIslandSettings() {

        return Collections.emptyMap();
    }

    @Override
    public Difficulty getDifficulty() {

        return Difficulty.EASY;
    }

    @Override
    public void setDifficulty(Difficulty difficulty) {


    }

    @Override
    public String getFriendlyName() {

        return "friendly_name";
    }

    @Override
    public int getIslandDistance() {

        return 0;
    }

    @Override
    public int getIslandHeight() {

        return 0;
    }

    @Override
    public int getIslandProtectionRange() {

        return 0;
    }

    @Override
    public int getIslandStartX() {

        return 0;
    }

    @Override
    public int getIslandStartZ() {

        return 0;
    }

    @Override
    public int getIslandXOffset() {

        return 0;
    }

    @Override
    public int getIslandZOffset() {

        return 0;
    }

    @Override
    public List<String> getIvSettings() {

        return Collections.emptyList();
    }

    @Override
    public int getMaxHomes() {

        return 3;
    }

    @Override
    public int getMaxIslands() {

        return 0;
    }

    @Override
    public int getMaxTeamSize() {

        return 4;
    }

    @Override
    public int getNetherSpawnRadius() {

        return 10;
    }

    @Override
    public String getPermissionPrefix() {

        return "perm.";
    }

    @Override
    public Set<EntityType> getRemoveMobsWhitelist() {

        return Collections.emptySet();
    }

    @Override
    public int getSeaHeight() {

        return 0;
    }

    @Override
    public List<String> getHiddenFlags() {

        return Collections.emptyList();
    }

    @Override
    public List<String> getVisitorBannedCommands() {

        return Collections.emptyList();
    }

    @Override
    public Map<String, Boolean> getWorldFlags() {

        return Collections.emptyMap();
    }

    @Override
    public String getWorldName() {

        return "world_name";
    }

    @Override
    public boolean isDragonSpawn() {

        return false;
    }

    @Override
    public boolean isEndGenerate() {

        return true;
    }

    @Override
    public boolean isEndIslands() {

        return true;
    }

    @Override
    public boolean isNetherGenerate() {

        return true;
    }

    @Override
    public boolean isNetherIslands() {

        return true;
    }

    @Override
    public boolean isOnJoinResetEnderChest() {

        return false;
    }

    @Override
    public boolean isOnJoinResetInventory() {

        return false;
    }

    @Override
    public boolean isOnJoinResetMoney() {

        return false;
    }

    @Override
    public boolean isOnJoinResetHealth() {

        return false;
    }

    @Override
    public boolean isOnJoinResetHunger() {

        return false;
    }

    @Override
    public boolean isOnJoinResetXP() {

        return false;
    }

    @Override
    public @NonNull List<String> getOnJoinCommands() {

        return Collections.emptyList();
    }

    @Override
    public boolean isOnLeaveResetEnderChest() {

        return false;
    }

    @Override
    public boolean isOnLeaveResetInventory() {

        return false;
    }

    @Override
    public boolean isOnLeaveResetMoney() {

        return false;
    }

    @Override
    public boolean isOnLeaveResetHealth() {

        return false;
    }

    @Override
    public boolean isOnLeaveResetHunger() {

        return false;
    }

    @Override
    public boolean isOnLeaveResetXP() {

        return false;
    }

    @Override
    public @NonNull List<String> getOnLeaveCommands() {

        return Collections.emptyList();
    }

    @Override
    public boolean isUseOwnGenerator() {

        return false;
    }

    @Override
    public boolean isWaterUnsafe() {

        return false;
    }

    @Override
    public List<String> getGeoLimitSettings() {

        return Collections.emptyList();
    }

    @Override
    public int getResetLimit() {

        return 0;
    }

    @Override
    public long getResetEpoch() {

        return epoch;
    }

    @Override
    public void setResetEpoch(long timestamp) {
        this.epoch = timestamp;

    }

    @Override
    public boolean isTeamJoinDeathReset() {

        return false;
    }

    @Override
    public int getDeathsMax() {

        return 0;
    }

    @Override
    public boolean isDeathsCounted() {

        return true;
    }

    @Override
    public boolean isDeathsResetOnNewIsland() {

        return true;
    }

    @Override
    public boolean isAllowSetHomeInNether() {

        return false;
    }

    @Override
    public boolean isAllowSetHomeInTheEnd() {

        return false;
    }

    @Override
    public boolean isRequireConfirmationToSetHomeInNether() {

        return false;
    }

    @Override
    public boolean isRequireConfirmationToSetHomeInTheEnd() {

        return false;
    }

    @Override
    public int getBanLimit() {

        return 10;
    }

    @Override
    public boolean isLeaversLoseReset() {

        return true;
    }

    @Override
    public boolean isKickedKeepInventory() {

        return true;
    }

    @Override
    public boolean isCreateIslandOnFirstLoginEnabled() {

        return false;
    }

    @Override
    public int getCreateIslandOnFirstLoginDelay() {

        return 0;
    }

    @Override
    public boolean isCreateIslandOnFirstLoginAbortOnLogout() {

        return false;
    }

}
