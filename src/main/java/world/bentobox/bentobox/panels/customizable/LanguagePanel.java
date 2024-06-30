//
// Created by BONNe
// Copyright - 2022
//


package world.bentobox.bentobox.panels.customizable;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.BentoBoxLocale;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;


/**
 * This class generates Language Panel based on user specified file with name: "language_panel.yml".
 * If file with such name is located at gamemode panels directory, then that file will be used.
 * Otherwise, file in BentoBox/panels is used.
 */
public class LanguagePanel
{
    // ---------------------------------------------------------------------
    // Section: Constructor
    // ---------------------------------------------------------------------


    /**
     * This is internal constructor. It is used internally in current class to avoid creating objects everywhere.
     *
     * @param command The main addon command.
     * @param user User who opens panel
     */
    private LanguagePanel(@NonNull CompositeCommand command, @NonNull User user)
    {
        this.plugin = BentoBox.getInstance();
        this.mainCommand = command;
        this.user = user;

        this.elementList = BentoBox.getInstance().getLocalesManager().getAvailableLocales(true);
    }


    // ---------------------------------------------------------------------
    // Section: Methods
    // ---------------------------------------------------------------------


    /**
     * Build method manages current panel opening. It uses BentoBox PanelAPI that is easy to use and users can get nice
     * panels.
     */
    private void build()
    {
        // Do not open gui if there is no magic sticks.
        if (this.elementList.isEmpty())
        {
            this.plugin.logError("There are no available locales for selection!");
            this.user.sendMessage("no-locales",
                TextVariables.GAMEMODE, this.plugin.getDescription().getName());
            return;
        }

        // Start building panel.
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();

        // Set main template.
        if (this.doesCustomPanelExists(this.mainCommand.getAddon(), "language_panel"))
        {
            // Addon has its own island creation panel. Use it.
            panelBuilder.template("language_panel", new File(this.mainCommand.getAddon().getDataFolder(), "panels"));
        }
        else
        {
            // Use default island creation panel.
            panelBuilder.template("language_panel", new File(this.plugin.getDataFolder(), "panels"));
        }

        panelBuilder.user(this.user);
        panelBuilder.world(this.user.getWorld());

        // Register button builders
        panelBuilder.registerTypeBuilder(LOCALE, this::createLocaleButton);

        // Register next and previous builders
        panelBuilder.registerTypeBuilder(NEXT, this::createNextButton);
        panelBuilder.registerTypeBuilder(PREVIOUS, this::createPreviousButton);

        // Register unknown type builder.
        panelBuilder.build();
    }


