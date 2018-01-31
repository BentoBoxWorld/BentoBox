package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.listeners.FlagListener;
import us.tastybento.bskyblock.lists.Flags;

public class PlaceBlocksListener extends FlagListener {

    public PlaceBlocksListener() {
        super(BSkyBlock.getInstance());
    }
    
    /**
     * Prevents blocks from being placed
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent e) {

        // If this is not an Island World, skip
        if (!inWorld(e.getPlayer())) return;
        
        // Handle fake players
        if (plugin.getSettings().getFakePlayers().contains(e.getPlayer().getName())) return;
        
        // Real players
        // Get the island and if present, check the flag, react if required and return
        plugin.getIslands().getIslandAt(e.getBlock().getLocation()).ifPresent(x -> { 
                if (!x.isAllowed(User.getInstance(e.getPlayer()), Flags.PLACE_BLOCKS)) noGo(e);
                return;
             });
        
        // The player is in the world, but not on an island, so general world settings apply
        if (!Flags.PLACE_BLOCKS.isAllowed()) noGo(e);
    }
    
}
