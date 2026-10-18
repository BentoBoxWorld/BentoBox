package world.bentobox.bentobox.api.commands.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

class AdminResetNameCommandTest extends CommonTestSetup {
    private AdminResetNameCommand command;
    private User admin;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(plugin.getIslandsManager()).thenReturn(im);
        admin = mock(User.class);
        command = new AdminResetNameCommand(mock(CompositeCommand.class));
    }

    @Test
    void interleavedAdminsDoNotResetAnotherIsland() {
        User other = mock(User.class);
        Island otherIsland = mock(Island.class);
        var firstLocation = new org.bukkit.Location(world, 0, 64, 0);
        var otherLocation = new org.bukkit.Location(world, 1000, 64, 0);
        when(admin.getLocation()).thenReturn(firstLocation);
        when(other.getLocation()).thenReturn(otherLocation);
        when(im.getIslandAt(firstLocation)).thenReturn(Optional.of(island));
        when(im.getIslandAt(otherLocation)).thenReturn(Optional.of(otherIsland));
        assertTrue(command.canExecute(admin, "resetname", List.of()));
        assertTrue(command.canExecute(other, "resetname", List.of()));
        assertTrue(command.execute(admin, "resetname", List.of()));
        verify(island).setName(null);
        verify(otherIsland, never()).setName(any());
    }

    @Test
    void executionRechecksIslandAfterValidation() {
        when(im.getIslandAt(any())).thenReturn(Optional.of(island), Optional.empty());
        assertTrue(command.canExecute(admin, "resetname", List.of()));
        assertFalse(command.execute(admin, "resetname", List.of()));
        verify(island, never()).setName(any());
    }
}
