package world.bentobox.bentobox.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.PanelItem;

public class HeadGetter {
    private static Map<String,ItemStack> cachedHeads = new HashMap<>();
    private static final Map<String, PanelItem> names = new HashMap<>();
    private BentoBox plugin;
    private static Map<String,Set<HeadRequester>> headRequesters = new HashMap<>();

    /**
     * @param plugin - plugin
     */
    public HeadGetter(BentoBox plugin) {
        super();
        this.plugin = plugin;
        runPlayerHeadGetter();
    }

    @SuppressWarnings("deprecation")
    private void runPlayerHeadGetter() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            synchronized(names) {
                Iterator<Entry<String, PanelItem>> it = names.entrySet().iterator();
                if (it.hasNext()) {
                    Entry<String, PanelItem> en = it.next();
                    ItemStack playerSkull = new ItemStack(Material.PLAYER_HEAD, 1);
                    SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
                    meta.setOwner(en.getKey());
                    playerSkull.setItemMeta(meta);
                    // Save in cache
                    cachedHeads.put(en.getKey(), playerSkull);
                    // Tell requesters the head came in
                    if (headRequesters.containsKey(en.getKey())) {
                        for (HeadRequester req : headRequesters.get(en.getKey())) {
                            en.getValue().setHead(playerSkull.clone());
                            plugin.getServer().getScheduler().runTask(plugin, () -> req.setHead(en.getValue()));
                        }
                    }
                    it.remove();
                }
            }
        }, 0L, 20L);
    }
    
     /**
     * @param panelItem - head to update
     * @param requester - callback class
     */
    public static void getHead(PanelItem panelItem, HeadRequester requester) {        
        // Check if in cache
        if (cachedHeads.containsKey(panelItem.getName())) {
            panelItem.setHead(cachedHeads.get(panelItem.getName()).clone());
            requester.setHead(panelItem);
        } else {
            // Get the name
            headRequesters.putIfAbsent(panelItem.getName(), new HashSet<>());
            Set<HeadRequester> requesters = headRequesters.get(panelItem.getName());
            requesters.add(requester);
            headRequesters.put(panelItem.getName(), requesters);
            names.put(panelItem.getName(), panelItem);
        }
    }
    
}
