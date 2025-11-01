package app.cinematch.agent.langchain;

import app.cinematch.MovieRecommenderService;
import app.cinematch.agent.Profile;
import app.cinematch.agent.tools.LibraryTools;
import app.cinematch.agent.tools.WishlistTools;

import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

public final class LangChain4jAgentBridge {

    private final CineAssistant assistant;
    @SuppressWarnings("unused")
    private final Profile profile;

    public LangChain4jAgentBridge(String ollamaUrl, String modelName,
                                  Profile profile, MovieRecommenderService service) {
        this.profile = profile;

        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(modelName)
                .temperature(0.2)
                .build();

        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(6);

        this.assistant = AiServices.builder(CineAssistant.class)
                .chatLanguageModel(model)
                .tools(
                        new WishlistTools(),
                        new LibraryTools(service)
                )
                .chatMemory(memory)
                .build();
    }

    public String ask(String userPrompt) {
        return assistant.chat(userPrompt);
    }
}
