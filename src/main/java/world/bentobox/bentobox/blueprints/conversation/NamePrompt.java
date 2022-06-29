package world.bentobox.bentobox.blueprints.conversation;

import java.util.Locale;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import net.md_5.bungee.api.ChatColor;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.util.Util;


public class NamePrompt extends StringPrompt {

    private final GameModeAddon addon;
    @Nullable
    private final BlueprintBundle bb;
    @Nullable
    private Blueprint bp;

    public NamePrompt(@NonNull GameModeAddon addon, @Nullable BlueprintBundle bb) {
        this.addon = addon;
        this.bb = bb;
    }

    public NamePrompt(@NonNull GameModeAddon addon, @Nullable Blueprint bp, @Nullable BlueprintBundle bb) {
        this.addon = addon;
        this.bp = bp;
        this.bb = bb;
    }

    @Override
    public @NonNull String getPromptText(ConversationContext context) {
        User user = User.getInstance((Player)context.getForWhom());
        return user.getTranslation("commands.admin.blueprint.management.name.prompt");
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
        User user = User.getInstance((Player)context.getForWhom());
        // Convert color codes
        input = Util.translateColorCodes(input);
        if (ChatColor.stripColor(input).length() > 32) {
            context.getForWhom().sendRawMessage("Too long");
            return this;

            /*Check if unique name contains chars not supported in regex expression
            Cannot start, contain, or end with special char, cannot contain any numbers.
            Can only contain - for word separation*/
        }else if (ChatColor.stripColor(input).matches("^[a-zA-Z]+(?:-[a-zA-Z]+)*$")) {
             context.getForWhom().sendRawMessage(user.getTranslation("commands.admin.blueprint.management.name.invalid-char-in-unique-name"));
            return this;
        }
        if (bb == null || !bb.getUniqueId().equals(BlueprintsManager.DEFAULT_BUNDLE_NAME)) {
            // Make a uniqueid
            StringBuilder uniqueId = new StringBuilder(ChatColor.stripColor(input).toLowerCase(Locale.ENGLISH).replace(" ", "_"));
            // Check if this name is unique
            int max = 0;
            while (max++ < 32 && addon.getPlugin().getBlueprintsManager().getBlueprintBundles(addon).containsKey(uniqueId.toString())) {
                uniqueId.append("x");
            }
            if (max == 32) {
                context.getForWhom().sendRawMessage(user.getTranslation("commands.admin.blueprint.management.name.pick-a-unique-name"));
                return this;
            }
            context.setSessionData("uniqueId", uniqueId.toString());
        } else {
            // Default stays as default
            context.setSessionData("uniqueId", bb.getUniqueId());
        }
        context.setSessionData("name", input);
        return new NameSuccessPrompt(addon, bb, bp);
    }

}

