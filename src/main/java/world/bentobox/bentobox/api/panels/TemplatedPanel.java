//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.bentobox.api.panels;


import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.panels.reader.PanelTemplateRecord;
import world.bentobox.bentobox.api.user.User;


/**
 * This class creates a new Panel from the template record.
 * @author BONNe
 * @since 1.17.3
 */
public class TemplatedPanel extends Panel
{
    /**
     * TemplatedPanel constructor class which generates functional panel.
     * @param builder Builder that contains all information about the panel that must be generated.
     */
    public TemplatedPanel(@NonNull TemplatedPanelBuilder builder)
    {
        this.user = builder.getUser();
        this.setWorld(builder.getWorld());
        this.setListener(builder.getListener());

        this.panelTemplate = builder.getPanelTemplate();
        // Init type creators
        this.typeCreators = new HashMap<>(builder.getObjectCreatorMap());
        this.typeIndex = new HashMap<>(builder.getObjectCreatorMap().size());
        this.typeSlotMap = new HashMap<>(builder.getObjectCreatorMap().size());

        if (this.panelTemplate == null)
        {
            BentoBox.getInstance().logError("Cannot generate panel because template is not loaded.");
        }
        else
        {
            this.generatePanel();
        }
    }


    /**
     * This method generates the panel from the template.
     */
    private void generatePanel()
    {
        Map<Integer, PanelItem> items = switch (this.panelTemplate.type())
            {
                case INVENTORY -> this.populateInventoryPanel();
                case HOPPER -> this.populateHopperPanel();
                case DROPPER -> this.populateDropperPanel();
            };

        super.makePanel(this.user.getTranslation(this.panelTemplate.title()),
            items,
            items.keySet().stream().max(Comparator.naturalOrder()).orElse(9),
            this.user,
            this.getListener().orElse(null),
            this.panelTemplate.type());
    }


    /**
     * This method creates map with item indexes and their icons that will be added into
     * Inventory Panel.
     * @return Map that contains indexes linked to the correct panel item.
     */
    @NonNull
    private Map<Integer, PanelItem> populateInventoryPanel()
    {
        // Init item array with the max available size.
        PanelItem[][] itemArray = new PanelItem[6][9];

        // Analyze the GUI button layout a bit.
        for (int i = 0; i < this.panelTemplate.content().length; i++)
        {
            for (int k = 0; k < this.panelTemplate.content()[i].length; k++)
            {
                ItemTemplateRecord record = this.panelTemplate.content()[i][k];

                if (record != null && record.dataMap().containsKey("type"))
                {
                    String type = String.valueOf(record.dataMap().get("type"));

                    int counter = this.typeSlotMap.computeIfAbsent(type, key -> 0);
                    this.typeSlotMap.put(type, counter + 1);
                }
            }
        }

        // Make buttons for the GUI
        for (int i = 0; i < this.panelTemplate.content().length; i++)
        {
            for (int k = 0; k < this.panelTemplate.content()[i].length; k++)
            {
                itemArray[i][k] = this.makeButton(this.panelTemplate.content()[i][k]);
            }
        }

        // After items are created, remove empty lines.
        boolean[] showLine = this.panelTemplate.forcedRows();

        for (int i = 0; i < this.panelTemplate.content().length; i++)
        {
            boolean emptyLine = true;

            for (int k = 0; emptyLine && k < this.panelTemplate.content()[i].length; k++)
            {
                emptyLine = itemArray[i][k] == null;
            }

            // Do not generate fallback for "empty" lines.
            showLine[i] = showLine[i] || !emptyLine;
        }

        // Now fill the border.
        if (this.panelTemplate.border() != null)
        {
            PanelItem template = this.makeTemplate(this.panelTemplate.border());

            // Hard codded 6
            for (int i = 0; i < 6; i++)
            {
                if (i == 0 || i == 5)
                {
                    // Fill first and last row completely with border.
                    for (int k = 0; k < 9; k++)
                    {
                        if (itemArray[i][k] == null)
                        {
                            itemArray[i][k] = template;
                        }
                    }
                }
                else
                {
                    // Fill first and last element in row with border.
                    if (itemArray[i][0] == null)
                    {
                        itemArray[i][0] = template;
                    }

                    if (itemArray[i][8] == null)
                    {
                        itemArray[i][8] = template;
                    }
                }
            }

            showLine[0] = true;
            showLine[5] = true;
        }

        // Now fill the background.
        if (this.panelTemplate.background() != null)
        {
            PanelItem template = this.makeTemplate(this.panelTemplate.background());

            for (int i = 0; i < 6; i++)
            {
                for (int k = 0; k < 9; k++)
                {
                    if (itemArray[i][k] == null)
                    {
                        itemArray[i][k] = template;
                    }
                }
            }
        }

        // Now place all panel items with their indexes into item map.
        Map<Integer, PanelItem> itemMap = new HashMap<>(6 * 9);

        int correctIndex = 0;

        for (int i = 0; i < itemArray.length; i++)
        {
            final boolean iterate = showLine[i];

            for (int k = 0; iterate && k < itemArray[i].length; k++)
            {
                if (itemArray[i][k] != null)
                {
                    itemMap.put(correctIndex, itemArray[i][k]);
                }

                correctIndex++;
            }
        }

        return itemMap;
    }


