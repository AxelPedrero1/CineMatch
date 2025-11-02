package app.cinematch.agent.tools;

import app.cinematch.util.JsonStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BulkToolsTest {

    @Test
    @DisplayName("addManyToWishlist - CSV & \\n -> ajoute tous les titres en 'envie'")
    void givenCsv_whenAddMany_thenAllEnvie() {
        // GIVEN: JsonStorage mocké & une liste de titres
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            BulkTools tools = new BulkTools();

            // WHEN: ajout multiple
            String res = tools.addManyToWishlist("Alien, Heat\nDrive");

            // THEN: chaque titre est marqué 'envie' et le résumé indique 3 ajouts
            js.verify(() -> JsonStorage.addOrUpdate("Alien", "envie"));
            js.verify(() -> JsonStorage.addOrUpdate("Heat", "envie"));
            js.verify(() -> JsonStorage.addOrUpdate("Drive", "envie"));
            assertEquals("ADDED_MANY:3", res);
        }
    }

    @Test
    @DisplayName("setManyStatus - CSV -> applique 'deja_vu' à chaque titre")
    void givenCsv_whenSetManyStatus_thenAllUpdated() {
        // GIVEN: JsonStorage mocké & des titres
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            BulkTools tools = new BulkTools();

            // WHEN: on applique le statut 'deja_vu' en lot
            String res = tools.setManyStatus("Alien,Heat", "deja_vu");

            // THEN: chaque titre est mis à jour & le retour résume l'opération
            js.verify(() -> JsonStorage.addOrUpdate("Alien", "deja_vu"));
            js.verify(() -> JsonStorage.addOrUpdate("Heat", "deja_vu"));
            assertEquals("STATUS_MANY:2->deja_vu", res);
        }
    }
}
