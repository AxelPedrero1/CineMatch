package app.cinematch.api;

import java.util.ArrayDeque;

/** Test stub qui remplace la vraie implémentation pendant les tests. */
public class OllamaClient {

    // File de réponses simulées pour les appels chat(system,user)
    public static final ArrayDeque<String> RESPONSES = new ArrayDeque<>();

    public OllamaClient(String baseUrl, String model) {
        // no-op pour les tests
    }

    public String chat(String system, String user) {
        // Si rien n'a été poussé, on renvoie chaîne vide (cas "blank")
        return RESPONSES.isEmpty() ? "" : RESPONSES.removeFirst();
    }

    /** Utilitaire pour réinitialiser entre tests. */
    public static void reset() {
        RESPONSES.clear();
    }
}
