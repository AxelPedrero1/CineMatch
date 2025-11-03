package app.cinematch.agent.tools;

import app.cinematch.util.JsonStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests complets (GIVEN / WHEN / THEN) pour couvrir 100% de MaintenanceTools.
 */
class MaintenanceToolsTest {

    // --- utilitaire pour appeler les helpers privés/static de MaintenanceTools via réflexion ---
    private static Object callPrivate(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = MaintenanceTools.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    // ------------------------------------------------------------------------------------
    // TES TESTS D'ORIGINE + commentaires GIVEN/WHEN/THEN
    // ------------------------------------------------------------------------------------

    @Test
    @DisplayName("pruneBlanksInStatus - supprime entrées vides/invalides")
    void givenBlanks_whenPruneBlanksInStatus_thenMarkedNope() {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN: une liste 'envie' qui contient des entrées vides/quotes-only
            js.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(List.of("", "  ", "\"\"", "Alien"));

            MaintenanceTools tools = new MaintenanceTools();

            // WHEN: on lance le pruning des entrées vides
            String res = tools.pruneBlanksInStatus("envie");

            // THEN: les entrées vides sont marquées 'pas_interesse' et le message commence par PRUNED
            js.verify(() -> JsonStorage.addOrUpdate("", "pas_interesse"), atLeast(1));
            assertTrue(res.startsWith("PRUNED:"));
        }
    }

