//
// Created by BONNe
// Copyright - 2023
//


package world.bentobox.bentobox.panels.customizable;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.util.Util;


/**
 * This class generates Island Creation Panel based on user specified file with name: "island_creation_panel.yml".
 * If file with such name is located at gamemode panels directory, then that file will be used.
 * Otherwise, file in BentoBox/panels is used.
 */
public class IslandCreationPanel extends AbstractPanel
{
    // ---------------------------------------------------------------------
    // Section: Constants
    // ---------------------------------------------------------------------

    /**
     * This constant is used for button to indicate that it is Blueprint Bundle type.
     */
    private static final String BUNDLES = "BUNDLE";
    /**
     * This constant stores value for ERROR message that will be displayed upon failing to run creation commands.
     */
    private static final String ISLAND_CREATION_COMMANDS = "ISLAND_CREATION_COMMANDS";
    /**
     * Button reference
     */
    private static final String BUNDLE_BUTTON_REF = "panels.island_creation.buttons.bundle.";

    // ---------------------------------------------------------------------
    // Section: Variables
    // ---------------------------------------------------------------------

    /**
     * This variable stores filtered elements.
     */
    private final List<BlueprintBundle> elementList;


    /**
     * The world that this command applies to
     */
    private final World world;

    /**
     * true if this panel has been called by a reset command. Changes how the count of used islands is done.
     */
    private final boolean reset;

    // ---------------------------------------------------------------------
    // Section: Constructor
    // ---------------------------------------------------------------------


    /**
     * This is internal constructor. It is used internally in current class to avoid creating objects everywhere.
     *
     * @param command CompositeCommand object
     * @param label The main command label
     * @param user User who opens panel
     * @param reset 
     */
    private IslandCreationPanel(@NonNull CompositeCommand command,
            @NonNull User user, @NonNull String label, boolean reset)
    {
        super(command, user);
        this.mainLabel = label;
        this.world = command.getWorld();
        this.reset = reset;

        this.elementList = this.plugin.getBlueprintsManager().getBlueprintBundles(command.getAddon()).values().stream().
                sorted(Comparator.comparingInt(BlueprintBundle::getSlot).thenComparing(BlueprintBundle::getUniqueId))
                .filter(bundle -> !bundle.isRequirePermission() || this.user
                        .hasPermission(command.getPermissionPrefix() + "island.create." + bundle.getUniqueId()))
                .toList();

    }


    // ---------------------------------------------------------------------
    // Section: Methods
    // ---------------------------------------------------------------------


    /**
     * Build method manages current panel opening. It uses BentoBox PanelAPI that is easy to use and users can get nice
     * panels.
     */
    @Override
    protected void build()
    {
        // Do not open gui if there is no magic sticks.
        if (this.elementList.isEmpty())
        {
            this.plugin.logError("There are no available phases for selection!");
            this.user.sendMessage("no-phases",
                    TextVariables.GAMEMODE, this.plugin.getDescription().getName());
            return;
        }

        // Start building panel.
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();

        // Set main template.
        if (this.doesCustomPanelExists(this.command.getAddon(), "island_creation_panel"))
        {
            // Addon has its own island creation panel. Use it.
            panelBuilder.template("island_creation_panel", new File(this.command.getAddon().getDataFolder(), "panels"));
        }
        else
        {
            // Use default island creation panel.
            panelBuilder.template("island_creation_panel", new File(this.plugin.getDataFolder(), "panels"));
        }

        panelBuilder.user(this.user);
        panelBuilder.world(this.user.getWorld());

        // Register button builders
        panelBuilder.registerTypeBuilder(BUNDLES, this::createBundleButton);

        // Register next and previous builders
        panelBuilder.registerTypeBuilder(NEXT, this::createNextButton);
        panelBuilder.registerTypeBuilder(PREVIOUS, this::createPreviousButton);

        // Register unknown type builder.
        panelBuilder.build();
    }

    // ---------------------------------------------------------------------
    // Section: Buttons
    // ---------------------------------------------------------------------


