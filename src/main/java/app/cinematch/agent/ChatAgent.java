package app.cinematch.agent;

import app.cinematch.api.OllamaClient;
import java.util.List;

/**
 * Agent conversationnel connecté à Ollama avec mémoire et profil personnalisable.
 */
public final class ChatAgent {

    private final OllamaClient ollama;
    private Profile profile;
    // Memory est sans état -> on évite EI_EXPOSE_REP2 en ne stockant pas l'instance externe
    private final Memory memory;
    private final ConversationMemory convMemory; // 🧠 mémoire du chat


    public ChatAgent(final OllamaClient ollama, final Profile profile, final Memory ignored) {
        this.ollama = ollama;
        this.profile = profile;
        // Instance interne propre (Memory est stateless, donc pas de perte fonctionnelle)
        this.memory = new Memory();
        this.convMemory = new ConversationMemory(6); // garde les 6 derniers messages
    }

    public void setProfile(final Profile profile) {
        this.profile = profile;
    }

    /**
     * Envoie un message utilisateur à l'agent et récupère la réponse de l'IA.
     */
    public String ask(final String userPrompt) {
        // 1) Ajout du message utilisateur à la mémoire
        convMemory.addUserMessage(userPrompt);

        // 2) Construit le contexte mémoire des goûts
        final List<String> seen = memory.seen();
        final List<String> wishlist = memory.toWatch();
        final List<String> disliked = memory.notInterested();

        final String seenStr = seen.isEmpty() ? "aucun film enregistré" : String.join(", ", seen);
        final String wishStr = wishlist.isEmpty() ? "aucun film enregistré" : String.join(", ", wishlist);
        final String badStr  = disliked.isEmpty() ? "aucun film enregistré" : String.join(", ", disliked);

        // Construit le message système avec le contexte conversationnel
        final String system = String.format(
                """
                Tu es un expert du cinéma francophone, spécialiste des recommandations personnalisées.
                Tes réponses doivent toujours être en français, avec un ton naturel, amical et professionnel.

                Voici les informations sur les goûts de l’utilisateur :
                - Films déjà vus : %s
                - Films qu’il souhaite voir : %s
                - Films qu’il n’aime pas : %s

                Contexte récent de la conversation :
                %s

                Rappelle-toi :
                - Ne repropose jamais un film déjà vu ou non souhaité.
                - Inspire-toi du contexte précédent pour rester cohérent.
                - Réponds de façon fluide, ≤ 100 mots, sans répétition.
                """,
                seenStr, wishStr, badStr, convMemory.toPromptString()
        );
        // 4) Appel à Ollama
        String response = ollama.chat(system, userPrompt);

        // 5) Ajout de la réponse de l’IA à la mémoire
        convMemory.addAssistantMessage(response);

        return response;    }

    /**
     * Getter sans fuite de représentation interne : on renvoie une nouvelle instance.
     */
    public Memory getMemory() {
        return new Memory();
    }

    public Profile getProfile() {
        return profile;
    }
}
