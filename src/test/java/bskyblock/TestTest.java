package bskyblock;

import java.util.UUID;

import org.junit.Before;

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
