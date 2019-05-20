package world.bentobox.bentobox.blueprints.conversation;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;

import world.bentobox.bentobox.api.addons.GameModeAddon;

public class NamePrompt extends StringPrompt {

    private GameModeAddon addon;

    public NamePrompt(GameModeAddon addon) {
        this.addon = addon;
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
        // Make a uniqueid
        StringBuilder uniqueId = new StringBuilder(input.toLowerCase().replace(" ", "_"));
        // Check if this name is unique
        int max = 0;
        while (max++ < 32 && addon.getPlugin().getBlueprintsManager().getBlueprintBundles(addon).containsKey(uniqueId.toString())) {
            uniqueId.append("x");
        }
        if (max == 32) {
            context.getForWhom().sendRawMessage("Please pick a more unique name");
            return this;
        }
        context.setSessionData("uniqueId", uniqueId.toString());
        context.setSessionData("name", input);
        return new NameSuccessPrompt(addon);
    }

}

