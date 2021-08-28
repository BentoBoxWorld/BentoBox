//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.bentobox.api.panels.reader;


import com.google.common.base.Enums;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

        String title = configurationSection.getString("title");
        Panel.Type type = configurationSection.getObject("type", Panel.Type.class);

        PanelTemplateRecord.TemplateItem borderItem = null;

        // Read Border Icon.
        if (configurationSection.isConfigurationSection("border"))
        {
            // Process border icon if it contains more options.
            ConfigurationSection borderSection = configurationSection.getConfigurationSection("border");

            if (borderSection != null)
            {
                borderItem = new PanelTemplateRecord.TemplateItem(
                    ItemParser.parse((borderSection.getString("icon", Material.AIR.name()))),
                    borderSection.getString("name", null),
                    borderSection.getString("description", null));
            }
        }
        else if (configurationSection.isString("border"))
        {
            // Process border icon if it contains only icon.

            borderItem = new PanelTemplateRecord.TemplateItem(
                ItemParser.parse((configurationSection.getString("border", Material.AIR.name()))));
        }

        PanelTemplateRecord.TemplateItem backgroundItem = null;

        // Read Background block
        if (configurationSection.isConfigurationSection("background"))
        {
            // Process border icon if it contains more options.
            ConfigurationSection backgroundSection = configurationSection.getConfigurationSection("background");

            if (backgroundSection != null)
            {
                backgroundItem = new PanelTemplateRecord.TemplateItem(
                    ItemParser.parse((backgroundSection.getString("icon", Material.AIR.name()))),
                    backgroundSection.getString("name", null),
                    backgroundSection.getString("description", null));
            }
        }
        else if (configurationSection.isString("background"))
        {
            // Process background icon if it contains only icon.

            backgroundItem = new PanelTemplateRecord.TemplateItem(
                ItemParser.parse((configurationSection.getString("background", Material.AIR.name()))));
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

        // Create template record.
        PanelTemplateRecord template = new PanelTemplateRecord(type, title, borderItem, backgroundItem);

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

        if (section.isConfigurationSection("fallback"))
        {
            fallback = readPanelItemTemplate(section.getConfigurationSection("fallback"));
        }
        else if (section.isString("fallback") && reusableItemMap != null)
        {
            fallback = reusableItemMap.get(section.getString("fallback"));
        }
        else
        {
            fallback = null;
        }

        // Create Item Record
        ItemTemplateRecord itemRecord = new ItemTemplateRecord(ItemParser.parse(section.getString("icon")),
            section.getString("title", null),
            section.getString("description", null),
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
