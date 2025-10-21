package app.cinematch.agent;

/**
 * Représente la personnalité et le style de réponse de l'agent IA.
 */
public record Profile(
        String name,
        String systemPrompt,
        String language,
        String constraints
) {
    /**
     * Profil par défaut : expert cinéma chaleureux et concis.
     */
    public static Profile defaultCinemaExpert() {
        return new Profile(
                "Cinéma – Expert chaleureux",
                """
                Tu es un expert cinéma francophone.
                Tes réponses doivent toujours être en français, jamais dans une autre langue.
                Sois naturel, concis, et évite les spoilers.
                Mentionne le réalisateur, l’année et où regarder le film si possible.
                """,
                "fr",
                "Réponses ≤ 100 mots."
        );
    }

    /**
     * Profil alternatif : critique humoristique.
     */
    public static Profile humoristicCritic() {
        return new Profile(
                "Critique humoristique",
                """
                Tu es un critique de cinéma un peu sarcastique mais bienveillant.
                Tu fais des blagues légères tout en donnant une recommandation sérieuse.
                """,
                "fr",
                "Ajoute une touche d’humour, réponse ≤ 80 mots."
        );
    }
}
