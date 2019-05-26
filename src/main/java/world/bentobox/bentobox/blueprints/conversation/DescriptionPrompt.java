package world.bentobox.bentobox.blueprints.conversation;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;

/**
 * Collects a description
 * @author tastybento
 *
 */
public class DescriptionPrompt extends StringPrompt {

    private static final String DESCRIPTION = "description";
    private GameModeAddon addon;
    private BlueprintBundle bb;

    public DescriptionPrompt(GameModeAddon addon, BlueprintBundle bb) {
        this.addon = addon;
        this.bb = bb;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getPromptText(ConversationContext context) {
        User user = User.getInstance((Player)context.getForWhom());
        if (context.getSessionData(DESCRIPTION) != null) {
            StringBuilder sb = new StringBuilder();
            for (String line : ((List<String>) context.getSessionData(DESCRIPTION))) {
                sb.append(user.getTranslation("commands.admin.blueprint.management.description.default-color"));
                sb.append(line);
                sb.append(System.getProperty("line.separator"));
            }
            return sb.toString();
        }
        return user.getTranslation("commands.admin.blueprint.management.description.instructions", TextVariables.NAME, bb.getDisplayName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
        User user = User.getInstance((Player)context.getForWhom());
        if (input.equals(user.getTranslation("commands.admin.blueprint.management.description.quit"))) {
            return new DescriptionSuccessPrompt(addon, bb);
        }
        List<String> desc = new ArrayList<>();
        if (context.getSessionData(DESCRIPTION) != null) {
            desc = ((List<String>) context.getSessionData(DESCRIPTION));
        }
        desc.add(ChatColor.translateAlternateColorCodes('&', input));
        context.setSessionData(DESCRIPTION, desc);
        return this;
    }
}

