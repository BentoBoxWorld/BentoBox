/**
 * API for asking a player to type something: a name, a number, a few lines of text, or a
 * yes/no confirmation.
 *
 * <p>
 * Start from {@link world.bentobox.bentobox.api.prompts.Prompts}. A prompt is shown as a
 * {@link world.bentobox.bentobox.api.dialogs dialog} with a text box where the server supports
 * dialogs and {@code island.dialogs.prompts} is on, and otherwise asks in chat. The caller does
 * not need to know which: either way the answer is checked, the player is asked again if it is
 * wrong, and the result is delivered on the server's main thread.
 * </p>
 * <p>
 * This replaces Bukkit's {@code org.bukkit.conversations} API, which Paper has deprecated for
 * removal. Nothing in this package depends on it.
 * </p>
 * <pre>{@code
 * Prompts.text(user)
 *     .title("myaddon.rename.title")
 *     .initial(thing.getName())
 *     .maxLength(32)
 *     .validate(name -> taken(name) ? user.getTranslation("myaddon.rename.taken") : null)
 *     .onSubmit(thing::setName)
 *     .onCancel(() -> panel.open())
 *     .open();
 * }</pre>
 *
 * @author tastybento
 * @since 3.25.0
 */
package world.bentobox.bentobox.api.prompts;
