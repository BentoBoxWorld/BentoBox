package world.bentobox.bentobox.api.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link RequestedSettings}
 */
class RequestedSettingsTest {

    enum Kind {
        VANILLA, BARRIER
    }

    static class TestSettings {
        @ConfigEntry(path = "options.flag")
        private boolean flag = false;
        @ConfigEntry(path = "options.count")
        private int count = 1;
        @ConfigEntry(path = "type")
        private Kind type = Kind.VANILLA;
        @ConfigEntry(path = "name")
        private String name = "a";
        @ConfigEntry(path = "private.secret")
        private boolean secret = false;
        private String notConfig = "x";
    }

    @Test
    void testRequestsAreAppliedToACopy() {
        TestSettings settings = new TestSettings();
        settings.count = 5;
        settings.notConfig = "kept";
        Map<String, Object> requests = new LinkedHashMap<>();
        requests.put("options.flag", true);
        requests.put("options.count", 7);
        requests.put("type", "barrier");
        requests.put("name", "b");

        var result = RequestedSettings.apply(settings, requests, p -> true);

        assertNotSame(settings, result.settings());
        assertTrue(result.settings().flag);
        assertEquals(7, result.settings().count);
        assertEquals(Kind.BARRIER, result.settings().type);
        assertEquals("b", result.settings().name);
        assertEquals("kept", result.settings().notConfig);
        assertEquals(List.of("options.flag: true", "options.count: 7", "type: barrier", "name: b"), result.applied());
        assertTrue(result.refused().isEmpty());
        // The admin's settings are untouched
        assertFalse(settings.flag);
        assertEquals(5, settings.count);
        assertEquals(Kind.VANILLA, settings.type);
    }

    @Test
    void testRequestThatMatchesIsNotListed() {
        var result = RequestedSettings.apply(new TestSettings(), Map.of("options.count", 1), p -> true);
        assertTrue(result.applied().isEmpty());
        assertTrue(result.refused().isEmpty());
    }

    @Test
    void testBadRequestsAreRefused() {
        Map<String, Object> requests = new LinkedHashMap<>();
        requests.put("no.such.path", true);
        requests.put("private.secret", true);
        requests.put("options.flag", "yes");
        requests.put("type", "nonsense");

        var result = RequestedSettings.apply(new TestSettings(), requests, p -> !p.startsWith("private."));

        assertTrue(result.applied().isEmpty());
        assertEquals(4, result.refused().size());
        assertFalse(result.settings().secret);
        assertFalse(result.settings().flag);
        assertEquals(Kind.VANILLA, result.settings().type);
    }
}
