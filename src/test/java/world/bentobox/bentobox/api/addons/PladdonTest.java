package world.bentobox.bentobox.api.addons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

/**
 * Tests for how a {@link Pladdon} handles being dropped into the wrong folder.
 */
class PladdonTest {

    @TempDir
    Path plugins;

    private Pladdon pladdon;
    private Addon addon;

    @BeforeEach
    void setUp() {
        pladdon = mock(Pladdon.class, Mockito.CALLS_REAL_METHODS);
        addon = mock(Addon.class);
        doReturn(addon).when(pladdon).getAddon();
        doReturn(Logger.getAnonymousLogger()).when(pladdon).getLogger();
    }

    private File jarIn(Path folder) throws IOException {
        Files.createDirectories(folder);
        File jar = folder.resolve("BSkyBlock-1.0.jar").toFile();
        Files.writeString(jar.toPath(), "jar");
        doReturn(jar).when(pladdon).getFile();
        return jar;
    }

    @Test
    void testIsInAddonsFolder() {
        assertTrue(Pladdon.isInAddonsFolder(new File("plugins/BentoBox/addons/a.jar")));
        assertFalse(Pladdon.isInAddonsFolder(new File("plugins/a.jar")));
        assertFalse(Pladdon.isInAddonsFolder(new File("plugins/Other/addons/a.jar")));
        assertFalse(Pladdon.isInAddonsFolder(new File("a.jar")));
    }

    @Test
    void testOnLoadInAddonsFolderDoesNothing() throws IOException {
        File jar = jarIn(plugins.resolve("BentoBox").resolve("addons"));
        pladdon.onLoad();
        assertFalse(pladdon.isMisplaced());
        assertTrue(jar.exists());
        pladdon.onDisable();
        verify(addon).onDisable();
    }

    @Test
    void testOnLoadInPluginsFolderMovesJar() throws IOException {
        File jar = jarIn(plugins);
        pladdon.onLoad();
        assertTrue(pladdon.isMisplaced());
        assertFalse(jar.exists());
        assertTrue(plugins.resolve("BentoBox/addons/BSkyBlock-1.0.jar").toFile().exists());
        assertEquals("general.misplaced-addon.moved", pladdon.getMisplacedMessageKey());
        // The addon never ran, so it must not be disabled
        pladdon.onDisable();
        verify(addon, never()).onDisable();
    }

    @Test
    void testOnLoadWithCopyAlreadyInAddonsFolder() throws IOException {
        Path addons = plugins.resolve("BentoBox").resolve("addons");
        Files.createDirectories(addons);
        Files.writeString(addons.resolve("BSkyBlock-1.0.jar"), "other");
        File jar = jarIn(plugins);
        pladdon.onLoad();
        assertTrue(pladdon.isMisplaced());
        assertTrue(jar.exists());
        assertEquals("other", Files.readString(addons.resolve("BSkyBlock-1.0.jar")));
        assertEquals("general.misplaced-addon.duplicate", pladdon.getMisplacedMessageKey());
    }
}
