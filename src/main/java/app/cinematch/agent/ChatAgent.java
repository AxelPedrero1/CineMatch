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

    public ChatAgent(final OllamaClient ollama, final Profile profile, final Memory ignored) {
        this.ollama = ollama;
        this.profile = profile;
        // Instance interne propre (Memory est stateless, donc pas de perte fonctionnelle)
        this.memory = new Memory();
    }

    public void setProfile(final Profile profile) {
        this.profile = profile;
    }

    /**
     * Envoie un message utilisateur à l'agent et récupère la réponse de l'IA.
     */
    public String ask(final String userPrompt) {
        final List<String> seen = memory.seen();
        final List<String> wishlist = memory.toWatch();
        final List<String> disliked = memory.notInterested();

        final String seenStr = seen.isEmpty() ? "aucun film enregistré" : String.join(", ", seen);
        final String wishStr = wishlist.isEmpty() ? "aucun film enregistré" : String.join(", ", wishlist);
        final String badStr  = disliked.isEmpty() ? "aucun film enregistré" : String.join(", ", disliked);

        // %n au lieu de \n pour la portabilité (VA_FORMAT_STRING_USES_NEWLINE)
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
     * Getter sans fuite de représentation interne : on renvoie une nouvelle instance.
     */
    public Memory getMemory() {
        return new Memory();
    }

    public Profile getProfile() {
        return profile;
    }
}
