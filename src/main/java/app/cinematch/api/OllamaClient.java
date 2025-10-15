package app.cinematch.api;

import app.cinematch.model.LlmMessage;
import app.cinematch.model.LlmRequest;
import app.cinematch.model.LlmResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class OllamaClient {
    private final String baseUrl;
    private final String model;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaClient(String baseUrl, String model) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl;
        this.model = model;
    }

    public String chat(String system, String user) {
        try {
            var req = new LlmRequest(model, java.util.List.of(
                new LlmMessage("system", system),
                new LlmMessage("user", user)
            ));
            String json = mapper.writeValueAsString(req);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
            LlmResponse resp = mapper.readValue(res.body(), LlmResponse.class);
            return resp.message() != null ? resp.message().content() : "[vide]";
        } catch (Exception e) {
            return "[Erreur Ollama] " + e.getMessage();
        }
    }
}
