package app.cinematch.agent.langchain;

import app.cinematch.MovieRecommenderService;
import app.cinematch.agent.Profile;
import app.cinematch.agent.tools.LibraryTools;
import app.cinematch.agent.tools.WishlistTools;
import app.cinematch.agent.tools.BulkTools;
import app.cinematch.agent.tools.MaintenanceTools;
import app.cinematch.agent.tools.ViewingTools;


import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

public final class LangChain4jAgentBridge {

    private final CineAssistant assistant;
    @SuppressWarnings("unused")
    private final Profile profile;
    private final BulkTools bulkTools;
    public LangChain4jAgentBridge(String ollamaUrl, String modelName,
                                  Profile profile, MovieRecommenderService service) {
        this.profile = profile;
        this.bulkTools = new BulkTools();
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(modelName)
                .temperature(0.1)
                .build();

        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(6);

        this.assistant = AiServices.builder(CineAssistant.class)
                .chatLanguageModel(model)
                .tools(
                        new WishlistTools(),
                        new LibraryTools(service),
                        bulkTools,
                        new MaintenanceTools(),
                        new ViewingTools(service)
                )
                .chatMemory(memory)
                .build();
    }

    public String ask(String userPrompt) {
        String handled = tryClientSideBulkAdd(userPrompt);
        if (handled != null) return handled;
        return assistant.chat(userPrompt);
    }

    // --- Fallback local “ajout multiple” ---
    private String tryClientSideBulkAdd(String msg) {
        String low = msg.toLowerCase(java.util.Locale.ROOT);
        boolean looksLikeAdd =
                (low.contains("ajoute") || low.contains("ajouter") || low.contains("mets") || low.contains("met") || low.contains("add"))
                        && (low.contains("wishlist") || low.contains("liste d'envie") || low.contains("liste"));
        boolean looksLikeMany = low.contains(",") || low.contains("\n");

        if (!looksLikeAdd || !looksLikeMany) return null;

        // On extrait la partie titres en retirant la fin “à/ dans ma/ la wishlist …”
        String titles = msg
                .replaceAll("(?i)^.*?(ajoute|ajouter|mets|met|add)\\s*", "")
                .replaceAll("(?i)\\s*(dans|à|a)\\s*(ma|la)\\s*(wishlist|liste d'envie|liste).*", "")
                .trim();

        if (titles.isEmpty()) return null;

        System.out.println("[BRIDGE] Client-side bulk add: " + titles);
        String res = bulkTools.addManyToWishlist(titles);      // appelle le tool directement
        // Réponse utilisateur propre :
        if (res.startsWith("ADDED_MANY:")) {
            String n = res.substring("ADDED_MANY:".length());
            return "Ajouté(s) à ta liste d’envie : " + titles + " (" + n + ").";
        }
        return "Ajout effectué : " + titles + ".";
    }
}
