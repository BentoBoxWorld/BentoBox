package world.bentobox.bentobox.panels;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.conversation.CommandsPrompt;
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
    private static final int BUNDLES_PER_PAGE = 36;
    private static final int BLUEPRINTS_PER_PAGE = 18;
    private static final String INSTRUCTION = "instruction";
    private Entry<Integer, Blueprint> selected;
    private final Map<Integer, Blueprint> blueprints = new HashMap<>();
    private final User user;
    private final GameModeAddon addon;
    private int bundlePage;
    private int blueprintPage;

    /**
     * Class to display the Blueprint Management Panel
     * @param plugin - BentoBox
     * @param user - user to see the panel
     * @param addon - game mode addon requesting the panel
     */
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
        slotToEnvironment = Map.of(3, World.Environment.NORMAL, 5, World.Environment.NETHER, 7, World.Environment.THE_END);
        environmentToBlueprint = Map.of(World.Environment.NORMAL, normalBlueprint, World.Environment.NETHER, netherBlueprint, World.Environment.THE_END, endBlueprint);
    }

    /**
     * Translate "commands.admin.blueprint.management." + t reference
     * @param t - end of reference
     * @return translation
     */
    private String t(String t) {
        return user.getTranslation("commands.admin.blueprint.management." + t);
    }

    /**
     * Translate "commands.admin.blueprint.management." + t + vars reference
     * @param t end of reference
     * @param vars any other parameters
     * @return translation
     */
    private String t(String t, String... vars) {
        return user.getTranslation("commands.admin.blueprint.management." + t, vars);
    }

    /**
     * Opens the management panel
     */
    public void openPanel() {
        // Reset blueprint page when returning to the main panel
        blueprintPage = 0;
        // Show panel of blueprint bundles
        // Clicking on a bundle opens up the bundle edit panel
        // Create the panel
        PanelBuilder pb = new PanelBuilder().name(t("title")).user(user).size(45);
        // Panel has New Blueprint Bundle button - clicking in creates a new bundle
        pb.item(36, getNewBundle(addon));
        // Get the bundles sorted by display name
        Comparator<BlueprintBundle> sortByDisplayName = (p, o) -> p.getDisplayName().compareToIgnoreCase(o.getDisplayName());
        List<BlueprintBundle> allBundles = plugin.getBlueprintsManager().getBlueprintBundles(addon).values().stream()
                .sorted(sortByDisplayName).toList();
        int totalPages = Math.max(1, (int) Math.ceil((double) allBundles.size() / BUNDLES_PER_PAGE));
        // Clamp page
        if (bundlePage >= totalPages) {
            bundlePage = totalPages - 1;
        }
        if (bundlePage < 0) {
            bundlePage = 0;
        }
        int start = bundlePage * BUNDLES_PER_PAGE;
        int end = Math.min(start + BUNDLES_PER_PAGE, allBundles.size());
        int slot = 0;
        for (int i = start; i < end; i++) {
            BlueprintBundle bb = allBundles.get(i);
            // Make item
            PanelItem item = new PanelItemBuilder()
                    .name(bb.getDisplayName())
                    .description(t("edit"), t("rename"))
                    .icon(bb.getIconItemStack())
                    .clickHandler((panel, u, clickType, s) -> {
                        u.closeInventory();
                        if (clickType.equals(ClickType.RIGHT)) {
                            // Rename
                            askForName(u.getPlayer(), addon, bb);
                        } else {
                            blueprintPage = 0;
                            openBB(bb);
                        }
                        return true;
                    })
                    .build();
            pb.item(slot++, item);
        }
        // Previous page button
        if (bundlePage > 0) {
            pb.item(37, new PanelItemBuilder().icon(Material.ARROW)
                    .name(t("previous-page"))
                    .clickHandler((panel, u, clickType, s) -> {
                        bundlePage--;
                        openPanel();
                        return true;
                    }).build());
        }
        // Next page button
        if (bundlePage < totalPages - 1) {
            pb.item(38, new PanelItemBuilder().icon(Material.ARROW)
                    .name(t("next-page"))
                    .clickHandler((panel, u, clickType, s) -> {
                        bundlePage++;
                        openPanel();
                        return true;
                    }).build());
        }
        pb.build();
    }

    /**
     * Open the Blueprint Bundle panel
     * @param bb - blueprint bundle
     */
    public void openBB(BlueprintBundle bb) {
        blueprints.clear();
        List<Blueprint> allBlueprints = new ArrayList<>(plugin.getBlueprintsManager().getBlueprints(addon).values());
        int totalPages = Math.max(1, (int) Math.ceil((double) allBlueprints.size() / BLUEPRINTS_PER_PAGE));
        // Clamp page
        if (blueprintPage >= totalPages) {
            blueprintPage = totalPages - 1;
        }
        if (blueprintPage < 0) {
            blueprintPage = 0;
        }
        int start = blueprintPage * BLUEPRINTS_PER_PAGE;
        int end = Math.min(start + BLUEPRINTS_PER_PAGE, allBlueprints.size());
        int slot = 18;
        for (int i = start; i < end; i++) {
            blueprints.put(slot++, allBlueprints.get(i));
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
        blueprints.forEach((key, value) -> pb.item(getBlueprintItem(addon, key, bb, value)));
        // Buttons for non-default bundle
        if (bb.getUniqueId().equals(BlueprintsManager.DEFAULT_BUNDLE_NAME)) {
            // Panel has a No Trash icon. If right-clicked it is discarded
            pb.item(36, getNoTrashIcon());
            // Toggle permission - default is always allowed
            pb.item(39, getNoPermissionIcon());
        } else {
            // Panel has a Trash icon. If right-clicked it is discarded
            pb.item(36, getTrashIcon(addon, bb));
            // Toggle permission - default is always allowed
            pb.item(39, getPermissionIcon(addon, bb));
        }
        // Previous page button for blueprints
        if (blueprintPage > 0) {
            pb.item(37, new PanelItemBuilder().icon(Material.ARROW)
                    .name(t("previous-page"))
                    .clickHandler((panel, u, clickType, s) -> {
                        blueprintPage--;
                        selected = null;
                        openBB(bb);
                        return true;
                    }).build());
        }
        // Next page button for blueprints
        if (blueprintPage < totalPages - 1) {
            pb.item(38, new PanelItemBuilder().icon(Material.ARROW)
                    .name(t("next-page"))
                    .clickHandler((panel, u, clickType, s) -> {
                        blueprintPage++;
                        selected = null;
                        openBB(bb);
                        return true;
                    }).build());
        }
        if (plugin.getSettings().getIslandNumber() > 1) {
            // Number of times allowed
            pb.item(42, getTimesIcon(bb));
        }
        // Preferred slot
        pb.item(40, getSlotIcon(addon, bb));
        // Cost editor
        if (plugin.getSettings().isUseEconomy() && plugin.getVault().isPresent()) {
            pb.item(41, getCostIcon(bb));
        }
        // Commands button
        pb.item(43, getCommandsIcon(addon, bb));
        // Panel has a Back icon.
        pb.item(44, new PanelItemBuilder().icon(Material.OAK_DOOR).name(t("back")).clickHandler((panel, u, clickType, s) -> {
            openPanel();
            return true;
        }).build());

        pb.build();

    }

    private PanelItem getTimesIcon(BlueprintBundle bb) {
        return new PanelItemBuilder().icon(Material.CLOCK).name(t("times"))
                .description(bb.getTimes() == 0 ? t("unlimited-times")
                        : t("maximum-times", TextVariables.NUMBER, String.valueOf(bb.getTimes())))
                .clickHandler((panel, u, clickType, slot) -> {
                    // Left click up, right click down
                    u.getPlayer().playSound(u.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                    if (clickType == ClickType.LEFT) {
                        bb.setTimes(bb.getTimes() + 1);
                    } else if (clickType == ClickType.RIGHT && bb.getTimes() > 0) {
                        bb.setTimes(bb.getTimes() - 1);
                    }
                    // Save
                    plugin.getBlueprintsManager().saveBlueprintBundle(addon, bb);
                    panel.getInventory().setItem(42, getTimesIcon(bb).getItem());
                    return true;
                }).build();
    }

    private PanelItem getCostIcon(BlueprintBundle bb) {
        return new PanelItemBuilder().icon(Material.GOLD_INGOT).name(t("cost"))
                .description(bb.getCost() == 0 ? t("no-cost")
                        : t("cost-amount", TextVariables.COST,
                                plugin.getVault().map(vault -> vault.format(bb.getCost()))
                                        .orElse(String.valueOf(bb.getCost()))))
                .clickHandler((panel, u, clickType, slot) -> {
                    u.getPlayer().playSound(u.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                    if (clickType == ClickType.LEFT) {
                        bb.setCost(bb.getCost() + 1.0);
                    } else if (clickType == ClickType.SHIFT_LEFT) {
                        bb.setCost(bb.getCost() + 100.0);
                    } else if (clickType == ClickType.RIGHT && bb.getCost() >= 1.0) {
                        bb.setCost(bb.getCost() - 1.0);
                    } else if (clickType == ClickType.SHIFT_RIGHT && bb.getCost() >= 100.0) {
                        bb.setCost(bb.getCost() - 100.0);
                    }
                    if (bb.getCost() < 0) {
                        bb.setCost(0);
                    }
                    plugin.getBlueprintsManager().saveBlueprintBundle(addon, bb);
                    panel.getInventory().setItem(41, getCostIcon(bb).getItem());
                    return true;
                }).build();
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
                .description(bb.getDescription())
                .icon(bb.getIconItemStack())
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
        String worldName;
        switch (env) {
        case NORMAL -> {
            icon = Material.GRASS_BLOCK;
            worldName = normalBlueprint.getName();
        }
        case NETHER -> {
            icon = Material.NETHERRACK;
            worldName = netherBlueprint.getName();
        }
        case THE_END -> {
            icon = Material.END_STONE;
            worldName = endBlueprint.getName();
        }
        default -> {
            icon = Material.STONE;
            worldName = Util.prettifyText(env.name());
        }
        }

        return new PanelItemBuilder()
                .name(t("world-name-syntax", TextVariables.NAME, worldName))
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

    private PanelItem getNoTrashIcon() {
        return new PanelItemBuilder()
                .name(t("no-trash"))
                .description(t("no-trash-instructions"))
                .icon(Material.TNT)
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

    private PanelItem getNoPermissionIcon() {
        return new PanelItemBuilder().icon(Material.PAINTING).name(t("no-permission"))
                .description(t("no-perm-required"))
                .build();
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
        desc = new ArrayList<>(desc); // Must be mutable
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
                .icon(blueprint.getIcon())
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
     * Gets the panel item for the commands button
     * @param addon - game mode addon
     * @param bb - blueprint bundle
     * @return panel item
     */
    protected PanelItem getCommandsIcon(GameModeAddon addon, BlueprintBundle bb) {
        List<String> cmds = bb.getCommands();
        return new PanelItemBuilder().icon(Material.COMMAND_BLOCK).name(t("edit-commands"))
                .description(cmds.isEmpty() ? List.of(t("no-commands")) : cmds)
                .clickHandler((panel, u, clickType, slot) -> {
                    u.closeInventory();
                    askForCommands(u.getPlayer(), addon, bb);
                    return true;
                }).build();
    }

    /**
     * Opens a conversation to collect commands for the blueprint bundle
     * @param whom - the conversable player
     * @param addon - game mode addon
     * @param bb - blueprint bundle
     */
    public void askForCommands(Conversable whom, GameModeAddon addon, BlueprintBundle bb) {
        new ConversationFactory(BentoBox.getInstance())
        .withModality(true)
        .withLocalEcho(false)
        .withPrefix(new NameConversationPrefix())
        .withTimeout(90)
        .withFirstPrompt(new CommandsPrompt(addon, bb))
        .withEscapeSequence(t("commands.quit"))
        .buildConversation(whom).begin();
    }

    /**
     * @return the selected
     */
    public Entry<Integer, Blueprint> getSelected() {
        return selected;
    }


}
