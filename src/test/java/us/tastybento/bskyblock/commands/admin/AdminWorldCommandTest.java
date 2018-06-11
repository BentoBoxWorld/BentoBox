/**
 * 
 */
package us.tastybento.bskyblock.commands.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.commands.AdminCommand;
import us.tastybento.bskyblock.managers.CommandsManager;
import us.tastybento.bskyblock.managers.FlagsManager;
import us.tastybento.bskyblock.managers.IslandWorldManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.LocalesManager;
import us.tastybento.bskyblock.managers.PlayersManager;
import us.tastybento.bskyblock.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BSkyBlock.class, User.class, Util.class})
public class AdminWorldCommandTest {

    private BSkyBlock plugin;
    private AdminCommand ac;
    private UUID uuid;
    private User user;
    private IslandsManager im;
    private PlayersManager pm;
    private UUID notUUID;
    private Player p;
    private World world;


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(s.getResetWait()).thenReturn(0L);
        when(s.getResetLimit()).thenReturn(3);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while(notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        
        User.setPlugin(plugin);

        // Parent command has no aliases
        ac = mock(AdminCommand.class);
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        world = mock(World.class);
        when(iwm.getIslandWorld()).thenReturn(world);
        when(iwm.getFriendlyName(Mockito.any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);
        when(user.getWorld()).thenReturn(world);
        when(iwm.getWorldName(Mockito.any())).thenReturn("BSkyBlock_world");


        // Player has island to begin with 
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(),Mockito.any())).thenReturn(true);
        when(im.getTeamLeader(Mockito.any(),Mockito.any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        // Has team 
        pm = mock(PlayersManager.class);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments().length > 1 ? invocation.getArgumentAt(1, String.class) : "mock";
            }});
        when(plugin.getLocalesManager()).thenReturn(lm);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(world);
        
        // Inventory
        
        Inventory inv = mock(Inventory.class);
        when(Bukkit.createInventory(Mockito.any(InventoryHolder.class), Mockito.anyInt(), Mockito.anyString())).thenReturn(inv);

    }

    /**
     * Test method for {@link us.tastybento.bskyblock.commands.admin.AdminWorldCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteConsole() {
        // Not for console
        AdminWorldCommand awc = new AdminWorldCommand(ac);
        // , Mockito.withSettings().verboseLogging()
        CommandSender sender = mock(CommandSender.class);
        String[] args = {};
        assertFalse(awc.execute(sender, "world", args));
        Mockito.verify(sender).sendMessage("general.errors.use-in-game");
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.commands.admin.AdminWorldCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecutePlayerWrongWorld() {
        AdminWorldCommand awc = new AdminWorldCommand(ac);
        // Set world to something other that what the user is in
        awc.setWorld(mock(World.class));
        assertFalse(awc.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage("general.errors.wrong-world");
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.commands.admin.AdminWorldCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecutePlayerRightWorld() {
        AdminWorldCommand awc = new AdminWorldCommand(ac);
        // Set world correctly
        awc.setWorld(world);
        // Flags manager
        FlagsManager fm = mock(FlagsManager.class);
        when(plugin.getFlagsManager()).thenReturn(fm);
        List<Flag> list = new ArrayList<>();
        Flag flag = mock(Flag.class);
        when(flag.getType()).thenReturn(Flag.Type.WORLD_SETTING);
        PanelItem pi = mock(PanelItem.class);
        when(pi.getItem()).thenReturn(new ItemStack(Material.GLASS));
        when(flag.toPanelItem(Mockito.any(), Mockito.any())).thenReturn(pi);
        list.add(flag);
        when(fm.getFlags()).thenReturn(list);
        assertTrue(awc.execute(user, new ArrayList<>()));
        Mockito.verify(user).getTranslation(
                "protection.panel.world-settings",
                "[world_name]",
                "BSkyBlock_world"
            );
    }
}
