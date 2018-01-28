package us.tastybento.bskyblock.managers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import us.tastybento.bskyblock.BSkyBlock;

public class RanksManager {

    // Constants that define the hard coded rank values
    private static final String OWNER_RANK_REF = "ranks.owner";
    private static final String MEMBER_RANK_REF = "ranks.member";
    private static final String VISITOR_RANK_REF = "ranks.visitor";
    private static final String BANNED_RANK_REF = "ranks.banned";
    private static final Integer OWNER_RANK = 1000;
    private static final Integer MEMBER_RANK = 900;
    private static final Integer VISITOR_RANK = 0;
    private static final Integer BANNED_RANK = -1;

    private BSkyBlock plugin;

    // The store of ranks
    private LinkedHashMap<String, Integer> ranks = new LinkedHashMap<>();
    {
        // Hard coded ranks
        addRank(OWNER_RANK_REF, OWNER_RANK);
        addRank(MEMBER_RANK_REF, MEMBER_RANK);
        addRank(VISITOR_RANK_REF, VISITOR_RANK);
        addRank(BANNED_RANK_REF, BANNED_RANK);
    }

    /**
     * @param plugin
     */
    public RanksManager(BSkyBlock plugin) {
        super();
        this.plugin = plugin;
        loadCustomRanks();
    }

    /**
     * Loads the custom ranks from the settings
     */
    public void loadCustomRanks() {
        for (Entry<String, Integer> en : plugin.getSettings().getCustomRanks().entrySet()) {
            if (!addRank(en.getKey(),en.getValue())) {
                plugin.getLogger().severe("Error loading custom rank: " + en.getKey() + " " + en.getValue() + " skipping...");
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
                || reference.equalsIgnoreCase(BANNED_RANK_REF)) {
            return false;
        }
        ranks.put(reference, value);
        // Sort
        ranks = ranks.entrySet().stream()
        .sorted(Map.Entry.comparingByValue())
        .collect(Collectors.toMap(
           Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        return true;
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
                || reference.equalsIgnoreCase(BANNED_RANK_REF)) {
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
    public LinkedHashMap<String, Integer> getRanks() {
        return new LinkedHashMap<>(ranks);
    }

    /**
     * @return the ownerRankRef
     */
    public static String getOwnerRankRef() {
        return OWNER_RANK_REF;
    }

    /**
     * @return the memberRankRef
     */
    public static String getMemberRankRef() {
        return MEMBER_RANK_REF;
    }

    /**
     * @return the visitorRankRef
     */
    public static String getVisitorRankRef() {
        return VISITOR_RANK_REF;
    }

    /**
     * @return the bannedRankRef
     */
    public static String getBannedRankRef() {
        return BANNED_RANK_REF;
    }

    /**
     * @return the ownerRank
     */
    public static Integer getOwnerRank() {
        return OWNER_RANK;
    }

    /**
     * @return the memberRank
     */
    public static Integer getMemberRank() {
        return MEMBER_RANK;
    }

    /**
     * @return the visitorRank
     */
    public static Integer getVisitorRank() {
        return VISITOR_RANK;
    }

    /**
     * @return the bannedRank
     */
    public static Integer getBannedRank() {
        return BANNED_RANK;
    }

}
