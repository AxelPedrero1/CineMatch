package app.cinematch.agent.tools;

import app.cinematch.MovieRecommenderService;
import app.cinematch.util.JsonStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LibraryToolsTest {

    @Test
    @DisplayName("markAsSeen - simple -> 'deja_vu'")
    void givenTitle_whenMarkAsSeen_thenStoredAsSeen() {
        // GIVEN: JsonStorage mocké & un titre
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN: marquer 'Dune' comme déjà vu
            String res = tools.markAsSeen(" Dune ");

            // THEN: statut 'deja_vu' & message de confirmation
            js.verify(() -> JsonStorage.addOrUpdate("Dune", "deja_vu"));
            assertEquals("SEEN:Dune", res);
        }
    }

    @Test
    @DisplayName("markAsDisliked - simple -> 'pas_interesse'")
    void givenTitle_whenMarkAsDisliked_thenStoredAsNope() {
        // GIVEN
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN
            String res = tools.markAsDisliked("\"Matrix\"");

            // THEN
            js.verify(() -> JsonStorage.addOrUpdate("Matrix", "pas_interesse"));
            assertEquals("DISLIKED:Matrix", res);
        }
    }

    @Test
    @DisplayName("setStatus - simple -> applique le statut demandé")
    void givenTitleAndStatus_whenSetStatus_thenUpdated() {
        // GIVEN
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN
            String res = tools.setStatus(" Jojo Rabbit ", "pas_interesse");

            // THEN
            js.verify(() -> JsonStorage.addOrUpdate("Jojo Rabbit", "pas_interesse"));
            assertEquals("STATUS_CHANGED:Jojo Rabbit->pas_interesse", res);
        }
    }

    @Test
    @DisplayName("generateDescription - délègue au service")
    void givenTitle_whenGenerateDescription_thenDelegatesToService() {
        // GIVEN: service mocké
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        when(svc.generateDescription("Alien")).thenReturn("Desc courte Alien");
        LibraryTools tools = new LibraryTools(svc);

        // WHEN: demande de description
        String res = tools.generateDescription(" Alien ");

        // THEN: le service est appelé & la réponse est relayée
        assertEquals("Desc courte Alien", res);
        verify(svc).generateDescription("Alien");
    }
}
