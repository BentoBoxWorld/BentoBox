package world.bentobox.bentobox.blueprints.conversation;

import java.util.Locale;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;

public class NamePrompt extends StringPrompt {

    private GameModeAddon addon;
    private BlueprintBundle bb;
    private Blueprint bp;

    public NamePrompt(GameModeAddon addon, BlueprintBundle bb) {
        this.addon = addon;
        this.bb = bb;
    }

    public NamePrompt(GameModeAddon addon, Blueprint bp, BlueprintBundle bb) {
        this.addon = addon;
        this.bp = bp;
        this.bb = bb;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        User user = User.getInstance((Player)context.getForWhom());
        return user.getTranslation("commands.admin.blueprint.management.name.prompt");
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
        User user = User.getInstance((Player)context.getForWhom());
        // Convert color codes
        input = ChatColor.translateAlternateColorCodes('&', input);
        if (ChatColor.stripColor(input).length() > 32) {
            context.getForWhom().sendRawMessage("Too long");
            return this;
        }
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
        context.setSessionData("name", input);
        return new NameSuccessPrompt(addon, bb, bp);
    }

}

