package world.bentobox.bentobox.mocks;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.UnsafeValues;
import org.eclipse.jdt.annotation.NonNull;
import org.jspecify.annotations.Nullable;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

/**
 * Utility class for creating mocked instances of the Bukkit Server and its associated components.
 * This is used primarily for testing purposes.
 */
public final class ServerMocks {

    /**
     * Mock implementation of the Paper RegistryAccess interface.
     */
    private static class MockRegistryAccess implements RegistryAccess {
        @Override
        public <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> registryKey) {
            @SuppressWarnings("unchecked")
            Registry<T> registry = mock(Registry.class); // Return a mocked Registry for the given key.
            return registry;
        }

        @Override
        public <T extends Keyed> @Nullable Registry<T> getRegistry(Class<T> type) {
            @SuppressWarnings("unchecked")
            Registry<T> registry = mock(Registry.class); // Return a mocked Registry for the given type.
            return registry;
        }
    }

    /**
     * Creates and returns a mocked Server instance with all necessary dependencies mocked.
     *
     * @return a mocked Server instance
     */
    public static @NonNull Server newServer() {
        // Mock the static ServerBuildInfo class to return mock data
        PowerMockito.mockStatic(ServerBuildInfo.class, Mockito.RETURNS_MOCKS);
        ServerBuildInfo sbi = mock(io.papermc.paper.ServerBuildInfo.class);
        when(ServerBuildInfo.buildInfo()).thenReturn(sbi);
        when(sbi.asString(io.papermc.paper.ServerBuildInfo.StringRepresentation.VERSION_FULL))
                .thenReturn("1.21.4-R0.1-SNAPSHOT");

        // Mock the Server object
        Server serverMock = mock(Server.class);

        // Mock a no-op Logger
        Logger noOp = mock(Logger.class);
        when(serverMock.getLogger()).thenReturn(noOp);
        when(serverMock.isPrimaryThread()).thenReturn(true);
        when(serverMock.getVersion()).thenReturn("123");

        // Mock UnsafeValues for unsafe operations
        UnsafeValues unsafe = mock(UnsafeValues.class);
        when(serverMock.getUnsafe()).thenReturn(unsafe);

        // Mock Paper's RegistryAccess functionality
        mockPaperRegistryAccess();

        // Set the mocked server as the active Bukkit server
        Bukkit.setServer(serverMock);

        // Mock registries for Bukkit static constants
        Map<Class<? extends Keyed>, Object> registers = new HashMap<>();
        doAnswer(invocationGetRegistry -> registers.computeIfAbsent(invocationGetRegistry.getArgument(0), clazz -> {
            Registry<?> registry = mock(Registry.class);
            Map<NamespacedKey, Keyed> cache = new HashMap<>();
            doAnswer(invocationGetEntry -> {
                NamespacedKey key = invocationGetEntry.getArgument(0);

                // Determine the class type of the keyed object from the field name
                Class<? extends Keyed> constantClazz;
                try {
                    constantClazz = (Class<? extends Keyed>) clazz
                            .getField(key.getKey().toUpperCase(Locale.ROOT).replace('.', '_')).getType();
                } catch (ClassCastException | NoSuchFieldException e) {
                    return null;
                }

                // Cache and return mocked Keyed instances
                return cache.computeIfAbsent(key, key1 -> {
                    Keyed keyed = mock(constantClazz);
                    doReturn(key).when(keyed).getKey();
                    return keyed;
                });
            }).when(registry).get((NamespacedKey) notNull());
            return registry;
        })).when(serverMock).getRegistry(notNull());

        // Mock Tags functionality
        doAnswer(invocationGetTag -> {
            Tag<?> tag = mock(Tag.class);
            doReturn(invocationGetTag.getArgument(1)).when(tag).getKey();
            doReturn(Set.of()).when(tag).getValues();
            doAnswer(invocationIsTagged -> {
                Keyed keyed = invocationIsTagged.getArgument(0);
                Class<?> type = invocationGetTag.getArgument(2);

                // Verify if the Keyed object matches the tag
                return type.isAssignableFrom(keyed.getClass()) && (tag.getValues().contains(keyed)
                        || tag.getValues().stream().anyMatch(value -> value.getKey().equals(keyed.getKey())));
            }).when(tag).isTagged(notNull());
            return tag;
        }).when(serverMock).getTag(notNull(), notNull(), notNull());

        // Initialize certain Bukkit classes that rely on static constants
        try {
            Class.forName("org.bukkit.inventory.ItemType");
            Class.forName("org.bukkit.block.BlockType");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return serverMock;
    }

    /**
     * Mocks Paper's RegistryAccess functionality by replacing the RegistryAccess singleton.
     */
    private static void mockPaperRegistryAccess() {
        try {
            RegistryAccess registryAccess = new MockRegistryAccess();

            // Use Unsafe to modify the singleton instance of RegistryAccessHolder
            Field theUnsafe = Class.forName("jdk.internal.misc.Unsafe").getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);

            Field instanceField = Class.forName("io.papermc.paper.registry.RegistryAccessHolder")
                    .getDeclaredField("INSTANCE");
            Method staticFieldBase = unsafe.getClass().getMethod("staticFieldBase", Field.class);
            Method staticFieldOffset = unsafe.getClass().getMethod("staticFieldOffset", Field.class);
            Method putObject = unsafe.getClass().getMethod("putObject", Object.class, long.class, Object.class);

            Object base = staticFieldBase.invoke(unsafe, instanceField);
            long offset = (long) staticFieldOffset.invoke(unsafe, instanceField);
            putObject.invoke(unsafe, base, offset, Optional.of(registryAccess));

        } catch (Exception e) {
            throw new RuntimeException("Failed to mock Paper RegistryAccess", e);
        }
    }

    /**
     * Resets the Bukkit server instance to null. This is useful for cleaning up after tests.
     */
    public static void unsetBukkitServer() {
        try {
            Field server = Bukkit.class.getDeclaredField("server");
            server.setAccessible(true);
            server.set(null, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // Private constructor to prevent instantiation
    private ServerMocks() {
    }
}