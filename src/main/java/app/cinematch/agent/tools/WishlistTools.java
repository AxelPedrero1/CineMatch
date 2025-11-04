package app.cinematch.agent.tools;

import app.cinematch.util.JsonStorage;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.text.Normalizer;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Outils "liste d'envie" exposés au LLM.
 * Le LLM peut les appeler pour modifier/consulter le stockage JSON.
 */
public class WishlistTools {

    // Coupe “... dans/à ma wishlist / liste d’envie / liste”
    private static final Pattern TAIL_BUCKET = Pattern.compile(
            "(?iu)\\s+(?:dans|in|to|into|à|a)\\s*(?:ma|la|the|my)?\\s*(?:wish\\s*list|wishlist|liste(?:\\s*d['’]?envie)?|liste)\\s*$"
    );
    // Coupe “... en pas_interesse / en deja_vu / en envie / seen / not interested”
    private static final Pattern TAIL_STATUS = Pattern.compile(
            "(?iu)\\s+(?:en|as|to)\\s*(?:pas[_\\s-]*int[eé]ress[eé]?|pas_?interesse|not\\s*interested|"
                    + "deja[_\\s-]*vu|d[eé]j[aà][_\\s-]*vu|seen|envie)\\s*$"
    );

    @Tool("Ajoute un film à la liste d'envie (statut 'envie').")
    public String addToWishlist(@P("title") String title) {
        String cleaned = normalize(title);
        if (cleaned.isBlank()) return "ERROR:EMPTY_TITLE";

        // Fallback : si l'utilisateur a donné plusieurs titres (virgules / retours ligne)
        if (cleaned.contains(",") || cleaned.contains("\n")) {
            int n = 0;
            for (String part : cleaned.split("[,\n]")) {
                String t = normalize(part);
                if (!t.isBlank()) { JsonStorage.addOrUpdate(t, "envie"); n++; }
            }
            return "ADDED_MANY:" + n;
        }

        JsonStorage.addOrUpdate(cleaned, "envie");
        return "ADDED:" + cleaned;
    }


    @Tool("Retire un film de la liste d'envie en le marquant 'pas_interesse'.")
    public String removeFromWishlist(@P("title") String title) {
        String cleaned = normalize(title);
        if (cleaned.isBlank()) {
            return "ERROR:EMPTY_TITLE";
        }
        JsonStorage.addOrUpdate(cleaned, "pas_interesse");
        return "REMOVED:" + cleaned;
    }

    @Tool("Retourne la liste des films pour un statut donné ('envie', 'pas_interesse', 'deja_vu').")
    public List<String> getListByStatus(@P("status") String status) {
        String s = normalizeStatus(status);
        if (s == null) s = "envie"; // fallback
        return JsonStorage.getByStatus(s).stream()
                .map(this::normalize)
                .filter(t -> !t.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }


    @Tool("Marque un film comme 'pas_interesse'.")
    public String markAsDisliked(@P("title") String title) {
        String t = normalize(title);
        if (t.isBlank()) return "ERROR:EMPTY_TITLE";
        JsonStorage.addOrUpdate(t, "pas_interesse");
        return "DISLIKED:" + t;
    }

    @Tool("Marque un film comme 'deja_vu'.")
    public String markAsSeen(@P("title") String title) {
        String t = normalize(title);
        if (t.isBlank()) return "ERROR:EMPTY_TITLE";
        JsonStorage.addOrUpdate(t, "deja_vu");
        return "SEEN:" + t;
    }

    @Tool("Change le statut d'un film ('envie', 'deja_vu', 'pas_interesse').")
    public String setStatus(@P("title") String title, @P("status") String status) {
        String t = normalize(title);
        String s = normalizeStatus(status);
        if (t.isBlank()) return "ERROR:EMPTY_TITLE";
        if (s == null)   return "ERROR:BAD_STATUS";
        JsonStorage.addOrUpdate(t, s);
        return "STATUS_CHANGED:" + t + ":" + s;
    }


    private String normalize(String s) {
        if (s == null) return "";

        // 1) Nettoyage simple
        String t = s.replaceAll("[\"“”«»]", " ").trim();
        t = t.replaceAll("\\s{2,}", " ");

        // 2) Coupe les queues “... dans/à ma wishlist|liste d’envie|liste”
        t = TAIL_BUCKET.matcher(t).replaceFirst("");

        // 3) Coupe les queues “... en pas_interesse|deja_vu|envie / seen / not interested”
        //    (insensible aux accents et variantes)
        String deAccented = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        if (TAIL_STATUS.matcher(deAccented).find()) {
            t = TAIL_STATUS.matcher(t).replaceFirst("");
        }

        // 4) Ponctuation finale inutile
        t = t.replaceAll("[.。…]+$", "").trim();

        return t;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "envie";
        String s = status.trim().toLowerCase(Locale.ROOT)
                .replace('’','\'')
                .replace("-", "_")
                .replace(" ", "_");

        // alias usuels
        if (s.contains("deja") && s.contains("vu")) s = "deja_vu";
        if (s.contains("pas") && (s.contains("interesse") || s.contains("intéresse"))) s = "pas_interesse";

        // whitelist
        return switch (s) {
            case "envie", "deja_vu", "pas_interesse" -> s;
            default -> null;
        };
    }

}
