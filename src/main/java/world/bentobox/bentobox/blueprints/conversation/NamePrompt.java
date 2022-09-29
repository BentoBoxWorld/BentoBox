package world.bentobox.bentobox.blueprints.conversation;

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


public class NamePrompt extends StringPrompt
{
    private final GameModeAddon addon;

    @Nullable
    private final BlueprintBundle bb;

    @Nullable
    private Blueprint bp;


    public NamePrompt(@NonNull GameModeAddon addon, @Nullable BlueprintBundle bb)
    {
        this.addon = addon;
        this.bb = bb;
    }


    public NamePrompt(@NonNull GameModeAddon addon, @Nullable Blueprint bp, @Nullable BlueprintBundle bb)
    {
        this.addon = addon;
        this.bp = bp;
        this.bb = bb;
    }


    @Override
    public @NonNull String getPromptText(ConversationContext context)
    {
        User user = User.getInstance((Player) context.getForWhom());
        return user.getTranslation("commands.admin.blueprint.management.name.prompt");
    }


    @Override
    public Prompt acceptInput(ConversationContext context, String input)
    {
        User user = User.getInstance((Player) context.getForWhom());
        String uniqueId = Util.sanitizeInput(input);

        // Convert color codes
        if (ChatColor.stripColor(Util.translateColorCodes(input)).length() > 32)
        {
            context.getForWhom().sendRawMessage(
                user.getTranslation("commands.admin.blueprint.management.name.too-long"));
            return this;
        }

        if (this.bb == null || !this.bb.getUniqueId().equals(BlueprintsManager.DEFAULT_BUNDLE_NAME))
        {
            // Check if this name is unique
            if (this.addon.getPlugin().getBlueprintsManager().getBlueprintBundles(this.addon).containsKey(uniqueId))
            {
                context.getForWhom().sendRawMessage(
                    user.getTranslation("commands.admin.blueprint.management.name.pick-a-unique-name"));
                return this;
            }

            context.setSessionData("uniqueId", uniqueId);
        }
        else
        {
            // Default stays as default
            context.setSessionData("uniqueId", this.bb.getUniqueId());
        }

        context.setSessionData("name", input);

        return new NameSuccessPrompt(this.addon, this.bb, this.bp);
    }
}