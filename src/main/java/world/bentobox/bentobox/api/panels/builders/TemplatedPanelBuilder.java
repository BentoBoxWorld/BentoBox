//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.bentobox.api.panels.builders;


import java.io.File;
import java.util.*;
import java.util.function.BiFunction;

import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelListener;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.panels.reader.PanelTemplateRecord;
import world.bentobox.bentobox.api.panels.reader.TemplateReader;
import world.bentobox.bentobox.api.user.User;


/**
 * Builds {@link TemplatedPanel}'s
 * @author BONNe
 * @since 1.17.3
 */
public class TemplatedPanelBuilder
{
// ---------------------------------------------------------------------
// Section: Builder
// ---------------------------------------------------------------------

    /**
     * Adds the template that must be loaded for Template panel builder.
     *
     * @param guiName the gui name
     * @param dataFolder the data folder
     * @return the template panel builder
     */
    public TemplatedPanelBuilder template(String guiName, File dataFolder)
    {
        this.panelTemplate = TemplateReader.readTemplatePanel(guiName, dataFolder);
        return this;
    }


    /**
     * Adds the user for template panel builder.
     *
     * @param user the user
     * @return the template panel builder
     */
    public TemplatedPanelBuilder user(User user)
    {
        this.user = user;
        return this;
    }


    /**
     * Adds the world for template panel builder.
     *
     * @param world the world
     * @return the template panel builder
     */
    public TemplatedPanelBuilder world(World world)
    {
        this.world = world;
        return this;
    }


    /**
     * Parameters for title of templated panel.
     *
     * @param parameters the parameters for title
     * @return the templated panel builder
     * @since 1.20.0
     */
    public TemplatedPanelBuilder parameters(@NonNull String... parameters)
    {
        if (parameters.length > 0)
        {
            this.parameters.addAll(Arrays.stream(parameters).toList());
        }

        return this;
    }


    /**
     * Adds the panel listener for template panel builder.
     *
     * @param listener the listener
     * @return the template panel builder
     */
    public TemplatedPanelBuilder listener(PanelListener listener)
    {
        this.listener = listener;
        return this;
    }


    /**
     * Registers new button type builder for template panel builder.
     *
     * @param type the type
     * @param buttonCreator the button creator
     * @return the template panel builder
     */
    public TemplatedPanelBuilder registerTypeBuilder(String type, BiFunction<ItemTemplateRecord, TemplatedPanel.ItemSlot, PanelItem> buttonCreator)
    {
        this.objectCreatorMap.put(type, buttonCreator);
        return this;
    }


    /**
     * Build templated panel.
     *
     * @return the templated panel
     */
    public TemplatedPanel build()
    {
        return new TemplatedPanel(this);
    }


// ---------------------------------------------------------------------
// Section: Getters
// ---------------------------------------------------------------------


    /**
     * Gets panel template.
     *
     * @return the panel template
     */
    public PanelTemplateRecord getPanelTemplate()
    {
        return this.panelTemplate;
    }


    /**
     * Gets user.
     *
     * @return the user
     */
    public User getUser()
    {
        return this.user;
    }


    /**
     * Gets world.
     *
     * @return the world
     */
    public World getWorld()
    {
        return this.world;
    }


    /**
     * Get title parameters for panel title.
     *
     * @return the list of parameters for title.
     */
    public List<String> getParameters()
    {
        return this.parameters;
    }


    /**
     * Gets listener.
     *
     * @return the listener
     */
    public PanelListener getListener()
    {
        return this.listener;
    }


    /**
     * Gets object creator map.
     *
     * @return the object creator map
     */
    public Map<String, BiFunction<ItemTemplateRecord, TemplatedPanel.ItemSlot, PanelItem>> getObjectCreatorMap()
    {
        return this.objectCreatorMap;
    }


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------


    /**
     * The GUI template record.
     */
    private PanelTemplateRecord panelTemplate;

    /**
     * The user who opens the GUI.
     */
    private User user;

    /**
     * The world where GUI operates.
     */
    private World world;

    /**
     * Panel Listener
     */
    private PanelListener listener;

    /**
     * The list of parameters for title object.
     */
    private final List<String> parameters = new ArrayList<>(0);

    /**
     * Map that links objects with their panel item creators.
     */
    private final Map<String, BiFunction<ItemTemplateRecord, TemplatedPanel.ItemSlot, PanelItem>> objectCreatorMap = new HashMap<>();
}
