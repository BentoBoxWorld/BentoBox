//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.bentobox.api.panels.reader;


import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.api.panels.Panel;


/**
 * This is template object for the panel reader. It contains data that can exist in the panel.
 * PanelBuilder will use this to build panel.
 *
 * @param type the type of GUI
 * @param title the title of GUI
 * @param border the border block for GUI
 * @param background the background block for GUI.
 * @param content The 2D array of ItemTemplateRecords
 *
 * @since 1.17.3
 */
public record PanelTemplateRecord(Panel.Type type,
                                  String title,
                                  TemplateItem border,
                                  TemplateItem background,
                                  ItemTemplateRecord[][] content)
{
    /**
     * Instantiates a new Panel template record with empty content.
     *
     * @param type the type
     * @param title the title
     * @param border the border
     * @param background the background
     */
    public PanelTemplateRecord(Panel.Type type, String title, TemplateItem border, TemplateItem background)
    {
        this(type, title, border, background, new ItemTemplateRecord[6][9]);
    }


    /**
     * This method adds give item template record in given slot.
     * @param rowIndex row index of content array
     * @param columnIndex column index of content array.
     * @param panelItemTemplate item template record that must be added.
     */
    public void addButtonTemplate(int rowIndex, int columnIndex, ItemTemplateRecord panelItemTemplate)
    {
        this.content[rowIndex][columnIndex] = panelItemTemplate;
    }


    // ---------------------------------------------------------------------
    // Section: Classes
    // ---------------------------------------------------------------------


    /**
     * This record contains info about border and background item.
     */
    public record TemplateItem(ItemStack icon, String title, String description)
    {
        public TemplateItem(ItemStack icon)
        {
            this(icon, null, null);
        }
    }
}