    /**
     * This method returns if panel with the requested name is located in GameModeAddon folder.
     * @param addon GameModeAddon that need to be checked.
     * @param name Name of the panel.
     * @return {@code true} if panel exists, {@code false} otherwise.
     */
    private boolean doesCustomPanelExists(GameModeAddon addon, String name)
    {
        return addon.getDataFolder().exists() &&
            new File(addon.getDataFolder(), "panels").exists() &&
            new File(addon.getDataFolder(), "panels" + File.separator + name + ".yml").exists();
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
    @Nullable
    private PanelItem createNextButton(@NonNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        int size = this.elementList.size();

        if (size <= slot.amountMap().getOrDefault(LOCALE, 1) ||
            1.0 * size / slot.amountMap().getOrDefault(LOCALE, 1) <= this.pageIndex + 1)
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
            builder.name(this.user.getTranslation(template.title()));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(template.description(),
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
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation( action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

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
    private PanelItem createPreviousButton(@NonNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
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
            builder.name(this.user.getTranslation(this.mainCommand.getWorld(), template.title()));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(this.mainCommand.getWorld(), template.description(),
                TextVariables.NUMBER, String.valueOf(previousPageIndex)));
        }

        // Add ClickHandler
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
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(this.mainCommand.getWorld(), action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

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
     * This method creates and returns locale button.
     *
     * @return PanelItem that represents locale button.
     */
    @Nullable
    private PanelItem createLocaleButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        if (this.elementList.isEmpty())
        {
            // Does not contain any sticks.
            return null;
        }

        int index = this.pageIndex * slot.amountMap().getOrDefault(LOCALE, 1) + slot.slot();

        Locale locale;

        if (index >= this.elementList.size())
        {
            // Out of index.
            locale = null;
        }
        else
        {
            locale = this.elementList.get(index);
        }

        if (template.dataMap().containsKey("lang_id"))
        {
            // Try to find locale with requested ID. if not found, use already collected locale.
            locale = this.elementList.stream().
                filter(localeID -> localeID.toLanguageTag().equals(template.dataMap().get("lang_id"))).
                findFirst().
                orElse(locale);
        }

        return this.createLocaleButton(template, locale);
    }


    // ---------------------------------------------------------------------
    // Section: Other methods
    // ---------------------------------------------------------------------


    /**
     * This method creates locale button.
     *
     * @return PanelItem that allows to select locale button
     */
    private PanelItem createLocaleButton(ItemTemplateRecord template, Locale locale)
    {
        if (locale == null)
        {
            // return as locale is null. Empty button will be created.
            return null;
        }

        final String reference = "panels.language.buttons.language.";

        // Get settings for island.
        PanelItemBuilder builder = new PanelItemBuilder();

        BentoBoxLocale language = this.plugin.getLocalesManager().getLanguages().get(locale);

        if (template.icon() != null)
        {
            builder.icon(template.icon().clone());
        }
        else
        {
            builder.icon(Objects.requireNonNullElseGet(language.getBanner(),
                () -> new ItemStack(Material.WHITE_BANNER, 1)));
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.mainCommand.getWorld(), template.title(),
                TextVariables.NAME, WordUtils.capitalize(locale.getDisplayName(this.user.getLocale()))));
        }
        else
        {
            builder.name(this.user.getTranslation(reference + "name",
                TextVariables.NAME, WordUtils.capitalize(locale.getDisplayName(this.user.getLocale()))));
        }

        final StringBuilder authors = new StringBuilder();
        authors.append(this.user.getTranslation(reference + "authors"));

        for (String author : language.getAuthors())
        {
            authors.append("\n").append(this.user.getTranslation(reference + "author", TextVariables.NAME, author));
        }

        final StringBuilder selected = new StringBuilder();

        if (this.user.getLocale().equals(locale))
        {
            selected.append(this.user.getTranslation(reference + "selected"));
        }

        String descriptionText;

        if (template.description() != null)
        {
            descriptionText = this.user.getTranslationOrNothing(template.description(),
                AUTHORS, authors.toString(),
                SELECTED, selected.toString());
        }
        else
        {
            descriptionText = this.user.getTranslationOrNothing(reference + "description",
                AUTHORS, authors.toString(),
                SELECTED, selected.toString());
        }

        descriptionText = descriptionText.replaceAll("(?m)^[ \\t]*\\r?\\n", "").
            replaceAll("(?<!\\\\)\\|", "\n").
            replaceAll("\\\\\\|", "|");

        builder.description(descriptionText);

        // Display actions only for non-selected locales.
        List<ItemTemplateRecord.ActionRecords> actions = template.actions().stream().
            filter(action -> !this.user.getLocale().equals(locale) &&
                (SELECT_ACTION.equalsIgnoreCase(action.actionType()) ||
                    COMMANDS_ACTION.equalsIgnoreCase(action.actionType()))).
            toList();

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            actions.forEach(action -> {
                if (clickType == action.clickType() || action.clickType() == ClickType.UNKNOWN)
                {
                    if (SELECT_ACTION.equalsIgnoreCase(action.actionType()))
                    {
                        this.plugin.getPlayers().setLocale(this.user.getUniqueId(), locale.toLanguageTag());
                        this.user.sendMessage("language.edited", "[lang]",
                            WordUtils.capitalize(locale.getDisplayName(this.user.getLocale())));

                        // Rebuild panel
                        this.build();
                    }
                    else if (COMMANDS_ACTION.equalsIgnoreCase(action.actionType()))
                    {
                        Util.runCommands(user,
                            Arrays.stream(action.content().
                                    replaceAll(Pattern.quote(TextVariables.LABEL), this.mainCommand.getTopLabel()).
                                    split("\n")).
                                toList(),
                            "CHANGE_LOCALE_COMMANDS");
                    }
                }
            });

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = actions.stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(this.mainCommand.getWorld(), action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(actions.size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
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
     * @param command The main addon command.
     * @param user User who opens panel
     */
    public static void openPanel(@NonNull CompositeCommand command, @NonNull User user)
    {
        new LanguagePanel(command, user).build();
    }


// ---------------------------------------------------------------------
// Section: Constants
// ---------------------------------------------------------------------


    /**
     * This constant is used for button to indicate that it is Language type.
     */
    private static final String LOCALE = "LOCALE";

    /**
     * This constant is used for button to indicate that it is previous page type.
     */
    private static final String PREVIOUS = "PREVIOUS";

    /**
     * This constant is used for button to indicate that it is next page type.
     */
    private static final String NEXT = "NEXT";

    /**
     * This constant is used for indicating that pages should contain numbering.
     */
    private static final String INDEXING = "indexing";

    /**
     * This constant stores value for SELECT action that is used in panels.
     */
    private static final String SELECT_ACTION = "SELECT";

    /**
     * This constant stores value for COMMANDS action that is used in panels.
     */
    private static final String COMMANDS_ACTION = "COMMANDS";

    /**
     * This constant stores value for AUTHORS label that is used in panels.
     */
    public static final String AUTHORS = "[authors]";

    /**
     * This constant stores value for SELECTED label that is used in panels.
     */
    public static final String SELECTED = "[selected]";


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------


    /**
     * This variable allows to access plugin object.
     */
    private final BentoBox plugin;

    /**
     * This variable stores the main command object.
     */
    private final CompositeCommand mainCommand;

    /**
     * This variable holds user who opens panel. Without it panel cannot be opened.
     */
    private final User user;

    /**
     * This variable stores filtered elements.
     */
    private final List<Locale> elementList;

    /**
     * This variable holds current pageIndex for multi-page island choosing.
     */
    private int pageIndex;
}