    @Test
    @DisplayName("renameTitle - copie le statut et marque l'ancien en 'pas_interesse'")
    void givenOldAndNew_whenRenameTitle_thenCopiesStatusAndMarksOldNope() {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN: "Le Samouraï" est présent en 'envie'
            js.when(() -> JsonStorage.getByStatus("envie")).thenReturn(List.of("Le Samouraï"));
            js.when(() -> JsonStorage.getByStatus("pas_interesse")).thenReturn(List.of());
            js.when(() -> JsonStorage.getByStatus("deja_vu")).thenReturn(List.of());

            MaintenanceTools tools = new MaintenanceTools();

            // WHEN: on renomme "Le Samourai" -> "Le Samouraï"
            String res = tools.renameTitle("Le Samourai", "Le Samouraï");

            // THEN: le nouveau garde 'envie' et l'ancien passe en 'pas_interesse'
            js.verify(() -> JsonStorage.addOrUpdate("Le Samouraï", "envie"));
            js.verify(() -> JsonStorage.addOrUpdate("Le Samourai", "pas_interesse"));
            assertTrue(res.contains("RENAMED:"));
        }
    }

    @Test
    @DisplayName("getListByStatusSorted - tri ascendant")
    void givenList_whenGetSortedAsc_thenSorted() {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN: une liste 'envie' non triée
            js.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(List.of("Heat", "Alien", "Drive"));

            MaintenanceTools tools = new MaintenanceTools();

            // WHEN: on récupère la liste triée en ascendant
            var res = tools.getListByStatusSorted("envie", "asc");

            // THEN: l’ordre est A -> Z
            assertEquals(List.of("Alien", "Drive", "Heat"), res);
        }
    }

    @Test
    @DisplayName("getStats - comptes par statut")
    void whenGetStats_thenCountsReturned() {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN: 1 envie, 2 pas_interesse, 0 deja_vu
            js.when(() -> JsonStorage.getByStatus("envie")).thenReturn(List.of("A"));
            js.when(() -> JsonStorage.getByStatus("pas_interesse")).thenReturn(List.of("B","C"));
            js.when(() -> JsonStorage.getByStatus("deja_vu")).thenReturn(List.of());

            MaintenanceTools tools = new MaintenanceTools();

            // WHEN: on calcule les stats
            String res = tools.getStats("all");

            // THEN: le message récap est exact
            assertEquals("STATS: total=3 | envie=1 | pas_interesse=2 | deja_vu=0", res);
        }
    }

    // ------------------------------------------------------------------------------------
    // TESTS AJOUTÉS POUR COUVRIR TOUTES LES BRANCHES / HELPERS
    // ------------------------------------------------------------------------------------

    // ===== clearStatus (HARD) =====

    @Test
    @DisplayName("clearStatus: statut invalide -> message d'erreur amical")
    void givenBadStatus_whenClearHard_thenFriendlyError() {
        // GIVEN: un statut inconnu
        MaintenanceTools tools = new MaintenanceTools();

        // WHEN: on tente un clear hard
        String res = tools.clearStatus("inconnu", "hard");

        // THEN: on obtient un message d’erreur explicite
        assertEquals("Statut invalide. Utilisez « envie », « deja_vu » ou « pas_interesse ».", res);
    }

    @Test
    @DisplayName("clearStatus HARD: 0 élément supprimé -> 'Aucun film à supprimer…'")
    void givenHardZero_whenClear_thenNone() {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN: removeAllByStatus('pas_interesse') renvoie 0
            js.when(() -> JsonStorage.removeAllByStatus("pas_interesse")).thenReturn(0);
            MaintenanceTools tools = new MaintenanceTools();

            // WHEN: clear hard sur 'pas_interesse'
            String res = tools.clearStatus("pas_interesse", "hard");

            // THEN: message 'Aucun film à supprimer …'
            assertEquals("Aucun film à supprimer dans « pas intéressé ».", res);
        }
    }

    @Test
    @DisplayName("clearStatus HARD: 1 élément supprimé -> singulier")
    void givenHardOne_whenClear_thenSingular() {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN: removeAllByStatus('envie') renvoie 1
            js.when(() -> JsonStorage.removeAllByStatus("envie")).thenReturn(1);
            MaintenanceTools tools = new MaintenanceTools();

            // WHEN: clear hard sur 'envie'
            String res = tools.clearStatus("envie", "hard");

            // THEN: message avec le singulier
            assertEquals("Supprimé 1 film de « liste d’envie ».", res);
        }
    }

    @Test
    @DisplayName("clearStatus HARD: plusieurs éléments supprimés -> pluriel")
    void givenHardMany_whenClear_thenPlural() {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN: removeAllByStatus('deja_vu') renvoie 3
            js.when(() -> JsonStorage.removeAllByStatus("deja_vu")).thenReturn(3);
            MaintenanceTools tools = new MaintenanceTools();

            // WHEN: clear hard sur 'deja_vu'
            String res = tools.clearStatus("deja_vu", "hard");

            // THEN: message avec le pluriel
            assertEquals("Supprimé 3 films de « déjà vu ».", res);
        }
    }

    // ===== clearStatus (SOFT) =====

    @Test
    @DisplayName("clearStatus SOFT: liste vide -> 'Aucun film à déplacer…'")
    void givenSoftEmpty_whenClear_thenNoMove() {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN: la liste 'envie' est vide
            js.when(() -> JsonStorage.getByStatus("envie")).thenReturn(List.of());
            MaintenanceTools tools = new MaintenanceTools();

            // WHEN: clear soft sur 'envie'
            String res = tools.clearStatus("envie", "soft");

            // THEN: message “aucun film à déplacer…”
            assertEquals("Aucun film à déplacer depuis « liste d’envie ».", res);
        }
    }

    @Test
    @DisplayName("clearStatus SOFT: 'envie' -> déplacement vers 'pas intéressé' (pluriel)")
    void givenSoftEnvie_whenClear_thenMovedToNope() {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN: 2 titres valides + 2 entrées vides dans 'envie'
            js.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(List.of("", "Alien", "  ", "Drive"));
            MaintenanceTools tools = new MaintenanceTools();

            // WHEN: clear soft sur 'envie'
            String res = tools.clearStatus("envie", "soft");

            // THEN: seuls les titres non vides sont déplacés vers 'pas_interesse'
            js.verify(() -> JsonStorage.addOrUpdate("Alien", "pas_interesse"));
            js.verify(() -> JsonStorage.addOrUpdate("Drive", "pas_interesse"));
            assertEquals("Déplacé 2 films de « liste d’envie » vers « pas intéressé ».", res);
        }
    }

    @Test
    @DisplayName("clearStatus SOFT: 'pas_interesse' -> déplacement vers 'déjà vu' (singulier)")
    void givenSoftNope_whenClear_thenMovedToSeen() {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN: 1 seul titre dans 'pas_interesse'
            js.when(() -> JsonStorage.getByStatus("pas_interesse"))
                    .thenReturn(List.of("Jojo Rabbit"));
            MaintenanceTools tools = new MaintenanceTools();

            // WHEN: clear soft sur 'pas_interesse'
            String res = tools.clearStatus("pas_interesse", "soft");

            // THEN: déplacement vers 'deja_vu' + libellé au singulier
            js.verify(() -> JsonStorage.addOrUpdate("Jojo Rabbit", "deja_vu"));
            assertEquals("Déplacé 1 film de « pas intéressé » vers « déjà vu ».", res);
        }
    }

    // ===== getListByStatusSorted (DESC) =====

    @Test
    @DisplayName("getListByStatusSorted: tri descendant Z->A")
    void givenList_whenGetSortedDesc_thenSorted() {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN: une liste 'envie' non triée
            js.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(List.of("Heat", "Alien", "Drive"));
            MaintenanceTools tools = new MaintenanceTools();

            // WHEN: tri desc
            var res = tools.getListByStatusSorted("envie", "desc");

            // THEN: ordre Z -> A
            assertEquals(List.of("Heat", "Drive", "Alien"), res);
        }
    }

    // ===== Helpers privés via réflexion =====

    @Test
    @DisplayName("normalizeStatus: mapping accents/variantes + inconnu -> null")
    void normalizeStatus_mapping_and_unknown() throws Exception {
        // GIVEN: différentes variantes d'écriture et un statut inconnu

        // WHEN: normalisation des statuts
        String s1 = (String) callPrivate("normalizeStatus", new Class[]{String.class}, "pas intéressé");
        String s2 = (String) callPrivate("normalizeStatus", new Class[]{String.class}, "déjà-vu");
        String s3 = (String) callPrivate("normalizeStatus", new Class[]{String.class}, "Envie");
        String s4 = (String) callPrivate("normalizeStatus", new Class[]{String.class}, "foobar");

        // THEN: mapping attendu + null pour l’inconnu
        assertEquals("pas_interesse", s1);
        assertEquals("deja_vu", s2);
        assertEquals("envie", s3);
        assertNull(s4);
    }

    @Test
    @DisplayName("labelStatus: libellés FR attendus")
    void labelStatus_ok() throws Exception {
        // GIVEN / WHEN: on labellise chaque statut

        // THEN: libellés FR corrects (et passthrough pour valeur « autre »)
        assertEquals("liste d’envie", callPrivate("labelStatus", new Class[]{String.class}, "envie"));
        assertEquals("pas intéressé", callPrivate("labelStatus", new Class[]{String.class}, "pas_interesse"));
        assertEquals("déjà vu",       callPrivate("labelStatus", new Class[]{String.class}, "deja_vu"));
        assertEquals("autre",         callPrivate("labelStatus", new Class[]{String.class}, "autre"));
    }

    @Test
    @DisplayName("hasRemoveMethod: retourne false (pas de JsonStorage.remove(String))")
    void hasRemoveMethod_false() throws Exception {
        // GIVEN: la classe JsonStorage ne déclare pas remove(String)

        // WHEN: on interroge le helper
        Object res = callPrivate("hasRemoveMethod", new Class<?>[]{});
        assertEquals("pas_interesse",
                callPrivate("normalizeStatus", new Class<?>[]{String.class}, "pas intéressé"));
        assertEquals("liste d’envie",
                callPrivate("labelStatus", new Class<?>[]{String.class}, "envie"));
        callPrivate("norm", new Class<?>[]{ String.class }, "\"  Alien  \"");
        Object aa = callPrivate("normStatus", new Class<?>[]{String.class}, "deja_vu");
        assertEquals("envie",
                callPrivate("findStatusIgnoreCase", new Class<?>[]{String.class}, "alien"));
    }

    @Test
    @DisplayName("norm: retire guillemets/espaces doublons")
    void norm_ok() throws Exception {
        // GIVEN: une chaîne avec guillemets et espaces
        String out = (String) callPrivate(
                "norm",
                new Class<?>[]{ String.class },
                "\"  Alien  \""    // <-- l’argument attendu par norm(String)
        );


        // THEN: quotes virées + trim + réduction des espaces
        assertEquals("Alien", out);
    }

    @Test
    @DisplayName("normStatus: valeur connue sinon 'envie' par défaut")
    void normStatus_ok() throws Exception {
        // GIVEN / WHEN: normalisation d’un statut connu et d’un inconnu
        Object a = callPrivate("normStatus", new Class[]{String.class}, "deja_vu");
        Object b = callPrivate("normStatus", new Class[]{String.class}, "unknown");

        // THEN: on obtient la valeur ou le fallback 'envie'
        assertEquals("deja_vu", a);
        assertEquals("envie", b);
    }

    @Test
    @DisplayName("findStatusIgnoreCase: parcourt les 3 listes et peut ne rien trouver")
    void findStatusIgnoreCase_ok() throws Exception {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN: Alien en 'envie', Matrix en 'pas_interesse', Drive en 'deja_vu'
            js.when(() -> JsonStorage.getByStatus("envie")).thenReturn(List.of("Alien"));
            js.when(() -> JsonStorage.getByStatus("pas_interesse")).thenReturn(List.of("Matrix"));
            js.when(() -> JsonStorage.getByStatus("deja_vu")).thenReturn(List.of("Drive"));

            // WHEN / THEN: recherche insensible à la casse + cas non trouvé
            assertEquals("envie",
                    callPrivate("findStatusIgnoreCase", new Class[]{String.class}, "alien"));
            assertEquals("deja_vu",
                    callPrivate("findStatusIgnoreCase", new Class[]{String.class}, "Drive"));
            assertNull(
                    callPrivate("findStatusIgnoreCase", new Class[]{String.class}, "Heat"));
        }
    }

    @Test
    @DisplayName("renameTitle - retourne ERROR:EMPTY_TITLE si ancien ou nouveau titre est vide")
    void givenBlank_whenRenameTitle_thenErrorEmptyTitle() {
        // GIVEN: instance
        MaintenanceTools tools = new MaintenanceTools();
        // WHEN
        String r1 = tools.renameTitle("", "Alien");
        String r2 = tools.renameTitle("Alien", "   ");
        // THEN
        assertEquals("ERROR:EMPTY_TITLE", r1);
        assertEquals("ERROR:EMPTY_TITLE", r2);
    }

    @Test
    @DisplayName("clearStatus SOFT: 'deja_vu' -> déplacement vers 'pas intéressé'")
    void givenSoftSeen_whenClear_thenMovedToNope() {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN (G): 2 titres en 'deja_vu'
            js.when(() -> JsonStorage.getByStatus("deja_vu"))
                    .thenReturn(List.of("Blade Runner 2049", "Parasite"));

            MaintenanceTools tools = new MaintenanceTools();

            // WHEN (W): clear soft sur 'deja_vu'
            String res = tools.clearStatus("deja_vu", "soft");

            // THEN (T): déplacés vers 'pas_interesse' + message pluriel correct
            js.verify(() -> JsonStorage.addOrUpdate("Blade Runner 2049", "pas_interesse"));
            js.verify(() -> JsonStorage.addOrUpdate("Parasite", "pas_interesse"));
            assertEquals("Déplacé 2 films de « déjà vu » vers « pas intéressé ».", res);
        }
    }

    @Test
    @DisplayName("getListByStatusSorted - ignore les chaînes vides avant tri")
    void givenBlanks_whenGetSortedAsc_thenBlanksFiltered() {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN (G): la liste contient des vides qui doivent être filtrés
            js.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(List.of("", "  ", "Alien", "Drive"));

            MaintenanceTools tools = new MaintenanceTools();

            // WHEN (W): tri ascendant
            var res = tools.getListByStatusSorted("envie", "asc");

            // THEN (T): seuls les titres non vides restent, triés A->Z
            assertEquals(List.of("Alien", "Drive"), res);
        }
    }

    @Test
    @DisplayName("pruneBlanksInStatus - fonctionne aussi avec statut normalisé (ex: 'Déjà-vu')")
    void givenAccentedStatus_whenPrune_thenWorksOnNormalizedBucket() {
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            // GIVEN (G): bucket 'deja_vu' (status demandé 'Déjà-vu')
            js.when(() -> JsonStorage.getByStatus("deja_vu"))
                    .thenReturn(List.of("\"\"", "  ", "Heat"));

            MaintenanceTools tools = new MaintenanceTools();

            // WHEN (W): prune sur 'Déjà-vu' (accents/variantes)
            String res = tools.pruneBlanksInStatus("Déjà-vu");

            // THEN (T): vides marqués, message PRUNED
            js.verify(() -> JsonStorage.addOrUpdate("", "pas_interesse"), atLeast(1));
            assertTrue(res.startsWith("PRUNED:"));
        }
    }

}
