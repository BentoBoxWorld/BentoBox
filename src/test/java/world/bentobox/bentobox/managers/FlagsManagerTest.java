package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Comparator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

public class FlagsManagerTest extends CommonTestSetup {

    /**
     * Update this value if the number of registered listeners changes
     */
    private static final int NUMBER_OF_LISTENERS = 56;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Plugin is loaded
        when(plugin.isLoaded()).thenReturn(true);

        SkullMeta skullMeta = mock(SkullMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);
        mockedBukkit.when(() -> Bukkit.getItemFactory()).thenReturn(itemFactory);

        // Util
        mockedUtil.when(() -> Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testFlagsManager() {
        assertNotNull(new FlagsManager(plugin));
    }

    @Test
    public void testRegisterDuplicateFlag() {
        FlagsManager fm = new FlagsManager(plugin);
        // Try to register every single flag - it should fail every time
        Flags.values().forEach(dupe -> assertFalse(fm.registerFlag(dupe)));
    }

    @Test
    public void testRegisterOriginalFlagOriginalListener() {
        when(plugin.isLoaded()).thenReturn(true);
        FlagsManager fm = new FlagsManager(plugin);
        verify(pim, times(NUMBER_OF_LISTENERS)).registerEvents(any(), eq(plugin));
        verify(pim, times(NUMBER_OF_LISTENERS)).registerEvents(any(), eq(plugin));
        // This should pass
        OriginalListener ol = new OriginalListener();
        Flag originalFlag = new Flag.Builder("ORIGINAL", Material.EMERALD_BLOCK).listener(ol).build();
        assertTrue(fm.registerFlag(originalFlag));
        // Verify registered one more
        verify(pim, times(NUMBER_OF_LISTENERS+1)).registerEvents(any(), eq(plugin));
        // Register another flag with same listener
        Flag originalFlag2 = new Flag.Builder("ORIGINAL2", Material.COAL_ORE).listener(ol).build();
        assertTrue(fm.registerFlag(originalFlag2));
        // Verify registered only once more
        verify(pim, times(NUMBER_OF_LISTENERS+1)).registerEvents(any(), eq(plugin));
    }

    class OriginalListener implements Listener {
        // nothing here
    }


    /**
     * Test for {@link FlagsManager#getFlags()}
     */
    @Test
    public void testGetFlags() {
        FlagsManager fm = new FlagsManager(plugin);
        assertTrue(Flags.values().containsAll(fm.getFlags()));
        assertTrue(fm.getFlags().containsAll(Flags.values()));
    }

    /**
     * Test for {@link FlagsManager#getFlag(String)}
     */
    @Test
    public void testGetFlagByID() {
        FlagsManager fm = new FlagsManager(plugin);
        // Test in forward and reverse order so that any duplicates are caught
        Flags.values().stream().sorted().forEach(flag -> assertEquals(flag, fm.getFlag(flag.getID()).get()));
        Flags.values().stream().sorted(Comparator.reverseOrder()).forEach(flag -> assertEquals(flag, fm.getFlag(flag.getID()).get()));

    }

    /**
     * Test for {@link FlagsManager#unregister(Flag)}
     */
    @Test
    public void testUnregisterFlag() {
        MockedStatic<HandlerList> mockedHandler = Mockito.mockStatic(HandlerList.class);
        when(plugin.isLoaded()).thenReturn(true);
        FlagsManager fm = new FlagsManager(plugin);
        // Listener
        OriginalListener ol = new OriginalListener();
        Flag originalFlag = new Flag.Builder("ORIGINAL", Material.EMERALD_BLOCK).listener(ol).build();
        assertTrue(fm.registerFlag(originalFlag));
        assertEquals(originalFlag, fm.getFlag("ORIGINAL").get());
        // Remove
        fm.unregister(originalFlag);
        assertFalse(fm.getFlag("ORIGINAL").isPresent());
        // Verify the listener was removed
        mockedHandler.verify(() -> HandlerList.unregisterAll(ol));
    }

}
