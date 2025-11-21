package world.bentobox.bentobox.api.commands.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandDeletionManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class AdminRegisterCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;

    private UUID notUUID;

    private IslandDeletionManager idm;
    private AdminRegisterCommand itl;
    @Mock
    private Block block;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);

        Settings settings = new Settings();
        // Settings
        when(plugin.getSettings()).thenReturn(settings);
        // World
        when(ac.getWorld()).thenReturn(world);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while (notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getWorld()).thenReturn(world);
        when(user.getLocation()).thenReturn(location);
        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        User.getInstance(mockPlayer);
        User.setPlugin(plugin);

        // Util
        mockedUtil.when(() -> Util.getUUID("tastybento")).thenReturn(uuid);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Deletion Manager
        idm = mock(IslandDeletionManager.class);
        when(idm.inDeletion(any())).thenReturn(false);
        when(plugin.getIslandDeletionManager()).thenReturn(idm);

        // Island
        when(island.getWorld()).thenReturn(world);
        when(island.getCenter()).thenReturn(location);
        when(im.createIsland(any(), eq(uuid))).thenReturn(island);
        when(location.getBlock()).thenReturn(block);

        // DUT
        itl = new AdminRegisterCommand(ac);
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testCanExecuteNoTarget() {
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for
     * {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testCanExecuteWrongWorld() {
        when(user.getWorld()).thenReturn(mock(World.class));
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("general.errors.wrong-world");
    }

    /**
     * Test method for
     * {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testCanExecuteUnknownPlayer() {
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento2")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "tastybento2");
    }

    /**
     * Test method for
     * {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testCanExecuteNoIsland() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).getTranslation("commands.admin.register.no-island-here");
    }


    /**
     * Test method for {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testCanExecuteAlreadyOwnedIsland() {
        when(im.inTeam(any(), any())).thenReturn(false);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(pm.getUUID(any())).thenReturn(notUUID);
        Location loc = mock(Location.class);
        when(loc.toVector()).thenReturn(new Vector(1, 2, 3));
        // Island has owner
        when(island.getOwner()).thenReturn(uuid);
        when(island.isOwned()).thenReturn(true);
        when(island.getCenter()).thenReturn(loc);
        Optional<Island> opi = Optional.of(island);
        when(im.getIslandAt(any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(loc);

        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("commands.admin.register.already-owned");
    }

    /**
     * Test method for {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testCanExecuteInDeletionIsland() {
        when(idm.inDeletion(any())).thenReturn(true);
        when(im.inTeam(any(), any())).thenReturn(false);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(pm.getUUID(any())).thenReturn(notUUID);
        Location loc = mock(Location.class);

        // Island has owner
        when(island.getOwner()).thenReturn(uuid);
        Optional<Island> opi = Optional.of(island);
        when(im.getIslandAt(any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(loc);

        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("commands.admin.register.in-deletion");
    }

    /**
     * Test method for {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testCanExecuteSuccess() {
        when(location.toVector()).thenReturn(new Vector(123,123,432));
        when(island.getCenter()).thenReturn(location);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        Optional<Island> opi = Optional.of(island);
        when(im.getIslandAt(any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(location);
        when(pm.getUUID(any())).thenReturn(notUUID);

        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
    }

    /**
     * Test method for {@link AdminRegisterCommand#register(User, String)}.
     */
    @Test
    public void testRegister() {
        testCanExecuteSuccess();
        when(island.isSpawn()).thenReturn(true);
        itl.register(user, "tastybento");
        verify(im).setOwner(user, uuid, island, RanksManager.VISITOR_RANK);
        verify(im).clearSpawn(world);
        verify(user).sendMessage("commands.admin.register.registered-island", TextVariables.XYZ, "123,123,432", TextVariables.NAME,
                "tastybento");
        verify(user).sendMessage("general.success");
    }

    /**
     * Test method for {@link AdminRegisterCommand#reserve(User, String)}.
     */
    @Test
    public void testReserveCannotMakeIsland() {
        when(im.createIsland(any(), eq(uuid))).thenReturn(null);
        testCanExecuteNoIsland();
        itl.reserve(user, "tastybento");
        verify(im).createIsland(any(), eq(uuid));
        verify(user).sendMessage("commands.admin.register.cannot-make-island");
    }

    /**
     * Test method for {@link AdminRegisterCommand#reserve(User, String)}.
     */
    @Test
    public void testReserveCanMakeIsland() {
        testCanExecuteNoIsland();
        itl.reserve(user, "tastybento");
        verify(im).createIsland(any(), eq(uuid));
        verify(user, never()).sendMessage("commands.admin.register.cannot-make-island");
        verify(block).setType(Material.BEDROCK);
        verify(user).sendMessage("commands.admin.register.reserved-island", TextVariables.XYZ, "0,0,0", TextVariables.NAME,
                "tastybento");
    }

}
