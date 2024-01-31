package world.bentobox.bentobox.api.commands.island.team;

import java.util.Objects;
import java.util.UUID;

import world.bentobox.bentobox.database.objects.Island;

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
    private final Island island;

    /**
     * @param type - invitation type, e.g., coop, team, trust
     * @param inviter - UUID of inviter
     * @param invitee - UUID of invitee
     * @param island - the island this invite is for
     */
    public Invite(Type type, UUID inviter, UUID invitee, Island island) {
        this.type = type;
        this.inviter = inviter;
        this.invitee = invitee;
        this.island = island;
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

    /**
     * @return the island
     */
    public Island getIsland() {
        return island;
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
        if (!(obj instanceof Invite other)) {
            return false;
        }
        return Objects.equals(invitee, other.invitee) && Objects.equals(inviter, other.inviter) && type == other.type;
    }
}
