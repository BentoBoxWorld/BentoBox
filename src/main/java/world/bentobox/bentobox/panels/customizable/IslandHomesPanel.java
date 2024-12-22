package world.bentobox.bentobox.panels.customizable;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.island.IslandGoCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;


/**
 * Panel for island homes command
 */
public class IslandHomesPanel extends AbstractPanel
{

    private static final String ISLAND = "ISLAND";


    /**
     * This variable stores filtered elements.
     */
    private final Map<String, IslandInfo> islandMap;
    private final Map<Integer, String> order = new HashMap<>();


    /**
     * The world that this command applies to
     */
    private final World world;

    private final IslandGoCommand goCommand;

    // ---------------------------------------------------------------------
    // Section: Constructor
    // ---------------------------------------------------------------------


    /**
     * This is internal constructor. It is used internally in current class to avoid creating objects everywhere.
     *
     * @param command CompositeCommand
     * @param user User who opens panel
     */
    private IslandHomesPanel(@NonNull CompositeCommand command, @NonNull User user)
    {
        super(command, user);
        this.world = command.getWorld();
        this.islandMap = this.getNameIslandMap(user);
        int index = 0;
        for (String name : islandMap.keySet()) {
            order.put(index++, name);
        }
        goCommand = (IslandGoCommand) command.getParent().getSubCommand("go").orElse(null);
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
        // Do not open gui if there are no islands
        if (this.islandMap.isEmpty())
        {
            user.sendMessage("general.errors.no-island");
            return;
        }

        // Start building panel.
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();

        // Set main template.
        if (this.doesCustomPanelExists(this.command.getAddon(), "island_homes_panel"))
        {
            // Addon has its own island homes panel. Use it.
            panelBuilder.template("island_homes_panel", new File(this.command.getAddon().getDataFolder(), "panels"));
        }
        else
        {
            // Use default island creation panel.
            panelBuilder.template("island_homes_panel", new File(this.plugin.getDataFolder(), "panels"));
        }

        panelBuilder.user(this.user);
        panelBuilder.world(world);

        // Register button builders
        panelBuilder.registerTypeBuilder(ISLAND, this::createIslandButton);

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
        int size = this.islandMap.size();

        if (size <= slot.amountMap().getOrDefault(ISLAND, 1)
                || 1.0 * size / slot.amountMap().getOrDefault(ISLAND, 1) <= this.pageIndex + 1)
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
     * This method creates and returns island button.
     *
     * @return PanelItem that represents island button.
     */
    @Nullable
    private PanelItem createIslandButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        if (this.islandMap.isEmpty())
        {
            // Does not contain any islands.
            return null;
        }

        int index = this.pageIndex * slot.amountMap().getOrDefault(ISLAND, 1) + slot.slot();
        if (index >= this.islandMap.size())
        {
            // Out of index.
            return null;
        }
        return this.createIslandButtonDetail(template, slot);
    }


    /**
     * This method creates bundle button.
     *
     * @return PanelItem that allows to select bundle button
     */
    private PanelItem createIslandButtonDetail(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        // Get settings for island.
        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            builder.icon(template.icon().clone());
        }
        else
        {
            builder.icon(Material.GRASS_BLOCK);
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.command.getWorld(), template.title(),
                    TextVariables.NAME, order.get(slot.slot())));
        }
        else
        {
            builder.name(this.user.getTranslation("panels.island_homes.buttons.name", TextVariables.NAME,
                    order.get(slot.slot())));
        }

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) -> {
            template.actions().forEach(action -> {
                if (goCommand != null) {
                    String name = order.get(slot.slot());
                    user.closeInventory();
                    if (goCommand.canExecute(user, "", List.of(name))) {
                        goCommand.execute(user, "", List.of(name));
                    }
                }
            });

            // Always return true.
            return true;
        });

        return builder.build();
    }

    /**
     * Record of islands and the name to type
     */
    private record IslandInfo(Island island, boolean islandName) {
    }

    /**
     * This is duplicate code from the Go command.
     * @param user user
     * @return name and island info
     */
    private Map<String, IslandInfo> getNameIslandMap(User user) {
        Map<String, IslandInfo> islandMap = new HashMap<>();
        int index = 0;
        for (Island island : command.getIslands().getIslands(command.getWorld(), user.getUniqueId())) {
            index++;
            if (island.getName() != null && !island.getName().isBlank()) {
                // Name has been set
                islandMap.put(island.getName(), new IslandInfo(island, true));
            } else {
                // Name has not been set
                String text = user.getTranslation("protection.flags.ENTER_EXIT_MESSAGES.island", TextVariables.NAME,
                        user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName()) + " " + index;
                islandMap.put(text, new IslandInfo(island, true));
            }
            // Add homes. Homes do not need an island specified
            island.getHomes().keySet().stream().filter(n -> !n.isBlank())
                    .forEach(n -> islandMap.put(n, new IslandInfo(island, false)));
        }

        return islandMap;

    }

    // ---------------------------------------------------------------------
    // Section: Static methods
    // ---------------------------------------------------------------------

    /**
     * This method is used to open Panel outside this class. It will be much easier to open panel with single method
     * call then initializing new object.
     *
     * @param command CompositeCommand object
     * @param user User who opens panel
     */
    public static void openPanel(@NonNull CompositeCommand command, @NonNull User user) {
        new IslandHomesPanel(command, user).build();
    }


}
