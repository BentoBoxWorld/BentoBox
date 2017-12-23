package bskyblock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;

public class TestTest {
    private final UUID playerUUID = UUID.randomUUID();

    @Before
    public void setUp() {
        /*
        World world = mock(World.class);


        //Mockito.when(world.getWorldFolder()).thenReturn(worldFile);

        Server server = mock(Server.class);
        Mockito.when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        Mockito.when(server.getWorld("world")).thenReturn(world);
        Mockito.when(server.getVersion()).thenReturn("TestTestMocking");
        Mockito.when(server.getVersion()).thenReturn("TestTestMocking");
        Bukkit.setServer(server);
        */
    }

/*
    @Test
    public void createAndSave() {


        IslandBaseEvent event = TeamEvent.builder()
                //.island(getIslands().getIsland(playerUUID))
                .reason(TeamReason.INFO)
                .involvedPlayer(playerUUID)
                .build();
        assertEquals(playerUUID, event.getPlayerUUID());

    }*/
}
