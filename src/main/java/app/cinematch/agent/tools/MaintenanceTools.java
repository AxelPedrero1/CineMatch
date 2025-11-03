package app.cinematch.agent.tools;

import app.cinematch.util.JsonStorage;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.*;

public class MaintenanceTools {

    @Tool("Supprime visuellement les entrées vides/quotes-only d'une liste en les marquant 'pas_interesse'.")
    public String pruneBlanksInStatus(@P("status") String status) {
        String st = normStatus(status);
        int n = 0;
        for (String t : JsonStorage.getByStatus(st)) {
            String cleaned = norm(t);
            if (cleaned.isBlank()) { JsonStorage.addOrUpdate(cleaned, "pas_interesse"); n++; }
        }
        return "PRUNED:" + n + " in " + st;
    }

    @Tool("Renomme un titre : copie le statut si trouvé, puis marque l'ancien en 'pas_interesse'.")
    public String renameTitle(@P("oldTitle") String oldTitle, @P("newTitle") String newTitle) {
        String oldT = norm(oldTitle), newT = norm(newTitle);
        if (oldT.isBlank() || newT.isBlank()) return "ERROR:EMPTY_TITLE";
        String status = findStatusIgnoreCase(oldT);
        if (status == null) status = "envie";
        JsonStorage.addOrUpdate(newT, status);
        JsonStorage.addOrUpdate(oldT, "pas_interesse");
        return "RENAMED:" + oldT + "->" + newT + " (" + status + ")";
    }

    @Tool("Retourne la liste triée pour un statut donné ('asc' ou 'desc').")
    public java.util.List<String> getListByStatusSorted(@P("status") String status, @P("order") String order) {
        String st = normStatus(status);
        List<String> list = new ArrayList<>();
        for (String t : JsonStorage.getByStatus(st)) {
            String s = norm(t);
            if (!s.isBlank()) list.add(s);
        }
        list.sort(String::compareToIgnoreCase);
        if ("desc".equalsIgnoreCase(order)) Collections.reverse(list);
        return list;
    }

    @Tool("Donne des statistiques simples (compte par statut). Utilise detail='all' par défaut.")
    public String getStats(@P("detail") String detail) {
        int envie = JsonStorage.getByStatus("envie").size();
        int nope  = JsonStorage.getByStatus("pas_interesse").size();
        int seen  = JsonStorage.getByStatus("deja_vu").size();
        int total = envie + nope + seen;
        return "STATS: total=" + total + " | envie=" + envie + " | pas_interesse=" + nope + " | deja_vu=" + seen;
    }

    @Tool("Vide totalement une liste par statut. status ∈ {'envie','pas_interesse','deja_vu'}. "
            + "mode='hard' supprime physiquement, 'soft' déplace ailleurs.")
    public String clearStatus(@P("status") String status,
                              @P("mode") String mode) {
        String s = normalizeStatus(status);
        if (s == null) {
            return "Statut invalide. Utilisez « envie », « deja_vu » ou « pas_interesse ».";
        }

        boolean hard = (mode == null) || mode.equalsIgnoreCase("hard");

        // HARD : suppression physique
        if (hard) {
            int n = JsonStorage.removeAllByStatus(s);
            return friendlyClearedHard(s, n);
        }

        // SOFT : “vider” en déplaçant vers un autre statut
        java.util.List<String> items = JsonStorage.getByStatus(s);
        if (items == null || items.isEmpty()) {
            return "Aucun film à déplacer depuis « " + labelStatus(s) + " ».";
        }

        String target = switch (s) {
            case "envie" -> "pas_interesse";
            case "pas_interesse" -> "deja_vu";
            default -> "pas_interesse";
        };

        int n = 0;
        for (String t : items) {
            if (t != null && !t.isBlank()) {
                JsonStorage.addOrUpdate(t, target);
                n++;
            }
        }
        return "Déplacé " + n + (n > 1 ? " films" : " film") + " de « "
                + labelStatus(s) + " » vers « " + labelStatus(target) + " ».";
    }


    // --- helper (garde-le dans la classe) ---
    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return null;
        String s = status.trim().toLowerCase(Locale.ROOT)
                .replace('’','\'').replace("-", "_").replace(" ", "_");
        if (s.contains("deja") && s.contains("vu")) s = "deja_vu";
        if (s.contains("pas") && (s.contains("interesse") || s.contains("intéresse"))) s = "pas_interesse";
        return switch (s) {
            case "envie", "pas_interesse", "deja_vu" -> s;
            default -> null;
        };
    }

    private static String friendlyClearedHard(String status, int count) {
        String label = labelStatus(status);
        if (count == 0) return "Aucun film à supprimer dans « " + label + " ».";
        return "Supprimé " + count + (count > 1 ? " films" : " film") + " de « " + label + " ».";
    }

    private static String labelStatus(String s) {
        return switch (s) {
            case "envie" -> "liste d’envie";
            case "pas_interesse" -> "pas intéressé";
            case "deja_vu" -> "déjà vu";
            default -> s;
        };
    }


    private static boolean hasRemoveMethod() {
        try {
            app.cinematch.util.JsonStorage.class.getMethod("remove", String.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }


    private static String norm(String s) {
        if (s == null) return "";
        String t = s.replaceAll("[\"“”«»]", "").trim();
        return t.replaceAll("\\s{2,}", " ");
    }
    private static String normStatus(String s) {
        if (s == null) return "envie";
        String x = s.trim().toLowerCase(Locale.ROOT);
        return switch (x) { case "envie","pas_interesse","deja_vu" -> x; default -> "envie"; };
    }
    private static String findStatusIgnoreCase(String title) {
        for (String st : new String[]{"envie","pas_interesse","deja_vu"}) {
            for (String t : JsonStorage.getByStatus(st)) {
                if (t != null && t.equalsIgnoreCase(title)) return st;
            }
        }
        return null;
    }
}
