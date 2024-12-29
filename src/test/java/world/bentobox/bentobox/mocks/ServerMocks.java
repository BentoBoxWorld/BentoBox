package world.bentobox.bentobox.mocks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.papermc.paper.ServerBuildInfo;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ServerBuildInfo.class)
public final class ServerMocks {
    @Mock
    private static ServerBuildInfo sbi;

    public static @NonNull Server newServer() {
        PowerMockito.mockStatic(ServerBuildInfo.class, Mockito.RETURNS_MOCKS);
        when(sbi.asString(any())).thenReturn("Mock server version");
        when(ServerBuildInfo.buildInfo()).thenReturn(sbi);

        Server mock = mock(Server.class);

        Logger noOp = mock(Logger.class);
        when(mock.getLogger()).thenReturn(noOp);
        when(mock.isPrimaryThread()).thenReturn(true);

        // Unsafe
        UnsafeValues unsafe = mock(UnsafeValues.class);
        when(mock.getUnsafe()).thenReturn(unsafe);

        // Server must be available before tags can be mocked.
        Bukkit.setServer(mock);

        // Bukkit has a lot of static constants referencing registry values. To initialize those, the
        // registries must be able to be fetched before the classes are touched.
        Map<Class<? extends Keyed>, Object> registers = new HashMap<>();

        doAnswer(invocationGetRegistry -> registers.computeIfAbsent(invocationGetRegistry.getArgument(0), clazz -> {
            Registry<?> registry = mock(Registry.class);
            Map<NamespacedKey, Keyed> cache = new HashMap<>();
            doAnswer(invocationGetEntry -> {
                NamespacedKey key = invocationGetEntry.getArgument(0);
                // Some classes (like BlockType and ItemType) have extra generics that will be
                // erased during runtime calls. To ensure accurate typing, grab the constant's field.
                // This approach also allows us to return null for unsupported keys.
                Class<? extends Keyed> constantClazz;
                try {
                    //noinspection unchecked
                    constantClazz = (Class<? extends Keyed>) clazz
                            .getField(key.getKey().toUpperCase(Locale.ROOT).replace('.', '_')).getType();
                } catch (ClassCastException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchFieldException e) {
                    return null;
                }

                return cache.computeIfAbsent(key, key1 -> {
                    Keyed keyed = mock(constantClazz);
                    doReturn(key).when(keyed).getKey();
                    return keyed;
                });
            }).when(registry).get((NamespacedKey) notNull());
            return registry;
        })).when(mock).getRegistry(notNull());

        // Tags are dependent on registries, but use a different method.
        // This will set up blank tags for each constant; all that needs to be done to render them
        // functional is to re-mock Tag#getValues.
        doAnswer(invocationGetTag -> {
            Tag<?> tag = mock(Tag.class);
            doReturn(invocationGetTag.getArgument(1)).when(tag).getKey();
            doReturn(Set.of()).when(tag).getValues();
            doAnswer(invocationIsTagged -> {
                Keyed keyed = invocationIsTagged.getArgument(0);
                Class<?> type = invocationGetTag.getArgument(2);
                if (!type.isAssignableFrom(keyed.getClass())) {
                    return null;
                }
                // Since these are mocks, the exact instance might not be equal. Consider equal keys equal.
                return tag.getValues().contains(keyed)
                        || tag.getValues().stream().anyMatch(value -> value.getKey().equals(keyed.getKey()));
            }).when(tag).isTagged(notNull());
            return tag;
        }).when(mock).getTag(notNull(), notNull(), notNull());

        // Once the server is all set up, touch BlockType and ItemType to initialize.
        // This prevents issues when trying to access dependent methods from a Material constant.
        try {
            Class.forName("org.bukkit.inventory.ItemType");
            Class.forName("org.bukkit.block.BlockType");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return mock;
    }

    public static void unsetBukkitServer() {
        try {
            Field server = Bukkit.class.getDeclaredField("server");
            server.setAccessible(true);
            server.set(null, null);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private ServerMocks() {
    }

}