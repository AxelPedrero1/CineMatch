package app.cinematch.agent;

import app.cinematch.api.OllamaClient;
import app.cinematch.model.LlmMessage;
import app.cinematch.model.LlmRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent conversationnel connecté à Ollama avec mémoire et profil personnalisable.
 */
public class ChatAgent {

    private final OllamaClient ollama;
    private Profile profile;
    private final Memory memory;

    public ChatAgent(OllamaClient ollama, Profile profile, Memory memory) {
        this.ollama = ollama;
        this.profile = profile;
        this.memory = memory;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    /**
     * Envoie un message utilisateur à l'agent et récupère la réponse de l'IA.
     */
    public String ask(String userPrompt) {
        List<String> seen = memory.seen();
        List<String> wishlist = memory.toWatch();
        List<String> disliked = memory.notInterested();

        String system = """
        Tu es un expert du cinéma francophone, spécialiste des recommandations personnalisées.
        Tes réponses doivent toujours être en français, avec un ton naturel, amical et professionnel.
        
        Voici les informations sur les goûts de l’utilisateur :
        Films déjà vus : %s
        Films qu’il souhaite voir : %s
        Films qu’il n’aime pas ou qui ne l’intéressent pas : %s

        - Ne repropose jamais un film déjà vu ou marqué comme "pas intéressé".
        - Inspire-toi des films aimés pour proposer des recommandations cohérentes et variées.
        - Si tu cites un film, assure-toi qu’il existe réellement (existant sur IMDb ou TMDB).
        - Si l’utilisateur te demande quels films il a vus, veux voir ou n’aime pas, réponds à partir de ces listes.
        - Si une liste est vide, ignore-la naturellement dans ta réponse.

        Sois synthétique (réponses ≤ 100 mots) et évite les répétitions inutiles.
        """.formatted(
                seen.isEmpty() ? "aucun film enregistré" : String.join(", ", seen),
                wishlist.isEmpty() ? "aucun film enregistré" : String.join(", ", wishlist),
                disliked.isEmpty() ? "aucun film enregistré" : String.join(", ", disliked)
        );

        return ollama.chat(system, userPrompt);
    }


    public Memory getMemory() {
        return memory;
    }

    public Profile getProfile() {
        return profile;
    }
}
