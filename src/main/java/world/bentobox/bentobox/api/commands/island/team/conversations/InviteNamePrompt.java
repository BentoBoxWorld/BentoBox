package world.bentobox.bentobox.api.commands.island.team.conversations;

import java.util.List;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand;
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
        if (itic.canExecute(user, itic.getLabel(), List.of(input))) {
            itic.execute(user, itic.getLabel(), List.of(input));
        }
        return Prompt.END_OF_CONVERSATION;
    }

}
