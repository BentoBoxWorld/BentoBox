package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;

/**
 * Invites a player by search
 * @author tastybento
 *
 */
public class InviteNamePrompt extends StringPrompt {

    @NonNull
    private final User user;
    @NonNull
    private final IslandTeamInviteCommand itic;

    public InviteNamePrompt(@NonNull User user, IslandTeamInviteCommand islandTeamInviteCommand) {
        this.user = user;
        this.itic = islandTeamInviteCommand;
    }

    @Override
    @NonNull
    public String getPromptText(@NonNull ConversationContext context) {
        return user.getTranslation("commands.island.team.invite.gui.enter-name");
    }

    @Override
    public Prompt acceptInput(@NonNull ConversationContext context, String input) {
        // TODO remove this and pass the options back to the GUI
        if (itic.canExecute(user, itic.getLabel(), List.of(input))) {
            if (itic.execute(user, itic.getLabel(), List.of(input))) {
                return Prompt.END_OF_CONVERSATION;
            }
        }
        // Set the search item to what was entered
        itic.setSearchName(input);
        // Return to the GUI but give a second for the error to show
        // TODO: return the failed input and display the options in the GUI.
        Bukkit.getScheduler().runTaskLater(BentoBox.getInstance(), () -> itic.build(user), 20L);
        return Prompt.END_OF_CONVERSATION;
    }

}
