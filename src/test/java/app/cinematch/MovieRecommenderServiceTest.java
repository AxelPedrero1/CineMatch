package app.cinematch;

import app.cinematch.api.OllamaClient;
import app.cinematch.model.Recommendation;
import app.cinematch.util.JsonStorageMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de MovieRecommenderService.
 * Style BDD (Given / When / Then) avec commentaires explicatifs.
 */
public class MovieRecommenderServiceTest {

    // Méthode utilitaire pour instancier le service
    private MovieRecommenderService newService() {
        return new MovieRecommenderService("http://localhost:11434", "qwen2.5");
    }

    // Nettoyage entre les tests
    @AfterEach
    void tearDown() {
        OllamaClient.RESPONSES.clear();
        JsonStorageMock.reset();
    }

    // -------------------------------------------------------------------------
    // GIVEN / WHEN / THEN tests avec explications détaillées
    // -------------------------------------------------------------------------

    @Test
    void givenFullJson_whenRecommendFromLike_thenBuildsRecommendationWithYearAndSuffix() {
        // GIVEN  l’IA renvoie un JSON complet avec tous les champs.
        OllamaClient.RESPONSES.add("""
            {
              "title":"Inception",
              "pitch":"Un casse onirique audacieux",
              "year":"2010",
              "platform":"Netflix"
            }
            """);
        MovieRecommenderService service = newService();

        // WHEN  on demande une recommandation à partir du film "Interstellar".
        Recommendation result = service.recommendFromLike("Interstellar");

        // THEN  le service construit une recommandation complète :
        // titre, pitch, année, suffixe ajouté, plateforme.
        assertEquals("Inception", result.title());
        assertTrue(result.reason().contains("Un casse onirique audacieux"));
        assertTrue(result.reason().contains("année suggérée : 2010"));
        assertTrue(result.reason().contains("Inspiré de Interstellar"));
        assertEquals("Netflix", result.platform());
        assertNull(result.posterUrl());
    }

    @Test
    void givenPitchAlreadyMentionsLikedTitle_whenRecommendFromLike_thenNoSuffixIsAdded() {
        // GIVEN  le pitch contient déjà le titre du film aimé,
        // donc la méthode ne doit pas ajouter le suffixe.
        OllamaClient.RESPONSES.add("""
            {
              "title":"Blade Runner",
              "pitch":"Hommage appuyé à Interstellar dans ses thèmes.",
              "year":"1982",
              "platform":"Club Cinéphile"
            }
            """);
        MovieRecommenderService service = newService();

        // WHEN  on appelle recommendFromLike("Interstellar").
        Recommendation result = service.recommendFromLike("Interstellar");

        // THEN  le pitch d’origine est conservé, sans suffixe ajouté.
        assertTrue(result.reason().startsWith("Hommage appuyé à Interstellar"));
        assertTrue(result.reason().contains("année suggérée : 1982"));
        assertEquals("Blade Runner", result.title());
        assertEquals("Club Cinéphile", result.platform());
    }

    @Test
    void givenNoJson_whenRecommendRandom_thenUsesFallbacks() {
        // GIVEN  la réponse n’est pas un JSON, juste une ligne de texte.
        OllamaClient.RESPONSES.add("  - 🎥 The Matrix\nDu texte en plus\n");
        MovieRecommenderService service = newService();

        // WHEN  on appelle recommendRandom().
        Recommendation result = service.recommendRandom();

        // THEN  le titre est extrait de la première ligne lisible
        // et le reste utilise les valeurs par défaut.
        assertEquals("🎥 The Matrix", result.title());
        assertEquals("Suggestion IA", result.reason());
        assertTrue(Set.of("Cinéma du Coin+", "StreamFiction", "Club Cinéphile", "Festival Replay")
                .contains(result.platform()));
    }

