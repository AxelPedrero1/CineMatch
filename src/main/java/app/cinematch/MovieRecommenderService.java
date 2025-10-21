package app.cinematch;

import app.cinematch.api.OllamaClient;
import app.cinematch.model.Recommendation;
import app.cinematch.util.JsonStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.Random;

public class MovieRecommenderService {

    private static final ObjectMapper PARSER = new ObjectMapper();

    private final OllamaClient ollama;
    private final Random random = new Random();

    public MovieRecommenderService(String baseUrl, String model) {
        this.ollama = new OllamaClient(baseUrl, model);
    }

    public Recommendation recommendFromLike(String likedTitle) {
        String system = "Tu es un assistant cinéma ultra créatif. Tu connais les films existants et tu peux aussi imaginer " +
                "un faux service de streaming crédible. Réponds toujours en JSON strict, sans texte supplémentaire.";
        String user = "Film apprécié : '" + likedTitle + "'. Propose une recommandation nuancée avec ce format JSON : " +
                "{\"title\":\"Titre exact\",\"pitch\":\"Pourquoi ce choix\",\"year\":\"(optionnel)\",\"platform\":\"Plateforme fictive ou réelle\"}." +
                " Le pitch doit faire le lien avec le film donné.";

        Recommendation rec = requestRecommendation(system, user, "Inspiré de " + likedTitle);
        String reason = rec.reason();
        if (!reason.toLowerCase().contains(likedTitle.toLowerCase())) {
            reason = reason + " — Inspiré de " + likedTitle;
        }
        return new Recommendation(rec.title(), reason, rec.platform(), null);
    }

    public Recommendation recommendRandom() {
        String system = "Tu es un programmateur de ciné-club. Suggère un film ou une pépite à découvrir. " +
                "Réponds uniquement avec un JSON strict.";
        String user = "Génère une idée de film à regarder avec ce format : {\"title\":\"...\",\"pitch\":\"...\",\"year\":\"(optionnel)\",\"platform\":\"Plateforme fictive ou réelle\"}." +
                " Le pitch doit donner envie.";
        return requestRecommendation(system, user, "Suggestion IA");
    }

    public String generateDescription(String movieTitle) {
        String system = "Tu es un critique cinéma. Donne une courte description, sans spoiler.";
        String user = "Décris le film '" + movieTitle + "' en 2 à 3 phrases maximum avec un style immersif.";
        return ollama.chat(system, user);
    }

    public void mark(String title, String status) {
        JsonStorage.addOrUpdate(title, status);
    }

    private Recommendation requestRecommendation(String system, String user, String defaultReason) {
        String raw = ollama.chat(system, user).trim();
        ParsedRecommendation parsed = parse(raw);

        String title = firstNonBlank(parsed.title, extractFirstMeaningfulLine(raw), "Suggestion mystère");
        String pitch = firstNonBlank(parsed.pitch, defaultReason);
        if (parsed.year != null && !parsed.year.isBlank()) {
            pitch = pitch + " (année suggérée : " + parsed.year.trim() + ")";
        }
        String platform = firstNonBlank(parsed.platform, fallbackPlatform());

        return new Recommendation(title, pitch, platform, null);
    }

    private ParsedRecommendation parse(String raw) {
        if (raw == null || raw.isBlank()) return new ParsedRecommendation();
        String json = extractJsonObject(raw);
        if (json == null) return new ParsedRecommendation();
        try {
            JsonNode node = PARSER.readTree(json);
            ParsedRecommendation parsed = new ParsedRecommendation();
            parsed.title = Optional.ofNullable(node.get("title")).map(JsonNode::asText).orElse(null);
            parsed.pitch = Optional.ofNullable(node.get("pitch")).map(JsonNode::asText).orElse(null);
            parsed.year = Optional.ofNullable(node.get("year")).map(JsonNode::asText).orElse(null);
            parsed.platform = Optional.ofNullable(node.get("platform")).map(JsonNode::asText).orElse(null);
            return parsed;
        } catch (Exception ignored) {
            return new ParsedRecommendation();
        }
    }

    private String extractJsonObject(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return null;
    }

    private String extractFirstMeaningfulLine(String raw) {
        if (raw == null) return "";
        String[] lines = raw.split("\\R");
        for (String line : lines) {
            String cleaned = line.replaceAll("^[\t•\\-:\\s]+", "").trim();
            if (!cleaned.isEmpty()) return cleaned;
        }
        return "";
    }

    private String fallbackPlatform() {
        String[] options = {"Cinéma du Coin+", "StreamFiction", "Club Cinéphile", "Festival Replay"};
        return options[random.nextInt(options.length)];
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) return trimmed;
            }
        }
        return "";
    }

    private static class ParsedRecommendation {
        String title;
        String pitch;
        String year;
        String platform;
    }
}