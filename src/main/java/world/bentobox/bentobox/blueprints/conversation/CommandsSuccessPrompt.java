package world.bentobox.bentobox.blueprints.conversation;

import java.util.List;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.panels.BlueprintManagementPanel;

/**
 * Saves the collected commands to the blueprint bundle.
 * @author tastybento
 * @since 2.6.0
 */
public class CommandsSuccessPrompt extends MessagePrompt {

    private final GameModeAddon addon;
    private final BlueprintBundle bb;

    /**
     * @param addon game mode addon
     * @param bb blueprint bundle
     */
    public CommandsSuccessPrompt(GameModeAddon addon, BlueprintBundle bb) {
        this.addon = addon;
        this.bb = bb;
    }

    @Override
    public @NonNull String getPromptText(ConversationContext context) {
        User user = User.getInstance((Player) context.getForWhom());
        @SuppressWarnings("unchecked")
        List<String> commands = (List<String>) context.getSessionData("commands");
        String msg;
        if (commands != null) {
            bb.setCommands(commands);
            BentoBox.getInstance().getBlueprintsManager().addBlueprintBundle(addon, bb);
            BentoBox.getInstance().getBlueprintsManager().saveBlueprintBundle(addon, bb);
            new BlueprintManagementPanel(BentoBox.getInstance(), user, addon).openBB(bb);
            msg = user.getTranslation("commands.admin.blueprint.management.commands.success");
        } else {
            msg = user.getTranslation("commands.admin.blueprint.management.commands.cancelling");
        }
        user.sendRawMessage(msg);
        return "";
    }

    @Override
    protected Prompt getNextPrompt(@NonNull ConversationContext context) {
        return Prompt.END_OF_CONVERSATION;
    }

}
