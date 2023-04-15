package world.bentobox.bentobox.blueprints;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, User.class, Bukkit.class})
public class BlueprintPasterTest {

    private BlueprintPaster bp;
    private BlueprintPaster bp2;
    
    @Mock
    private BentoBox plugin;
    @Mock
    private @NonNull Blueprint blueprint;
    @Mock
    private World world;
    @Mock
    private @NonNull Island island;
    @Mock
    private Location location;
    @Mock
    private @NonNull BlueprintClipboard clipboard;
    @Mock
    private @NonNull User user;


    /**
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        
        // Scheduler
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        
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
        PowerMockito.mockStatic(User.class, Mockito.RETURNS_MOCKS);
        when(User.getInstance(any(UUID.class))).thenReturn(user);

        bp = new BlueprintPaster(plugin, blueprint, world, island);
        bp2 = new BlueprintPaster(plugin, clipboard, location);
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
       PowerMockito.verifyStatic(Bukkit.class, times(1));
       Bukkit.getScheduler();
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintPaster#paste()}.
     */
    @Test
    public void testPaste2() {
        CompletableFuture<Boolean> result = bp2.paste();
        assertNotNull(result);
        PowerMockito.verifyStatic(Bukkit.class, times(1));
        Bukkit.getScheduler();
    }


}