    @Test
    void givenPartialJson_whenRecommendRandom_thenUsesFallbackTitleAndPlatform() {
        // GIVEN  JSON partiel avec champs vides.
        OllamaClient.RESPONSES.add("""
            Intro de l'IA
            {"title":"   ","pitch":"Pitch présent","year":"  ","platform":"   "}
            Ligne finale
            """);
        MovieRecommenderService service = newService();

        // WHEN  on appelle recommendRandom().
        Recommendation result = service.recommendRandom();

        // THEN  le titre provient de la première ligne non vide,
        // le pitch est pris du JSON, et la plateforme est choisie aléatoirement.
        assertEquals("Intro de l'IA", result.title());
        assertEquals("Pitch présent", result.reason());
        assertTrue(Set.of("Cinéma du Coin+", "StreamFiction", "Club Cinéphile", "Festival Replay")
                .contains(result.platform()));
    }

    @Test
    void givenInvalidJsonStructure_whenRecommendRandom_thenCatchesParseError() {
        // GIVEN  JSON malformé (branche catch du parse()).
        OllamaClient.RESPONSES.add("intro { \"title\": OOPS, \"pitch\": , } fin");
        MovieRecommenderService service = newService();

        // WHEN  on appelle recommendRandom().
        Recommendation result = service.recommendRandom();

        // THEN  le service gère l’erreur et utilise les valeurs fallback.
        assertEquals("intro { \"title\": OOPS, \"pitch\": , } fin", result.title());
        assertEquals("Suggestion IA", result.reason());
        assertTrue(Set.of("Cinéma du Coin+", "StreamFiction", "Club Cinéphile", "Festival Replay")
                .contains(result.platform()));
    }

    @Test
    void givenEmptyResponse_whenRecommendRandom_thenUsesDoubleFallback() {
        // GIVEN  la réponse de l’IA est vide.
        OllamaClient.RESPONSES.add("");
        MovieRecommenderService service = newService();

        // WHEN  recommendRandom() est appelé.
        Recommendation result = service.recommendRandom();

        // THEN  le titre devient "Suggestion mystère" et le pitch "Suggestion IA".
        assertEquals("Suggestion mystère", result.title());
        assertEquals("Suggestion IA", result.reason());
        assertTrue(Set.of("Cinéma du Coin+", "StreamFiction", "Club Cinéphile", "Festival Replay")
                .contains(result.platform()));
    }

    @Test
    void givenMovieTitle_whenGenerateDescription_thenReturnsOllamaResponse() {
        // GIVEN  l’IA renvoie une courte description.
        OllamaClient.RESPONSES.add("Une description immersive.");
        MovieRecommenderService service = newService();

        // WHEN  on appelle generateDescription().
        String description = service.generateDescription("Parasite");

        // THEN  la méthode retourne exactement le texte renvoyé par l’IA.
        assertEquals("Une description immersive.", description);
    }

    @Test
    void givenTitleAndStatus_whenMark_thenDelegatesToJsonStorage() {
        MovieRecommenderService service = new MovieRecommenderService(
                "http://localhost:11434", "qwen2.5",
                app.cinematch.util.JsonStorageMock::addOrUpdate // ✅ injection du mock
        );

        service.mark("Inception", "liked");

        assertEquals("Inception", app.cinematch.util.JsonStorageMock.lastTitle);
        assertEquals("liked", app.cinematch.util.JsonStorageMock.lastStatus);
    }


    @Test
    void givenAllNullOrBlankValues_whenFirstNonBlank_thenReturnsEmptyString() throws Exception {
        // GIVEN  tous les paramètres sont nuls ou vides.
        MovieRecommenderService service = newService();
        Method m = MovieRecommenderService.class.getDeclaredMethod("firstNonBlank", String[].class);
        m.setAccessible(true);

        // WHEN  on appelle la méthode privée via réflexion.
        Object result = m.invoke(service, (Object) new String[]{null, "   ", "\t"});

        // THEN  la méthode retourne une chaîne vide "".
        assertEquals("", result);
    }

    @Test
    void givenNullRaw_whenExtractFirstMeaningfulLine_thenReturnsEmptyString() throws Exception {
        // GIVEN  on veut tester la branche où raw == null.
        MovieRecommenderService service = newService();
        Method m = MovieRecommenderService.class.getDeclaredMethod("extractFirstMeaningfulLine", String.class);
        m.setAccessible(true);

        // WHEN  on invoque la méthode avec null.
        Object result = m.invoke(service, (Object) null);

        // THEN  la méthode retourne une chaîne vide (aucune ligne exploitable).
        assertEquals("", result);
    }
}
