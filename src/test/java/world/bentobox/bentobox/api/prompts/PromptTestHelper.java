package world.bentobox.bentobox.api.prompts;

import java.util.UUID;

/**
 * Lets tests in other packages answer a prompt that is waiting in chat.
 */
public final class PromptTestHelper {

    private PromptTestHelper() {
        // Utility class
    }

    /**
     * Answers the player's waiting chat prompt, as if the chat message had already reached
     * the main thread.
     *
     * @param uuid the player
     * @param text what they say
     */
    public static void say(UUID uuid, String text) {
        ChatInputListener.accept(uuid, text);
    }

    /**
     * Drops any waiting prompt, so one test cannot leak into the next.
     *
     * @param uuid the player
     */
    public static void clear(UUID uuid) {
        ChatInputListener.stop(uuid);
    }
}
