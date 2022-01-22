//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.bentobox.api.panels.reader;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Enums;

import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.util.ItemParser;


/**
 * This class manages Template file reading, creating PanelTemplateRecord object and storing it internally.
 * This class just reads and returns given panel template. It does not create a functional panel.
 *
 * @since 1.17.3
 */
public class TemplateReader
{
    private static final String TITLE = "title";
    private static final String ICON = "icon";
    private static final String DESCRIPTION = "description";
    private static final String BACKGROUND = "background";
    private static final String BORDER = "border";
    private static final String FORCE_SHOWN = "force-shown";
    private static final String FALLBACK = "fallback";

    /**
     * Read template panel panel template record.
     *
     * @param panelName the panel name
     * @param panelLocation the panel location directory
     * @return the panel template record
     */
    public static PanelTemplateRecord readTemplatePanel(@NonNull String panelName, @NonNull File panelLocation)
    {
        if (!panelLocation.exists())
        {
            // Return null because folder does not exist.
            return null;
        }

        File file = new File(panelLocation, panelName.endsWith(".yml") ? panelName : panelName + ".yml");

        if (!file.exists())
        {
            // Return as file does not exist.
            return null;
        }

        // Check if panel is already crafted.
        if (TemplateReader.loadedPanels.containsKey(file.getAbsolutePath()))
        {
            return TemplateReader.loadedPanels.get(file.getAbsolutePath());
        }

        PanelTemplateRecord record;

        try
        {
            // Load config
            YamlConfiguration config = new YamlConfiguration();
            config.load(file);
            // Read panel
            record = readPanelTemplate(config.getConfigurationSection(panelName));
            // Put panel into memory
            TemplateReader.loadedPanels.put(file.getAbsolutePath(), record);
        }
        catch (IOException | InvalidConfigurationException e)
        {
            record = null;
        }

        return record;
    }


    /**
     * This method reads panel template from given configuration section.
     * @param configurationSection Section that contains panel template data.
     * @return Panel Template.
     */
    private static PanelTemplateRecord readPanelTemplate(@Nullable ConfigurationSection configurationSection)
    {
        if (configurationSection == null)
        {
            // No data to return.
            return null;
        }

        String title = configurationSection.getString(TITLE);
        Panel.Type type =
                Enums.getIfPresent(Panel.Type.class, configurationSection.getString("type", "INVENTORY")).
                or(Panel.Type.INVENTORY);

        PanelTemplateRecord.TemplateItem borderItem = null;

        // Read Border Icon.
        if (configurationSection.isConfigurationSection(BORDER))
        {
            // Process border icon if it contains more options.
            ConfigurationSection borderSection = configurationSection.getConfigurationSection(BORDER);

            if (borderSection != null)
            {
                borderItem = new PanelTemplateRecord.TemplateItem(
                        ItemParser.parse((borderSection.getString(ICON, Material.AIR.name()))),
                        borderSection.getString(TITLE, null),
                        borderSection.getString(DESCRIPTION, null));
            }
        }
        else if (configurationSection.isString(BORDER))
        {
            // Process border icon if it contains only icon.

            borderItem = new PanelTemplateRecord.TemplateItem(
                    ItemParser.parse((configurationSection.getString(BORDER, Material.AIR.name()))));
        }

        PanelTemplateRecord.TemplateItem backgroundItem = null;

        // Read Background block
        if (configurationSection.isConfigurationSection(BACKGROUND))
        {
            // Process border icon if it contains more options.
            ConfigurationSection backgroundSection = configurationSection.getConfigurationSection(BACKGROUND);

            if (backgroundSection != null)
            {
                backgroundItem = new PanelTemplateRecord.TemplateItem(
                        ItemParser.parse((backgroundSection.getString(ICON, Material.AIR.name()))),
                        backgroundSection.getString(TITLE, null),
                        backgroundSection.getString(DESCRIPTION, null));
            }
        }
        else if (configurationSection.isString(BACKGROUND))
        {
            // Process background icon if it contains only icon.

            backgroundItem = new PanelTemplateRecord.TemplateItem(
                    ItemParser.parse((configurationSection.getString(BACKGROUND, Material.AIR.name()))));
        }

        // Read reusable
        Map<String, ItemTemplateRecord> panelItemDataMap = new HashMap<>();
        ConfigurationSection reusable = configurationSection.getConfigurationSection("reusable");

        if (reusable != null)
        {
            // Add all reusables to the local storage.
            reusable.getKeys(false).forEach(key ->
            readPanelItemTemplate(reusable.getConfigurationSection(key), key, panelItemDataMap));
        }

        // Read content
        boolean[] forcedRows = readForcedRows(configurationSection);

        // Create template record.
        PanelTemplateRecord template = new PanelTemplateRecord(type, title, borderItem, backgroundItem, forcedRows);

        // Read content
        ConfigurationSection content = configurationSection.getConfigurationSection("content");

        if (content == null)
        {
            // Return empty template.
            return template;
        }

        for (int rowIndex = 0; rowIndex < 6; rowIndex++)
        {
            // Read each line.
            if (content.isConfigurationSection(String.valueOf(rowIndex + 1)))
            {
                ConfigurationSection line = content.getConfigurationSection(String.valueOf(rowIndex + 1));

                if (line != null)
                {
                    // Populate existing lines with items.
                    for (int columnIndex = 0; columnIndex < 9; columnIndex++)
                    {
                        if (line.isConfigurationSection(String.valueOf(columnIndex + 1)))
                        {
                            // If it contains a section, then build a new button template from it.
                            template.addButtonTemplate(rowIndex,
                                    columnIndex,
                                    readPanelItemTemplate(line.getConfigurationSection(String.valueOf(columnIndex + 1))));
                        }
                        else if (line.isString(String.valueOf(columnIndex + 1)))
                        {
                            // If it contains just a single word, assume it is a reusable.
                            template.addButtonTemplate(rowIndex,
                                    columnIndex,
                                    panelItemDataMap.get(line.getString(String.valueOf(columnIndex + 1))));
                        }
                    }
                }
            }
        }

        // Garbage collector.
        panelItemDataMap.clear();

        return template;
    }


