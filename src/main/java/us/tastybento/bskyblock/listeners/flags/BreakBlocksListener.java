package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.listeners.FlagListener;
import us.tastybento.bskyblock.lists.Flags;

public class BreakBlocksListener extends FlagListener {

    public BreakBlocksListener() {
        super(BSkyBlock.getInstance());
    }
    
    /**
     * Prevents blocks from being broken
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e) {

        // If this is not an Island World, skip
        if (!inWorld(e.getPlayer())) return;
        
        // Handle fake players
        if (plugin.getSettings().getFakePlayers().contains(e.getPlayer().getName())) return;
        
        // Real players
        // Get the island and if present, check the flag, react if required and return
        plugin.getIslands().getIslandAt(e.getBlock().getLocation()).ifPresent(x -> { 
                if (!x.isAllowed(User.getInstance(e.getPlayer()), Flags.BREAK_BLOCKS)) noGo(e);
                return;
             });
        
        // The player is in the world, but not on an island, so general world settings apply
        if (!Flags.BREAK_BLOCKS.isAllowed()) noGo(e);
    }
    
    
}
