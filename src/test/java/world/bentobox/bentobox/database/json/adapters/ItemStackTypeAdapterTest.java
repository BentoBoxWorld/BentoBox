/**
 *
 */
package world.bentobox.bentobox.database.json.adapters;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Tests the ItemStack type adapter for GSON
 * @author tastybento
 *
 */
@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PrepareForTest( {Bukkit.class} )
public class ItemStackTypeAdapterTest {

    private ItemStackTypeAdapter isa;
    private JsonWriter out;
    private JsonReader reader;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
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
        UnsafeValues unsafe = mock(UnsafeValues.class);
        when(unsafe.getDataVersion()).thenReturn(777);
        when(Bukkit.getUnsafe()).thenReturn(unsafe);
        isa = new ItemStackTypeAdapter();
        out = mock(JsonWriter.class, Mockito.withSettings().verboseLogging());
        reader = mock(JsonReader.class);
        when(reader.peek()).thenReturn(JsonToken.STRING);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.json.adapters.ItemStackTypeAdapter#write(com.google.gson.stream.JsonWriter, org.bukkit.inventory.ItemStack)}.
     * @throws IOException
     */
    @Test
    public void testWriteJsonWriterItemStack() throws IOException {
        ItemStack stack = new ItemStack(Material.STONE, 4);
        isa.write(out, stack);
        Mockito.verify(out).value(Mockito.contains("==: org.bukkit.inventory.ItemStack"));
        Mockito.verify(out).value(Mockito.contains("type: STONE"));
        Mockito.verify(out).value(Mockito.contains("amount: 4"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.json.adapters.ItemStackTypeAdapter#write(com.google.gson.stream.JsonWriter, org.bukkit.inventory.ItemStack)}.
     * @throws IOException
     */
    @Test
    public void testWriteJsonWriterItemStackNull() throws IOException {
        isa.write(out, null);
        Mockito.verify(out).nullValue();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.json.adapters.ItemStackTypeAdapter#read(com.google.gson.stream.JsonReader)}.
     * @throws IOException
     */
    @Test
    public void testReadJsonReaderNull() throws IOException {
        when(reader.peek()).thenReturn(JsonToken.NULL);
        assertNull(isa.read(reader));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.json.adapters.ItemStackTypeAdapter#read(com.google.gson.stream.JsonReader)}.
     * @throws IOException
     */
    @Test
    public void testReadJsonReader() throws IOException {
        // Write a file
        /*
        try (FileWriter writer = new FileWriter("test.json");
                Writer buffWriter = new BufferedWriter(writer);
                JsonWriter realOut = new JsonWriter(buffWriter)) {

            ItemStack stack = new ItemStack(Material.STONE, 4);
            isa.write(realOut, stack);
        }*/
        try (FileReader reader = new FileReader("test.json");
                Reader buffReader = new BufferedReader(reader);
                JsonReader realIn = new JsonReader(buffReader)) {

            ItemStack i = isa.read(realIn);
            System.out.println(i);
        }

    }


}
