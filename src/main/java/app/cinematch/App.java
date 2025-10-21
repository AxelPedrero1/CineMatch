package app.cinematch;

import app.cinematch.agent.ChatAgent;
import app.cinematch.agent.Memory;
import app.cinematch.agent.Profile;
import app.cinematch.api.OllamaClient;
import app.cinematch.MovieRecommenderService;
import app.cinematch.ui.swing.MainFrame;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } catch (Exception ignored) {}

            // 🔹 Variables d'environnement
            String ollamaUrl = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
            String ollamaModel = System.getenv().getOrDefault("OLLAMA_MODEL", "qwen2.5:7b-instruct");

            // 🔹 Service de recommandation existant
            MovieRecommenderService recommender = new MovieRecommenderService(
                    ollamaUrl, ollamaModel
            );

            // 🔹 Nouvel agent IA avec profil et mémoire persistante
            OllamaClient ollamaClient = new OllamaClient(ollamaUrl, ollamaModel);
            Profile profile = Profile.defaultCinemaExpert();
            Memory memory = new Memory();
            ChatAgent agent = new ChatAgent(ollamaClient, profile, memory);

            // 🔹 Lancement de l'interface principale
            new MainFrame(recommender, agent).setVisible(true);
        });
    }
}
