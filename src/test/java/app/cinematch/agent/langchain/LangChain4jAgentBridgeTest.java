package app.cinematch.agent.langchain;

import app.cinematch.MovieRecommenderService;
import app.cinematch.agent.Profile;
import app.cinematch.agent.tools.BulkTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LangChain4jAgentBridgeTest {

    @Test
    @DisplayName("ask - pré-parseur: 'Ajoute A, B, C à ma wishlist' -> addManyToWishlist")
    void givenBulkAddSentence_whenAsk_thenClientSideBulkIsTriggered() throws Exception {
        // GIVEN: bridge réel mais BulkTools espionné (spy) pour vérifier l'appel
        String url = "http://localhost:11434";
        String model = "llama3.1:8b-instruct";
        MovieRecommenderService svc = Mockito.mock(MovieRecommenderService.class);

        LangChain4jAgentBridge bridge =
                new LangChain4jAgentBridge(url, model, Profile.defaultCinemaExpert(), svc);

        BulkTools spyBulk = Mockito.spy(new BulkTools());
        Field f = LangChain4jAgentBridge.class.getDeclaredField("bulkTools");
        f.setAccessible(true);
        f.set(bridge, spyBulk);

        // WHEN: message utilisateur avec plusieurs titres
        String out = bridge.ask("Ajoute Alien, Heat, Drive à ma wishlist");

        // THEN: le pré-parseur court-circuite l'appel LLM pour déclencher addManyToWishlist
        verify(spyBulk, times(1)).addManyToWishlist("Alien, Heat, Drive");
        assertTrue(out.toLowerCase().contains("ajout"));
    }
}
