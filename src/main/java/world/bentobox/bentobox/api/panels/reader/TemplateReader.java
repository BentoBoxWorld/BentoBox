//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.bentobox.api.panels.reader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Enums;

import world.bentobox.bentobox.BentoBox;
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
    private static final String YML = ".yml";
    private static final String ACTIONS = "actions";
    private static final String TOOLTIP = "tooltip";
    private static final String CLICK_TYPE = "click-type";
    private static final String CONTENT = "content";
    private static final String TYPE = "type";


    /**
     * Utility classes, which are collections of static members, are not meant to be instantiated.
     * Even abstract utility classes, which can be extended, should not have public constructors.
     * Java adds an implicit public constructor to every class which does not define at least one explicitly.
     * Hence, at least one non-public constructor should be defined.
     */
    private TemplateReader() {}

    /**
     * Read template panel panel template record.
     *
     * @param panelName the panel name
     * @param panelLocation the panel location directory
     * @return the panel template record
     */
    public static PanelTemplateRecord readTemplatePanel(@NonNull String panelName, @NonNull File panelLocation)
    {
        return readTemplatePanel(panelName, panelName, panelLocation);
    }


    /**
     * Read template panel panel template record.
     *
     * @param panelName the panel name
     * @param templateName the template file name
     * @param panelLocation the panel location directory
     * @return the panel template record
     * @since 1.20.0
     */
    public static PanelTemplateRecord readTemplatePanel(@NonNull String panelName, @NonNull String templateName, @NonNull File panelLocation)
    {
        if (!panelLocation.exists())
        {
            BentoBox.getInstance().logError("Panel Template reader: Folder does not exist");
            // Return null because folder does not exist.
            return null;
        }

        File file = new File(panelLocation, templateName.endsWith(YML) ? templateName : templateName + YML);
        String absolutePath = file.getAbsolutePath();
        if (!file.exists())
        {
            // Try to get it from the JAR

            String keyword = "panels/";

            // Find the index of the keyword "panels/"
            int index = absolutePath.indexOf(keyword);

            // If the keyword is found, extract the substring starting from that index
            if (index != -1) {
                BentoBox.getInstance().saveResource(absolutePath.substring(index), false);
            } else {
                BentoBox.getInstance().logError(file.getAbsolutePath() + " does not exist for panel template");
                // Return as file does not exist.
                return null;
            }

        }

        final String panelKey = absolutePath + ":" + panelName;

        // Check if panel is already crafted.
        if (TemplateReader.loadedPanels.containsKey(panelKey))
        {
            return TemplateReader.loadedPanels.get(panelKey);
        }

        PanelTemplateRecord rec;

        try
        {
            // Load config
            YamlConfiguration config = new YamlConfiguration();
            config.load(file);
            // Read panel
            rec = readPanelTemplate(config.getConfigurationSection(panelName));
            // Put panel into memory
            TemplateReader.loadedPanels.put(panelKey, rec);
        }
        catch (IOException | InvalidConfigurationException e)
        {
            BentoBox.getInstance().logError("Error loading template");
            BentoBox.getInstance().logStacktrace(e);
            rec = null;
        }

        return rec;
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
            BentoBox.getInstance().logError("No configuration section!");
            // No data to return.
            return null;
        }

        String title = configurationSection.getString(TITLE);
        Panel.Type type =
                Enums.getIfPresent(Panel.Type.class, configurationSection.getString(TYPE, "INVENTORY")).
                or(Panel.Type.INVENTORY);

        PanelTemplateRecord.TemplateItem borderItem = readTemplateItem(configurationSection, BORDER);
        PanelTemplateRecord.TemplateItem backgroundItem = readTemplateItem(configurationSection, BACKGROUND);

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
        ConfigurationSection content = configurationSection.getConfigurationSection(CONTENT);

        if (content != null)
        {
            populateContentGrid(template, content, panelItemDataMap);
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

                // Force all rows from 1 to value (inclusive), so that force-shown: 6
                // results in a panel with 6 rows, not just forcing the 6th row alone.
                for (int i = 0; i < value && i < forceShow.length; i++)
                {
                    forceShow[i] = true;
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

        ItemTemplateRecord fallback = readFallback(section, reusableItemMap);

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
        if (section.isConfigurationSection(ACTIONS))
        {
            ConfigurationSection actionSection = section.getConfigurationSection(ACTIONS);

            if (actionSection != null)
            {
                readActionsFromSection(actionSection, itemRecord);
            }
        }
        else if (section.isList(ACTIONS))
        {
            readActionsFromList(section.getMapList(ACTIONS), itemRecord);
        }

        // Add item to the map
        if (reusableItemMap != null && itemKey != null)
        {
            reusableItemMap.put(itemKey, itemRecord);
        }

        return itemRecord;
    }


    /**
     * Reads a border or background TemplateItem from the given parent section and key.
     * Handles both section and plain-string formats.
     */
    private static PanelTemplateRecord.TemplateItem readTemplateItem(ConfigurationSection parent, String key)
    {
        if (parent.isConfigurationSection(key))
        {
            ConfigurationSection section = parent.getConfigurationSection(key);
            if (section == null) return null;
            return new PanelTemplateRecord.TemplateItem(
                    ItemParser.parse(section.getString(ICON, Material.AIR.name())),
                    section.getString(TITLE, null),
                    section.getString(DESCRIPTION, null));
        }
        else if (parent.isString(key))
        {
            return new PanelTemplateRecord.TemplateItem(
                    ItemParser.parse(parent.getString(key, Material.AIR.name())));
        }
        return null;
    }


    /**
     * Populates the panel template grid from a content configuration section.
     */
    private static void populateContentGrid(PanelTemplateRecord template,
            ConfigurationSection content,
            Map<String, ItemTemplateRecord> panelItemDataMap)
    {
        for (int rowIndex = 0; rowIndex < 6; rowIndex++)
        {
            String rowKey = String.valueOf(rowIndex + 1);
            ConfigurationSection line = content.getConfigurationSection(rowKey);
            if (line == null) continue;

            for (int colIndex = 0; colIndex < 9; colIndex++)
            {
                String colKey = String.valueOf(colIndex + 1);
                if (line.isConfigurationSection(colKey))
                {
                    template.addButtonTemplate(rowIndex, colIndex,
                            readPanelItemTemplate(line.getConfigurationSection(colKey), null, panelItemDataMap));
                }
                else if (line.isString(colKey))
                {
                    template.addButtonTemplate(rowIndex, colIndex,
                            panelItemDataMap.get(line.getString(colKey)));
                }
            }
        }
    }


    /**
     * Reads the fallback item for a panel item template section.
     */
    private static ItemTemplateRecord readFallback(ConfigurationSection section,
            Map<String, ItemTemplateRecord> reusableItemMap)
    {
        if (section.isConfigurationSection(FALLBACK))
        {
            return readPanelItemTemplate(section.getConfigurationSection(FALLBACK));
        }
        else if (section.isString(FALLBACK) && reusableItemMap != null)
        {
            return reusableItemMap.get(section.getString(FALLBACK));
        }
        return null;
    }


    /**
     * Reads action records from a ConfigurationSection and adds them to the item record.
     */
    private static void readActionsFromSection(ConfigurationSection actionSection,
            ItemTemplateRecord itemRecord)
    {
        actionSection.getKeys(false).forEach(actionKey -> {
            ConfigurationSection actionDataSection = actionSection.getConfigurationSection(actionKey);
            if (actionDataSection == null) return;
            ClickType clickType = Enums.getIfPresent(ClickType.class, actionKey.toUpperCase()).orNull();
            if (clickType != null)
            {
                itemRecord.addAction(new ItemTemplateRecord.ActionRecords(clickType,
                        actionDataSection.getString(TYPE),
                        actionDataSection.getString(CONTENT),
                        actionDataSection.getString(TOOLTIP)));
            }
            else if (actionDataSection.contains(CLICK_TYPE))
            {
                clickType = Enums.getIfPresent(ClickType.class,
                        actionDataSection.getString(CLICK_TYPE, "UNKNOWN").toUpperCase())
                        .or(ClickType.UNKNOWN);
                itemRecord.addAction(new ItemTemplateRecord.ActionRecords(clickType,
                        actionKey,
                        actionDataSection.getString(CONTENT),
                        actionDataSection.getString(TOOLTIP)));
            }
        });
    }


    /**
     * Reads action records from a list of maps and adds them to the item record.
     */
    private static void readActionsFromList(List<Map<?, ?>> actionList, ItemTemplateRecord itemRecord)
    {
        actionList.forEach(valueMap -> {
            ClickType clickType = Enums.getIfPresent(ClickType.class,
                    String.valueOf(valueMap.get(CLICK_TYPE)).toUpperCase()).orNull();
            if (clickType == null) return;
            itemRecord.addAction(new ItemTemplateRecord.ActionRecords(clickType,
                    valueMap.containsKey(TYPE) ? String.valueOf(valueMap.get(TYPE)) : null,
                    valueMap.containsKey(CONTENT) ? String.valueOf(valueMap.get(CONTENT)) : null,
                    valueMap.containsKey(TOOLTIP) ? String.valueOf(valueMap.get(TOOLTIP)) : null));
        });
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
