package world.bentobox.bentobox.panels;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.conversation.NameConversationPrefix;
import world.bentobox.bentobox.blueprints.conversation.NamePrompt;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class BlueprintManagementPanel {

    private static final String INFO = "Click on blueprint then click here";
    private BentoBox plugin;
    private final Blueprint overWorld = new Blueprint().setIcon(Material.GREEN_STAINED_GLASS_PANE).setName("Overworld").setDescription(INFO);
    private final Blueprint netherWorld = new Blueprint().setIcon(Material.RED_STAINED_GLASS_PANE).setName("Nether").setDescription(INFO);
    private final Blueprint endWorld = new Blueprint().setIcon(Material.YELLOW_STAINED_GLASS_PANE).setName("The End").setDescription(INFO);
    private Entry<Integer, Blueprint> selected;
    private Map<Integer, Blueprint> blueprints = new HashMap<>();

    public BlueprintManagementPanel(BentoBox plugin) {
        this.plugin = plugin;
    }

    private PanelItem trashIcon = new PanelItemBuilder()
            .name("Trash")
            .description("Click to delete")
            .icon(Material.TNT)
            .clickHandler(this::trashBundle)
            .build();

    public void openPanel(@NonNull User user, @NonNull GameModeAddon addon) {
        // Show panel of blueprint bundles
        // Clicking on a bundle opens up the bundle edit panel

        // Create the panel
        PanelBuilder pb = new PanelBuilder().name("Blueprint Manager").user(user).size(45);
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
        // Panel has a Trash icon. If a bundle is dragged to the trash icon then it is discarded
        pb.item(44, trashIcon);
        pb.build();
    }

    private void openBB(User user, @NonNull GameModeAddon addon, BlueprintBundle bb) {
        int index = 18;
        for (Blueprint bp : plugin.getBlueprintsManager().getBlueprints().getOrDefault(addon, new HashMap<>()).values()) {
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
                    // Description conversation
                    return true;
                })
                .build());

        pb.item(2, getBlueprintItem(bb, blueprints.getOrDefault(bb.getBlueprint(World.Environment.NORMAL), overWorld)));
        pb.item(3, getBlueprintItem(bb, blueprints.getOrDefault(bb.getBlueprint(World.Environment.NETHER), netherWorld)));
        pb.item(4, getBlueprintItem(bb, blueprints.getOrDefault(bb.getBlueprint(World.Environment.THE_END), endWorld)));
        for (int i = 9; i < 18; i++) {
            pb.item(i, new PanelItemBuilder().icon(Material.BLACK_STAINED_GLASS_PANE).name("-").build());
        }
        blueprints.values().stream().sorted().limit(18).forEach(b -> pb.item(getBlueprintItem(bb, b)));
        // Panel has a Back icon.
        pb.item(44, new PanelItemBuilder().icon(Material.ARROW).name("Back").build());
        pb.build();

    }

    private PanelItem getBlueprintItem(BlueprintBundle bb, Blueprint blueprint) {

        return new PanelItemBuilder()
                .name(blueprint.getDisplayName() == null ? blueprint.getName() : blueprint.getDisplayName())
                .description(blueprint.getDescription() == null ? new ArrayList<>() : blueprint.getDescription())
                .icon(blueprint.getIcon() == null ? Material.PAPER : blueprint.getIcon())
                .clickHandler((panel, u, clickType, slot) -> {
                    // Handle the world squares
                    if (slot > 1 && slot < 5) {
                        if (selected == null) {
                            u.sendRawMessage("Select Blueprint first");
                            u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_ANVIL_HIT, 1F, 1F);

                        } else if (clickType.equals(ClickType.RIGHT)) {
                            u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
                            // Remove the item and replace with the blank
                            if (slot == 2) {
                                // Overworld
                                bb.clearBlueprint(World.Environment.NORMAL);
                                panel.getItems().put(2, getBlueprintItem(bb, overWorld));
                            } else if (slot == 3) {
                                // Nether
                                bb.clearBlueprint(World.Environment.NETHER);
                                panel.getItems().put(3, getBlueprintItem(bb, netherWorld));
                            } else if (slot == 4) {
                                // Overworld
                                bb.clearBlueprint(World.Environment.THE_END);
                                panel.getItems().put(4, getBlueprintItem(bb, endWorld));
                            }
                        } else {
                            // Add
                            u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
                            // make slot the chosen one
                            if (slot == 2) {
                                // Overworld
                                bb.setBlueprint(World.Environment.NORMAL, selected.getValue());
                                panel.getItems().put(2, getBlueprintItem(bb, blueprint));
                                panel.getInventory().setItem(2, getBlueprintItem(bb, blueprint).getItem());
                            } else if (slot == 3) {
                                // Nether
                                bb.setBlueprint(World.Environment.NETHER, selected.getValue());
                                panel.getItems().put(3, getBlueprintItem(bb, blueprint));
                                panel.getInventory().setItem(3, getBlueprintItem(bb, blueprint).getItem());
                            } else if (slot == 4) {
                                // Overworld
                                bb.setBlueprint(World.Environment.THE_END, selected.getValue());
                                panel.getItems().put(4, getBlueprintItem(bb, blueprint));
                                panel.getInventory().setItem(4, getBlueprintItem(bb, blueprint).getItem());
                            }
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
                                u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
                                selected = null;
                                panel.getInventory().getItem(slot).removeEnchantment(Enchantment.ARROW_DAMAGE);
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
                    BlueprintBundle bb = new BlueprintBundle();
                    bb.setIcon(Material.SNOW_BLOCK);
                    bb.setUniqueId(UUID.randomUUID().toString());
                    bb.setDisplayName("New Bundle");
                    askForName(u.getPlayer(), addon, bb);
                    return true;
                })
                .build();
    }

    public boolean trashBundle(Panel panel, User user, ClickType clickType, int slot ) {
        return true;
    }

    public void askForName(Conversable whom, GameModeAddon addon, BlueprintBundle bb) {
        new ConversationFactory(BentoBox.getInstance())
        .withModality(true)
        .withLocalEcho(false)
        .withPrefix(new NameConversationPrefix())
        .withTimeout(90)
        .withFirstPrompt(new NamePrompt(addon, bb))
        .withEscapeSequence("exit")
        .withEscapeSequence("quit")
        .buildConversation(whom).begin();
    }


}
