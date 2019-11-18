package world.bentobox.bentobox.blueprints.conversation;

import org.bukkit.Material;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.panels.BlueprintManagementPanel;

public class NameSuccessPrompt extends MessagePrompt {

    private GameModeAddon addon;
    private BlueprintBundle bb;
    private Blueprint bp;

    /**
     * Handles the name processing
     * @param addon - Game Mode addon
     * @param bb - Blueprint Bundle
     * @param bp - blueprint
     */
    public NameSuccessPrompt(GameModeAddon addon, BlueprintBundle bb, Blueprint bp) {
        this.addon = addon;
        this.bb = bb;
        this.bp = bp;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        String name = (String) context.getSessionData("name");
        String uniqueId = (String) context.getSessionData("uniqueId");
        User user = User.getInstance((Player)context.getForWhom());
        // Rename blueprint
        if (bp != null) {
            BentoBox.getInstance().getBlueprintsManager().renameBlueprint(addon, bp, name);
            new BlueprintManagementPanel(BentoBox.getInstance(), user, addon).openBB(bb);
            return user.getTranslation("commands.admin.blueprint.management.description.success");
        } else {
            // Blueprint Bundle
            if (bb == null) {
                // New Blueprint bundle
                bb = new BlueprintBundle();
                bb.setIcon(Material.RED_WOOL);
            } else {
                // Rename - remove old named file
                BentoBox.getInstance().getBlueprintsManager().deleteBlueprintBundle(addon, bb);
            }
            bb.setDisplayName(name);
            bb.setUniqueId(uniqueId);
            BentoBox.getInstance().getBlueprintsManager().addBlueprintBundle(addon, bb);
            BentoBox.getInstance().getBlueprintsManager().saveBlueprintBundle(addon, bb);

            new BlueprintManagementPanel(BentoBox.getInstance(), user, addon).openPanel();
            // Set the name
            // if successfully
            return user.getTranslation("commands.admin.blueprint.management.description.success");
        }
    }

    @Override
    protected Prompt getNextPrompt(ConversationContext context) {
        return Prompt.END_OF_CONVERSATION;
    }

}