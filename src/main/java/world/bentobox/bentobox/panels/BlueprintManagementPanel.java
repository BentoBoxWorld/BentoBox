package world.bentobox.bentobox.panels;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;

import com.google.common.collect.ImmutableMap;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.conversation.DescriptionPrompt;
import world.bentobox.bentobox.blueprints.conversation.NameConversationPrefix;
import world.bentobox.bentobox.blueprints.conversation.NamePrompt;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class BlueprintManagementPanel {

    private final BentoBox plugin;
    private final Blueprint normalBlueprint;
    private final Blueprint netherBlueprint;
    private final Blueprint endBlueprint;
    private final Map<Integer, World.Environment> slotToEnvironment;
    private final Map<World.Environment, Blueprint> environmentToBlueprint;
    private static final int MAX_WORLD_SLOT = 9;
    private static final int MIN_WORLD_SLOT = 0;
    public static final int MAX_BP_SLOT = 35;
    private static final String INSTRUCTION = "instruction";
    private Entry<Integer, Blueprint> selected;
    private Map<Integer, Blueprint> blueprints = new HashMap<>();
    private final User user;
    private final GameModeAddon addon;

    public BlueprintManagementPanel(@NonNull BentoBox plugin, @NonNull User user, @NonNull GameModeAddon addon) {
        this.plugin = plugin;
        this.user = user;
        this.addon = addon;
        normalBlueprint = new Blueprint().setIcon(Material.GREEN_STAINED_GLASS_PANE)
                .setName(user.getTranslation("general.worlds.overworld"))
                .setDescription(t(INSTRUCTION));
        netherBlueprint = new Blueprint().setIcon(Material.RED_STAINED_GLASS_PANE)
                .setName(user.getTranslation("general.worlds.nether"))
                .setDescription(t(INSTRUCTION));
        endBlueprint = new Blueprint().setIcon(Material.YELLOW_STAINED_GLASS_PANE)
                .setName(user.getTranslation("general.worlds.the-end"))
                .setDescription(t(INSTRUCTION));
        slotToEnvironment = ImmutableMap.of(3, World.Environment.NORMAL, 5, World.Environment.NETHER, 7, World.Environment.THE_END);
        environmentToBlueprint = ImmutableMap.of(World.Environment.NORMAL, normalBlueprint, World.Environment.NETHER, netherBlueprint, World.Environment.THE_END, endBlueprint);
    }

    private String t(String t) {
        return user.getTranslation("commands.admin.blueprint.management." + t);
    }

    private String t(String t, String... vars) {
        return user.getTranslation("commands.admin.blueprint.management." + t, vars);
    }

    /**
     * Opens the management panel
     */
    public void openPanel() {
        // Show panel of blueprint bundles
        // Clicking on a bundle opens up the bundle edit panel
        // Create the panel
        PanelBuilder pb = new PanelBuilder().name(t("title")).user(user).size(45);
        // Panel has New Blueprint Bundle button - clicking in creates a new bundle
        pb.item(36, getNewBundle(addon));
        // Get the bundles
        Comparator<BlueprintBundle> sortByDisplayName = (p, o) -> p.getDisplayName().compareToIgnoreCase(o.getDisplayName());
        plugin.getBlueprintsManager().getBlueprintBundles(addon).values().stream().limit(36)
        .sorted(sortByDisplayName)
        .forEach(bb -> {
            // Make item
            PanelItem item = new PanelItemBuilder()
                    .name(bb.getDisplayName())
                    .description(t("edit"),
                            !bb.getUniqueId().equals(BlueprintsManager.DEFAULT_BUNDLE_NAME) ? t("rename") : "")
                    .icon(bb.getIcon())
                    .clickHandler((panel, u, clickType, slot) -> {

                        u.closeInventory();
                        if (clickType.equals(ClickType.RIGHT) && !bb.getUniqueId().equals(BlueprintsManager.DEFAULT_BUNDLE_NAME)) {
                            // Rename
                            askForName(u.getPlayer(), addon, bb);
                        } else {
                            openBB(bb);
                        }
                        return true;
                    })
                    .build();
            // Determine slot
            if (bb.getSlot() < 0 || bb.getSlot() > MAX_BP_SLOT) {
                bb.setSlot(0);
            }
            if (pb.slotOccupied(bb.getSlot())) {
                int slot = getFirstAvailableSlot(pb);
                if (slot == -1) {
                    // TODO add paging
                    plugin.logError("Too many blueprint bundles to show!");
                    pb.item(item);
                } else {
                    pb.item(slot, item);
                }
            } else {
                pb.item(bb.getSlot(), item);
            }
        });

        pb.build();
    }

    /**
     * @param pb - panel builder
     * @return first available slot, or -1 if none
     */
    private static int getFirstAvailableSlot(PanelBuilder pb) {
        for (int i = 0; i < BlueprintManagementPanel.MAX_BP_SLOT; i++) {
            if (!pb.slotOccupied(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Open the Blueprint Bundle panel
     * @param bb - blueprint bundle
     */
    public void openBB(BlueprintBundle bb) {
        int index = 18;
        for (Blueprint bp : plugin.getBlueprintsManager().getBlueprints(addon).values()) {
            blueprints.put(index++, bp);
        }
        // Create the panel
        PanelBuilder pb = new PanelBuilder().name(bb.getDisplayName()).user(user).size(45).listener(new IconChanger(plugin, addon, this, bb));
        // Display bundle icon
        pb.item(0, getBundleIcon(bb));
        slotToEnvironment.forEach((k, v) -> {
            String bpName = bb.getBlueprint(v);
            pb.item(k-1, getWorldInstrTile(v));
            pb.item(k, getBlueprintItem(addon, k, bb, plugin.getBlueprintsManager().getBlueprints(addon).getOrDefault(bpName, environmentToBlueprint.get(v))));
        });

        for (int i = 9; i < 18; i++) {
            pb.item(i, new PanelItemBuilder().icon(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
        }
        blueprints.entrySet().stream().limit(18).forEach(b -> pb.item(getBlueprintItem(addon, b.getKey(), bb, b.getValue())));
        // Buttons for non-default bundle
        if (!bb.getUniqueId().equals(BlueprintsManager.DEFAULT_BUNDLE_NAME)) {
            // Panel has a Trash icon. If right clicked it is discarded
            pb.item(36, getTrashIcon(addon, bb));
            // Toggle permission - default is always allowed
            pb.item(39, getPermissionIcon(addon, bb));
        }
        // Preferred slot
        pb.item(40, getSlotIcon(addon, bb));
        // Panel has a Back icon.
        pb.item(44, new PanelItemBuilder().icon(Material.OAK_DOOR).name(t("back")).clickHandler((panel, u, clickType, slot) -> {
            openPanel();
            return true;
        }).build());

        pb.build();

    }

    /**
     * Gets the preferred slot icon
     * @param addon - addon
     * @param bb - blueprint bundle
     * @return slot panel item
     */
    private PanelItem getSlotIcon(GameModeAddon addon, BlueprintBundle bb) {
        return new PanelItemBuilder()
                .name(t("slot", TextVariables.NUMBER, String.valueOf(bb.getSlot())))
                .description(t("slot-instructions"))
                .icon(Material.IRON_TRAPDOOR)
                .clickHandler((panel, u, clickType, slot) -> {
                    // Increment or decrement slot
                    if (clickType.isLeftClick()) {
                        bb.setSlot(bb.getSlot() + 1);
                        if (bb.getSlot() > MAX_BP_SLOT) {
                            bb.setSlot(0);
                        }
                    } else if (clickType.isRightClick()) {
                        bb.setSlot(bb.getSlot() - 1);
                        if (bb.getSlot() < 0) {
                            bb.setSlot(MAX_BP_SLOT);
                        }
                    }
                    u.getPlayer().playSound(u.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                    // Save
                    plugin.getBlueprintsManager().saveBlueprintBundle(addon, bb);
                    panel.getInventory().setItem(40, getSlotIcon(addon, bb).getItem());
                    return true;
                })
                .build();
    }

    /**
     * Gets the panel item for Blueprint Bundle
     * @param bb - blueprint bundle
     * @return - panel item
     */
    protected PanelItem getBundleIcon(BlueprintBundle bb) {
        return new PanelItemBuilder()
                .name(t("edit-description"))
                .description(bb.getDescription().stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).collect(Collectors.toList()))
                .icon(bb.getIcon())
                .clickHandler((panel, u, clickType, slot) -> {
                    u.closeInventory();
                    // Description conversation
                    askForDescription(u.getPlayer(), addon, bb);
                    return true;
                })
                .build();
    }

    private PanelItem getWorldInstrTile(Environment env) {
        Material icon;
        if (env.equals(Environment.NORMAL)) icon = Material.GRASS_BLOCK;
        else if (env.equals(Environment.NETHER)) icon = Material.NETHERRACK;
        else icon = Material.END_STONE;

        return new PanelItemBuilder()
                .name(t("world-name-syntax", TextVariables.NAME, Util.prettifyText(env.name())))
                .description(t("world-instructions"))
                .glow(true)
                .icon(icon)
                .build();
    }

    private PanelItem getTrashIcon(@NonNull GameModeAddon addon, BlueprintBundle bb) {
        return new PanelItemBuilder()
                .name(t("trash"))
                .description(t("trash-instructions"))
                .icon(Material.TNT)
                .clickHandler((panel, u, clickType, slot) -> {
                    if (clickType.equals(ClickType.RIGHT)) {
                        u.getPlayer().playSound(u.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1F, 1F);
                        plugin.getBlueprintsManager().deleteBlueprintBundle(addon, bb);
                        openPanel();
                    }
                    return true;
                })
                .build();
    }

    private PanelItem getPermissionIcon(@NonNull GameModeAddon addon, BlueprintBundle bb) {
        return new PanelItemBuilder().icon(Material.PAINTING).name(t("permission"))
                .description(bb.isRequirePermission() ? t("perm-required") : t("perm-not-required"))
                .description(bb.isRequirePermission() ? t("perm-format") + addon.getPermissionPrefix() + "island.create."  + bb.getUniqueId() : "")
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

    /**
     * Gets a panel item that fully represents a blueprint in a bundle for an addon
     * @param addon - the GameMode Addon
     * @param pos - the position where this icon will be placed - the description changes
     * @param bb - the blueprint bundle this blueprint is in, if any
     * @param blueprint - blueprint itself
     * @return a panel item
     */
    protected PanelItem getBlueprintItem(GameModeAddon addon, int pos, BlueprintBundle bb, Blueprint blueprint) {
        // Create description
        List<String> desc = blueprint.getDescription() == null ? new ArrayList<>() : blueprint.getDescription();
        desc = desc.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).collect(Collectors.toList());
        if ((!blueprint.equals(endBlueprint) && !blueprint.equals(normalBlueprint) && !blueprint.equals(netherBlueprint))) {
            if ((pos > MIN_WORLD_SLOT && pos < MAX_WORLD_SLOT)) {
                desc.add(t("remove"));
            } else {
                desc.add(t("blueprint-instruction"));
            }
        }
        return new PanelItemBuilder()
                .name(blueprint.getDisplayName() == null ? blueprint.getName() : blueprint.getDisplayName())
                .description(desc)
                .icon(blueprint.getIcon() == null ? Material.PAPER : blueprint.getIcon())
                .glow(selected != null && pos == selected.getKey())
                .clickHandler((panel, u, clickType, slot) -> {
                    // Handle the world squares
                    if (slot > MIN_WORLD_SLOT && slot < MAX_WORLD_SLOT) {
                        if (clickType.equals(ClickType.RIGHT)) {
                            u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
                            // Remove the item and replace with the blank
                            bb.clearBlueprint(slotToEnvironment.get(slot));
                            // Save
                            plugin.getBlueprintsManager().saveBlueprintBundle(addon, bb);
                            openBB(bb);
                        } else if (selected == null) {
                            u.sendMessage("commands.admin.blueprint.management.select-first");
                            u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_ANVIL_HIT, 1F, 1F);
                        } else {
                            // Add
                            u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
                            Blueprint bp = selected.getValue();
                            // make slot the chosen one
                            bb.setBlueprint(slotToEnvironment.get(slot), bp);
                            // Save
                            plugin.getBlueprintsManager().saveBlueprintBundle(addon, bb);
                            openBB(bb);
                        }
                    } else {
                        // Select blueprint
                        if (blueprints.containsKey(slot)) {
                            // Renaming blueprint
                            if (clickType.equals(ClickType.RIGHT)) {
                                u.closeInventory();
                                this.askForBlueprintName(u.getPlayer(), addon, blueprint, bb);
                                return true;
                            }
                            if (selected != null && slot == selected.getKey()){
                                // Clicked on same item - deselect
                                selected = null;
                            } else {
                                // Set selected
                                selected = new AbstractMap.SimpleEntry<>(slot, blueprints.get(slot));
                            }
                            u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 2F);
                            openBB(bb);
                        }
                    }
                    return true;
                })
                .build();
    }

    private PanelItem getNewBundle(@NonNull GameModeAddon addon) {
        return new PanelItemBuilder()
                .name(t("new-bundle"))
                .description(t("new-bundle-instructions"))
                .icon(Material.GREEN_BANNER)
                .clickHandler((panel, u, clickType, slot) -> {
                    u.closeInventory();
                    askForName(u.getPlayer(), addon, null);
                    return true;
                })
                .build();
    }

    public void askForName(Conversable whom, GameModeAddon addon, BlueprintBundle bb) {
        new ConversationFactory(BentoBox.getInstance())
        .withModality(true)
        .withLocalEcho(false)
        .withPrefix(new NameConversationPrefix())
        .withTimeout(90)
        .withFirstPrompt(new NamePrompt(addon, bb))
        .withEscapeSequence(t("name.quit"))
        .buildConversation(whom).begin();
    }

    public void askForBlueprintName(Conversable whom, GameModeAddon addon, Blueprint bp, BlueprintBundle bb) {
        new ConversationFactory(BentoBox.getInstance())
        .withModality(true)
        .withLocalEcho(false)
        .withPrefix(new NameConversationPrefix())
        .withTimeout(90)
        .withFirstPrompt(new NamePrompt(addon, bp, bb))
        .withEscapeSequence(t("name.quit"))
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

    /**
     * @return the selected
     */
    public Entry<Integer, Blueprint> getSelected() {
        return selected;
    }


}
