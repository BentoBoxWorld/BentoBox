/**
 * API for server-driven modal dialog creation and usage.
 *
 * <p>
 * Alongside the {@link world.bentobox.bentobox.api.panels Panels API}, BentoBox
 * provides a Dialogs API that wraps Paper's dialog system
 * ({@code io.papermc.paper.dialog.Dialog}) so core and addons can present real
 * modal choices to players without them having to type commands. Unlike
 * inventory-GUI panels, dialogs are true modal UI: buttons, optional body text,
 * and they can be configured so that pressing <kbd>Esc</kbd> does not dismiss
 * them.
 * </p>
 * <p>
 * Dialogs are built with a fluent {@link world.bentobox.bentobox.api.dialogs.DialogBuilder}
 * in the spirit of {@code PanelBuilder}. Titles and body text are localized
 * through the existing {@link world.bentobox.bentobox.api.user.User} translation
 * system and are carried end-to-end as Adventure
 * {@link net.kyori.adventure.text.Component Components}, so click/interaction
 * data survives (the legacy {@code §}-string chat path cannot carry it).
 * </p>
 * <p>
 * Buttons carry a {@link world.bentobox.bentobox.api.dialogs.DialogButton#onClick()}
 * callback that runs on the server's main thread when the player clicks. Because
 * the dialog classes only exist on Minecraft 26+, callers that need to degrade
 * gracefully should guard with {@link world.bentobox.bentobox.api.dialogs.Dialogs#isSupported()}.
 * </p>
 *
 * @author tastybento
 * @since 3.21.0
 */
package world.bentobox.bentobox.api.dialogs;
