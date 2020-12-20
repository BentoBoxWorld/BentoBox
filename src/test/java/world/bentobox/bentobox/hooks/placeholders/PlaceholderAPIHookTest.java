package world.bentobox.bentobox.hooks.placeholders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.placeholders.placeholderapi.BentoBoxPlaceholderExpansion;
import world.bentobox.bentobox.managers.IslandWorldManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, PlaceholderAPI.class, Bukkit.class })
public class PlaceholderAPIHookTest {
  // Class under test
  private PlaceholderAPIHook pah;

  @Mock
  private BentoBox plugin;

  @Mock
  private Addon addon;

  @Mock
  private BentoBoxPlaceholderExpansion bentoboxExpansion;

  @Mock
  private IslandWorldManager iwm;

  @Mock
  private GameModeAddon gma;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    // Set up plugin
    plugin = mock(BentoBox.class);
    Whitebox.setInternalState(BentoBox.class, "instance", plugin);
    when(plugin.getIWM()).thenReturn(iwm);
    when(iwm.getAddon(any())).thenReturn(Optional.of(gma));
    PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
    // Desc
    AddonDescription desc = new AddonDescription.Builder("main", "name", "1.0").build();
    when(addon.getDescription()).thenReturn(desc);
    when(gma.getDescription()).thenReturn(desc);
    // PlaceholderAPI
    PowerMockito.mockStatic(PlaceholderAPI.class, Mockito.RETURNS_MOCKS);
    when(PlaceholderAPI.setPlaceholders(any(Player.class), anyString()))
      .thenAnswer((Answer<String>) i -> i.getArgument(1, String.class));
    pah = new PlaceholderAPIHook();
    // Set a default bentoboxExpansion
    pah.setBentoboxExpansion(bentoboxExpansion);
  }

  /**
   * Test method for {@link world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook#getFailureCause()}.
   */
  @Test
  public void testGetFailureCause() {
    assertEquals("could not register BentoBox's expansion", pah.getFailureCause());
  }

  /**
   * Test method for {@link world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook#registerPlaceholder(java.lang.String, world.bentobox.bentobox.api.placeholders.PlaceholderReplacer)}.
   */
  @Test
  public void testRegisterPlaceholderStringPlaceholderReplacer() {
    PlaceholderReplacer replacer = mock(PlaceholderReplacer.class);
    pah.registerPlaceholder("bentobox.placeholder", replacer);
    verify(bentoboxExpansion)
      .registerPlaceholder(eq("bentobox.placeholder"), eq(replacer));
  }

  /**
   * Test method for {@link world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook#registerPlaceholder(world.bentobox.bentobox.api.addons.Addon, java.lang.String, world.bentobox.bentobox.api.placeholders.PlaceholderReplacer)}.
   */
  @Test
  public void testRegisterPlaceholderAddonStringPlaceholderReplacer() {
    PlaceholderReplacer replacer = mock(PlaceholderReplacer.class);
    pah.registerPlaceholder(addon, "testing.placeholder", replacer);
    assertTrue(pah.isPlaceholder(addon, "testing.placeholder"));
  }

  /**
   * Test method for {@link world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook#unregisterPlaceholder(java.lang.String)}.
   */
  @Test
  public void testUnregisterPlaceholderString() {
    testRegisterPlaceholderAddonStringPlaceholderReplacer();
    pah.unregisterPlaceholder("testing.placeholder");
    assertTrue(pah.isPlaceholder(addon, "testing.placeholder"));
    verify(bentoboxExpansion).unregisterPlaceholder(eq("testing.placeholder"));
  }

  /**
   * Test method for {@link world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook#unregisterPlaceholder(world.bentobox.bentobox.api.addons.Addon, java.lang.String)}.
   */
  @Test
  public void testUnregisterPlaceholderAddonString() {
    testRegisterPlaceholderAddonStringPlaceholderReplacer();
    pah.unregisterPlaceholder(addon, "testing.placeholder");
    assertFalse(pah.isPlaceholder(addon, "testing.placeholder"));
  }

  /**
   * Test method for {@link world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook#isPlaceholder(world.bentobox.bentobox.api.addons.Addon, java.lang.String)}.
   */
  @Test
  public void testIsPlaceholder() {
    testRegisterPlaceholderAddonStringPlaceholderReplacer();
    assertFalse(pah.isPlaceholder(addon, "not.a.placeholder"));
  }

  /**
   * Test method for {@link world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook#replacePlaceholders(org.bukkit.entity.Player, java.lang.String)}.
   */
  @Test
  public void testReplacePlaceholders() {
    assertEquals(
      "This is a %test.name.level% test, with %placeholders%, and %name%",
      pah.replacePlaceholders(
        mock(Player.class),
        "This is a %test.[gamemode].level% test, with %placeholders%, and %[gamemode]%"
      )
    );
  }

  /**
   * Test method for {@link world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook#replacePlaceholders(org.bukkit.entity.Player, java.lang.String)}.
   */
  @Test
  public void testReplacePlaceholdersNonGameWorld() {
    when(iwm.getAddon(any())).thenReturn(Optional.empty());
    assertEquals(
      "This is a  test, with %placeholders%, and ",
      pah.replacePlaceholders(
        mock(Player.class),
        "This is a %test.[gamemode].level% test, with %placeholders%, and %[gamemode]%"
      )
    );
  }
}
