package world.bentobox.bentobox.blueprints.conversation;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.panels.BlueprintManagementPanel;

public class NameSuccessPrompt extends MessagePrompt {

    private GameModeAddon addon;
    private BlueprintBundle bb;

    public NameSuccessPrompt(GameModeAddon addon, BlueprintBundle bb) {
        this.addon = addon;
        this.bb = bb;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        String name = (String) context.getSessionData("name");
        bb.setDisplayName(name);
        BentoBox.getInstance().getBlueprintsManager().addBlueprintBundle(addon, bb);
        BentoBox.getInstance().getBlueprintsManager().saveBlueprintBundle(addon, bb);
        new BlueprintManagementPanel(BentoBox.getInstance()).openPanel(User.getInstance((Player)context.getForWhom()), addon);
        // Set the name
        // if successfully
        return "Success!";
        // Else return failure
    }

    @Override
    protected Prompt getNextPrompt(ConversationContext context) {
        return Prompt.END_OF_CONVERSATION;
    }

}