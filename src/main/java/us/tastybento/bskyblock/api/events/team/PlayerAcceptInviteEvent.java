package us.tastybento.bskyblock.api.events.team;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player accepts an invite to join a team.
 *
 * @author Poslovitch
 * @since 1.0
 */
public class PlayerAcceptInviteEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
	private final Player player;
	
	/**
	 * @param player
	 */
	public PlayerAcceptInviteEvent(Player player) {
		this.player = player;
	}
	
	/**
	 * @return the player
	 */
	public Player getPlayer() {
		return this.player;
	}
	
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
