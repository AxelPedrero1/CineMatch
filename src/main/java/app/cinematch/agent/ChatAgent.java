package app.cinematch.agent;

import app.cinematch.api.OllamaClient;
import java.util.List;

/**
 * Représente un agent conversationnel connecté à un modèle de langage Ollama,
 * capable de générer des recommandations de films personnalisées en fonction
 * des préférences de l’utilisateur.
 *
 * <p>L’agent maintient un profil utilisateur et une mémoire interne (stateless)
 * permettant de conserver la trace des films vus, à voir ou non appréciés.
 * Il formate dynamiquement le prompt système envoyé au modèle Ollama pour
 * obtenir des réponses cohérentes et pertinentes.</p>
 *
 * <p>Exemple d’utilisation :
 * <pre>{@code
 * OllamaClient ollama = new OllamaClient("http://localhost:11434", "mistral");
 * Profile profil = new Profile("Simon");
 * ChatAgent agent = new ChatAgent(ollama, profil, null);
 * String reponse = agent.ask("Peux-tu me recommander un film français récent ?");
 * }</pre>
 *
 * @see app.cinematch.api.OllamaClient
 * @see app.cinematch.agent.Memory
 * @see app.cinematch.agent.Profile
 */
public final class ChatAgent {

    /** Client Ollama utilisé pour communiquer avec le modèle de langage. */
    private final OllamaClient ollama;

    /** Profil utilisateur contenant les préférences générales. */
    private Profile profile;

    /**
     * Mémoire interne stateless.
     * Une nouvelle instance est créée localement pour éviter toute fuite de représentation
     * (évite l’avertissement SpotBugs EI_EXPOSE_REP2).
     */
    private final Memory memory;
    private final ConversationMemory convMemory; // 🧠 mémoire du chat


    /**
     * Construit un nouvel agent conversationnel basé sur Ollama, avec un profil et une mémoire interne.
     *
     * @param ollama  le client Ollama à utiliser pour les échanges
     * @param profile le profil utilisateur associé à cet agent
     * @param ignored paramètre mémoire ignoré pour éviter l’exposition d’une instance externe
     */
    public ChatAgent(final OllamaClient ollama, final Profile profile, final Memory ignored) {
        this.ollama = ollama;
        this.profile = profile;
        // Instance interne propre (Memory est stateless, donc pas de perte fonctionnelle)
        this.memory = new Memory();
        this.convMemory = new ConversationMemory(6); // garde les 6 derniers messages
    }

    /**
     * Met à jour le profil utilisateur associé à l’agent.
     *
     * @param profile le nouveau profil utilisateur à appliquer
     */
    public void setProfile(final Profile profile) {
        this.profile = profile;
    }

    /**
     * Envoie un message de l’utilisateur à l’agent et retourne la réponse générée par Ollama.
     *
     * <p>La méthode récupère les listes internes de films vus, souhaités et non appréciés
     * depuis {@link Memory}, et les insère dans le prompt système envoyé au modèle.
     * Le modèle adapte alors ses recommandations en conséquence.</p>
     *
     * @param userPrompt le message de l’utilisateur (question, demande de recommandation, etc.)
     * @return la réponse générée par le modèle de langage
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
     * Retourne une nouvelle instance de {@link Memory}, garantissant l’absence
     * de fuite de représentation interne.
     *
     * @return une nouvelle instance de {@link Memory}
     */
    public Memory getMemory() {
        return new Memory();
    }

    /**
     * Retourne le profil utilisateur actuellement associé à l’agent.
     *
     * @return le profil de l’utilisateur
     */
    public Profile getProfile() {
        return profile;
    }
}
