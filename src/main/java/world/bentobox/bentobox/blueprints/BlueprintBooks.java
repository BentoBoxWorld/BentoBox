package world.bentobox.bentobox.blueprints;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.WritableBookMeta;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * Fills in books that a blueprint marks with a tag, in the way {@link TextVariables#START_TEXT}
 * marks a sign.
 * <p>
 * A book, or book and quill, whose first page is only a tag such as {@code [welcome]} is
 * replaced when the blueprint is pasted. The text comes from the locale files of the game
 * mode the island is in, in the island owner's language:
 * <pre>
 * bskyblock:
 *   books:
 *     welcome:
 *       title: "Welcome"
 *       author: "The server"
 *       signed: true
 *       text: |
 *         Hello [name]!
 *         [page]
 *         This starts a new page.
 * </pre>
 * The text can use the same formatting as other messages. It is spread over as many pages as
 * it needs, and {@code [page]} starts a new page. The book is signed, so it cannot be edited,
 * unless {@code signed} is {@code false}, which leaves a book and quill. If the locale has no
 * text for the tag, the book is left as it is in the blueprint.
 *
 * @author tastybento
 * @since 3.25.0
 */
public final class BlueprintBooks {

    /**
     * Put on a line of its own in a book's text to start a new page
     */
    public static final String PAGE_BREAK = "[page]";
    private static final Pattern TAG = Pattern.compile("\\[([a-z0-9_-]+)]", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORMATTING = Pattern.compile("<[^<>]*>|[&§]#[0-9a-fA-F]{6}|[&§][0-9a-fk-orA-FK-OR]");
    /**
     * The game wraps a book's text by its width on the player's screen, which the server
     * cannot know. These are low enough that a page of the widest ordinary letters fits.
     */
    static final int LINE_WIDTH = 19;
    static final int PAGE_LINES = 14;
    private static final int MAX_PAGES = 100;
    private static final int MAX_TITLE = 32;

    private BlueprintBooks() {
    }

    /**
     * Gets the tag that a book is marked with.
     * @param item an item
     * @return the tag without its brackets, in lower case, or null if the item is not a book
     *         whose first page is only a tag
     */
    @Nullable
    public static String getTag(@Nullable ItemStack item) {
        if (item == null || (item.getType() != Material.WRITABLE_BOOK && item.getType() != Material.WRITTEN_BOOK)
                || !(item.getItemMeta() instanceof WritableBookMeta meta) || !meta.hasPages()) {
            return null;
        }
        Matcher m = TAG.matcher(FORMATTING.matcher(meta.getPage(1)).replaceAll("").strip());
        return m.matches() ? m.group(1).toLowerCase(Locale.ENGLISH) : null;
    }

    /**
     * Fills in a book if it is marked with a tag that the game mode has text for.
     * @param island the island being pasted, which may be null
     * @param item an item from a blueprint, which may be null
     * @return the book with its text, or the item as it was
     */
    @Nullable
    public static ItemStack resolve(@Nullable Island island, @Nullable ItemStack item) {
        String tag = getTag(item);
        if (tag == null || island == null || island.getWorld() == null) {
            return item;
        }
        BentoBox plugin = BentoBox.getInstance();
        String reference = plugin.getIWM().getAddon(island.getWorld())
                .map(addon -> addon.getDescription().getName().toLowerCase(Locale.ENGLISH)).orElse("") + ".books."
                + tag + ".";
        // The owner's language is used, or the server's if there is no owner yet
        User owner = Optional.ofNullable(island.getOwner()).map(User::getInstance).orElse(null);
        String text = plugin.getLocalesManager().getOrDefault(owner, reference + "text", "");
        if (text.isBlank()) {
            return item;
        }
        String name = island.getOwner() == null ? "" : plugin.getPlayers().getName(island.getOwner());
        List<String> pages = paginate(text.replace(TextVariables.NAME, name));
        boolean signed = !"false".equalsIgnoreCase(
                plugin.getLocalesManager().getOrDefault(owner, reference + "signed", "true").strip());
        ItemStack book = new ItemStack(signed ? Material.WRITTEN_BOOK : Material.WRITABLE_BOOK, item.getAmount());
        if (signed && book.getItemMeta() instanceof BookMeta meta) {
            String title = strip(plugin.getLocalesManager().getOrDefault(owner, reference + "title", tag));
            title = title.strip();
            meta.setTitle(title.length() > MAX_TITLE ? title.substring(0, MAX_TITLE) : title);
            // The title and the author are plain text
            meta.setAuthor(strip(plugin.getLocalesManager().getOrDefault(owner, reference + "author", "")
                    .replace(TextVariables.NAME, name)).strip());
            setPages(meta, pages.stream().map(Util::parseMiniMessageOrLegacy).toList());
            book.setItemMeta(meta);
        } else if (book.getItemMeta() instanceof WritableBookMeta meta) {
            // A book and quill holds plain text
            meta.setPages(pages.stream().map(BlueprintBooks::strip).toList());
            book.setItemMeta(meta);
        }
        return book;
    }

    /**
     * Writes the pages of a signed book. Pages are components, so they can have links and
     * hover text as well as colour.
     */
    static void setPages(BookMeta meta, List<Component> pages) {
        meta.pages(pages);
    }

    /**
     * Spreads text over pages. Lines that are too long are broken between words, and a line
     * that is only {@link #PAGE_BREAK} starts a new page. Formatting is not counted towards
     * the length of a line. It starts afresh on each page, so text that runs over a page
     * loses the colour it had.
     * @param text the text, with lines separated by newlines
     * @return the pages, at least one and no more than a book can hold
     */
    @NonNull
    static List<String> paginate(@NonNull String text) {
        List<String> pages = new ArrayList<>();
        List<String> page = new ArrayList<>();
        for (String line : text.strip().split("\\R", -1)) {
            if (line.strip().equalsIgnoreCase(PAGE_BREAK)) {
                pages.add(String.join("\n", page));
                page = new ArrayList<>();
                continue;
            }
            for (String part : wrap(line)) {
                if (page.size() == PAGE_LINES) {
                    pages.add(String.join("\n", page));
                    page = new ArrayList<>();
                }
                // A page does not start with an empty line left over from the page before
                if (!page.isEmpty() || !part.isBlank()) {
                    page.add(part);
                }
            }
        }
        pages.add(String.join("\n", page));
        return pages.size() > MAX_PAGES ? pages.subList(0, MAX_PAGES) : pages;
    }

    /**
     * Breaks a line between words so that no part is wider than a page.
     */
    private static List<String> wrap(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int width = 0;
        for (String word : line.split(" ", -1)) {
            int length = strip(word).length();
            if (width > 0 && width + 1 + length > LINE_WIDTH) {
                parts.add(current.toString());
                current = new StringBuilder();
                width = 0;
            }
            if (width > 0 || !current.isEmpty()) {
                current.append(' ');
                width++;
            }
            current.append(word);
            width += length;
        }
        parts.add(current.toString());
        return parts;
    }

    /**
     * @return text without its formatting
     */
    private static String strip(String text) {
        return FORMATTING.matcher(text).replaceAll("");
    }
}
