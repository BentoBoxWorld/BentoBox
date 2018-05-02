package us.tastybento.bskyblock.managers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import us.tastybento.bskyblock.BSkyBlock;

public class RanksManager {

    // Constants that define the hard coded rank values
    public static final String ADMIN_RANK_REF = "ranks.admin";
    public static final String MOD_RANK_REF = "ranks.mod";
    public static final String OWNER_RANK_REF = "ranks.owner";
    public static final String MEMBER_RANK_REF = "ranks.member";
    public static final String VISITOR_RANK_REF = "ranks.visitor";
    public static final String BANNED_RANK_REF = "ranks.banned";
    public static final int ADMIN_RANK = 10000;
    public static final int MOD_RANK = 5000;
    public static final int OWNER_RANK = 1000;
    public static final int MEMBER_RANK = 900;
    public static final int VISITOR_RANK = 0;
    public static final int BANNED_RANK = -1;

    private BSkyBlock plugin;

    // The store of ranks
    private LinkedHashMap<String, Integer> ranks = new LinkedHashMap<>();

    /**
     * @param plugin - BSkyBlock plugin object
     */
    public RanksManager(BSkyBlock plugin) {
        super();
        this.plugin = plugin;
        // Hard coded ranks
        ranksPut(ADMIN_RANK_REF, ADMIN_RANK);
        ranksPut(MOD_RANK_REF, MOD_RANK);
        ranksPut(OWNER_RANK_REF, OWNER_RANK);
        ranksPut(MEMBER_RANK_REF, MEMBER_RANK);
        ranksPut(VISITOR_RANK_REF, VISITOR_RANK);
        ranksPut(BANNED_RANK_REF, BANNED_RANK);
        loadCustomRanks();
    }

    /**
     * Loads the custom ranks from the settings
     */
    public void loadCustomRanks() {
        for (Entry<String, Integer> en : plugin.getSettings().getCustomRanks().entrySet()) {
            if (!addRank(en.getKey(),en.getValue())) {
                plugin.logError("Error loading custom rank: " + en.getKey() + " " + en.getValue() + " skipping...");
            }
        }
    }

    /**
     * Try to add a new rank. Owner, member, visitor and banned ranks cannot be changed.
     * @param reference
     * @param value
     * @return true if the rank was successfully added
     */
    public boolean addRank(String reference, int value) {
        if (reference.equalsIgnoreCase(OWNER_RANK_REF)
                || reference.equalsIgnoreCase(MEMBER_RANK_REF)
                || reference.equalsIgnoreCase(VISITOR_RANK_REF)
                || reference.equalsIgnoreCase(BANNED_RANK_REF)
                || reference.equalsIgnoreCase(ADMIN_RANK_REF)
                || reference.equalsIgnoreCase(MOD_RANK_REF)) {
            return false;
        }
        ranksPut(reference, value);

        return true;
    }

    private void ranksPut(String reference, int value) {
        ranks.put(reference, value);
        // Sort
        ranks = ranks.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));   
    }

    /**
     * Try to remove a rank. Owner, member, visitor and banned ranks cannot be removed.
     * @param reference
     * @return true if removed
     */
    public boolean removeRank(String reference) {
        if (reference.equalsIgnoreCase(OWNER_RANK_REF)
                || reference.equalsIgnoreCase(MEMBER_RANK_REF)
                || reference.equalsIgnoreCase(VISITOR_RANK_REF)
                || reference.equalsIgnoreCase(BANNED_RANK_REF)
                || reference.equalsIgnoreCase(ADMIN_RANK_REF)
                || reference.equalsIgnoreCase(MOD_RANK_REF)) {
            return false;
        }

        return ranks.remove(reference) == null ? false : true;
    }

    /**
     * Get the rank value for this reference
     * @param reference - locale reference to the name of this rank
     * @return rank value or zero if this is an unknown rank
     */
    public int getRankValue(String reference) {
        return ranks.getOrDefault(reference, VISITOR_RANK);
    }

    /**
     * Get the ranks. Ranks are listed in ascending order
     * @return immutable map of ranks
     */
    public Map<String, Integer> getRanks() {
        return new LinkedHashMap<>(ranks);
    }


    /**
     * Gets the next rank value above the current rank. Highest is {@link RanksManager.OWNER_RANK}
     * @param currentRank
     * @return Optional rank value
     */
    public int getRankUpValue(int currentRank) {
        return getRanks().values().stream().mapToInt(x -> {
            if (x > currentRank) {
                return x;
            }
            return OWNER_RANK;
        }).min().orElse(currentRank);  
    }

    /**
     * Gets the previous rank value below the current rank. Lowest is {@link RanksManager.VISITOR_RANK}
     * @param currentRank
     * @return Optional rank value
     */
    public int getRankDownValue(int currentRank) {
        return getRanks().values().stream().mapToInt(x -> {
            if (x < currentRank) {
                return x;
            }
            return VISITOR_RANK;
        }).max().orElse(currentRank);       
    }

    /**
     * Gets the reference to the rank name for value
     * @param rank - value
     * @return Reference
     */
    public String getRank(int rank) {
        for (Entry<String, Integer> en : ranks.entrySet()) {
            if (rank == en.getValue()) {
                return en.getKey();
            }
        }
        return "";
    }
}
