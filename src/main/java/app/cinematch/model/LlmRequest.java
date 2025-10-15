package app.cinematch.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmRequest(String model, java.util.List<LlmMessage> messages, boolean stream) {
    public LlmRequest(String model, java.util.List<LlmMessage> messages) { this(model, messages, false); }
}
