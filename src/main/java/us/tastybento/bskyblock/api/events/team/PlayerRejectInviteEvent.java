/*******************************************************************************
 * This file is part of ASkyBlock.
 * <p>
 * ASkyBlock is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * ASkyBlock is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package us.tastybento.bskyblock.api.events.team;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player rejects an invite to join a team.
 *
 * @author Poslovitch
 * @since 1.0
 */
public class PlayerRejectInviteEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final Player player;

	/**
	 * @param player
	 */
	public PlayerRejectInviteEvent(Player player) {
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
