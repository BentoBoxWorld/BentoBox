package us.tastybento.bskyblock.api.events.team;

import java.util.UUID;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * This event is fired when a player talks in TeamChat
 *
 * @author Poslovitch
 * @since 1.0
 */
public class TeamChatEvent extends IslandEvent {
	private final UUID player;
	private String message;

	/**
	 * @param island
	 * @param player
	 * @param message
	 */
	public TeamChatEvent(Island island, UUID player, String message) {
		super(island);
		this.player = player;
		this.message = message;
	}

	/**
	 * @return the player who talked
	 */
	public UUID getPlayer() {
		return player;
	}

	/**
	 * Gets the message that the player is attempting to send.
	 * @return the message
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Sets the message that the player will send.
	 * @param message the message to send
	 */
	public void setMessage(String message) {
		this.message = message;
	}
}
