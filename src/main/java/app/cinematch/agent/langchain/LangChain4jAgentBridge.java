package app.cinematch.agent.langchain;

import app.cinematch.MovieRecommenderService;
import app.cinematch.agent.Profile;
import app.cinematch.agent.tools.*;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import java.text.Normalizer;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LangChain4jAgentBridge {

    private final CineAssistant assistant;
    @SuppressWarnings("unused")
    private final Profile profile;

    private final BulkTools bulkTools = new BulkTools();
    private final WishlistTools wishlistTools = new WishlistTools();
    private final MaintenanceTools maintenanceTools = new MaintenanceTools();

    // (?iu) = case-insensitive + unicode (accents)
    private static final Pattern VERB_PREFIX =
            Pattern.compile("(?iu)^.*?(ajoute|ajouter|mets|met|add)\\s*");
    private static final Pattern LIST_TAIL  =
            Pattern.compile("(?iu)\\s*(?:dans|à|a|to|into|in)\\s*(?:ma|la|my|the)?\\s*(?:wish\\s*list|wishlist|liste d'envie|liste)\\s*[.!?\\s]*$");

    // --- Regex FR robustes ---
    private static final Pattern P_NOT_INTERESTED =
            Pattern.compile("(?iu)\\b(je\\s+ne\\s+suis\\s+pas\\s+int[eé]ress[eé]\\s+par|je\\s+n['’]?aime\\s+pas)\\s+(.+)$");
    private static final Pattern P_SEEN =
            Pattern.compile("(?iu)\\b(j['’]?ai\\s+vu|marque[- ]?le\\s+d[eé]j[aà][- ]?vu)\\s+(.+)$");
    private static final Pattern P_ADD_SINGLE =
            Pattern.compile("(?iu)\\b(ajoute|ajouter|mets|met)\\s+(.+?)(?:\\s+(?:a|à)\\s+(?:ma\\s+)?(?:liste|wishlist))?$");
    private static final Pattern P_REMOVE_SINGLE =
            Pattern.compile("(?iu)\\b(enl[eè]ve|enlever|supprime|supprimer|retire|retirer)\\s+(.+?)\\s+(?:de|d['’]e?)\\s+(?:ma|la|the)?\\s*(?:liste(?:\\s*d'envie)?|wishlist)\\b");
    private static final Pattern P_REMOVE_SOLO =
            Pattern.compile("(?iu)\\b(enl[eè]ve|enlever|supprime|supprimer|retire|retirer)\\s+(.+)$");
    private static final Pattern P_CLEAR_ENVIE =
            Pattern.compile("(?iu)\\b(supprime|enl[eè]ve|vide|efface)\\s+(tout|tous|toutes).*(wishlist|liste\\s*d'envie|envie)\\b");
    private static final Pattern P_CLEAR_NOPE =
            Pattern.compile("(?iu)\\b(supprime|enl[eè]ve|vide|efface)\\s+(tout|tous|toutes).*?(?:pas[ _]*int[eé]ress[ée]s?|pas_?interesse)\\b");

    private static final Pattern P_CLEAR_SEEN =
            Pattern.compile("(?iu)\\b(supprime|enl[eè]ve|vide|efface)\\s+(tout|tous|toutes).*?(?:d[eé]j[aà][ _]*vu|deja_?vu)\\b");

    private static final Pattern CLEAR_ALL = Pattern.compile(
            "(?iu)\\b(supprime|enl[eè]ve|vide|efface|clear|remove|delete)\\s+(tout|tous|toutes|all)"
                    + "(?:\\s+(?:dans|de|du|des|la|le))?\\s+(.*)$");

    public LangChain4jAgentBridge(String ollamaUrl, String modelName,
                                  Profile profile, MovieRecommenderService service) {
        this.profile = profile;

        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(modelName)
                .temperature(0.1)
                .build();

        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(6);

        this.assistant = AiServices.builder(CineAssistant.class)
                .chatLanguageModel(model)
                .tools(
                        wishlistTools,
                        new LibraryTools(service),
                        bulkTools,
                        maintenanceTools,
                        new ViewingTools(service)
                )
                .chatMemory(memory)
                .build();
    }

    private static String unquote(String t) {
        return t == null ? "" : t.replaceAll("[\"“”«»]", "").trim();
    }

    private String tryDirectSingleAction(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) return null;
        Matcher m;

        // Pas intéressé
        m = P_NOT_INTERESTED.matcher(userPrompt);
        if (m.find()) {
            String title = unquote(m.group(2));
            if (!title.isBlank()) {
                wishlistTools.markAsDisliked(title);
                return "« " + title + " » marqué en pas_interesse.";
            }
        }

        // Déjà vu
        m = P_SEEN.matcher(userPrompt);
        if (m.find()) {
            String title = unquote(m.group(2));
            if (!title.isBlank()) {
                wishlistTools.markAsSeen(title);
                return "« " + title + " » marqué en deja_vu.";
            }
        }

        // Ajouter (1 titre)
        m = P_ADD_SINGLE.matcher(userPrompt);
        if (m.find()) {
            String title = unquote(m.group(2));
            if (!title.isBlank()) {
                wishlistTools.addToWishlist(title);
                return "« " + title + " » ajouté à votre wishlist.";
            }
        }

        // Supprimer (1 titre)
        m = P_REMOVE_SINGLE.matcher(userPrompt);
        if (m.find()) {
            String title = unquote(m.group(2));
            if (!title.isBlank()) {
                wishlistTools.removeFromWishlist(title);
                return "« " + title + " » retiré de votre wishlist.";
            }
        }
        // Supprimer (1 titre) sans "de ma liste"
        m = P_REMOVE_SOLO.matcher(userPrompt);
        if (m.find()) {
            String title = unquote(m.group(2));
            // on ignore le cas "supprime tout ..." qui est traité plus haut
            if (!title.isBlank() && !title.toLowerCase(Locale.ROOT).startsWith("tout")) {
                wishlistTools.removeFromWishlist(title);
                return "« " + title + " » retiré de votre wishlist.";
            }
        }


        return null;
    }
    private static String mapStatusFromTail(String tailRaw) {
        if (tailRaw == null) return null;

        // 1) Normalise et enlève les accents pour matcher "intéressé", "intéressés", etc.
        String t = Normalizer.normalize(tailRaw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")          // enlève les diacritiques
                .toLowerCase(Locale.ROOT)
                .replace('’','\'')
                .replace("_", " ")
                .replace("-", " ")
                .trim();

        // 2) Déductions tolérantes (FR/EN, singulier/pluriel)
        if (t.matches(".*\\b(envie|wishlist|wish\\s*list)\\b.*")) {
            return "envie";
        }
        if (t.matches(".*\\bdej[au]\\s*vu\\b.*|.*\\bseen\\b.*")) {
            return "deja_vu";
        }
        if (t.matches(".*\\bpas\\s*(?:interesse|interesses?|interet)s?\\b.*"      // "pas interessé/s", "pas interet"
                + "|.*\\bdislike\\b.*"
                + "|.*\\bnot\\s*interested\\b.*")) {
            return "pas_interesse";
        }
        return null;
    }


    public String ask(String userPrompt) {
        final String msg = userPrompt == null ? "" : userPrompt.trim();
        // 0) Raccourci générique "supprime/vide tout ..." -> suppression physique (hard)
        Matcher clr = CLEAR_ALL.matcher(msg);
        if (clr.find()) {
            String tail = clr.group(3);
            String status = mapStatusFromTail(tail);
            if (status == null) {
                return "Statut non reconnu. Dites : « supprime tout dans envie », « supprime tout dans pas intéressé » ou « supprime tout dans déjà vu ».";
            }
            return maintenanceTools.clearStatus(status, "hard");
        }

        // 0) CLEAR ALL — à faire en premier
        if (P_CLEAR_ENVIE.matcher(msg).find()) {
            return maintenanceTools.clearStatus("envie", "hard");
        }
        if (P_CLEAR_NOPE.matcher(msg).find()) {
            return maintenanceTools.clearStatus("pas_interesse", "hard");
        }
        if (P_CLEAR_SEEN.matcher(msg).find()) {
            return maintenanceTools.clearStatus("deja_vu", "hard");
        }

        // 1) Multi-actions (ex: "ajoute X et supprime Y")
        if (looksLikeMulti(msg) || MultiActionTools.shouldForceMulti(msg)) {
            return new MultiActionTools().mixedActions(msg);
        }

        // 2) Ajout multiple côté client (CSV / retours ligne)
        String handled = tryClientSideBulkAdd(msg);
        if (handled != null) return handled;

        // 3) Action simple (add/remove/seen/disliked) — nécessite "... de ma liste / wishlist"
        String direct = tryDirectSingleAction(msg);
        if (direct != null) return direct;

        // 4) Sinon : laisser le LLM utiliser les tools
        return assistant.chat(msg);
    }


    // --- Fallback local “ajout multiple” ---
    private String tryClientSideBulkAdd(String msg) {
        if (msg == null) return null;
        String low = msg.toLowerCase(Locale.ROOT);

        // Pas besoin d'exiger "à ma wishlist"
        boolean hasAddVerb = low.contains("ajoute") || low.contains("ajouter")
                || low.contains("mets") || low.contains("met") || low.contains("add");
        boolean looksLikeMany = low.contains(",") || low.contains("\n");
        if (!hasAddVerb || !looksLikeMany) return null;

        // Extraction robuste : retire le verbe éventuel + la queue éventuelle
        String titles = extractTitlesForBulk(msg);
        if (titles.isEmpty()) return null;

        String res = bulkTools.addManyToWishlist(titles);
        if (res.startsWith("ADDED_MANY:")) {
            String n = res.substring("ADDED_MANY:".length());
            return "Ajouté(s) à ta liste d’envie : " + titles + " (" + n + ").";
        }
        return "Ajout effectué : " + titles + ".";
    }

    /** Heuristique simple : connecteur + au moins deux verbes d'action -> multi-actions. */
    private static boolean looksLikeMulti(String s) {
        if (s == null) return false;
        String lc = s.toLowerCase(Locale.ROOT);

        boolean hasConnector = lc.contains(" et ")
                || lc.contains(" puis ")
                || lc.contains(";")
                || lc.contains(".");

        int verbHits = 0;
        String[] verbs = {
                "ajoute", "ajouter", "mets", "met",
                "enlève", "enlever", "supprime", "supprimer",
                "retire", "retirer", "marque", "marquer"
        };
        for (String v : verbs) {
            if (lc.contains(v + " ")) verbHits++;
        }
        return hasConnector && verbHits >= 2;
    }

    // Méthode utilitaire: applique les 2 regex en chaîne (verbe puis queue)
    private static String extractTitlesForBulk(String msg) {
        String noVerb = VERB_PREFIX.matcher(msg).replaceFirst("");
        String noTail = LIST_TAIL.matcher(noVerb).replaceFirst("");
        return noTail.trim();
    }
}
