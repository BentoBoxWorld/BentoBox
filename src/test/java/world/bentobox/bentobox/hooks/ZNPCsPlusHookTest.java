package world.bentobox.bentobox.hooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import lol.pyr.znpcsplus.api.NpcApi;
import lol.pyr.znpcsplus.api.NpcApiProvider;
import lol.pyr.znpcsplus.api.npc.Npc;
import lol.pyr.znpcsplus.api.npc.NpcEntry;
import lol.pyr.znpcsplus.api.npc.NpcRegistry;
import lol.pyr.znpcsplus.api.serialization.NpcSerializer;
import lol.pyr.znpcsplus.api.serialization.NpcSerializerRegistry;
import lol.pyr.znpcsplus.util.NpcLocation;
import world.bentobox.bentobox.BentoBox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Bukkit.class, NpcApiProvider.class })
public class ZNPCsPlusHookTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private PluginManager pim;
    @Mock
    private Plugin mythicMobs;
    @Mock
    private Location location;
    @Mock
    private World world;
    @Mock
    private Plugin npcPlugin;
    private ZNPCsPlusHook hook;
    @Mock
    private NpcEntry entry;
    @Mock
    private NpcApi npcApi;
    @Mock
    private NpcSerializerRegistry npcSerReg;
    @Mock
    private NpcSerializer<Object> ser;
    @Mock
    private NpcRegistry registry;
    @Mock
    private Npc npc;
    @Mock
    private NpcLocation npcLoc;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Bukkit.getPluginManager()).thenReturn(pim);
        when(npcPlugin.getDescription()).thenReturn(new PluginDescriptionFile("ZNPCsPlus", "2.0.0-SNAPSHOT", "main"));
        when(pim.getPlugin("ZNPCsPlus")).thenReturn(npcPlugin);
        // Location
        when(world.getName()).thenReturn("bskyblock");
        when(location.getWorld()).thenReturn(world);
        // NpcApiProvider
        PowerMockito.mockStatic(NpcApiProvider.class, Mockito.RETURNS_MOCKS);
        when(NpcApiProvider.get()).thenReturn(npcApi);

        when(registry.getAll()).thenAnswer(invocation -> List.of(entry));

        when(npcLoc.getBlockX()).thenReturn(0);
        when(npcLoc.getBlockY()).thenReturn(0);
        when(npcLoc.getBlockZ()).thenReturn(0);
        when(npc.getWorld()).thenReturn(world);

        when(npc.getLocation()).thenReturn(npcLoc);

        when(npcApi.getNpcRegistry()).thenReturn(registry);
        when(npcApi.getNpcSerializerRegistry()).thenReturn(npcSerReg);
        when(npcSerReg.getSerializer(any())).thenReturn(ser);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("test", "test");
        when(ser.serialize(any())).thenReturn(yaml);
        when(entry.getNpc()).thenReturn(npc);
        when(ser.deserialize(any())).thenReturn(entry);


        hook = new ZNPCsPlusHook();
    }


    /**
     * Test method for {@link world.bentobox.bentobox.hooks.ZNPCsPlusHook#hook()}.
     */
    @Test
    public void testHook() {
        // Not hooked
        assertFalse(hook.hook());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.ZNPCsPlusHook#getFailureCause()}.
     */
    @Test
    public void testGetFailureCause() {
        when(npcPlugin.getDescription()).thenReturn(new PluginDescriptionFile("ZNPCsPlus", "1.0.0", "main"));
        assertEquals("ZNPCsPlus version 2.0.0-SNAPSHOT required or later. You are running 1.0.0",
                hook.getFailureCause());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.ZNPCsPlusHook#ZNPCsPlusHook()}.
     */
    @Test
    public void testZNPCsPlusHook() {
        assertNotNull(hook);
        assertEquals(Material.PLAYER_HEAD, hook.getIcon());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.ZNPCsPlusHook#serializeNPC(lol.pyr.znpcsplus.api.npc.NpcEntry, org.bukkit.util.Vector)}.
     */
    @Test
    public void testSerializeNPC() {
        assertEquals("test: test\n", hook.serializeNPC(entry, new Vector(1, 1, 1)));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.ZNPCsPlusHook#spawnNpc(java.lang.String, org.bukkit.Location)}.
     */
    @Test
    public void testSpawnNpc() {
        try {
            assertTrue(hook.spawnNpc("", location));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.ZNPCsPlusHook#getNpcsInArea(org.bukkit.World, java.util.List, org.bukkit.util.Vector)}.
     */
    @Test
    public void testGetNpcsInArea() {
        hook.getNpcsInArea(world, List.of(new Vector(0, 0, 0)), new Vector(0, 0, 0));
    }

}
