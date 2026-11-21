package world.bentobox.bentobox.database.objects;

import java.util.Objects;
import java.util.UUID;

import com.google.gson.annotations.Expose;

/**
 * Data object for team invites
 */
@Table(name = "TeamInvites")
public class TeamInvite implements DataObject {
    
    /**
     * Type of invitation
     *
     */
    public enum Type {
        COOP,
        TEAM,
        TRUST
    }

    @Expose
    private Type type;
    @Expose
    private UUID inviter;
    @Expose
    private String islandID;
    
    @Expose
    private String uniqueId;

    /**
     * When the invite was made, in epoch milliseconds. Zero for invites stored
     * before this field existed.
     * @since 3.23.8
     */
    @Expose
    private long created;
    
    /**
     * @param type - invitation type, e.g., coop, team, trust
     * @param inviter - UUID of inviter
     * @param invitee - UUID of invitee
     * @param islandID - the unique ID of the island this invite is for
     */
    public TeamInvite(Type type, UUID inviter, UUID invitee, String islandID) {
        this.type = type;
        this.uniqueId = invitee.toString();
        this.inviter = inviter;
        this.islandID = islandID;
        this.created = System.currentTimeMillis();
    }

    /**
     * @return when the invite was made in epoch milliseconds, or 0 if unknown
     * @since 3.23.8
     */
    public long getCreated() {
        return created;
    }

    /**
     * @param created when the invite was made, in epoch milliseconds
     * @since 3.23.8
     */
    public void setCreated(long created) {
        this.created = created;
    }

    @Override
    public String getUniqueId() {
        // Inviter
        return this.uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @return the invitee
     */
    public UUID getInvitee() {
        return UUID.fromString(uniqueId);
    }

    /**
     * @return the inviter
     */
    public UUID getInviter() {
        return inviter;
    }

    /**
     * @return the islandID
     */
    public String getIslandID() {
        return islandID;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(inviter, uniqueId, type);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TeamInvite other)) {
            return false;
        }
        return Objects.equals(inviter, other.inviter) && Objects.equals(uniqueId, other.getUniqueId())
                && type == other.type;
    }

}
