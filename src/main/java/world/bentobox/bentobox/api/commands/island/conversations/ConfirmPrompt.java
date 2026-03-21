package world.bentobox.bentobox.api.commands.island.conversations;

import org.bukkit.Bukkit;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;

/**
 * Require a confirmation in chat
 * @author tastybento
 *
 */
public class ConfirmPrompt extends StringPrompt {

    @NonNull
    private final User user;
    private final BentoBox plugin;
    private final String instructions;
    private final String response;
    private final Runnable action;

    public ConfirmPrompt(@NonNull User user, BentoBox plugin, String instructions, String response, Runnable action) {
        super();
        this.user = user;
        this.plugin = plugin;
        this.instructions = instructions;
        this.response = response;
        this.action = action;
    }

    @Override
    @NonNull
    public String getPromptText(@NonNull ConversationContext context) {
        return user.getTranslation(instructions);
    }

    @Override
    public Prompt acceptInput(@NonNull ConversationContext context, String input) {
        if (input != null && input.equals(response)) {
            Bukkit.getScheduler().runTask(plugin, () -> user.sendMessage("general.success"));
            Bukkit.getScheduler().runTask(plugin, action);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> user.sendMessage("general.errors.command-cancelled"));
        }
        return Prompt.END_OF_CONVERSATION;
    }

}
