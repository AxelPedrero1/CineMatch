package app.cinematch.agent.tools;

import app.cinematch.util.JsonStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de WishlistTools suivant le pattern GIVEN / WHEN / THEN.
 */
class WishlistToolsTest {

    private WishlistTools tools;

    @BeforeEach
    void setUp() {
        tools = new WishlistTools();
    }

    @Test
    void givenValidTitle_whenAddToWishlist_thenReturnsAddedMessageAndCallsJsonStorage() {
        // GIVEN
        String title = " Inception ";

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            // WHEN
            String result = tools.addToWishlist(title);

            // THEN
            mocked.verify(() -> JsonStorage.addOrUpdate("Inception", "envie"));
            assertEquals("ADDED:Inception", result);
        }
    }

    @Test
    void givenBlankTitle_whenAddToWishlist_thenReturnsErrorAndDoesNotCallStorage() {
        // GIVEN
        String title = "   ";

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            // WHEN
            String result = tools.addToWishlist(title);

            // THEN
            mocked.verifyNoInteractions();
            assertEquals("ERROR:EMPTY_TITLE", result);
        }
    }

    @Test
    void givenNullTitle_whenAddToWishlist_thenReturnsError() {
        // GIVEN
        String title = null;

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            // WHEN
            String result = tools.addToWishlist(title);

            // THEN
            mocked.verifyNoInteractions();
            assertEquals("ERROR:EMPTY_TITLE", result);
        }
    }

    @Test
    void givenValidTitle_whenRemoveFromWishlist_thenReturnsRemovedMessage() {
        // GIVEN
        String title = " Titanic ";

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            // WHEN
            String result = tools.removeFromWishlist(title);

            // THEN
            mocked.verify(() -> JsonStorage.addOrUpdate("Titanic", "pas_interesse"));
            assertEquals("REMOVED:Titanic", result);
        }
    }

    @Test
    void givenBlankTitle_whenRemoveFromWishlist_thenReturnsError() {
        // GIVEN
        String title = "   ";

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            // WHEN
            String result = tools.removeFromWishlist(title);

            // THEN
            mocked.verifyNoInteractions();
            assertEquals("ERROR:EMPTY_TITLE", result);
        }
    }

    @Test
    void givenNullTitle_whenRemoveFromWishlist_thenReturnsError() {
        // GIVEN
        String title = null;

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            // WHEN
            String result = tools.removeFromWishlist(title);

            // THEN
            mocked.verifyNoInteractions();
            assertEquals("ERROR:EMPTY_TITLE", result);
        }
    }

    @Test
    void givenStatus_whenGetListByStatus_thenReturnsNormalizedFilteredList() {
        // GIVEN
        List<String> mockData = Arrays.asList(" Inception ", "\"Titanic\"", "  ");
        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(() -> JsonStorage.getByStatus("envie")).thenReturn(mockData);

            // WHEN
            List<String> result = tools.getListByStatus("envie");

            // THEN
            assertEquals(List.of("Inception", "Titanic"), result);
        }
    }

    @Test
    void givenBlankStatus_whenGetListByStatus_thenDefaultsToEnvie() {
        // GIVEN
        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(Collections.singletonList("Interstellar"));

            // WHEN
            List<String> result = tools.getListByStatus("  ");

            // THEN
            mocked.verify(() -> JsonStorage.getByStatus("envie"));
            assertEquals(List.of("Interstellar"), result);
        }
    }

    @Test
    void givenNullStatus_whenGetListByStatus_thenDefaultsToEnvie() {
        // GIVEN
        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(Collections.singletonList("Dune"));

            // WHEN
            List<String> result = tools.getListByStatus(null);

            // THEN
            mocked.verify(() -> JsonStorage.getByStatus("envie"));
            assertEquals(List.of("Dune"), result);
        }
    }

    @Test
    void givenDuplicates_whenGetListByStatus_thenRemovesDuplicates() {
        // GIVEN
        List<String> mockData = Arrays.asList(" Inception ", "Inception", "Inception  ");
        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(() -> JsonStorage.getByStatus("envie")).thenReturn(mockData);

            // WHEN
            List<String> result = tools.getListByStatus("envie");

            // THEN
            assertEquals(List.of("Inception"), result);
        }
    }

    @Test
    void givenVariousStrings_whenNormalize_thenCleansCorrectly() throws Exception {
        // GIVEN : on accède à la méthode privée normalize
        var normalize = WishlistTools.class.getDeclaredMethod("normalize", String.class);
        normalize.setAccessible(true);

        // WHEN / THEN : on teste plusieurs cas avec un argument à chaque fois
        assertEquals("Inception", normalize.invoke(tools, "  Inception  "));
        assertEquals("Titanic", normalize.invoke(tools, "\"Titanic\""));
        assertEquals("Dune", normalize.invoke(tools, "« Dune »"));
        assertEquals("", normalize.invoke(tools, new Object[]{null}));
        assertEquals("A B", normalize.invoke(tools, "A    B"));
    }
}
