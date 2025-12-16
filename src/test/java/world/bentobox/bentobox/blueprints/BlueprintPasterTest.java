package world.bentobox.bentobox.blueprints;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento
 *
 */
@Disabled("NMS Cannot be tested")
public class BlueprintPasterTest extends CommonTestSetup {

    private BlueprintPaster bp;
    private BlueprintPaster bp2;
    
    @Mock
    private @NonNull Blueprint blueprint;
    @Mock
    private @NonNull BlueprintClipboard clipboard;
    @Mock
    private @NonNull User user;


    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        
        Settings settings = new Settings();
        // Settings
        when(plugin.getSettings()).thenReturn(settings);
        
        // Location
        when(location.toVector()).thenReturn(new Vector(1D,2D,3D));
        
        // Island
        when(island.getProtectionCenter()).thenReturn(location);
        when(island.getOwner()).thenReturn(UUID.randomUUID());
        
        // Clipboard
        when(clipboard.getBlueprint()).thenReturn(blueprint);
        
        // User
        MockedStatic<User> mockedUser = Mockito.mockStatic(User.class, Mockito.RETURNS_MOCKS);
        mockedUser.when(() -> User.getInstance(any(UUID.class))).thenReturn(user);

        bp = new BlueprintPaster(plugin, blueprint, world, island);
        bp2 = new BlueprintPaster(plugin, clipboard, location);
    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintPaster#BlueprintPaster(world.bentobox.bentobox.BentoBox, world.bentobox.bentobox.blueprints.BlueprintClipboard, org.bukkit.Location)}.
     */
    @Test
    public void testBlueprintPasterBentoBoxBlueprintClipboardLocation() {
        assertNotNull(bp2);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintPaster#BlueprintPaster(world.bentobox.bentobox.BentoBox, world.bentobox.bentobox.blueprints.Blueprint, org.bukkit.World, world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testBlueprintPasterBentoBoxBlueprintWorldIsland() {
        assertNotNull(bp);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintPaster#paste()}.
     */
    @Test
    public void testPaste() {
       CompletableFuture<Boolean> result = bp.paste();
       assertNotNull(result);
       mockedBukkit.verify(() -> Bukkit.getScheduler());
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintPaster#paste()}.
     */
    @Test
    public void testPaste2() {
        CompletableFuture<Boolean> result = bp2.paste();
        assertNotNull(result);
        mockedBukkit.verify(() -> Bukkit.getScheduler());
    }


}
