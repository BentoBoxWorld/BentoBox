package world.bentobox.bentobox.util.heads;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URL;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.destroystokyo.paper.profile.PlayerProfile;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.panels.PanelItem;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tests the online-player fast path of {@link HeadGetter#getHead}.
 * @author tastybento
 */
class HeadGetterTest extends CommonTestSetup {

    @Mock
    private HeadRequester requester;
    @Mock
    private PanelItem panelItem;
    @Mock
    private PlayerProfile profile;
    @Mock
    private PlayerTextures textures;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        when(itemFactory.getItemMeta(any())).thenReturn(mock(SkullMeta.class));

        when(mockPlayer.getPlayerProfile()).thenReturn(profile);
        when(profile.getTextures()).thenReturn(textures);
    }

    /**
     * An online player with a textured profile must be delivered synchronously from the
     * server's own profile - no queuing, no web lookup.
     */
    @Test
    void testGetHeadOnlinePlayerDeliversSynchronously() throws Exception {
        URL skin = new URI("https://textures.minecraft.net/texture/test-online").toURL();
        when(textures.getSkin()).thenReturn(skin);
        when(panelItem.getPlayerHeadName()).thenReturn("hg_online");
        mockedBukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
        mockedBukkit.when(() -> Bukkit.getPlayerExact("hg_online")).thenReturn(mockPlayer);

        HeadGetter.getHead(panelItem, requester);

        verify(panelItem).setHead(any(ItemStack.class));
        verify(requester).setHead(panelItem);
    }

    /**
     * An online player whose live profile has no skin texture must not short-circuit -
     * the request falls through to the async resolver queue.
     */
    @Test
    void testGetHeadOnlinePlayerWithoutTextureIsQueued() {
        when(textures.getSkin()).thenReturn(null);
        when(panelItem.getPlayerHeadName()).thenReturn("hg_no_texture");
        mockedBukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
        mockedBukkit.when(() -> Bukkit.getPlayerExact("hg_no_texture")).thenReturn(mockPlayer);

        HeadGetter.getHead(panelItem, requester);

        verify(panelItem, never()).setHead(any());
        verify(requester, never()).setHead(any());
    }

    /**
     * An offline player cannot be resolved synchronously and must be queued.
     */
    @Test
    void testGetHeadOfflinePlayerIsQueued() {
        when(panelItem.getPlayerHeadName()).thenReturn("hg_offline");
        mockedBukkit.when(Bukkit::isPrimaryThread).thenReturn(true);

        HeadGetter.getHead(panelItem, requester);

        verify(panelItem, never()).setHead(any());
        verify(requester, never()).setHead(any());
    }

    /**
     * Once the fast path has run, a second request for the same name is served from the
     * head cache.
     */
    @Test
    void testGetHeadSecondRequestServedFromCache() throws Exception {
        URL skin = new URI("https://textures.minecraft.net/texture/test-cached").toURL();
        when(textures.getSkin()).thenReturn(skin);
        when(panelItem.getPlayerHeadName()).thenReturn("hg_cached");
        mockedBukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
        mockedBukkit.when(() -> Bukkit.getPlayerExact("hg_cached")).thenReturn(mockPlayer);

        HeadGetter.getHead(panelItem, requester);

        // Player logs off; the cache must still serve the head.
        mockedBukkit.when(() -> Bukkit.getPlayerExact("hg_cached")).thenReturn(null);

        HeadRequester secondRequester = mock(HeadRequester.class);
        HeadGetter.getHead(panelItem, secondRequester);

        verify(secondRequester).setHead(panelItem);
    }

    /**
     * The async head task iterates a name's requester set while the main thread may still be
     * adding requesters for the same name, so the set must tolerate concurrent modification.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testRequesterSetSurvivesAddWhileIterating() throws Exception {
        when(panelItem.getPlayerHeadName()).thenReturn("hg_concurrent");
        mockedBukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
        mockedBukkit.when(() -> Bukkit.getPlayerExact("hg_concurrent")).thenReturn(null);

        HeadGetter.getHead(panelItem, requester);

        Field f = HeadGetter.class.getDeclaredField("headRequesters");
        f.setAccessible(true);
        Map<String, Set<HeadRequester>> requesters = (Map<String, Set<HeadRequester>>) f.get(null);
        Set<HeadRequester> set = requesters.get("hg_concurrent");
        assertNotNull(set);
        assertInstanceOf(ConcurrentHashMap.KeySetView.class, set);

        // Simulate the async task iterating while the main thread adds a second requester
        Iterator<HeadRequester> it = set.iterator();
        HeadGetter.getHead(panelItem, mock(HeadRequester.class));
        assertDoesNotThrow(() -> {
            while (it.hasNext()) {
                it.next();
            }
        });
        assertEquals(2, set.size());
    }

}
