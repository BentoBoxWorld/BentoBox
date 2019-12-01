package world.bentobox.bentobox.api.commands.island.team;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents an invite
 * @author tastybento
 * @since 1.8.0
 */
public class Invite {

    /**
     * Type of invitation
     *
     */
    public enum Type {
        COOP,
        TEAM,
        TRUST
    }

    private final Type type;
    private final UUID inviter;
    private final UUID invitee;

    /**
     * @param type - invitation type, e.g., coop, team, trust
     * @param inviter - UUID of inviter
     * @param invitee - UUID of invitee
     */
    public Invite(Type type, UUID inviter, UUID invitee) {
        this.type = type;
        this.inviter = inviter;
        this.invitee = invitee;
    }

    /**
     * @return the type
     */
    public Type getType() {
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
