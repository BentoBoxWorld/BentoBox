package world.bentobox.bentobox.api.events.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.PluginManager;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.IslandDeletion;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Bukkit.class })
public class IslandEventTest {

    private Island island;
    private UUID uuid;
    @Mock
    private Location location;
    @Mock
    private @NonNull BlueprintBundle blueprintBundle;
    @Mock
    private IslandDeletion deletedIslandInfo;
    @Mock
    private PluginManager pim;

    /**
     */
    @Before
    public void setUp() throws Exception {
        uuid = UUID.randomUUID();
        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);
        // Island
        island = new Island();
        when(location.clone()).thenReturn(location);
        island.setCenter(location);
    }

    /**
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.events.island.IslandEvent#IslandEvent(world.bentobox.bentobox.database.objects.Island, java.util.UUID, boolean, org.bukkit.Location, world.bentobox.bentobox.api.events.island.IslandEvent.Reason)}.
     */
    @Test
    public void testIslandEvent() {
        for (Reason reason: Reason.values()) {
            IslandEvent e = new IslandEvent(island, uuid, false, location, reason);
            assertEquals(reason, e.getReason());
            assertEquals(island, e.getIsland());
            assertEquals(uuid, e.getPlayerUUID());
            assertEquals(location, e.getLocation());
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.events.island.IslandEvent#builder()}.
     */
    @Test
    public void testBuilder() {
        for (Reason reason: Reason.values()) {
            IslandBaseEvent e = IslandEvent.builder()
                    .admin(true)
                    .blueprintBundle(blueprintBundle)
                    .deletedIslandInfo(deletedIslandInfo)
                    .involvedPlayer(uuid)
                    .island(island)
                    .location(location)
                    .oldIsland(island)
                    .protectionRange(120, 100)
                    .reason(reason)
                    .build();
            e = e.getNewEvent().orElse(e);
            switch (reason) {
                case BAN -> assertTrue(e instanceof IslandBanEvent);
                case CREATE -> assertTrue(e instanceof IslandCreateEvent);
                case CREATED -> assertTrue(e instanceof IslandCreatedEvent);
                case DELETE -> assertTrue(e instanceof IslandDeleteEvent);
                case DELETED -> assertTrue(e instanceof IslandDeletedEvent);
                case DELETE_CHUNKS -> assertTrue(e instanceof IslandDeleteChunksEvent);
                case ENTER -> assertTrue(e instanceof IslandEnterEvent);
                case EXIT -> assertTrue(e instanceof IslandExitEvent);
                case EXPEL -> assertTrue(e instanceof IslandExpelEvent);
                case LOCK -> assertTrue(e instanceof IslandLockEvent);
                case PRECLEAR -> assertTrue(e instanceof IslandPreclearEvent);
                case RANGE_CHANGE -> assertTrue(e instanceof IslandProtectionRangeChangeEvent);
                case REGISTERED -> assertTrue(e instanceof IslandRegisteredEvent);
                case RESERVED -> assertTrue(e instanceof IslandReservedEvent);
                case RESET -> assertTrue(e instanceof IslandResetEvent);
                case RESETTED -> assertTrue(e instanceof IslandResettedEvent);
                case UNBAN -> assertTrue(e instanceof IslandUnbanEvent);
                case UNKNOWN -> assertTrue(e instanceof IslandGeneralEvent);
                case UNLOCK -> assertTrue(e instanceof IslandUnlockEvent);
                case UNREGISTERED -> assertTrue(e instanceof IslandUnregisteredEvent);
                default -> {
                }
            }
        }
        verify(pim, Mockito.times(Reason.values().length * 2)).callEvent(any());
    }

}
