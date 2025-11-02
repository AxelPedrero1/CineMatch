package app.cinematch.agent.tools;

import app.cinematch.MovieRecommenderService;
import app.cinematch.util.JsonStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ViewingToolsTest {

    @Test
    @DisplayName("pickNextToWatch - random + description")
    void givenWishlist_whenPickRandomWithDescription_thenReturnsTitleAndDesc() {
        // GIVEN: wishlist non vide & service qui sait décrire
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            js.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(List.of("Alien"));

            MovieRecommenderService svc = mock(MovieRecommenderService.class);
            when(svc.generateDescription("Alien")).thenReturn("Desc Alien");

            ViewingTools tools = new ViewingTools(svc);

            // WHEN: on demande un choix random avec description
            String res = tools.pickNextToWatch("random", "true");

            // THEN: la réponse commence par NEXT:Alien et contient la description
            assertTrue(res.startsWith("NEXT:Alien"));
            assertTrue(res.contains("Desc Alien"));
        }
    }
}
