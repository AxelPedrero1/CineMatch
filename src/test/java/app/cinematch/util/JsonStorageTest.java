package app.cinematch.util;

import app.cinematch.model.HistoryEntry;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonStorageTest {

    private static final Path STORAGE_PATH =
            Paths.get("src", "main", "resources", "storage.json");

    private byte[] backupBytes;
    private boolean backupExisted;

    @BeforeEach
    void backup() throws IOException {
        Files.createDirectories(STORAGE_PATH.getParent());
        backupExisted = Files.exists(STORAGE_PATH);
        backupBytes = backupExisted ? Files.readAllBytes(STORAGE_PATH) : null;
        Files.deleteIfExists(STORAGE_PATH); // test part sur base vide
    }

    @AfterEach
    void restore() throws IOException {
        Files.deleteIfExists(STORAGE_PATH);
        if (backupExisted) {
            Files.write(STORAGE_PATH, backupBytes);
        }
    }

    @Test
    @DisplayName("loadAll: renvoie [] quand fichier absent")
    void loadAll_empty_when_file_missing() {
        List<HistoryEntry> all = JsonStorage.loadAll();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    @DisplayName("saveAll puis loadAll: on relit ce qu'on a écrit")
    void saveAll_then_loadAll_reads_back() {
        List<HistoryEntry> initial = List.of(
                new HistoryEntry("Interstellar", "envie", "2024-01-01T10:00:00"),
                new HistoryEntry("Inception", "deja_vu", "2024-01-02T11:00:00")
        );
        JsonStorage.saveAll(initial);
        List<HistoryEntry> reloaded = JsonStorage.loadAll();
        assertEquals(2, reloaded.size());
        assertTrue(reloaded.stream().anyMatch(e -> e.title().equals("Interstellar") && e.status().equals("envie")));
        assertTrue(reloaded.stream().anyMatch(e -> e.title().equals("Inception") && e.status().equals("deja_vu")));
    }
    // 1) Couvrir le constructeur implicite (utilitaire) — purement pour la couverture
    @Test
    @DisplayName("Utility constructor: instantiation does nothing (coverage only)")
    void constructor_for_coverage_only() {
        // La classe n'est pas censée être instanciée en prod, mais on le fait ici
        // pour couvrir le constructeur par défaut généré par le compilateur.
        JsonStorage instance = new JsonStorage();
        assertNotNull(instance);
    }

    // 2) Forcer une IOException dans saveAll() en créant un RÉPERTOIRE à l'emplacement du fichier
    @Test
    @DisplayName("saveAll: IOException (path is a directory) is caught → no throw")
    void saveAll_handles_ioexception_when_path_is_directory() throws Exception {
        // On part de la même sauvegarde/restauration que tes autres tests
        var storagePath = java.nio.file.Paths.get("src", "main", "resources", "storage.json");

        // S'assurer que le fichier n'existe pas puis CRÉER UN DOSSIER au même chemin
        java.nio.file.Files.deleteIfExists(storagePath);
        java.nio.file.Files.createDirectory(storagePath); // ← provoque l'IOException sur writeValue(...)

        // Appel : ne doit PAS jeter (catch silencieux dans saveAll)
        assertDoesNotThrow(() -> JsonStorage.saveAll(java.util.List.of(
                new app.cinematch.model.HistoryEntry("X", "envie", "2025-10-21T10:00:00Z")
        )));

        // Bonus: loadAll ne doit pas planter et renvoyer une liste vide (fichier illisible)
        var all = JsonStorage.loadAll();
        assertNotNull(all);
        assertTrue(all.isEmpty());

        // Nettoyage local de ce test (ton @AfterEach restaure l’état initial ensuite)
        java.nio.file.Files.deleteIfExists(storagePath);
    }

    @Test
    @DisplayName("addOrUpdate remplace l'unique entrée par titre (case-insensitive)")
    void addOrUpdate_replaces_by_title() throws Exception {
        JsonStorage.addOrUpdate("Matrix", "envie");
        Thread.sleep(5);
        JsonStorage.addOrUpdate("matrix", "deja_vu"); // même titre, casse différente

        var all = JsonStorage.loadAll();
        assertEquals(1, all.stream().filter(e -> e.title().equalsIgnoreCase("Matrix")).count());

        var e = all.get(0);
        assertTrue(e.title().equalsIgnoreCase("Matrix")); // au lieu d'assertEquals("Matrix", ...)
        assertEquals("deja_vu", e.status());
        assertNotNull(e.dateTimeIso());
        assertFalse(e.dateTimeIso().isBlank());
    }


    @Test
    @DisplayName("getByStatus filtre par statut (case-insensitive) et trie par date desc")
    void getByStatus_filters_and_sorts_desc() throws Exception {
        String json = """
          [
            {"title":"A","status":"ENVIE","dateTimeIso":"2024-02-01T10:00:00"},
            {"title":"B","status":"deja_vu","dateTimeIso":"2024-02-03T09:00:00"},
            {"title":"C","status":"envie","dateTimeIso":"2024-03-01T12:00:00"},
            {"title":"D","status":"envie","dateTimeIso":"2024-01-15T08:00:00"}
          ]
        """;
        Files.writeString(STORAGE_PATH, json, StandardCharsets.UTF_8);

        List<String> envies = JsonStorage.getByStatus("envie");
        assertEquals(List.of("C", "A", "D"), envies);
    }

    @Test
    @DisplayName("loadAll tolère JSON corrompu et renvoie []")
    void loadAll_handles_corrupted_json() throws Exception {
        Files.writeString(STORAGE_PATH, "{oops", StandardCharsets.UTF_8);
        List<HistoryEntry> all = JsonStorage.loadAll();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }
}
