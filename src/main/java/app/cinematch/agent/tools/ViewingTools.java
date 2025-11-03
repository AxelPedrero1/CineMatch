package app.cinematch.agent.tools;

import app.cinematch.MovieRecommenderService;
import app.cinematch.util.JsonStorage;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ViewingTools {

    private final MovieRecommenderService service;

    public ViewingTools(MovieRecommenderService service) {
        this.service = service;
    }

    @Tool("Propose le prochain film à regarder à partir de la wishlist. " +
            "strategy='random' ou 'first'. withDescription='true' pour inclure une courte description.")
    public String pickNextToWatch(@P("strategy") String strategy, @P("withDescription") String withDescription) {
        // copie défensive -> liste mutable
        List<String> wl = new ArrayList<>(JsonStorage.getByStatus("envie"));

        wl.removeIf(s -> s == null || s.trim().isEmpty());
        if (wl.isEmpty()) return "NEXT:EMPTY";

        String pick;
        if ("first".equalsIgnoreCase(strategy)) {
            pick = wl.get(0);
        } else {
            pick = wl.get(ThreadLocalRandom.current().nextInt(wl.size()));
        }
        pick = pick.replaceAll("[\"“”«»]", "").trim();

        if ("true".equalsIgnoreCase(withDescription)) {
            String desc = service.generateDescription(pick);
            return "NEXT:" + pick + " | " + desc;
        }
        return "NEXT:" + pick;
    }
}
