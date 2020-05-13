package world.bentobox.bentobox.panels;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.web.credits.Contributor;

/**
 * @since 1.9.0
 * @author Poslovitch
 */
public class CreditsPanel {

    private static final String LOCALE_REF = "panel.credits.";
    private static final int[] PANES = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    private CreditsPanel() {}

    public static void openPanel(User user, String repository) {
        BentoBox plugin = BentoBox.getInstance();

        PanelBuilder builder = new PanelBuilder()
                .name(user.getTranslation(LOCALE_REF + "title", TextVariables.NAME, repository.split("/")[1]))
                .size(45);

        // Setup header and corners
        for (int i : PANES) {
            builder.item(i, new PanelItemBuilder().icon(plugin.getSettings().getPanelFillerMaterial()).name(" ").build());
        }

        if (plugin.getWebManager().getContributors(repository).isEmpty()) {
            looksEmpty(builder, user);
        } else {
            for (Contributor contributor : plugin.getWebManager().getContributors(repository)) {
                int slot = getFirstAvailableSlot(builder);

                if (slot == -1) {
                    break; //TODO support multi paging
                }

                PanelItem contributorItem = new PanelItemBuilder()
                        .icon(contributor.getName())
                        .name(user.getTranslation(LOCALE_REF + "contributor.name", TextVariables.NAME, contributor.getName()))
                        .description(user.getTranslation(LOCALE_REF + "contributor.description",
                                "[commits]", String.valueOf(contributor.getCommits())))
                        .clickHandler((panel, user1, clickType, slot1) -> {
                            user.sendRawMessage(ChatColor.GRAY + contributor.getURL());
                            return true;
                        })
                        .build();
                builder.item(getFirstAvailableSlot(builder), contributorItem);
            }
        }

        builder.build().open(user);
    }

    public static void openPanel(User user, Addon addon) {
        openPanel(user, addon.getDescription().getRepository());
    }

    private static void looksEmpty(@NonNull PanelBuilder builder, @NonNull User user) {
        PanelItem emptyHere = new PanelItemBuilder()
                .icon(Material.STRUCTURE_VOID)
                .name(user.getTranslation(LOCALE_REF + "empty-here.name"))
                .description(user.getTranslation(LOCALE_REF + "empty-here.description"))
                .build();

        builder.item(22, emptyHere);
    }

    /**
     * @param pb - panel builder
     * @return first available slot, or -1 if none
     */
    private static int getFirstAvailableSlot(PanelBuilder pb) {
        for (int i = 0; i < 35; i++) {
            if (!pb.slotOccupied(i)) {
                return i;
            }
        }
        return -1;
    }
}
