package world.bentobox.bentobox.listeners;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.AsyncStructureSpawnEvent;
import org.bukkit.generator.structure.Structure;

import io.papermc.paper.event.world.StructuresLocateEvent;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;

/**
 * Suppresses vanilla structures in a game mode's worlds, both when they would be
 * <em>placed</em> and when they are <em>searched</em> for.
 *
 * <p>Game modes whose worlds delegate to vanilla generation (e.g. Boxed, SkyGrid,
 * CaveBlock) can generate structures that fill or unbalance an island world. The
 * {@link org.bukkit.generator.ChunkGenerator} flag can only turn all structures on or
 * off, so this listener provides per-structure control driven by BentoBox's global
 * {@code world.disabled-structures} list and each world's
 * {@link WorldSettings#getStructureSettings()} override.</p>
 *
 * <p>Suppression has two halves:</p>
 * <ul>
 *   <li>{@link AsyncStructureSpawnEvent} — stops a disabled structure being placed during
 *       chunk generation. It fires off the main thread, so this handler only reads config
 *       and inspects the event; it performs no world or block mutation, which is what makes
 *       it safe to run async.</li>
 *   <li>{@link StructuresLocateEvent} — fires before any structure search ({@code /locate},
 *       Eyes of Ender, explorer/treasure maps, dolphins, villager cartographer trades).
 *       Cancelling the spawn alone leaves the world's placement rules intact, so a search
 *       for a suppressed structure never succeeds and scans out to the radius cap, freezing
 *       the main thread. Removing disabled structures from the search — and cancelling when
 *       nothing enabled remains — skips that scan entirely.</li>
 * </ul>
 *
 * <p>One instance is registered per {@link GameModeAddon}, immediately before its worlds are
 * created, so it is active for the initial spawn-area generation. Worlds are matched by
 * configured name rather than {@link world.bentobox.bentobox.managers.IslandWorldManager#inWorld(World)}
 * because the spawn chunks generate during {@code createWorlds()}, before the game mode's
 * worlds are registered with the world manager.</p>
 *
 * @author tastybento
 * @since 3.19.1
 */
public class StructureListener implements Listener {

    private final BentoBox plugin;
    private final GameModeAddon gameMode;

    /**
     * @param plugin BentoBox instance
     * @param gameMode the game mode whose worlds this listener guards
     */
    public StructureListener(BentoBox plugin, GameModeAddon gameMode) {
        this.plugin = plugin;
        this.gameMode = gameMode;
    }

    /**
     * Cancels the spawn of a disabled structure in one of this game mode's worlds.
     *
     * @param event the structure spawn event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onStructureSpawn(AsyncStructureSpawnEvent event) {
        if (!isGameModeWorld(event.getWorld())) {
            return;
        }
        if (isDisabled(event.getStructure().getKey().getKey())) {
            event.setCancelled(true);
        }
    }

    /**
     * Removes disabled structures from a structure search in one of this game mode's worlds,
     * cancelling outright when nothing enabled remains so the expensive scan is skipped.
     *
     * @param event the structure locate event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onStructuresLocate(StructuresLocateEvent event) {
        if (!isGameModeWorld(event.getWorld())) {
            return;
        }
        List<Structure> targets = event.getStructures();
        List<Structure> allowed = targets.stream()
                .filter(structure -> !isDisabled(structure.getKey().getKey()))
                .toList();
        if (allowed.size() == targets.size()) {
            // Nothing disabled in this search — let it run normally.
            return;
        }
        if (allowed.isEmpty()) {
            // Every requested structure is disabled here: skip the expensive scan entirely.
            event.setCancelled(true);
        } else {
            event.setStructures(allowed);
        }
    }

    /**
     * @param world a world
     * @return {@code true} if {@code world} is this game mode's overworld, nether or end.
     *
     * <p>Matches on the configured world name rather than the world manager because the
     * spawn-area chunks generate during {@code createWorlds()}, before the game mode's worlds
     * are registered. At that point a world-manager lookup would not yet know the world and
     * the first structures would slip through.</p>
     */
    private boolean isGameModeWorld(World world) {
        String base = gameMode.getWorldSettings().getWorldName();
        if (base == null) {
            return false;
        }
        String name = world.getName();
        return name.equalsIgnoreCase(base) || name.equalsIgnoreCase(base + "_nether")
                || name.equalsIgnoreCase(base + "_the_end");
    }

    /**
     * Decides whether a structure is disabled in this game mode. A per-world entry in
     * {@link WorldSettings#getStructureSettings()} always wins ({@code false} disables,
     * {@code true} force-enables); otherwise the global {@code world.disabled-structures}
     * list applies.
     *
     * @param structureKey the vanilla structure key path, e.g. {@code ancient_city}
     * @return {@code true} if this structure should be suppressed
     */
    private boolean isDisabled(String structureKey) {
        String normalizedKey = normalize(structureKey);
        // Per-world override wins: value is whether the structure should generate.
        Map<String, Boolean> overrides = gameMode.getWorldSettings().getStructureSettings();
        if (overrides != null) {
            for (Map.Entry<String, Boolean> entry : overrides.entrySet()) {
                if (normalize(entry.getKey()).equals(normalizedKey)) {
                    return Boolean.FALSE.equals(entry.getValue());
                }
            }
        }
        // Otherwise fall back to the global default list.
        return plugin.getSettings().getDisabledStructures().stream()
                .anyMatch(key -> normalize(key).equals(normalizedKey));
    }

    private String normalize(String key) {
        return key.toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
