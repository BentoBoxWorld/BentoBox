package world.bentobox.bentobox.database.json.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
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

        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getServer()).thenReturn(server);

        ItemMeta meta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(meta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);

        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);
    }

    @Test
    public void testFlagAdapter() {
        new FlagAdapter(plugin);
    }

    @Test
    public void testWriteJsonWriterFlag() throws IOException {
        FlagAdapter fa = new FlagAdapter(plugin);
        JsonWriter out = mock(JsonWriter.class);
        Flag value = Flags.ANIMAL_SPAWN;
        fa.write(out, value);
        Mockito.verify(out).value("ANIMAL_SPAWN");
    }

    @Test
    public void testWriteJsonWriterFlagNull() throws IOException {
        FlagAdapter fa = new FlagAdapter(plugin);
        JsonWriter out = mock(JsonWriter.class);
        Flag value = null;
        fa.write(out, value);
        Mockito.verify(out).nullValue();
    }

    @Test
    public void testReadJsonReaderNull() throws IOException {
        FlagAdapter fa = new FlagAdapter(plugin);
        JsonReader reader = mock(JsonReader.class);
        Mockito.when(reader.peek()).thenReturn(JsonToken.NULL);
        Flag flag = fa.read(reader);
        Mockito.verify(reader).nextNull();
        assertNull(flag);
    }

    @Test
    public void testReadJsonReader() throws IOException {
        FlagAdapter fa = new FlagAdapter(plugin);
        JsonReader reader = mock(JsonReader.class);
        Mockito.when(reader.peek()).thenReturn(JsonToken.STRING);
        Mockito.when(reader.nextString()).thenReturn("ANIMAL_SPAWN");
        Flag flag = fa.read(reader);
        Mockito.verify(reader).nextString();
        assertEquals(Flags.ANIMAL_SPAWN, flag);
    }
}
