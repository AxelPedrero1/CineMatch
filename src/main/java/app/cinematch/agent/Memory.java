package app.cinematch.agent;

import app.cinematch.model.HistoryEntry;
import app.cinematch.util.JsonStorage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gère la mémoire persistante de l'utilisateur :
 * - Films déjà vus (status = "deja_vu")
 * - Films à voir (status = "envie")
 * - Films non aimés (status = "pas_interesse")
 *
 * Utilise JsonStorage pour sauvegarder et recharger les données.
 */
public class Memory {

    /** Marque un film comme "déjà vu". */
    public void addSeen(String title) {
        JsonStorage.addOrUpdate(title, "deja_vu");
    }

    /** Marque un film comme "envie de voir". */
    public void addToWatch(String title) {
        JsonStorage.addOrUpdate(title, "envie");
    }

    /** Marque un film comme "pas intéressé". */
    public void addNotInterested(String title) {
        JsonStorage.addOrUpdate(title, "pas_interesse");
    }

    /** Retourne la liste des films marqués "déjà vus". */
    public List<String> seen() {
        return JsonStorage.loadAll().stream()
                .filter(e -> "deja_vu".equalsIgnoreCase(e.status()))
                .map(HistoryEntry::title)
                .distinct()
                .collect(Collectors.toList());
    }

    /** Retourne la liste des films marqués "envie". */
    public List<String> toWatch() {
        return JsonStorage.loadAll().stream()
                .filter(e -> "envie".equalsIgnoreCase(e.status()))
                .map(HistoryEntry::title)
                .distinct()
                .collect(Collectors.toList());
    }

    /** Retourne la liste des films marqués "pas_interesse". */
    public List<String> notInterested() {
        return JsonStorage.loadAll().stream()
                .filter(e -> "pas_interesse".equalsIgnoreCase(e.status()))
                .map(HistoryEntry::title)
                .distinct()
                .collect(Collectors.toList());
    }

    /** Retourne tout l’historique complet. */
    public List<HistoryEntry> history() {
        return JsonStorage.loadAll();
    }

    @Override
    public String toString() {
        return "Mémoire : " + seen().size() + " vus, " +
                toWatch().size() + " envies, " +
                notInterested().size() + " pas intéressés.";
    }
}
