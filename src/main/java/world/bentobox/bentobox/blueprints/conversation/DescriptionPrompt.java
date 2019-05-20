package world.bentobox.bentobox.blueprints.conversation;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;

/**
 * Collects a description
 * @author tastybento
 *
 */
public class DescriptionPrompt extends StringPrompt {

    private GameModeAddon addon;
    private BlueprintBundle bb;

    public DescriptionPrompt(GameModeAddon addon, BlueprintBundle bb) {
        this.addon = addon;
        this.bb = bb;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getPromptText(ConversationContext context) {
        if (context.getSessionData("description") != null) {
            StringBuilder sb = new StringBuilder();
            for (String line : ((List<String>) context.getSessionData("description"))) {
                sb.append(ChatColor.DARK_PURPLE);
                sb.append(line);
                sb.append(System.getProperty("line.separator"));
            }
            return sb.toString();
        }
        return "Enter a multi line description for " + bb.getDisplayName() + System.getProperty("line.separator")
        + ChatColor.GOLD + " and 'quit' on a line by itself to finish.";
    }

    @SuppressWarnings("unchecked")
    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
        if (input.equals("quit")) {
            return new DescriptionSuccessPrompt(addon, bb);
        }
        List<String> desc = new ArrayList<>();
        if (context.getSessionData("description") != null) {
            desc = ((List<String>) context.getSessionData("description"));
        }
        desc.add(ChatColor.translateAlternateColorCodes('&', input));
        context.setSessionData("description", desc);
        return this;
    }
}

