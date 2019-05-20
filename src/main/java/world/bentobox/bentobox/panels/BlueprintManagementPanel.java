package world.bentobox.bentobox.panels;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;

import com.google.common.collect.ImmutableMap;

import net.md_5.bungee.api.ChatColor;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.conversation.DescriptionPrompt;
import world.bentobox.bentobox.blueprints.conversation.NameConversationPrefix;
import world.bentobox.bentobox.blueprints.conversation.NamePrompt;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class BlueprintManagementPanel {

    private static final String INFO = "Click on blueprint then click here";
    private BentoBox plugin;
    private final static Blueprint NORMAL_BP = new Blueprint().setIcon(Material.GREEN_STAINED_GLASS_PANE).setName("Normal").setDescription(INFO);
    private final static Blueprint NETHER_BP = new Blueprint().setIcon(Material.RED_STAINED_GLASS_PANE).setName("Nether").setDescription(INFO);
    private final static Blueprint END_BP = new Blueprint().setIcon(Material.YELLOW_STAINED_GLASS_PANE).setName("The End").setDescription(INFO);
    private final static Map<Integer, World.Environment> SLOT_TO_ENV = ImmutableMap.of(3, World.Environment.NORMAL, 5, World.Environment.NETHER, 7, World.Environment.THE_END);
    private final static Map<World.Environment, Blueprint> ENV_TO_BP = ImmutableMap.of(World.Environment.NORMAL, NORMAL_BP, World.Environment.NETHER, NETHER_BP, World.Environment.THE_END, END_BP);
    private static final int MAX_WORLD_SLOT = 9;
    private static final int MIN_WORLD_SLOT = 0;
    private Entry<Integer, Blueprint> selected;
    private Map<Integer, Blueprint> blueprints = new HashMap<>();

    public BlueprintManagementPanel(BentoBox plugin) {
        this.plugin = plugin;
    }

    public void openPanel(@NonNull User user, @NonNull GameModeAddon addon) {
        // Show panel of blueprint bundles
        // Clicking on a bundle opens up the bundle edit panel

        // Create the panel
        PanelBuilder pb = new PanelBuilder().name("Blueprint Bundle Manager").user(user).size(45);
        // Get the bundles
        plugin.getBlueprintsManager().getBlueprintBundles(addon).values().stream().limit(36)
        .forEach(bb -> pb.item(new PanelItemBuilder()
                .name(bb.getDisplayName())
                .description("Click to edit")
                .icon(bb.getIcon())
                .clickHandler((panel, u, clickType, slot) -> {
                    u.closeInventory();
                    openBB(u, addon, bb);
                    return true;
                })
                .build()));

        // Panel has New Blueprint Bundle button - clicking in creates a new bundle
        pb.item(36, getNewBundle(user, addon));

        pb.build();
    }

    public void openBB(User user, @NonNull GameModeAddon addon, BlueprintBundle bb) {
        int index = 18;
        for (Blueprint bp : plugin.getBlueprintsManager().getBlueprints(addon).values()) {
            blueprints.put(index++, bp);
        }
        // Create the panel
        PanelBuilder pb = new PanelBuilder().name(bb.getDisplayName()).user(user).size(45);
        // Display bundle icon
        pb.item(0, new PanelItemBuilder()
                .name("Click to edit description")
                .description(bb.getDescription())
                .icon(bb.getIcon())
                .clickHandler((panel, u, clickType, slot) -> {
                    u.closeInventory();
                    // Description conversation
                    askForDescription(u.getPlayer(), addon, bb);
                    return true;
                })
                .build());
        SLOT_TO_ENV.forEach((k,v) -> {
            String bpName = bb.getBlueprint(v);
            pb.item(k-1, getWorldInstrTile(v));
            pb.item(k, getBlueprintItem(addon, k, bb, plugin.getBlueprintsManager().getBlueprints(addon).getOrDefault(bpName, ENV_TO_BP.get(v))));
        });

        for (int i = 9; i < 18; i++) {
            pb.item(i, new PanelItemBuilder().icon(Material.BLACK_STAINED_GLASS_PANE).name("-").build());
        }
        blueprints.entrySet().stream().limit(18).forEach(b -> pb.item(getBlueprintItem(addon, b.getKey(), bb, b.getValue())));
        // Buttons for non-default bundle
        if (!bb.getUniqueId().equals("default")) {
            // Panel has a Trash icon. If right clicked it is discarded
            pb.item(36, getTrashIcon(addon, bb));
            // Toggle permission - default is always allowed
            pb.item(39, getPermissionIcon(addon, bb));
        }
        // Panel has a Back icon.
        pb.item(44, new PanelItemBuilder().icon(Material.ARROW).name("Back").clickHandler((panel, u, clickType, slot) -> {
            openPanel(u,addon);
            return true;
        }).build());

        pb.build();

    }

    private PanelItem getWorldInstrTile(Environment env) {
        return new PanelItemBuilder()
                .name(Util.prettifyText(env.name()) + " world")
                .description("Place bluprint", "to right to set")
                .icon(Material.GRAY_STAINED_GLASS_PANE)
                .build();
    }

    private PanelItem getTrashIcon(@NonNull GameModeAddon addon, BlueprintBundle bb) {
        return new PanelItemBuilder()
                .name("Trash")
                .description("Right click here to delete")
                .icon(Material.TNT)
                .clickHandler((panel, u, clickType, slot) -> {
                    if (clickType.equals(ClickType.RIGHT)) {
                        u.getPlayer().playSound(u.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1F, 1F);
                        plugin.getBlueprintsManager().deleteBlueprintBundle(addon, bb);
                        openPanel(u,addon);
                    }
                    return true;
                })
                .build();
    }

    private PanelItem getPermissionIcon(@NonNull GameModeAddon addon, BlueprintBundle bb) {
        return new PanelItemBuilder().icon(Material.PAINTING).name("Permission")
                .description(bb.isRequirePermission() ? ChatColor.RED + "Required" : ChatColor.GREEN + "Not required")
                .description(bb.isRequirePermission() ? addon.getPermissionPrefix() + "island.create."  + bb.getUniqueId() : "")
                .clickHandler((panel, u, clickType, slot) -> {
                    // Toggle permission
                    u.getPlayer().playSound(u.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                    bb.setRequirePermission(!bb.isRequirePermission());
                    // Save
                    plugin.getBlueprintsManager().saveBlueprintBundle(addon, bb);
                    panel.getInventory().setItem(39, getPermissionIcon(addon, bb).getItem());
                    return true;
                }).build();
    }

    private PanelItem getBlueprintItem(GameModeAddon addon, int pos, BlueprintBundle bb, Blueprint blueprint) {
        // Create description
        List<String> desc = blueprint.getDescription() == null ? new ArrayList<>() : blueprint.getDescription();
        if ((!blueprint.equals(END_BP) && !blueprint.equals(NORMAL_BP) && !blueprint.equals(NETHER_BP))) {
            if ((pos > MIN_WORLD_SLOT && pos < MAX_WORLD_SLOT)) {
                desc.add(ChatColor.RED + "Right click to remove");
            } else {
                desc.add(ChatColor.GREEN + "Click to select, then add to bundle");
            }
        }
        return new PanelItemBuilder()
                .name(blueprint.getDisplayName() == null ? blueprint.getName() : blueprint.getDisplayName())
                .description(desc)
                .icon(blueprint.getIcon() == null ? Material.PAPER : blueprint.getIcon())
                .clickHandler((panel, u, clickType, slot) -> {
                    // Handle the world squares
                    if (slot > MIN_WORLD_SLOT && slot < MAX_WORLD_SLOT) {
                        if (clickType.equals(ClickType.RIGHT)) {
                            u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
                            PanelItem item = getBlueprintItem(addon, slot, bb, ENV_TO_BP.get(SLOT_TO_ENV.get(slot)));
                            // Remove the item and replace with the blank
                            bb.clearBlueprint(SLOT_TO_ENV.get(slot));
                            panel.getItems().put(slot, item);
                            panel.getInventory().setItem(slot, item.getItem());
                            // Save
                            plugin.getBlueprintsManager().saveBlueprintBundle(addon, bb);
                        } else if (selected == null) {
                            u.sendRawMessage("Select Blueprint first");
                            u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_ANVIL_HIT, 1F, 1F);
                        } else {
                            // Add
                            u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
                            Blueprint bp = selected.getValue();
                            PanelItem item = getBlueprintItem(addon, slot, bb, bp);
                            // make slot the chosen one
                            bb.setBlueprint(SLOT_TO_ENV.get(slot), bp);
                            panel.getItems().put(slot, item);
                            panel.getInventory().setItem(slot, item.getItem());
                            // Save
                            plugin.getBlueprintsManager().saveBlueprintBundle(addon, bb);
                        }
                    } else {
                        // Select blueprint
                        if (blueprints.containsKey(slot)) {
                            if (selected == null) {
                                // Nothing selected
                                u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 2F);
                                selected = new AbstractMap.SimpleEntry<>(slot, blueprints.get(slot));
                                panel.getInventory().getItem(slot).addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
                            } else if (slot == selected.getKey()){
                                // Clicked on same item
                            } else {
                                // Another item already selected
                                panel.getInventory().getItem(selected.getKey()).removeEnchantment(Enchantment.ARROW_DAMAGE);
                                u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 2F);
                                selected = new AbstractMap.SimpleEntry<>(slot, blueprints.get(slot));
                                panel.getInventory().getItem(slot).addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
                            }
                        }

                    }

                    return true;
                })
                .build();
    }

    private PanelItem getNewBundle(@NonNull User user, @NonNull GameModeAddon addon) {
        return new PanelItemBuilder()
                .name("New Bundle")
                .description("Click to make a new bundle")
                .icon(Material.GREEN_BANNER)
                .clickHandler((panel, u, clickType, slot) -> {
                    u.closeInventory();
                    askForName(u.getPlayer(), addon);
                    return true;
                })
                .build();
    }

    public boolean trashBundle(Panel panel, User user, ClickType clickType, int slot ) {
        return true;
    }

    public void askForName(Conversable whom, GameModeAddon addon) {
        new ConversationFactory(BentoBox.getInstance())
        .withModality(true)
        .withLocalEcho(false)
        .withPrefix(new NameConversationPrefix())
        .withTimeout(90)
        .withFirstPrompt(new NamePrompt(addon))
        .withEscapeSequence("exit")
        .withEscapeSequence("quit")
        .buildConversation(whom).begin();
    }

    public void askForDescription(Conversable whom, GameModeAddon addon, BlueprintBundle bb) {
        new ConversationFactory(BentoBox.getInstance())
        .withModality(true)
        .withLocalEcho(false)
        .withPrefix(new NameConversationPrefix())
        .withTimeout(90)
        .withFirstPrompt(new DescriptionPrompt(addon, bb))
        .buildConversation(whom).begin();
    }


}
