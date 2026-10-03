package world.bentobox.bentobox.hooks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import world.bentobox.bentobox.CommonTestSetup;

class MultiverseCore4HookTest extends CommonTestSetup {

    @Mock
    private MultiverseCore core;
    @Mock
    private MVWorldManager worldManager;
    @Mock
    private MultiverseWorld mvWorld;
    @Mock
    private World bentoWorld;

    private MultiverseCore4Hook hook;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(pim.getPlugin("Multiverse-Core")).thenReturn(core);
        when(core.getMVWorldManager()).thenReturn(worldManager);
        when(worldManager.getMVWorld("bskyblock_world")).thenReturn(mvWorld);
        when(bentoWorld.getName()).thenReturn("bskyblock_world");
        when(bentoWorld.getEnvironment()).thenReturn(Environment.NORMAL);
        when(bentoWorld.getWorldType()).thenReturn(WorldType.NORMAL);
        hook = new MultiverseCore4Hook();
    }

    /**
     * A world Multiverse has never seen is added with auto-load turned off, so BentoBox loads it.
     */
    @Test
    void testRegisterWorldNewWorldSetsAutoLoadFalse() {
        when(worldManager.addWorld(eq("bskyblock_world"), any(), anyString(), any(), any(), isNull()))
                .thenReturn(true);

        hook.registerWorld(bentoWorld, false);

        verify(mvWorld).setAutoLoad(false);
        verify(worldManager).saveWorldsConfig();
    }

    /**
     * A world Multiverse already knows is left alone, so an admin's auto-load: true survives restarts.
     */
    @Test
    void testRegisterWorldExistingWorldLeavesAutoLoadAlone() {
        when(worldManager.addWorld(eq("bskyblock_world"), any(), anyString(), any(), any(), isNull()))
                .thenReturn(false);

        hook.registerWorld(bentoWorld, false);

        verify(mvWorld, never()).setAutoLoad(Mockito.anyBoolean());
        verify(worldManager, never()).saveWorldsConfig();
    }
}
