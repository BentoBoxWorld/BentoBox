package world.bentobox.bentobox.blueprints.conversation;

import org.bukkit.Material;
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

    public NameSuccessPrompt(GameModeAddon addon) {
        this.addon = addon;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        String name = (String) context.getSessionData("name");
        String uniqueId = (String) context.getSessionData("uniqueId");
        BlueprintBundle bb = new BlueprintBundle();
        bb.setIcon(Material.RED_WOOL);
        bb.setUniqueId(uniqueId);
        bb.setDisplayName(name);
        BentoBox.getInstance().getBlueprintsManager().addBlueprintBundle(addon, bb);
        BentoBox.getInstance().getBlueprintsManager().saveBlueprintBundle(addon, bb);
        User user = User.getInstance((Player)context.getForWhom());
        new BlueprintManagementPanel(BentoBox.getInstance(), user, addon).openPanel();
        // Set the name
        // if successfully
        return user.getTranslation("commands.admin.blueprint.management.description.success");
        // Else return failure
    }

    @Override
    protected Prompt getNextPrompt(ConversationContext context) {
        return Prompt.END_OF_CONVERSATION;
    }

}