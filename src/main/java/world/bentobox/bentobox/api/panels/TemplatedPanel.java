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

        this.parameters = builder.getParameters().toArray(new String[0]);

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
        Map<Integer, PanelItem> items = switch (this.panelTemplate.type()) {
            case INVENTORY -> this.populateInventoryPanel(new PanelItem[6][9]);
            case HOPPER -> this.populateInventoryPanel(new PanelItem[1][5]);
            case DROPPER -> this.populateInventoryPanel(new PanelItem[3][3]);
        };

        super.makePanel(this.user.getTranslation(this.panelTemplate.title(), this.parameters),
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
    private Map<Integer, PanelItem> populateInventoryPanel(PanelItem[][] itemArray)
    {
        this.preProcessPanelTemplate(itemArray);
        this.processItemData(itemArray);
        this.removeEmptyLines(itemArray);
        this.fillBorder(itemArray);
        this.fillBackground(itemArray);

        return this.createItemMap(itemArray);
    }


    /**
     * This method processes what items should be added into the panel.
     * It counts how many same type buttons should be generated.
     * This cannot be done in the same step as creating button.
     * @param itemArray The double array with items into panel
     */
    private void preProcessPanelTemplate(PanelItem[][] itemArray)
    {
        final int numRows = itemArray.length;
        final int numCols = itemArray[0].length;

        // Analyze the GUI button layout a bit.
        for (int i = 0; i < numRows; i++)
        {
            for (int k = 0; k < numCols; k++)
            {
                ItemTemplateRecord rec = this.panelTemplate.content()[i][k];

                if (rec != null && rec.dataMap().containsKey("type"))
                {
                    String type = String.valueOf(rec.dataMap().get("type"));

                    int counter = this.typeSlotMap.computeIfAbsent(type, key -> 0);
                    this.typeSlotMap.put(type, counter + 1);
                }
            }
        }
    }


    /**
     * This method populates item array with all buttons.
     * @param itemArray The double array with items into panel
     */
    private void processItemData(PanelItem[][] itemArray)
    {
        final int numRows = itemArray.length;
        final int numCols = itemArray[0].length;

        for (int i = 0; i < numRows; i++)
        {
            for (int k = 0; k < numCols; k++)
            {
                itemArray[i][k] = this.makeButton(this.panelTemplate.content()[i][k]);
            }
        }
    }


    /**
     * This method removes all empty lines if they are not forced to be
     * showed.
     * @param itemArray The double array with items into panel
     */
    private void removeEmptyLines(PanelItem[][] itemArray)
    {
        // After items are created, remove empty lines.
        boolean[] showLine = this.panelTemplate.forcedRows();

        final int numRows = itemArray.length;
        final int numCols = itemArray[0].length;

        for (int i = 0; i < numRows; i++)
        {
            boolean emptyLine = true;

            for (int k = 0; emptyLine && k < numCols; k++)
            {
                emptyLine = itemArray[i][k] == null;
            }

            // Do not generate fallback for "empty" lines.
            showLine[i] = showLine[i] || !emptyLine;
        }
    }


    /**
     * This method fills border elements with item from template.
     * @param itemArray The double array with items into panel
     */
    private void fillBorder(PanelItem[][] itemArray)
    {
        if (this.panelTemplate.border() == null)
        {
            // Ugly return because tasty does not like extra {}.
            return;
        }

        PanelItem template = makeTemplate(this.panelTemplate.border());
        final int numRows = itemArray.length;
        final int numCols = itemArray[0].length;

        for (int i = 0; i < numRows; i++)
        {
            if (i == 0 || i == numRows - 1)
            {
                // Fill first and last row completely with border.
                for (int k = 0; k < numCols; k++)
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

                if (itemArray[i][numCols - 1] == null)
                {
                    itemArray[i][numCols - 1] = template;
                }
            }
        }

        this.panelTemplate.forcedRows()[0] = true;
        this.panelTemplate.forcedRows()[numRows - 1] = true;
    }


    /**
     * This method fills background elements with item from template.
     * @param itemArray The double array with items into panel
     */
    private void fillBackground(PanelItem[][] itemArray)
    {
        if (this.panelTemplate.background() == null)
        {
            return;
        }

        PanelItem template = this.makeTemplate(this.panelTemplate.background());
        final int numRows = itemArray.length;
        final int numCols = itemArray[0].length;

        for (int i = 0; i < numRows; i++)
        {
            for (int k = 0; k < numCols; k++)
            {
                if (itemArray[i][k] == null)
                {
                    itemArray[i][k] = template;
                }
            }
        }
    }


    /**
     * This method converts to PanelItem array to the correct item map.
     * @param itemArray The double array with items into panel
     * @return The map that links index of button to panel item.
     */
    private Map<Integer, PanelItem> createItemMap(PanelItem[][] itemArray)
    {
        Map<Integer, PanelItem> itemMap = new HashMap<>(itemArray.length * itemArray[0].length);
        int correctIndex = 0;

        for (int i = 0; i < itemArray.length; i++)
        {
            final boolean iterate = this.panelTemplate.forcedRows()[i];

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
     * This method passes button creation from given record template.
     * @param rec Template of the button that must be created.
     * @return PanelItem of the template, otherwise null.
     */
    @Nullable
    private PanelItem makeButton(@Nullable ItemTemplateRecord rec)
    {
        if (rec == null)
        {
            // Immediate exit if record is null.
            return null;
        }

        if (rec.dataMap().containsKey("type"))
        {
            // If dataMap is not null, and it is not empty, then pass button to the object creator function.

            return this.makeAddonButton(rec);
        }
        else
        {
            PanelItemBuilder itemBuilder = new PanelItemBuilder();

            if (rec.icon() != null)
            {
                itemBuilder.icon(rec.icon().clone());
            }

            if (rec.title() != null)
            {
                itemBuilder.name(this.user.getTranslation(rec.title()));
            }

            if (rec.description() != null)
            {
                itemBuilder.description(this.user.getTranslation(rec.description()));
            }

            // If there are generic click handlers that could be added, then this is a place
            // where to process them.

             // Click Handlers are managed by custom addon buttons.
             return itemBuilder.build();
        }
    }


    /**
     * This method passes button to the type creator, if that exists.
     * @param rec Template of the button that must be created.
     * @return PanelItem of the button created by typeCreator, otherwise null.
     */
    @Nullable
    private PanelItem makeAddonButton(@NonNull ItemTemplateRecord rec)
    {
        // Get object type.
        String type = String.valueOf(rec.dataMap().getOrDefault("type", ""));

        if (!this.typeCreators.containsKey(type))
        {
            // There are no object with a given type.
            return this.makeFallBack(rec.fallback());
        }

        BiFunction<ItemTemplateRecord, ItemSlot, PanelItem> buttonBuilder = this.typeCreators.get(type);

        // Get next slot index.
        ItemSlot itemSlot = this.typeIndex.containsKey(type) ?
            this.typeIndex.get(type) :
            new ItemSlot(0, this);
        this.typeIndex.put(type, itemSlot.nextItemSlot());

        // Try to get next object.
        PanelItem item = buttonBuilder.apply(rec, itemSlot);
        return item == null ? this.makeFallBack(rec.fallback()) : item;
    }


    /**
     * This method creates a fall back button for given record.
     * @param rec Record which fallback must be created.
     * @return PanelItem if fallback was creates successfully, otherwise null.
     */
    @Nullable
    private PanelItem makeFallBack(@Nullable ItemTemplateRecord rec)
    {
        return rec == null ? null : this.makeButton(rec.fallback());
    }


    /**
     * This method translates template record into a panel item.
     * @param rec Record that must be translated.
     * @return PanelItem that contains all information from the record.
     */
    private PanelItem makeTemplate(PanelTemplateRecord.TemplateItem rec)
    {
        PanelItemBuilder itemBuilder = new PanelItemBuilder();

        // Read icon only if it is not null.
        if (rec.icon() != null)
        {
            itemBuilder.icon(rec.icon().clone());
        }

        // Read title only if it is not null.
        if (rec.title() != null)
        {
            itemBuilder.name(this.user.getTranslation(rec.title()));
        }

        // Read description only if it is not null.
        if (rec.description() != null)
        {
            itemBuilder.description(this.user.getTranslation(rec.description()));
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
     * @param parentPanel The parent panel for current Item.
     */
    public record ItemSlot(int slot, TemplatedPanel parentPanel)
    {
        /**
         * This method returns new record object with iterative slot index.
         * @return New ItemSlot object that has increased slot index by 1.
         */
        ItemSlot nextItemSlot()
        {
            return new ItemSlot(this.slot() + 1, this.parentPanel());
        }


        /**
         * This method returns map that links button types with a number of slots that this button
         * is present.
         * @return Map that links button type to amount in the gui.
         * @deprecated Use {@link #amount(String)} instead.
         */
        @Deprecated
        public Map<String, Integer> amountMap()
        {
            return this.parentPanel.typeSlotMap;
        }


        /**
         * This returns amount of slots for given button type.
         * @param type Type of the button.
         * @return Number of slots in panel.
         */
        public int amount(String type)
        {
            return this.parentPanel.typeSlotMap.getOrDefault(type, 0);
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

    /**
     * Stores the parameters for panel title object.
     * @since 1.20.0
     */
    private final String[] parameters;
}
