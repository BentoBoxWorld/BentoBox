package world.bentobox.bentobox.blueprints.conversation;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;

public class NamePrompt extends StringPrompt {

    private GameModeAddon addon;
    private BlueprintBundle bb;

    public NamePrompt(GameModeAddon addon, BlueprintBundle bb) {
        this.addon = addon;
        this.bb = bb;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        return "Enter a name, or 'quit' to quit";
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
        if (input.length() > 32) {
            context.getForWhom().sendRawMessage("Too long");
            return this;
        }
        context.setSessionData("name", input);
        return new NameSuccessPrompt(addon, bb);
    }

}

