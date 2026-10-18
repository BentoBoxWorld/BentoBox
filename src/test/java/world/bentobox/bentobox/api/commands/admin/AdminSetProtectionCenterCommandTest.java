package world.bentobox.bentobox.api.commands.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

class AdminSetProtectionCenterCommandTest extends CommonTestSetup {
    private AdminSetProtectionCenterCommand command;
    private User user;
    private final List<Runnable> confirmations = new ArrayList<>();
    private final List<String> first = List.of("1", "64", "2");
    private Location firstLocation;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        user = mock(User.class);
        CompositeCommand parent = mock(CompositeCommand.class);
        when(parent.getWorld()).thenReturn(world);
        firstLocation = new Location(world, 1, 64, 2);
        when(island.getCenter()).thenReturn(firstLocation);
        when(im.getIslandAt(firstLocation)).thenReturn(Optional.of(island));
        command = spy(new AdminSetProtectionCenterCommand(parent));
        doAnswer(call -> {
            confirmations.add(call.getArgument(2));
            return null;
        }).when(command).askConfirmation(any(), any(), any(Runnable.class));
    }

    @Test
    void interleavedConfirmationsKeepEachIslandAndLocation() throws Exception {
        Island other = mock(Island.class);
        Location secondLocation = new Location(world, 256, 70, 256);
        when(other.getCenter()).thenReturn(secondLocation);
        when(im.getIslandAt(secondLocation)).thenReturn(Optional.of(other));
        List<String> second = List.of("256", "70", "256");
        assertTrue(command.canExecute(user, "", first));
        assertTrue(command.canExecute(user, "", second));
        assertTrue(command.execute(user, "", first));
        assertTrue(command.execute(user, "", second));
        confirmations.getFirst().run();
        verify(island).setProtectionCenter(firstLocation);
        verify(other, never()).setProtectionCenter(any());
        confirmations.getLast().run();
        verify(other).setProtectionCenter(secondLocation);
    }

    @Test
    void replacedIslandCannotReusePendingConfirmation() throws Exception {
        assertTrue(command.execute(user, "", first));
        Island replacement = mock(Island.class);
        when(im.getIslandAt(firstLocation)).thenReturn(Optional.of(replacement));
        confirmations.getFirst().run();
        verify(island, never()).setProtectionCenter(any());
        verify(replacement, never()).setProtectionCenter(any());
        verify(user).sendMessage("commands.admin.setspawnpoint.no-island-here");
    }

    @Test
    void removedIslandIsRecheckedBeforeConfirmationPrompt() {
        assertTrue(command.canExecute(user, "", first));
        when(im.getIslandAt(firstLocation)).thenReturn(Optional.empty());
        assertFalse(command.execute(user, "", first));
        assertTrue(confirmations.isEmpty());
    }

    @Test
    void invalidCoordinatesDoNotOpenConfirmation() {
        assertFalse(command.execute(user, "", List.of("bad", "64", "2")));
        assertTrue(confirmations.isEmpty());
        verify(user).sendMessage("commands.admin.setprotectionlocation.xyz-error");
    }

    @Test
    void consoleWithoutCoordinatesIsRejected() {
        assertFalse(command.execute(user, "", List.of()));
        assertTrue(confirmations.isEmpty());
        verify(user).sendMessage("general.errors.use-in-game");
    }

    @Test
    void incompleteCoordinatesDoNotUsePlayersCurrentLocation() {
        assertFalse(command.execute(user, "", List.of("1", "64")));
        assertTrue(confirmations.isEmpty());
        verify(user, never()).getLocation();
    }

    @Test
    void failedCenterChangeReportsOriginalFailureWithoutAddon() throws Exception {
        doThrow(new java.io.IOException("outside island bounds")).when(island).setProtectionCenter(firstLocation);
        assertTrue(command.execute(user, "", first));
        confirmations.getFirst().run();
        verify(plugin).logError("Protection location could not be changed: outside island bounds");
        verify(user).sendMessage("commands.admin.setprotectionlocation.failure",
                TextVariables.XYZ, Util.xyz(firstLocation.toVector()));
    }
}
