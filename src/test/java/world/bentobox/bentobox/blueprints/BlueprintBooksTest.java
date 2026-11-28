package world.bentobox.bentobox.blueprints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.WritableBookMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * Tests books that a blueprint marks with a tag.
 */
class BlueprintBooksTest extends CommonTestSetup {

    private static final String PREFIX = "bskyblock.books.welcome.";

    private MockedStatic<BlueprintBooks> books;
    /**
     * The pages that were written to the last signed book. MockBukkit cannot hold pages that
     * are components, so they are caught on the way in.
     */
    private List<Component> written;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Real items and real text
        mockedBukkit.when(Bukkit::getItemFactory).thenReturn(server.getItemFactory());
        mockedUtil.when(() -> Util.parseMiniMessageOrLegacy(anyString())).thenCallRealMethod();

        GameModeAddon addon = mock(GameModeAddon.class);
        when(addon.getDescription()).thenReturn(new AddonDescription.Builder("main", "BSkyBlock", "1.0").build());
        when(iwm.getAddon(any())).thenReturn(Optional.of(addon));
        when(island.getWorld()).thenReturn(world);
        when(island.getOwner()).thenReturn(uuid);
        PlayersManager players = plugin.getPlayers();
        when(players.getName(uuid)).thenReturn("tastybento");
        // Nothing is in the locale unless a test puts it there
        when(lm.getOrDefault(any(), anyString(), anyString())).thenAnswer(i -> i.getArgument(2));
        books = Mockito.mockStatic(BlueprintBooks.class, Mockito.CALLS_REAL_METHODS);
        books.when(() -> BlueprintBooks.setPages(any(), any())).thenAnswer(i -> {
            written = i.getArgument(1);
            return null;
        });
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        books.closeOnDemand();
        super.tearDown();
    }

    private void locale(String key, String value) {
        when(lm.getOrDefault(any(), eq(PREFIX + key), anyString())).thenReturn(value);
    }

    private static ItemStack tagged(Material type, String firstPage) {
        ItemStack item = new ItemStack(type);
        WritableBookMeta meta = (WritableBookMeta) item.getItemMeta();
        meta.addPage(firstPage);
        item.setItemMeta(meta);
        return item;
    }

    private String plain(int page) {
        return PlainTextComponentSerializer.plainText().serialize(written.get(page - 1));
    }

    @Test
    void testTagIsTheWholeFirstPage() {
        assertEquals("welcome", BlueprintBooks.getTag(tagged(Material.WRITABLE_BOOK, "[welcome]")));
        assertEquals("welcome", BlueprintBooks.getTag(tagged(Material.WRITTEN_BOOK, " [Welcome]\n")));
        assertEquals("book_1", BlueprintBooks.getTag(tagged(Material.WRITABLE_BOOK, "&a[book_1]")));
        assertNull(BlueprintBooks.getTag(tagged(Material.WRITABLE_BOOK, "See [welcome] for more")));
        assertNull(BlueprintBooks.getTag(tagged(Material.WRITABLE_BOOK, "[two words]")));
        assertNull(BlueprintBooks.getTag(new ItemStack(Material.WRITABLE_BOOK)));
        assertNull(BlueprintBooks.getTag(new ItemStack(Material.STONE)));
        assertNull(BlueprintBooks.getTag(null));
    }

    @Test
    void testBookIsWrittenAndSigned() {
        locale("text", "Hello [name]!\n[page]\n&a The second page");
        locale("title", "&6 Welcome");
        locale("author", "The server");
        ItemStack book = BlueprintBooks.resolve(island, tagged(Material.WRITABLE_BOOK, "[welcome]"));
        assertEquals(Material.WRITTEN_BOOK, book.getType());
        BookMeta meta = (BookMeta) book.getItemMeta();
        assertEquals(2, written.size());
        assertEquals("Hello tastybento!", plain(1));
        assertEquals("The second page", plain(2).strip());
        assertEquals("Welcome", meta.getTitle());
        assertEquals("The server", meta.getAuthor());
    }

    @Test
    void testTitleDefaultsToTheTagAndIsNotTooLong() {
        locale("text", "Hello");
        BookMeta meta = (BookMeta) BlueprintBooks.resolve(island, tagged(Material.WRITABLE_BOOK, "[welcome]"))
                .getItemMeta();
        assertEquals("welcome", meta.getTitle());
        locale("title", "A title that is far too long for the cover of a book");
        meta = (BookMeta) BlueprintBooks.resolve(island, tagged(Material.WRITABLE_BOOK, "[welcome]")).getItemMeta();
        assertEquals(32, meta.getTitle().length());
    }

    @Test
    void testUnsignedBookStaysABookAndQuill() {
        locale("text", "&a Write here");
        locale("signed", "false");
        ItemStack book = BlueprintBooks.resolve(island, tagged(Material.WRITTEN_BOOK, "[welcome]"));
        assertEquals(Material.WRITABLE_BOOK, book.getType());
        WritableBookMeta meta = (WritableBookMeta) book.getItemMeta();
        assertEquals(" Write here", meta.getPage(1));
    }

    @Test
    void testBooksWithoutTextAreLeftAlone() {
        ItemStack unknown = tagged(Material.WRITABLE_BOOK, "[welcome]");
        assertSame(unknown, BlueprintBooks.resolve(island, unknown));
        locale("text", "Hello");
        // Not a tag, not a book, no island
        ItemStack diary = tagged(Material.WRITABLE_BOOK, "Dear diary");
        assertSame(diary, BlueprintBooks.resolve(island, diary));
        ItemStack stone = new ItemStack(Material.STONE);
        assertSame(stone, BlueprintBooks.resolve(island, stone));
        assertSame(unknown, BlueprintBooks.resolve(null, unknown));
        assertNull(BlueprintBooks.resolve(island, null));
    }

    @Test
    void testStackSizeIsKept() {
        locale("text", "Hello");
        ItemStack stack = tagged(Material.WRITTEN_BOOK, "[welcome]");
        stack.setAmount(3);
        assertEquals(3, BlueprintBooks.resolve(island, stack).getAmount());
    }

    @Test
    void testNoOwnerUsesTheServersLanguage() {
        when(island.getOwner()).thenReturn(null);
        locale("text", "Hello [name]");
        ItemStack book = BlueprintBooks.resolve(island, tagged(Material.WRITABLE_BOOK, "[welcome]"));
        assertEquals(Material.WRITTEN_BOOK, book.getType());
        assertEquals("Hello", plain(1).strip());
    }

    @Test
    void testShortTextIsOnePage() {
        assertEquals(List.of("Hello\nthere"), BlueprintBooks.paginate("Hello\nthere\n"));
        assertEquals(List.of(""), BlueprintBooks.paginate(""));
    }

    @Test
    void testLongLinesBreakBetweenWords() {
        List<String> pages = BlueprintBooks.paginate("The quick brown fox jumps over the lazy dog");
        assertEquals(1, pages.size());
        for (String line : pages.get(0).split("\n")) {
            assertTrue(line.length() <= BlueprintBooks.LINE_WIDTH, line);
        }
        assertEquals("The quick brown fox jumps over the lazy dog", pages.get(0).replace('\n', ' '));
        // A word that is too long for a line is kept whole
        assertEquals(List.of("a\nsupercalifragilisticexpialidocious\nb"),
                BlueprintBooks.paginate("a supercalifragilisticexpialidocious b"));
    }

    @Test
    void testFormattingDoesNotCountTowardsWidth() {
        // Nineteen letters fit on a line, however much formatting there is
        String line = "<green>abcdefghi</green> &b&l" + "jklmnopqr";
        assertEquals(List.of(line), BlueprintBooks.paginate(line));
    }

    @Test
    void testTextFlowsOntoMorePages() {
        String text = "line\n".repeat(BlueprintBooks.PAGE_LINES * 2 + 3);
        List<String> pages = BlueprintBooks.paginate(text);
        assertEquals(3, pages.size());
        assertEquals(BlueprintBooks.PAGE_LINES, pages.get(0).split("\n").length);
        assertEquals(3, pages.get(2).split("\n").length);
    }

    @Test
    void testPageBreak() {
        assertEquals(List.of("one", "two\nthree"), BlueprintBooks.paginate("one\n [PAGE] \ntwo\nthree"));
        // Formatting on the line with the break is ignored, and goes with it
        assertEquals(List.of("one", "two"), BlueprintBooks.paginate("one\n[page]<gold><blue>\ntwo"));
        assertEquals(List.of("one", "two"), BlueprintBooks.paginate("one\n&a[page]\n\ntwo"));
        // In the middle of a line it is only text
        assertEquals(List.of("see [page] here"), BlueprintBooks.paginate("see [page] here"));
        // Blank lines are kept inside a page but not at the top of one
        assertEquals(List.of("one\n\ntwo", "three"), BlueprintBooks.paginate("one\n\ntwo\n[page]\n\nthree"));
    }

    @Test
    void testABookCannotHaveMoreThanAHundredPages() {
        assertEquals(100, BlueprintBooks.paginate("x\n[page]\n".repeat(150)).size());
        assertFalse(BlueprintBooks.paginate("x").isEmpty());
    }
}
