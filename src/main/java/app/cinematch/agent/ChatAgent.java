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
        final List<String> seen = memory.seen();
        final List<String> wishlist = memory.toWatch();
        final List<String> disliked = memory.notInterested();

        final String seenStr = seen.isEmpty() ? "aucun film enregistré" : String.join(", ", seen);
        final String wishStr = wishlist.isEmpty() ? "aucun film enregistré" : String.join(", ", wishlist);
        final String badStr  = disliked.isEmpty() ? "aucun film enregistré" : String.join(", ", disliked);

        // Utilise %n au lieu de \n pour la portabilité entre systèmes (SpotBugs : VA_FORMAT_STRING_USES_NEWLINE)
        final String system = String.format(
                "Tu es un expert du cinéma francophone, spécialiste des recommandations personnalisées.%n"
                        + "Tes réponses doivent toujours être en français, avec un ton naturel, amical et professionnel.%n%n"
                        + "Voici les informations sur les goûts de l’utilisateur :%n"
                        + "Films déjà vus : %s%n"
                        + "Films qu’il souhaite voir : %s%n"
                        + "Films qu’il n’aime pas ou qui ne l’intéressent pas : %s%n%n"
                        + "- Ne repropose jamais un film déjà vu ou marqué comme \"pas intéressé\".%n"
                        + "- Inspire-toi des films aimés pour proposer des recommandations cohérentes et variées.%n"
                        + "- Si tu cites un film, assure-toi qu’il existe réellement (existant sur IMDb ou un site de référence fiable).%n"
                        + "- Si l’utilisateur te demande quels films il a vus, veut voir ou n’aime pas, réponds à partir de ces listes.%n"
                        + "- Si une liste est vide, ignore-la naturellement dans ta réponse.%n%n"
                        + "Sois synthétique (réponses ≤ 100 mots) et évite les répétitions inutiles.",
                seenStr, wishStr, badStr
        );

        return ollama.chat(system, userPrompt);
    }

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
