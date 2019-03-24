package world.bentobox.bentobox.managers;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.GameModePlaceholders;
import world.bentobox.bentobox.util.Util;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

/**
 * 
 * Registers default placeholders for all GameModes. Will not overwrite any that the gamemode addon itself implements.
 * @author tastybento
 *
 */
public class GameModePlaceholderManager {
	
	private BentoBox plugin;

	public GameModePlaceholderManager(BentoBox plugin) {
		super();
		this.plugin = plugin;
	}
	
	public void registerGameModePlaceholders(GameModeAddon addon) {
		String prefix = addon.getDescription().getName().toLowerCase();
		Map<GameModePlaceholders, String> placeholders = new EnumMap<>(GameModePlaceholders.class);
		Arrays.stream(GameModePlaceholders.values()).forEach(placeholder -> placeholders.put(placeholder, prefix + "-" + placeholder.getPlaceholder()));
		
		// Register placeholders only if they have not already been registered by the addon itself
		placeholders.entrySet().stream().filter(en -> !plugin.getPlaceholdersManager().isPlaceholder(addon, en.getValue()))
		.forEach(en -> plugin.getPlaceholdersManager().registerPlaceholder(en.getValue(), new DefaultPlaceholder(addon, en.getKey())));
	}
}

class DefaultPlaceholder implements PlaceholderReplacer {
	private final GameModeAddon addon;
	private final GameModePlaceholders type;
	public DefaultPlaceholder(GameModeAddon addon, GameModePlaceholders type) {
		super();
		this.addon = addon;
		this.type = type;
	}
	/* (non-Javadoc)
	 * @see world.bentobox.bentobox.api.placeholders.PlaceholderReplacer#onReplace(world.bentobox.bentobox.api.user.User)
	 */
	@Override
	public String onReplace(User user) {
		if (user == null) {
			return "";
		}
		Island island = addon.getIslands().getIsland(addon.getOverWorld(), user);
		switch (type) {
		case WORLD_FRIENDLY_NAME:
			return addon.getWorldSettings().getFriendlyName();
		case ISLAND_CREATION_DATE:
			return island == null ? "" : DateFormat.getInstance().format(Date.from(Instant.ofEpochMilli(island.getCreatedDate())));
		case ISLAND_DISTANCE:
			return String.valueOf(addon.getWorldSettings().getIslandDistance());
		case ISLAND_NAME:
			return island == null ? "" : (island.getName() == null ? "" : island.getName());
		case ISLAND_OWNER:
			return island == null ? "" : addon.getPlayers().getName(island.getOwner());
		case ISLAND_PROTECTION_RANGE:			
			return island == null ? "" : String.valueOf(island.getProtectionRange());
		case ISLAND_SPAWNPOINT:
			return island == null ? "" : Util.xyz(island.getCenter().toVector());
		default:
			return "";		
		}
	}
	
}



