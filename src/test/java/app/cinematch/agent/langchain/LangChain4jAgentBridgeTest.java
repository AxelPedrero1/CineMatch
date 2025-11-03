package app.cinematch.agent.langchain;

import app.cinematch.MovieRecommenderService;
import app.cinematch.agent.Profile;
import app.cinematch.agent.tools.BulkTools;
import app.cinematch.agent.tools.MaintenanceTools;
import app.cinematch.agent.tools.WishlistTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests LangChain4jAgentBridge en style GIVEN / WHEN / THEN
 * On vérifie le pré-parseur client (add en lot), le clear-all tolérant,
 * les actions simples (dislike/seen/add/remove) et les helpers privés.
 */
class LangChain4jAgentBridgeTest {

    /* ---------- Utils réflexion ---------- */

    /** Set un champ privé (permet d’injecter des mocks). */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /** Appel d’une méthode privée (instance). */
    private static Object callPrivate(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    /** Appel d’une méthode privée statique. */
    private static Object callPrivateStatic(Class<?> cls, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = cls.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    /* ====================================================================== */
    /* ask(...) — déclenchement du pré-parseur (cas FR avec virgules)         */
    /* ====================================================================== */
    @Test
    @DisplayName("ask: 'Ajoute A, B, C à ma wishlist' -> addManyToWishlist + message '(3).'")
    void givenBulkAddSentence_whenAsk_thenClientSideBulkIsTriggered() throws Exception {
        // GIVEN: bridge réel, BulkTools espionné                                      // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        BulkTools spyBulk = spy(new BulkTools());
        setPrivateField(bridge, "bulkTools", spyBulk);

        // WHEN: message utilisateur avec plusieurs titres                            // WHEN
        String out = bridge.ask("Ajoute Alien, Heat, Drive à ma wishlist");

        // THEN: le pré-parseur court-circuite l'appel LLM                             // THEN
        verify(spyBulk, times(1)).addManyToWishlist("Alien, Heat, Drive");
        assertTrue(out.contains("(3)."));
        assertTrue(out.toLowerCase(Locale.ROOT).contains("ajout"));
    }

    /* ====================================================================== */
    /* ask(...) — pré-parseur avec guillemets/retours ligne                   */
    /* ====================================================================== */
    @Test
    @DisplayName("ask: titres sur plusieurs lignes + guillemets -> '(3).'")
    void givenBulkAddWithQuotesAndNewlines_whenAsk_thenClientSideBulkWorks() throws Exception {
        // GIVEN                                                                     // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        BulkTools spyBulk = spy(new BulkTools());
        setPrivateField(bridge, "bulkTools", spyBulk);

        String prompt = "Mets \"Alien\",\n«Heat», Drive dans la liste d'envie";

        // WHEN                                                                       // WHEN
        String out = bridge.ask(prompt);

        // THEN                                                                       // THEN
        verify(spyBulk, times(1)).addManyToWishlist(anyString());
        assertTrue(out.contains("(3)."));
    }

    /* ====================================================================== */
    /* tryClientSideBulkAdd(...) — pas de séparateurs -> null                 */
    /* ====================================================================== */
    @Test
    @DisplayName("tryClientSideBulkAdd: pas de virgule/\\n -> null")
    void givenNoCommaNorNewline_whenTryClientSideBulkAdd_thenNull() throws Exception {
        // GIVEN                                                                     // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        // WHEN                                                                       // WHEN
        Object res = callPrivate(bridge, "tryClientSideBulkAdd",
                new Class[]{String.class}, "Ajoute Alien à ma wishlist");

        // THEN                                                                       // THEN
        assertNull(res);
    }

    /* ====================================================================== */
    /* tryClientSideBulkAdd(...) — titres vides -> null                       */
    /* ====================================================================== */
    @Test
    @DisplayName("tryClientSideBulkAdd: 'Ajoute   à ma wishlist' -> null (vide)")
    void givenNoTitles_whenTryClientSideBulkAdd_thenNull() throws Exception {
        // GIVEN                                                                     // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        // WHEN                                                                       // WHEN
        Object res = callPrivate(bridge, "tryClientSideBulkAdd",
                new Class[]{String.class}, "Ajoute   à ma wishlist");

        // THEN                                                                       // THEN
        assertNull(res);
    }

    /* ====================================================================== */
    /* tryClientSideBulkAdd(...) — anglais                                     */
    /* ====================================================================== */
    @Test
    @DisplayName("tryClientSideBulkAdd: 'Add A, B to wishlist' -> appel + '(2).'")
    void givenEnglishAdd_whenTryClientSideBulkAdd_thenOk() throws Exception {
        // GIVEN                                                                     // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        BulkTools spyBulk = spy(new BulkTools());
        setPrivateField(bridge, "bulkTools", spyBulk);

        // WHEN                                                                       // WHEN
        String out = (String) callPrivate(bridge, "tryClientSideBulkAdd",
                new Class[]{String.class}, "Add Alien, Heat to wishlist");

        // THEN                                                                       // THEN
        verify(spyBulk, times(1)).addManyToWishlist("Alien, Heat");
        assertTrue(out.endsWith("(2)."));
    }

    /* ====================================================================== */
    /* ask(...) — CLEAR ALL tolérant (mapStatusFromTail)                      */
    /* ====================================================================== */
    @Test
    @DisplayName("ask: 'supprime tout dans pas intéressés' -> maintenanceTools.clearStatus('pas_interesse','hard')")
    void givenClearAllPasInteresse_whenAsk_thenMaintenanceCalled() throws Exception {
        // GIVEN                                                                     // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        MaintenanceTools mockMaint = mock(MaintenanceTools.class);
        when(mockMaint.clearStatus("pas_interesse", "hard"))
                .thenReturn("OK: cleared pas_interesse");
        setPrivateField(bridge, "maintenanceTools", mockMaint);

        // WHEN                                                                       // WHEN
        String out = bridge.ask("supprime tout dans pas intéressés");

        // THEN                                                                       // THEN
        assertEquals("OK: cleared pas_interesse", out);
        verify(mockMaint).clearStatus("pas_interesse", "hard");
    }

    @Test
    @DisplayName("ask: CLEAR ALL avec statut inconnu -> message d’erreur")
    void givenClearAllUnknown_whenAsk_thenFriendlyError() throws Exception {
        // GIVEN                                                                     // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        // WHEN                                                                       // WHEN
        String out = bridge.ask("efface tous les films dans demain");

        // THEN                                                                       // THEN
        assertTrue(out.toLowerCase(Locale.ROOT).contains("statut non reconnu"));
    }

    /* ====================================================================== */
    /* ask(...) — actions simples via tryDirectSingleAction                    */
    /* ====================================================================== */
    @Test
    @DisplayName("ask: 'je ne suis pas intéressé par X' -> markAsDisliked(X)")
    void givenNotInterestedSentence_whenAsk_thenDisliked() throws Exception {
        // GIVEN                                                                     // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        WishlistTools wl = mock(WishlistTools.class);
        when(wl.markAsDisliked("Matrix")).thenReturn("DISLIKED:Matrix");
        setPrivateField(bridge, "wishlistTools", wl);

        // WHEN                                                                       // WHEN
        String out = bridge.ask("Je ne suis pas intéressé par Matrix");

        // THEN                                                                       // THEN
        verify(wl).markAsDisliked("Matrix");
        assertTrue(out.contains("Matrix"));
    }

    @Test
    @DisplayName("ask: 'j’ai vu Drive' -> markAsSeen(Drive)")
    void givenSeenSentence_whenAsk_thenSeen() throws Exception {
        // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        WishlistTools wl = mock(WishlistTools.class);
        when(wl.markAsSeen("Drive")).thenReturn("SEEN:Drive");
        setPrivateField(bridge, "wishlistTools", wl);

        // WHEN
        String out = bridge.ask("J’ai vu Drive");

        // THEN
        verify(wl).markAsSeen("Drive");
        assertTrue(out.contains("deja_vu") || out.contains("déjà"));
    }

    @Test
    @DisplayName("ask: 'ajoute Alien' -> addToWishlist(Alien)")
    void givenAddSingle_whenAsk_thenAdd() throws Exception {
        // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        WishlistTools wl = mock(WishlistTools.class);
        when(wl.addToWishlist("Alien")).thenReturn("ADDED:Alien");
        setPrivateField(bridge, "wishlistTools", wl);

        // WHEN
        String out = bridge.ask("Ajoute Alien");

        // THEN
        verify(wl).addToWishlist("Alien");
        assertTrue(out.toLowerCase(Locale.ROOT).contains("ajout"));
    }

    @Test
    @DisplayName("ask: 'supprime Dune de ma liste' -> removeFromWishlist(Dune)")
    void givenRemoveWithListPhrase_whenAsk_thenRemove() throws Exception {
        // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        WishlistTools wl = mock(WishlistTools.class);
        when(wl.removeFromWishlist("Dune")).thenReturn("REMOVED:Dune");
        setPrivateField(bridge, "wishlistTools", wl);

        // WHEN
        String out = bridge.ask("Supprime Dune de ma liste");

        // THEN
        verify(wl).removeFromWishlist("Dune");
        assertTrue(out.toLowerCase(Locale.ROOT).contains("retiré"));
    }

    @Test
    @DisplayName("ask: 'supprime Dune' (sans 'de ma liste') -> removeFromWishlist(Dune)")
    void givenRemoveSolo_whenAsk_thenRemove() throws Exception {
        // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        WishlistTools wl = mock(WishlistTools.class);
        when(wl.removeFromWishlist("Dune")).thenReturn("REMOVED:Dune");
        setPrivateField(bridge, "wishlistTools", wl);

        // WHEN
        String out = bridge.ask("Supprime Dune");

        // THEN
        verify(wl).removeFromWishlist("Dune");
        assertTrue(out.toLowerCase(Locale.ROOT).contains("retiré"));
    }

    /* ====================================================================== */
    /* Helpers privés : unquote, looksLikeMulti, extractTitlesForBulk, map…   */
    /* ====================================================================== */
    @Test
    @DisplayName("unquote: retire guillemets FR/EN et trim")
    void unquote_ok() throws Exception {
        assertEquals("Heat",
                callPrivateStatic(LangChain4jAgentBridge.class, "unquote",
                        new Class[]{String.class}, " «Heat» "));
    }

    @Test
    @DisplayName("looksLikeMulti: 'ajoute A et supprime B' -> true ; 'ajoute A' -> false")
    void looksLikeMulti_ok() throws Exception {
        assertTrue((Boolean) callPrivateStatic(LangChain4jAgentBridge.class, "looksLikeMulti",
                new Class[]{String.class}, "ajoute Alien et supprime Dune"));
        assertFalse((Boolean) callPrivateStatic(LangChain4jAgentBridge.class, "looksLikeMulti",
                new Class[]{String.class}, "ajoute Alien"));
    }

    @Test
    @DisplayName("extractTitlesForBulk: enlève verbe et queue '... à ma wishlist'")
    void extractTitlesForBulk_ok() throws Exception {
        assertEquals("Alien, Heat",
                callPrivateStatic(LangChain4jAgentBridge.class, "extractTitlesForBulk",
                        new Class[]{String.class}, "Ajoute Alien, Heat à ma wishlist"));
    }

    @Test
    @DisplayName("mapStatusFromTail: envie / déjà vu / pas intéressé (FR/EN, accents)")
    void mapStatusFromTail_ok() throws Exception {
        assertEquals("envie",
                callPrivateStatic(LangChain4jAgentBridge.class, "mapStatusFromTail",
                        new Class[]{String.class}, "dans la wishlist"));
        assertEquals("deja_vu",
                callPrivateStatic(LangChain4jAgentBridge.class, "mapStatusFromTail",
                        new Class[]{String.class}, "déjà-vu"));
        assertEquals("pas_interesse",
                callPrivateStatic(LangChain4jAgentBridge.class, "mapStatusFromTail",
                        new Class[]{String.class}, "not interested"));
    }

    @Test
    @DisplayName("ask: aucun pattern -> appel assistant.chat(...) (mocké)")
    void givenGenericMessage_whenAsk_thenAssistantChatIsUsed() throws Exception {
        // GIVEN: bridge + assistant mocké                                          // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        CineAssistant assistantMock = mock(CineAssistant.class);
        // on remplace le champ private final 'assistant'
        Field f = LangChain4jAgentBridge.class.getDeclaredField("assistant");
        f.setAccessible(true);
        f.set(bridge, assistantMock);
        when(assistantMock.chat("Bonjour l'agent")).thenReturn("SALUT!");

        // WHEN: un message qui ne déclenche aucun des pré-parsers                 // WHEN
        String out = bridge.ask("Bonjour l'agent");

        // THEN: on a bien délégué au mock                                         // THEN
        verify(assistantMock, times(1)).chat("Bonjour l'agent");
        assertEquals("SALUT!", out);
    }

    /* ====================================================================== */
    /* tryClientSideBulkAdd — branche 'Ajout effectué : ...'                   */
    /* ====================================================================== */
    @Test
    @DisplayName("tryClientSideBulkAdd: tool ne renvoie pas ADDED_MANY -> 'Ajout effectué : ...'")
    void givenToolReturnsOtherToken_whenBulkAdd_thenAjoutEffectue() throws Exception {
        // GIVEN                                                                  // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        BulkTools bulkMock = mock(BulkTools.class);
        setPrivateField(bridge, "bulkTools", bulkMock);
        when(bulkMock.addManyToWishlist("Alien, Heat")).thenReturn("OK"); // <- pas ADDED_MANY

        // WHEN                                                                   // WHEN
        String out = (String) callPrivate(bridge, "tryClientSideBulkAdd",
                new Class[]{String.class}, "Ajoute Alien, Heat à ma wishlist");

        // THEN                                                                   // THEN
        verify(bulkMock).addManyToWishlist("Alien, Heat");
        assertEquals("Ajout effectué : Alien, Heat.", out);
    }

    /* ====================================================================== */
    /* tryDirectSingleAction — cas ignoré 'supprime tout ...'                  */
    /* ====================================================================== */
    @Test
    @DisplayName("tryDirectSingleAction: 'supprime tout Dune' -> ignoré (null)")
    void givenRemoveSoloStartingWithTout_whenDirect_thenNull() throws Exception {
        // GIVEN                                                                  // GIVEN
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(
                "http://localhost:11434", "llama3.1:8b-instruct",
                Profile.defaultCinemaExpert(), svc);

        // WHEN: on appelle la méthode privée directement                          // WHEN
        Object res = callPrivate(bridge, "tryDirectSingleAction",
                new Class[]{String.class}, "supprime tout Dune");

        // THEN: pas d’action, renvoie null                                        // THEN
        assertNull(res);
    }

    /* ====================================================================== */
    /* mapStatusFromTail — variantes supplémentaires                           */
    /* ====================================================================== */
    @Test
    @DisplayName("mapStatusFromTail: 'dislike' et 'pas interesses' -> pas_interesse")
    void mapStatusFromTail_moreVariants() throws Exception {
        assertEquals("pas_interesse",
                callPrivateStatic(LangChain4jAgentBridge.class, "mapStatusFromTail",
                        new Class[]{String.class}, "users who DISLIKE these movies"));
        assertEquals("pas_interesse",
                callPrivateStatic(LangChain4jAgentBridge.class, "mapStatusFromTail",
                        new Class[]{String.class}, "vider les films pas interesses"));
    }

    /* ====================================================================== */
    /* unquote — branche null -> ""                                            */
    /* ====================================================================== */
    @Test
    @DisplayName("unquote: null -> chaîne vide")
    void unquote_null() throws Exception {
        assertEquals("",
                callPrivateStatic(LangChain4jAgentBridge.class, "unquote",
                        new Class[]{String.class}, new Object[]{null}));
    }
}
