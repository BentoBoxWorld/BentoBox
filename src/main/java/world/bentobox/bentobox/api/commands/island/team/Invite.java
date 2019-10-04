package world.bentobox.bentobox.api.commands.island.team;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents an invite
 * @author tastybento
 *
 */
public class Invite {

    /**
     * Type of invitation
     *
     */
    public enum InviteType {
        COOP,
        TEAM,
        TRUST
    }

    private final InviteType type;
    private final UUID inviter;
    private final UUID invitee;

    /**
     * @param type
     * @param inviter
     * @param invitee
     */
    public Invite(InviteType type, UUID inviter, UUID invitee) {
        this.type = type;
        this.inviter = inviter;
        this.invitee = invitee;
    }

    /**
     * @return the type
     */
    public InviteType getType() {
        return type;
    }

    /**
     * @return the inviter
     */
    public UUID getInviter() {
        return inviter;
    }

    /**
     * @return the invitee
     */
    public UUID getInvitee() {
        return invitee;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(invitee, inviter, type);
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
        if (!(obj instanceof Invite)) {
            return false;
        }
        Invite other = (Invite) obj;
        return Objects.equals(invitee, other.invitee) && Objects.equals(inviter, other.inviter) && type == other.type;
    }


}