    /**
     * This method reads force shown rows that must be always displayed.
     * @param section Configuration section that contains force-shown path.
     * @return boolean array that contains which lines are force loaded.
     */
    private static boolean[] readForcedRows(@Nullable ConfigurationSection section)
    {
        boolean[] forceShow = new boolean[6];

        if (section != null && section.contains(FORCE_SHOWN))
        {
            if (section.isInt(FORCE_SHOWN))
            {
                int value = section.getInt(FORCE_SHOWN);

                if (value > 0 && value < 7)
                {
                    forceShow[value-1] = true;
                }
            }
            else if (section.isList(FORCE_SHOWN))
            {
                section.getIntegerList(FORCE_SHOWN).forEach(number -> {
                    if (number > 0 && number < 7)
                    {
                        forceShow[number-1] = true;
                    }
                });
            }
        }

        return forceShow;
    }


    /**
     * This method creates PanelItemTemplate from a given configuration section.
     * @param section Section that should contain all information about the panel item template.
     * @return PanelItemTemplate that should represent button from a section.
     */
    @Nullable
    private static ItemTemplateRecord readPanelItemTemplate(@Nullable ConfigurationSection section)
    {
        return readPanelItemTemplate(section, null, null);
    }



    /**
     * This method creates PanelItemTemplate from a given configuration section.
     * @param section Section that should contain all information about the panel item template.
     * @return PanelItemTemplate that should represent button from a section.
     */
    @Nullable
    private static ItemTemplateRecord readPanelItemTemplate(@Nullable ConfigurationSection section,
            String itemKey,
            Map<String, ItemTemplateRecord> reusableItemMap)
    {
        if (section == null)
        {
            // No section, no item.
            return null;
        }

        ItemTemplateRecord fallback;

        if (section.isConfigurationSection(FALLBACK))
        {
            fallback = readPanelItemTemplate(section.getConfigurationSection(FALLBACK));
        }
        else if (section.isString(FALLBACK) && reusableItemMap != null)
        {
            fallback = reusableItemMap.get(section.getString(FALLBACK));
        }
        else
        {
            fallback = null;
        }

        // Create Item Record
        ItemTemplateRecord itemRecord = new ItemTemplateRecord(ItemParser.parse(section.getString(ICON)),
                section.getString(TITLE, null),
                section.getString(DESCRIPTION, null),
                fallback);

        // Read data
        if (section.isConfigurationSection("data"))
        {
            ConfigurationSection dataSection = section.getConfigurationSection("data");

            if (dataSection != null)
            {
                dataSection.getKeys(false).forEach(key -> itemRecord.addData(key, dataSection.get(key)));
            }
        }

        // Read Click data
        if (section.isConfigurationSection("actions"))
        {
            ConfigurationSection actionSection = section.getConfigurationSection("actions");

            if (actionSection != null)
            {
                actionSection.getKeys(false).forEach(actionKey -> {
                    ClickType clickType = Enums.getIfPresent(ClickType.class, actionKey.toUpperCase()).orNull();

                    if (clickType != null)
                    {
                        ConfigurationSection actionDataSection = actionSection.getConfigurationSection(actionKey);

                        if (actionDataSection != null)
                        {
                            ItemTemplateRecord.ActionRecords actionData =
                                    new ItemTemplateRecord.ActionRecords(clickType,
                                            actionDataSection.getString("type"),
                                            actionDataSection.getString("content"),
                                            actionDataSection.getString("tooltip"));
                            itemRecord.addAction(actionData);
                        }
                    }
                });
            }
        }

        // Add item to the map
        if (reusableItemMap != null && itemKey != null)
        {
            reusableItemMap.put(itemKey, itemRecord);
        }

        return itemRecord;
    }


    /**
     * This method clears loaded panels from the cache.
     */
    public static void clearPanels()
    {
        loadedPanels.clear();
    }


    /**
     * This map contains already read panels and their location.
     * This improves performance for GUI opening, with a some memory usage.
     */
    private static final Map<String, PanelTemplateRecord> loadedPanels = new HashMap<>();
}
