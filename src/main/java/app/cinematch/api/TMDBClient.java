package app.cinematch.api;

import app.cinematch.model.Movie;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public class TMDBClient {
    private final String apiKey;
    private final String region;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public TMDBClient(String apiKey, String region) {
        this.apiKey = apiKey;
        this.region = region != null ? region : "FR";
    }

    public Optional<Integer> searchMovieId(String title) {
        if (apiKey == null || apiKey.isBlank()) return Optional.empty();
        try {
            String q = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String url = "https://api.themoviedb.org/3/search/movie?query=" + q + "&api_key=" + apiKey + "&language=fr-FR&page=1";
            var req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return Optional.empty();
            JsonNode root = mapper.readTree(res.body());
            if (root.has("results") && root.get("results").size() > 0) {
                return Optional.of(root.get("results").get(0).path("id").asInt());
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    public Optional<Movie> similarFromTitle(String likedTitle) {
        if (apiKey == null || apiKey.isBlank()) return Optional.empty();
        try {
            var idOpt = searchMovieId(likedTitle);
            if (idOpt.isEmpty()) return Optional.empty();
            int id = idOpt.get();
            String url = "https://api.themoviedb.org/3/movie/" + id + "/similar?api_key=" + apiKey + "&language=fr-FR&page=1";
            var req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return Optional.empty();
            JsonNode root = mapper.readTree(res.body());
            if (root.has("results") && root.get("results").size() > 0) {
                JsonNode m = root.get("results").get(0);
                String date = m.path("release_date").asText("");
                String year = date.length() >= 4 ? date.substring(0,4) : "";
                return Optional.of(new Movie(
                        m.path("id").asInt(),
                        m.path("title").asText(""),
                        m.path("overview").asText(""),
                        year,
                        m.path("poster_path").asText("")
                ));
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    public Optional<String> providersFor(int movieId) {
        if (apiKey == null || apiKey.isBlank()) return Optional.of("Plateforme inconnue");
        try {
            String url = "https://api.themoviedb.org/3/movie/" + movieId + "/watch/providers?api_key=" + apiKey;
            var req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return Optional.empty();
            JsonNode root = mapper.readTree(res.body());
            JsonNode fr = root.path("results").path(region);
            if (fr.isMissingNode()) return Optional.of("Non disponible en " + region);
            StringBuilder sb = new StringBuilder();
            appendProviders(sb, fr.path("flatrate"), "SVOD");
            appendProviders(sb, fr.path("rent"), "Location");
            appendProviders(sb, fr.path("buy"), "Achat");
            String s = sb.toString().trim();
            return Optional.of(s.isEmpty() ? "Aucune plateforme listée" : s);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void appendProviders(StringBuilder sb, JsonNode arr, String label) {
        if (arr != null && arr.isArray() && arr.size() > 0) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(label).append(": ");
            for (int i=0;i<arr.size();i++) {
                if (i>0) sb.append(", ");
                sb.append(arr.get(i).path("provider_name").asText());
            }
        }
    }

    public static String posterUrl(String path) {
        if (path == null || path.isBlank()) return null;
        return "https://image.tmdb.org/t/p/w500" + path;
    }
}
