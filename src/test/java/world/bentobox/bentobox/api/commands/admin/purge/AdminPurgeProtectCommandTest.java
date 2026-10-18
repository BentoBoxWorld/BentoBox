package world.bentobox.bentobox.api.commands.admin.purge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

class AdminPurgeProtectCommandTest extends CommonTestSetup {
    private AdminPurgeProtectCommand command;
    private User admin;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        admin = mock(User.class);
        when(admin.getLocation()).thenReturn(location);
        when(im.getIslandAt(location)).thenReturn(Optional.of(island));
        command = new AdminPurgeProtectCommand(mock(CompositeCommand.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void interleavedAdminsDoNotToggleAnotherIsland(boolean initiallyProtected) {
        when(island.isPurgeProtected()).thenReturn(initiallyProtected);
        User other = mock(User.class);
        Location otherLocation = mock(Location.class);
        Island otherIsland = mock(Island.class);
        when(other.getLocation()).thenReturn(otherLocation);
        when(im.getIslandAt(otherLocation)).thenReturn(Optional.of(otherIsland));
        assertTrue(command.canExecute(admin, "protect", List.of()));
        assertTrue(command.canExecute(other, "protect", List.of()));
        assertTrue(command.execute(admin, "protect", List.of()));
        verify(island).setPurgeProtected(!initiallyProtected);
        verify(admin).sendMessage(initiallyProtected ? "commands.admin.purge.protect.unprotecting"
                : "commands.admin.purge.protect.protecting");
        verify(otherIsland, never()).setPurgeProtected(anyBoolean());
    }

    @Test
    void leavingIslandBeforeExecutionDoesNotToggleOldIsland() {
        assertTrue(command.canExecute(admin, "protect", List.of()));
        when(im.getIslandAt(location)).thenReturn(Optional.empty());
        assertFalse(command.execute(admin, "protect", List.of()));
        verify(island, never()).setPurgeProtected(anyBoolean());
    }
}
