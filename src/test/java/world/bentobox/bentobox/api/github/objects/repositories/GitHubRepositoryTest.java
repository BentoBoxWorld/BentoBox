package world.bentobox.bentobox.api.github.objects.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import world.bentobox.bentobox.api.github.GitHubWebAPI;

class GitHubRepositoryTest {

    private GitHubWebAPI api;
    private GitHubRepository repo;

    @BeforeEach
    void setUp() {
        api = mock(GitHubWebAPI.class);
        repo = new GitHubRepository(api, "BentoBoxWorld/BentoBox");
    }

    @Test
    void testGetLatestTagName_returnsFirstTagName() throws Exception {
        JsonArray tags = new JsonArray();
        JsonObject tag1 = new JsonObject();
        tag1.addProperty("name", "3.12.0");
        JsonObject tag2 = new JsonObject();
        tag2.addProperty("name", "3.11.2");
        tags.add(tag1);
        tags.add(tag2);
        when(api.fetchArray("repos/BentoBoxWorld/BentoBox/tags")).thenReturn(tags);

        assertEquals("3.12.0", repo.getLatestTagName());
    }

    @Test
    void testGetLatestTagName_emptyArray_returnsEmptyString() throws Exception {
        when(api.fetchArray("repos/BentoBoxWorld/BentoBox/tags")).thenReturn(new JsonArray());

        assertEquals("", repo.getLatestTagName());
    }

    @Test
    void testGetLatestTagName_apiThrows_propagatesException() throws Exception {
        when(api.fetchArray("repos/BentoBoxWorld/BentoBox/tags")).thenThrow(new IOException("Network error"));

        assertThrows(IOException.class, () -> repo.getLatestTagName());
    }
}
