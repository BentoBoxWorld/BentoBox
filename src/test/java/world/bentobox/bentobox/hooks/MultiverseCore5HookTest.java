package world.bentobox.bentobox.hooks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.ImportWorldOptions;
import org.mvplugins.multiverse.core.world.reasons.ImportFailureReason;

import world.bentobox.bentobox.CommonTestSetup;

class MultiverseCore5HookTest extends CommonTestSetup {

    @Mock
    private MultiverseCoreApi api;
    @Mock
    private WorldManager worldManager;
    @Mock
    private LoadedMultiverseWorld mvWorld;
    @Mock
    private World bentoWorld;

    private MockedStatic<MultiverseCoreApi> mockedApi;
    private MultiverseCore5Hook hook;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockedApi = Mockito.mockStatic(MultiverseCoreApi.class);
        mockedApi.when(MultiverseCoreApi::get).thenReturn(api);
        when(api.getWorldManager()).thenReturn(worldManager);
        when(bentoWorld.getName()).thenReturn("bskyblock_world");
        when(bentoWorld.getEnvironment()).thenReturn(Environment.NORMAL);
        hook = new MultiverseCore5Hook();
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        mockedApi.close();
        super.tearDown();
    }

    /**
     * A world Multiverse has never seen is imported with auto-load turned off, so BentoBox loads it.
     */
    @Test
    void testRegisterWorldNewWorldSetsAutoLoadFalse() {
        when(worldManager.importWorld(any(ImportWorldOptions.class))).thenReturn(Attempt.success(mvWorld));

        hook.registerWorld(bentoWorld, false);

        verify(mvWorld).setAutoLoad(false);
        verify(worldManager).saveWorldsConfig();
    }

    /**
     * A world Multiverse already knows is left alone, so an admin's auto-load: true survives restarts.
     */
    @Test
    void testRegisterWorldExistingWorldLeavesAutoLoadAlone() {
        when(worldManager.importWorld(any(ImportWorldOptions.class)))
                .thenReturn(Attempt.failure(ImportFailureReason.WORLD_EXIST_LOADED));

        hook.registerWorld(bentoWorld, false);

        verify(mvWorld, never()).setAutoLoad(Mockito.anyBoolean());
        verify(worldManager, never()).saveWorldsConfig();
    }

    @Test
    void testRegisterWorldNull() {
        hook.registerWorld(null, false);

        verify(worldManager, never()).importWorld(any());
    }
}
