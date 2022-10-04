package world.bentobox.bentobox.blueprints.conversation;

import org.bukkit.Material;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.panels.BlueprintManagementPanel;

public class NameSuccessPrompt extends MessagePrompt
{

    private final GameModeAddon addon;

    private BlueprintBundle bb;

    private final Blueprint bp;


    /**
     * Handles the name processing
     *
     * @param addon - Game Mode addon
     * @param bb - Blueprint Bundle
     * @param bp - blueprint
     */
    public NameSuccessPrompt(@NonNull GameModeAddon addon, @Nullable BlueprintBundle bb, @Nullable Blueprint bp)
    {
        this.addon = addon;
        this.bb = bb;
        this.bp = bp;
    }


    @Override
    public @NonNull String getPromptText(ConversationContext context)
    {
        String name = (String) context.getSessionData("name");
        String uniqueId = (String) context.getSessionData("uniqueId");

        User user = User.getInstance((Player) context.getForWhom());

        // Rename blueprint
        if (this.bp != null)
        {
            this.addon.getPlugin().getBlueprintsManager().renameBlueprint(this.addon, this.bp, uniqueId, name);
            new BlueprintManagementPanel(this.addon.getPlugin(), user, this.addon).openBB(this.bb);
        }
        else
        {
            // Blueprint Bundle
            if (this.bb == null)
            {
                // New Blueprint bundle
                this.bb = new BlueprintBundle();
                this.bb.setIcon(Material.RED_WOOL);
            }
            else
            {
                // Rename - remove old named file
                this.addon.getPlugin().getBlueprintsManager().deleteBlueprintBundle(this.addon, this.bb);
            }

            this.bb.setDisplayName(name);
            this.bb.setUniqueId(uniqueId);
            this.addon.getPlugin().getBlueprintsManager().addBlueprintBundle(this.addon, this.bb);
            this.addon.getPlugin().getBlueprintsManager().saveBlueprintBundle(this.addon, this.bb);

            new BlueprintManagementPanel(this.addon.getPlugin(), user, this.addon).openPanel();
            // Set the name
        }

        return user.getTranslation("commands.admin.blueprint.management.description.success");
    }


    @Override
    protected Prompt getNextPrompt(@NonNull ConversationContext context)
    {
        return Prompt.END_OF_CONVERSATION;
    }
}