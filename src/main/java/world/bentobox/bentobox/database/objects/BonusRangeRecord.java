package world.bentobox.bentobox.database.objects;

import com.google.gson.annotations.Expose;

/**
 * Record for bonus ranges
 * @author tastybento
 * @param id an id to identify this bonus
 * @param range the additional bonus range
 * @param message the reference key to a locale message related to this bonus. May be blank.
 */
public class BonusRangeRecord {
    @Expose
    private String uniqueId;
    @Expose
    private int range;
    @Expose
    private String message;
    /**
     * @param uniqueId
     * @param range
     * @param message
     */
    public BonusRangeRecord(String uniqueId, int range, String message) {
        this.uniqueId = uniqueId;
        this.range = range;
        this.message = message;
    }
    /**
     * @return the uniqueId
     */
    public String getUniqueId() {
        return uniqueId;
    }
    /**
     * @param uniqueId the uniqueId to set
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
    /**
     * @return the range
     */
    public int getRange() {
        return range;
    }
    /**
     * @param range the range to set
     */
    public void setRange(int range) {
        this.range = range;
    }
    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }
    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }


}