    /**
     * Create next button panel item.
     *
     * @param template the template
     * @param slot the slot
     * @return the panel item
     */
    @Override
    @Nullable
    protected PanelItem createNextButton(@NonNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        int size = this.elementList.size();

        if (size <= slot.amountMap().getOrDefault(BUNDLES, 1) ||
                1.0 * size / slot.amountMap().getOrDefault(BUNDLES, 1) <= this.pageIndex + 1)
        {
            // There are no next elements
            return null;
        }

        int nextPageIndex = this.pageIndex + 2;

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            ItemStack clone = template.icon().clone();

            if ((boolean) template.dataMap().getOrDefault(INDEXING, false))
            {
                clone.setAmount(nextPageIndex);
            }

            builder.icon(clone);
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.command.getWorld(), template.title()));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(this.command.getWorld(), template.description(),
                    TextVariables.NUMBER, String.valueOf(nextPageIndex)));
        }

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            template.actions().forEach(action -> {
                if ((clickType == action.clickType() ||
                        action.clickType() == ClickType.UNKNOWN) && NEXT.equalsIgnoreCase(action.actionType()))
                {
                    // Next button ignores click type currently.
                    this.pageIndex++;
                    this.build();
                }

            });

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = template.actions().stream().
                filter(action -> action.tooltip() != null)
                .map(action -> this.user.getTranslation(this.command.getWorld(), action.tooltip()))
                .filter(text -> !text.isBlank())
                .collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        return builder.build();
    }


    /**
     * Create previous button panel item.
     *
     * @param template the template
     * @param slot the slot
     * @return the panel item
     */
    @Nullable
    @Override
    protected PanelItem createPreviousButton(@NonNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        if (this.pageIndex == 0)
        {
            // There are no next elements
            return null;
        }

        int previousPageIndex = this.pageIndex;

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            ItemStack clone = template.icon().clone();

            if ((boolean) template.dataMap().getOrDefault(INDEXING, false))
            {
                clone.setAmount(previousPageIndex);
            }

            builder.icon(clone);
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.command.getWorld(), template.title()));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(this.command.getWorld(), template.description(),
                    TextVariables.NUMBER, String.valueOf(previousPageIndex)));
        }

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            template.actions().forEach(action -> {
                if ((clickType == action.clickType() ||
                        action.clickType() == ClickType.UNKNOWN) && PREVIOUS.equalsIgnoreCase(action.actionType()))
                {
                    // Next button ignores click type currently.
                    this.pageIndex--;
                    this.build();
                }

            });

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = template.actions().stream().
                filter(action -> action.tooltip() != null)
                .map(action -> this.user.getTranslation(this.command.getWorld(), action.tooltip()))
                .filter(text -> !text.isBlank())
                .collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        return builder.build();
    }


    /**
     * This method creates and returns bundle button.
     *
     * @return PanelItem that represents bundle button.
     */
    @Nullable
    private PanelItem createBundleButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        if (this.elementList.isEmpty())
        {
            // Does not contain any blueprints.
            return null;
        }

        int index = this.pageIndex * slot.amountMap().getOrDefault(BUNDLES, 1) + slot.slot();

        BlueprintBundle blueprintBundle;

        if (index >= this.elementList.size())
        {
            // Out of index.
            blueprintBundle = null;
        }
        else
        {
            blueprintBundle = this.elementList.get(index);
        }

        if (template.dataMap().containsKey("unique_id"))
        {
            // Try to find bundle with requested ID. if not found, use already collected bundle.
            blueprintBundle = this.elementList.stream().
                    filter(bundle -> bundle.getUniqueId().equals(template.dataMap().get("unique_id"))).findFirst()
                    .orElse(blueprintBundle);
        }

        return this.createBundleButton(template, blueprintBundle);
    }


    // ---------------------------------------------------------------------
    // Section: Other methods
    // ---------------------------------------------------------------------


    /**
     * This method creates bundle button.
     *
     * @return PanelItem that allows to select bundle button
     */
    private PanelItem createBundleButton(ItemTemplateRecord template, BlueprintBundle bundle)
    {
        if (bundle == null)
        {
            // return as bundle is null. Empty button will be created.
            return null;
        }

        // Get settings for island.
        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            builder.icon(template.icon().clone());
        }
        else
        {
            builder.icon(bundle.getIcon());
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.command.getWorld(), template.title(),
                    TextVariables.NAME, bundle.getDisplayName()));
        }
        else
        {
            builder.name(this.user.getTranslation(BUNDLE_BUTTON_REF + "name",
                    TextVariables.NAME, bundle.getDisplayName()));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(this.command.getWorld(), template.description(),
                    TextVariables.DESCRIPTION, String.join("\n", bundle.getDescription())));
        }
        else
        {
            builder.description(this.user.getTranslation(BUNDLE_BUTTON_REF + "description",
                    TextVariables.DESCRIPTION, String.join("\n", bundle.getDescription())));
        }
        boolean usedUp = false;
        if (plugin.getSettings().getIslandNumber() > 1) {
            // Show how many times this bundle can be used
            int maxTimes = bundle.getTimes();
            if (maxTimes > 0) {
                long uses = plugin.getIslands().getIslands(world, user).stream()
                        .filter(is -> is.getMetaData("bundle")
                                .map(mdv -> bundle.getDisplayName().equalsIgnoreCase(mdv.asString())
                                        && !(reset && is.isPrimary(user.getUniqueId()))) // If this is a reset, then ignore the use of the island being reset
                                .orElse(false))
                        .count();
                builder.description(this.user.getTranslation(BUNDLE_BUTTON_REF + "uses", TextVariables.NUMBER,
                        String.valueOf(uses), "[max]", String.valueOf(maxTimes)));
                if (uses >= maxTimes) {
                    usedUp = true;
                }
            } else {
                builder.description(this.user.getTranslation(BUNDLE_BUTTON_REF + "unlimited"));
            }
        }

        if (usedUp) {
            if (plugin.getSettings().isHideUsedBlueprints()) {
                // Do not show used up blueprints
                return null;
            }
        } else {
            List<ItemTemplateRecord.ActionRecords> actions = template.actions().stream()
                    .filter(action -> SELECT_ACTION.equalsIgnoreCase(action.actionType())
                            || COMMANDS_ACTION.equalsIgnoreCase(action.actionType()))
                    .toList();
            // Add ClickHandler
            builder.clickHandler((panel, user, clickType, i) -> {
                actions.forEach(action -> {
                    if (clickType == action.clickType() || action.clickType() == ClickType.UNKNOWN)
                    {
                        if (SELECT_ACTION.equalsIgnoreCase(action.actionType())) {
                            user.closeInventory();
                            this.command.execute(user, this.mainLabel,
                                    Collections.singletonList(bundle.getUniqueId()));
                        } else if (COMMANDS_ACTION.equalsIgnoreCase(action.actionType())) {
                            Util.runCommands(user,
                                    Arrays.stream(action.content()
                                            .replaceAll(Pattern.quote(TextVariables.LABEL),
                                                    this.command.getTopLabel())
                                            .split("\n")).toList(),
                                    ISLAND_CREATION_COMMANDS);
                        }
                    }
                });

                // Always return true.
                return true;
            });

            // Collect tooltips.
            List<String> tooltips = actions.stream().filter(action -> action.tooltip() != null)
                    .map(action -> this.user.getTranslation(this.command.getWorld(), action.tooltip()))
                    .filter(text -> !text.isBlank())
                    .collect(Collectors.toCollection(() -> new ArrayList<>(actions.size())));

            // Add tooltips.
            if (!tooltips.isEmpty()) {
                // Empty line and tooltips.
                builder.description("");
                builder.description(tooltips);
            }
        }
        return builder.build();
    }

    // ---------------------------------------------------------------------
    // Section: Static methods
    // ---------------------------------------------------------------------

    /**
     * This method is used to open Panel outside this class. It will be much easier to open panel with single method
     * call then initializing new object.
     *
     * @param command CompositeCommand object
     * @param label The main command label
     * @param user User who opens panel
     * @param reset true if this is an island reset
     */
    public static void openPanel(@NonNull CompositeCommand command, @NonNull User user, @NonNull String label,
            boolean reset) {
        new IslandCreationPanel(command, user, label, reset).build();
    }

}
