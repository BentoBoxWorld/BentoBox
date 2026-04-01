package world.bentobox.bentobox.blueprints.conversation;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;


/**
 * Collects a list of commands to run when a blueprint bundle is pasted.
 * Each line typed is added as a command. Typing "quit" saves and finishes.
 * Typing "clear" removes all previously entered commands in this session.
 * @author tastybento
 * @since 2.6.0
 */
public class CommandsPrompt extends StringPrompt {

    private static final String COMMANDS = "commands";
    private final GameModeAddon addon;
    private final BlueprintBundle bb;

    public CommandsPrompt(GameModeAddon addon, BlueprintBundle bb) {
        this.addon = addon;
        this.bb = bb;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull String getPromptText(ConversationContext context) {
        User user = User.getInstance((Player) context.getForWhom());
        if (context.getSessionData(COMMANDS) != null) {
            StringBuilder sb = new StringBuilder();
            for (String line : ((List<String>) context.getSessionData(COMMANDS))) {
                sb.append(user.getTranslation("commands.admin.blueprint.management.commands.default-color"));
                sb.append(line);
                sb.append(System.lineSeparator());
            }
            // Send formatted message directly since Bukkit conversations don't parse MiniMessage
            user.sendRawMessage(sb.toString());
            return "";
        }
        String msg = user.getTranslation("commands.admin.blueprint.management.commands.instructions",
                TextVariables.NAME, bb.getDisplayName());
        user.sendRawMessage(msg);
        return "";
    }

    @SuppressWarnings("unchecked")
    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
        User user = User.getInstance((Player) context.getForWhom());
        if (input.equals(user.getTranslation("commands.admin.blueprint.management.commands.quit"))) {
            return new CommandsSuccessPrompt(addon, bb);
        }
        if (input.equals(user.getTranslation("commands.admin.blueprint.management.commands.clear"))) {
            context.setSessionData(COMMANDS, new ArrayList<>());
            return this;
        }
        List<String> cmds = new ArrayList<>();
        if (context.getSessionData(COMMANDS) != null) {
            cmds = ((List<String>) context.getSessionData(COMMANDS));
        }
        cmds.add(input);
        context.setSessionData(COMMANDS, cmds);
        return this;
    }
}
