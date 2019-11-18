package world.bentobox.bentobox.blueprints.conversation;

import java.util.List;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.panels.BlueprintManagementPanel;

public class DescriptionSuccessPrompt extends MessagePrompt {

    private GameModeAddon addon;
    private BlueprintBundle bb;

    /**
     * @param addon game mode addon
     * @param bb blueprint bundle
     */
    public DescriptionSuccessPrompt(GameModeAddon addon, BlueprintBundle bb) {
        this.addon = addon;
        this.bb = bb;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        User user = User.getInstance((Player)context.getForWhom());
        @SuppressWarnings("unchecked")
        List<String> description = (List<String>)context.getSessionData("description");
        if (description != null) {
            bb.setDescription(description);
            BentoBox.getInstance().getBlueprintsManager().addBlueprintBundle(addon, bb);
            BentoBox.getInstance().getBlueprintsManager().saveBlueprintBundle(addon, bb);
            new BlueprintManagementPanel(BentoBox.getInstance(), user, addon).openBB(bb);
            // Set the name
            // if successfully
            return user.getTranslation("commands.admin.blueprint.management.description.success");
        } else {
            return user.getTranslation("commands.admin.blueprint.management.description.cancelling");
        }
    }

    @Override
    protected Prompt getNextPrompt(ConversationContext context) {
        return Prompt.END_OF_CONVERSATION;
    }

}
