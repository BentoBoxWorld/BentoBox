//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.bentobox.api.panels.reader;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;


/**
 * This Record contains all necessary information about Item Template that can be used to craft panel item.
 *
 * @param icon ItemStack of the Item
 * @param title Title of the item
 * @param description Lore message of the item
 * @param actions List of Actions for a button
 * @param dataMap DataMap that links additional objects for a button.
 * @param fallback FallBack item if current one is not possible to generate.
 *
 * @since 1.17.3
 */
public record ItemTemplateRecord(@Nullable ItemStack icon,
        @Nullable String title,
        @Nullable String description,
        @NonNull List<ActionRecords> actions,
        @NonNull Map<String, Object> dataMap,
        @Nullable ItemTemplateRecord fallback)
{
    /**
     * Instantiates a new Item template record without actions and data map.
     *
     * @param icon the icon
     * @param title the title
     * @param description the description
     * @param fallback the fallback
     */
    public ItemTemplateRecord(ItemStack icon, String title, String description, ItemTemplateRecord fallback)
    {
        this(icon, title, description, new ArrayList<>(6), new HashMap<>(0), fallback);
    }


    /**
     * This method adds given object associated with key into data map.
     * @param key Key value of object.
     * @param data Data that is associated with a key.
     */
    public void addData(String key, Object data)
    {
        this.dataMap.put(key, data);
    }


    /**
     * Add action to the actions list.
     *
     * @param actionData the action data
     */
    public void addAction(ActionRecords actionData)
    {
        this.actions.add(actionData);
    }


    // ---------------------------------------------------------------------
    // Section: Classes
    // ---------------------------------------------------------------------


    /**
     * The Action Records holds data about each action.
     *
     * @param clickType the click type
     * @param actionType the string that represents action type
     * @param content the content of the action
     * @param tooltip the tooltip of action
     */
    public record ActionRecords(ClickType clickType, String actionType, String content, String tooltip) {}
}