    /**
     * This method creates map with item indexes and their icons that will be added into
     * hopper Panel.
     * @return Map that contains indexes linked to the correct panel item.
     */
    @NonNull
    private Map<Integer, PanelItem> populateHopperPanel()
    {
        // Init item array with the max available size.
        PanelItem[] itemArray = new PanelItem[5];

        // Analyze the template
        for (int i = 0; i < 5; i++)
        {
            ItemTemplateRecord record = this.panelTemplate.content()[0][i];

            if (record != null && record.dataMap().containsKey("type"))
            {
                String type = String.valueOf(record.dataMap().get("type"));

                int counter = this.typeSlotMap.computeIfAbsent(type, key -> 0);
                this.typeSlotMap.put(type, counter + 1);
            }
        }

        // Make buttons
        for (int i = 0; i < 5; i++)
        {
            itemArray[i] = this.makeButton(this.panelTemplate.content()[0][i]);
        }

        // Now fill the background.
        if (this.panelTemplate.background() != null)
        {
            PanelItem template = this.makeTemplate(this.panelTemplate.background());

            for (int i = 0; i < 5; i++)
            {
                if (itemArray[i] == null)
                {
                    itemArray[i] = template;
                }
            }
        }

        // Now place all panel items with their indexes into item map.
        Map<Integer, PanelItem> itemMap = new HashMap<>(5);

        int correctIndex = 0;

        for (PanelItem panelItem : itemArray)
        {
            if (panelItem != null)
            {
                itemMap.put(correctIndex, panelItem);
            }

            correctIndex++;
        }

        return itemMap;
    }


    /**
     * This method creates map with item indexes and their icons that will be added into
     * dropper Panel.
     * @return Map that contains indexes linked to the correct panel item.
     */
    @NonNull
    private Map<Integer, PanelItem> populateDropperPanel()
    {
        // Analyze the template
        for (int i = 0; i < 3; i++)
        {
            for (int k = 0; k < 3; k++)
            {
                ItemTemplateRecord record = this.panelTemplate.content()[i][k];

                if (record != null && record.dataMap().containsKey("type"))
                {
                    String type = String.valueOf(record.dataMap().get("type"));

                    int counter = this.typeSlotMap.computeIfAbsent(type, key -> 0);
                    this.typeSlotMap.put(type, counter + 1);
                }
            }
        }

        // Init item array with the max available size.
        PanelItem[][] itemArray = new PanelItem[3][3];

        // Make buttons
        for (int i = 0; i < 3; i++)
        {
            for (int k = 0; k < 3; k++)
            {
                itemArray[i][k] = this.makeButton(this.panelTemplate.content()[i][k]);
            }
        }

        // Now fill the background.
        if (this.panelTemplate.background() != null)
        {
            PanelItem template = this.makeTemplate(this.panelTemplate.background());

            for (int i = 0; i < 3; i++)
            {
                for (int k = 0; k < 3; k++)
                {
                    if (itemArray[i][k] == null)
                    {
                        itemArray[i][k] = template;
                    }
                }
            }
        }

        // Init item map with the max available size.
        Map<Integer, PanelItem> itemMap = new HashMap<>(9);

        int correctIndex = 0;

        for (int i = 0; i < 3; i++)
        {
            for (int k = 0; k < 3; k++)
            {
                if (itemArray[i][k] != null)
                {
                    itemMap.put(correctIndex, itemArray[i][k]);
                }

                correctIndex++;
            }
        }

        return itemMap;
    }


