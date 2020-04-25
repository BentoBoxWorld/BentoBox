package world.bentobox.bentobox.util.heads;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            synchronized(names) {
                Iterator<Entry<String, PanelItem>> it = names.entrySet().iterator();
                if (it.hasNext()) {
                    Entry<String, PanelItem> en = it.next();
                    ItemStack playerSkull = new ItemStack(Material.PLAYER_HEAD, en.getValue().getItem().getAmount());
                    SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(en.getKey());
                    meta.setOwningPlayer(offlinePlayer);
                    playerSkull.setItemMeta(meta);
                    // Save in cache
                    cachedHeads.put(en.getKey(), playerSkull);
                    // Tell requesters the head came in
                    if (headRequesters.containsKey(en.getKey())) {
                        for (HeadRequester req : headRequesters.get(en.getKey())) {
                            en.getValue().setHead(playerSkull.clone());
                            Bukkit.getServer().getScheduler().runTask(plugin, () -> req.setHead(en.getValue()));
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
        if (cachedHeads.containsKey(panelItem.getPlayerHeadName())) {
            panelItem.setHead(cachedHeads.get(panelItem.getPlayerHeadName()).clone());
            requester.setHead(panelItem);
        } else {
            BentoBox.getInstance().logDebug("Not in cache");
            // Show Steve's head for now
            panelItem.setHead(new ItemStack(Material.CREEPER_HEAD));
            // Get the name
            headRequesters.putIfAbsent(panelItem.getPlayerHeadName(), new HashSet<>());
            Set<HeadRequester> requesters = headRequesters.get(panelItem.getPlayerHeadName());
            requesters.add(requester);
            headRequesters.put(panelItem.getPlayerHeadName(), requesters);
            names.put(panelItem.getPlayerHeadName(), panelItem);
        }
    }

}
