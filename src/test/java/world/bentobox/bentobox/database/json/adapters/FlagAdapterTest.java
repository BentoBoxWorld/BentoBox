package world.bentobox.bentobox.database.json.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {Bukkit.class, BentoBox.class, Flags.class, Util.class} )
public class FlagAdapterTest {

    private BentoBox plugin;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        Server server = mock(Server.class);

        PluginManager pim = mock(PluginManager.class);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getServer()).thenReturn(server);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        ItemMeta meta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(meta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);

        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);
    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    @Test
    public void testWriteJsonWriterFlag() throws IOException {
        FlagTypeAdapter fa = new FlagTypeAdapter(plugin);
        JsonWriter out = mock(JsonWriter.class);
        Flag value = Flags.ANVIL;
        fa.write(out, value);
        Mockito.verify(out).value("ANVIL");
    }

    @Test
    public void testWriteJsonWriterFlagNull() throws IOException {
        FlagTypeAdapter fa = new FlagTypeAdapter(plugin);
        JsonWriter out = mock(JsonWriter.class);
        Flag value = null;
        fa.write(out, value);
        Mockito.verify(out).nullValue();
    }

    @Test
    public void testReadJsonReaderNull() throws IOException {
        FlagTypeAdapter fa = new FlagTypeAdapter(plugin);
        JsonReader reader = mock(JsonReader.class);
        Mockito.when(reader.peek()).thenReturn(JsonToken.NULL);
        Flag flag = fa.read(reader);
        Mockito.verify(reader).nextNull();
        assertNull(flag);
    }

    @Test
    public void testReadJsonReader() throws IOException {
        FlagTypeAdapter fa = new FlagTypeAdapter(plugin);
        JsonReader reader = mock(JsonReader.class);
        Mockito.when(reader.peek()).thenReturn(JsonToken.STRING);
        Mockito.when(reader.nextString()).thenReturn("ANVIL");
        Flag flag = fa.read(reader);
        Mockito.verify(reader).nextString();
        assertEquals(Flags.ANVIL, flag);
    }

    @Test
    public void testReadJsonReaderNoSuchFlag() throws IOException {
        FlagTypeAdapter fa = new FlagTypeAdapter(plugin);
        JsonReader reader = mock(JsonReader.class);
        Mockito.when(reader.peek()).thenReturn(JsonToken.STRING);
        Mockito.when(reader.nextString()).thenReturn("MUMBO_JUMBO");
        Flag flag = fa.read(reader);
        Mockito.verify(reader).nextString();
        assertNotNull(flag);
        assertTrue(flag.getID().startsWith("NULL_FLAG_"));
    }
}
