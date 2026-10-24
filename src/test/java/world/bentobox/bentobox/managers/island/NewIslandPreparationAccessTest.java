package world.bentobox.bentobox.managers.island;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.Test;

class NewIslandPreparationAccessTest {
    @Test void rejectsOwnerHomeVisitorWarpAndWalkingInAllIslandDimensions() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("islands");
        var guard = new NewIsland.PreparationAccessGuard(new Location(world, 100, 64, 100), 50);
        for (String name : new String[] {"islands", "islands_nether", "islands_the_end"}) {
            World dimension = mock(World.class);
            when(dimension.getName()).thenReturn(name);
            var target = new Location(dimension, 100, 64, 100);
            for (var cause : new PlayerTeleportEvent.TeleportCause[] {
                    PlayerTeleportEvent.TeleportCause.COMMAND, PlayerTeleportEvent.TeleportCause.PLUGIN,
                    PlayerTeleportEvent.TeleportCause.NETHER_PORTAL}) {
                var teleport = new PlayerTeleportEvent(mock(Player.class), new Location(world, 0, 64, 0), target, cause);
                guard.teleport(teleport);
                assertTrue(teleport.isCancelled());
            }
            var move = new PlayerMoveEvent(mock(Player.class), new Location(world, 49, 64, 100), target);
            guard.move(move);
            assertTrue(move.isCancelled());
        }
    }

    @Test void leavesOtherIslandsAndWorldsAccessibleAndChecksFullIslandBounds() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("islands");
        var guard = new NewIsland.PreparationAccessGuard(new Location(world, 100, 64, 100), 50);
        assertTrue(guard.blocks(new Location(world, 50, 64, 50)));
        assertTrue(guard.blocks(new Location(world, 149, 64, 149)));
        assertFalse(guard.blocks(new Location(world, 150, 64, 100)));
        assertFalse(guard.blocks(new Location(world, 49, 64, 100)));
        assertFalse(guard.blocks(null));
        World other = mock(World.class);
        when(other.getName()).thenReturn("another");
        assertFalse(guard.blocks(new Location(other, 100, 64, 100)));
    }
}