    /**
     * This method passes button creation from given record template.
     * @param record Template of the button that must be created.
     * @return PanelItem of the template, otherwise null.
     */
    @Nullable
    private PanelItem makeButton(@Nullable ItemTemplateRecord record)
    {
        if (record == null)
        {
            // Immediate exit if record is null.
            return null;
        }

        if (record.dataMap().containsKey("type"))
        {
            // If dataMap is not null, and it is not empty, then pass button to the object creator function.

            return this.makeAddonButton(record);
        }
        else
        {
            PanelItemBuilder itemBuilder = new PanelItemBuilder();

            if (record.icon() != null)
            {
                itemBuilder.icon(record.icon().clone());
            }

            if (record.title() != null)
            {
                itemBuilder.name(this.user.getTranslation(record.title()));
            }

            if (record.description() != null)
            {
                itemBuilder.description(this.user.getTranslation(record.description()));
            }

            // If there are generic click handlers that could be added, then this is a place
            // where to process them.

             // Click Handlers are managed by custom addon buttons.
             return itemBuilder.build();
        }
    }


    /**
     * This method passes button to the type creator, if that exists.
     * @param record Template of the button that must be created.
     * @return PanelItem of the button created by typeCreator, otherwise null.
     */
    @Nullable
    private PanelItem makeAddonButton(@NonNull ItemTemplateRecord record)
    {
        // Get object type.
        String type = String.valueOf(record.dataMap().getOrDefault("type", ""));

        if (!this.typeCreators.containsKey(type))
        {
            // There are no object with a given type.
            return this.makeFallBack(record.fallback());
        }

        BiFunction<ItemTemplateRecord, ItemSlot, PanelItem> buttonBuilder = this.typeCreators.get(type);

        // Get next slot index.
        ItemSlot itemSlot = this.typeIndex.containsKey(type) ?
            this.typeIndex.get(type) :
            new ItemSlot(0, this.typeSlotMap);
        this.typeIndex.put(type, itemSlot.nextItemSlot());

        // Try to get next object.
        PanelItem item = buttonBuilder.apply(record, itemSlot);
        return item == null ? this.makeFallBack(record.fallback()) : item;
    }


    /**
     * This method creates a fall back button for given record.
     * @param record Record which fallback must be created.
     * @return PanelItem if fallback was creates successfully, otherwise null.
     */
    @Nullable
    private PanelItem makeFallBack(@Nullable ItemTemplateRecord record)
    {
        return record == null ? null : this.makeButton(record.fallback());
    }


    /**
     * This method translates template record into a panel item.
     * @param record Record that must be translated.
     * @return PanelItem that contains all information from the record.
     */
    private PanelItem makeTemplate(PanelTemplateRecord.TemplateItem record)
    {
        PanelItemBuilder itemBuilder = new PanelItemBuilder();

        // Read icon only if it is not null.
        if (record.icon() != null)
        {
            itemBuilder.icon(record.icon().clone());
        }

        // Read title only if it is not null.
        if (record.title() != null)
        {
            itemBuilder.name(this.user.getTranslation(record.title()));
        }

        // Read description only if it is not null.
        if (record.description() != null)
        {
            itemBuilder.description(this.user.getTranslation(record.description()));
        }

        // Click Handlers are managed by custom addon buttons.
        return itemBuilder.build();
    }


// ---------------------------------------------------------------------
// Section: Classes
// ---------------------------------------------------------------------


    /**
     * This record contains current slot object and map that links types with a number of slots in
     * panel with it.
     * Some buttons need information about all types, like previous/next.
     * @param slot Index of object in current panel.
     * @param amountMap Map that links types with number of objects in panel.
     */
    public record ItemSlot(int slot, Map<String, Integer> amountMap)
    {
        /**
         * This method returns new record object with iterative slot index.
         * @return New ItemSlot object that has increased slot index by 1.
         */
        ItemSlot nextItemSlot()
        {
            return new ItemSlot(this.slot() + 1, this.amountMap());
        }
    }


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------


    /**
     * The GUI template record.
     */
    private final PanelTemplateRecord panelTemplate;

    /**
     * The user who opens the GUI.
     */
    private final User user;

    /**
     * This map links custom types with their info object.
     */
    private final Map<String, BiFunction<ItemTemplateRecord, ItemSlot, PanelItem>> typeCreators;

    /**
     * Stores the item slot information for each type.
     */
    private final Map<String, ItemSlot> typeIndex;

    /**
     * Stores the number of items with given type in whole panel.
     */
    private final Map<String, Integer> typeSlotMap;
}
