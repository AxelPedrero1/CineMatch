package app.cinematch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmRequest(String model, List<LlmMessage> messages, boolean stream) {

    // Canonical constructor avec copie défensive
    public LlmRequest(String model, List<LlmMessage> messages, boolean stream) {
        this.model = model;
        this.messages = List.copyOf(messages != null ? messages : List.of());
        this.stream = stream;
    }

    // Convenience constructor
    public LlmRequest(String model, List<LlmMessage> messages) {
        this(model, messages, false);
    }

    // Accessor qui ne fuite pas l'interne (retourne une vue immuable/copie)
    @Override
    public List<LlmMessage> messages() {
        // messages est déjà immuable via List.copyOf, mais on peut renvoyer une copie immuable par prudence
        return List.copyOf(this.messages);
    }
}
