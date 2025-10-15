package app.cinematch;

import app.cinematch.api.OllamaClient;
import app.cinematch.api.TMDBClient;
import app.cinematch.model.Movie;
import app.cinematch.model.Recommendation;
import app.cinematch.util.JsonStorage;

import java.util.Optional;
import java.util.Random;

public class MovieRecommenderService {

    private final OllamaClient ollama;
    private final TMDBClient tmdb;
    private final Random random = new Random();

    public MovieRecommenderService(String baseUrl, String model, String tmdbApiKey, String tmdbRegion) {
        this.ollama = new OllamaClient(baseUrl, model);
        this.tmdb = new TMDBClient(tmdbApiKey, tmdbRegion);
    }

    public Recommendation recommendFromLike(String likedTitle) {
        // Try TMDB similar
        Optional<Movie> m = tmdb.similarFromTitle(likedTitle);
        if (m.isPresent()) {
            Movie mv = m.get();
            String platform = tmdb.providersFor(mv.id()).orElse("Plateforme inconnue");
            return new Recommendation(mv.title(), "Similaire à " + likedTitle, platform, TMDBClient.posterUrl(mv.posterPath()));
        }
        // Fallback LLM
        String system = "Tu es un expert cinéma, propose un film similaire à celui donné (titre exact seulement).";
        String user = "L'utilisateur a aimé le film '" + likedTitle + "'. Propose un film du même style (réponds uniquement par le titre exact).";
        String suggestion = ollama.chat(system, user).split("\n")[0].trim();
        return new Recommendation(normalizeTitle(suggestion), "Similaire à " + likedTitle, randomPlatform(), null);
    }

    public Recommendation recommendRandom() {
        String system = "Tu es un expert cinéma, propose un film intéressant à découvrir (titre exact seulement).";
        String user = "Propose un film populaire ou original à voir (réponds uniquement par le titre exact).";
        String suggestion = ollama.chat(system, user).split("\n")[0].trim();
        return new Recommendation(normalizeTitle(suggestion), "Suggestion IA", randomPlatform(), null);
    }

    public String generateDescription(String movieTitle) {
        String system = "Tu es un critique cinéma. Donne une courte description, sans spoiler.";
        String user = "Décris le film '" + movieTitle + "' en 2 à 3 phrases maximum avec un style immersif.";
        return ollama.chat(system, user);
    }

    public void mark(String title, String status) {
        JsonStorage.addOrUpdate(title, status);
    }

    private String randomPlatform() {
        String[] p = {"Netflix", "Prime Video", "Disney+", "Canal+", "Apple TV+"};
        return p[random.nextInt(p.length)] + " (à vérifier)";
    }

    private String normalizeTitle(String s) {
        if (s == null) return "";
        String line = s.split("\n")[0].trim();
        return line.replaceAll("^[-•🎥\s]+", "").trim();
    }
}
