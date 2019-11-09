package world.bentobox.bentobox.listeners.flags.protection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.Flag;

public class TestWorldSettings implements WorldSettings {

    private Map<String, Boolean> map = new HashMap<>();

    @Override
    public GameMode getDefaultGameMode() {

        return null;
    }

    @Override
    public Map<Flag, Integer> getDefaultIslandFlags() {

        return null;
    }

    @Override
    public Map<Flag, Integer> getDefaultIslandSettings() {

        return null;
    }

    @Override
    public Difficulty getDifficulty() {

        return null;
    }

    @Override
    public void setDifficulty(Difficulty difficulty) {


    }

    @Override
    public String getFriendlyName() {

        return null;
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

        return null;
    }

    @Override
    public int getMaxHomes() {

        return 0;
    }

    @Override
    public int getMaxIslands() {

        return 0;
    }

    @Override
    public int getMaxTeamSize() {

        return 0;
    }

    @Override
    public int getNetherSpawnRadius() {

        return 0;
    }

    @Override
    public String getPermissionPrefix() {

        return null;
    }

    @Override
    public Set<EntityType> getRemoveMobsWhitelist() {

        return null;
    }

    @Override
    public int getSeaHeight() {

        return 0;
    }

    @Override
    public List<String> getHiddenFlags() {

        return null;
    }

    @Override
    public List<String> getVisitorBannedCommands() {

        return null;
    }

    @Override
    public Map<String, Boolean> getWorldFlags() {
        return map ;
    }

    /**
     * @return the map
     */
    public Map<String, Boolean> getMap() {
        return map;
    }

    /**
     * @param map the map to set
     */
    public void setMap(Map<String, Boolean> map) {
        this.map = map;
    }

    @Override
    public String getWorldName() {

        return null;
    }

    @Override
    public boolean isDragonSpawn() {

        return false;
    }

    @Override
    public boolean isEndGenerate() {

        return false;
    }

    @Override
    public boolean isEndIslands() {

        return false;
    }

    @Override
    public boolean isNetherGenerate() {

        return false;
    }

    @Override
    public boolean isNetherIslands() {

        return false;
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

        return null;
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

        return null;
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

        return null;
    }

    @Override
    public int getResetLimit() {

        return 0;
    }

    @Override
    public long getResetEpoch() {

        return 0;
    }

    @Override
    public void setResetEpoch(long timestamp) {


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

        return false;
    }

    @Override
    public boolean isDeathsResetOnNewIsland() {

        return false;
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

        return 0;
    }

    @Override
    public boolean isLeaversLoseReset() {

        return false;
    }

    @Override
    public boolean isKickedKeepInventory() {

        return false;
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
