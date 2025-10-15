package app.cinematch;

import app.cinematch.ui.swing.MainFrame;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(new FlatDarkLaf()); } catch (Exception ignored) {}
            new MainFrame(
                new MovieRecommenderService(
                    System.getenv().getOrDefault("OLLAMA_BASE_URL","http://localhost:11434"),
                    System.getenv().getOrDefault("OLLAMA_MODEL","qwen2.5:7b-instruct"),
                    System.getenv("TMDB_API_KEY"),
                    System.getenv().getOrDefault("TMDB_REGION","FR")
                )
            ).setVisible(true);
        });
    }
}
