package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.model.HistoryEntry;
import app.cinematch.util.JsonStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests BDD pour HistoryPanel.
 * Objectif : vérifier le chargement, l’affichage et les interactions.
 */
public class HistoryPanelTest {

    private MainFrame dummyFrame;
    private MovieRecommenderService dummyService;

    @BeforeEach
    void setup() {
        dummyFrame = new MainFrame(null); // mock simple (on ne teste pas la frame)
        dummyService = new MovieRecommenderService("http://localhost:11434", "qwen2.5");
        JsonStorage.reset();
    }

    @AfterEach
    void teardown() {
        JsonStorage.reset();
    }

    @Test
    void givenNoHistory_whenCreatePanel_thenTableIsEmpty() {
        // GIVEN — JsonStorage vide
        JsonStorage.setMockHistory(List.of());

        // WHEN — on instancie le panneau
        HistoryPanel panel = new HistoryPanel(dummyService, dummyFrame);

        // THEN — la table doit exister mais être vide
        JTable table = (JTable) getPrivateField(panel, "table");
        assertEquals(0, ((DefaultTableModel) table.getModel()).getRowCount());
    }

    @Test
    void givenHistoryEntries_whenLoadHistory_thenTableIsPopulatedAndSortedDescending() {
        // GIVEN — une liste de trois entrées désordonnées
        JsonStorage.setMockHistory(List.of(
                new HistoryEntry("A", "liked", "2023-10-21T18:00:00Z"),
                new HistoryEntry("B", "disliked", "2025-10-21T18:00:00Z"),
                new HistoryEntry("C", "liked", "2024-10-21T18:00:00Z")
        ));

        // WHEN — création du panel (loadHistory() auto-appelé)
        HistoryPanel panel = new HistoryPanel(dummyService, dummyFrame);

        // THEN — la table doit être triée par date décroissante (B, C, A)
        JTable table = (JTable) getPrivateField(panel, "table");
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        assertEquals(3, model.getRowCount());
        assertEquals("B", model.getValueAt(0, 0));
        assertEquals("C", model.getValueAt(1, 0));
        assertEquals("A", model.getValueAt(2, 0));
    }

    @Test
    void givenUserClicksRefresh_whenLoadHistory_thenTableReloads() {
        // GIVEN — historique initial
        JsonStorage.setMockHistory(List.of(
                new HistoryEntry("Matrix", "liked", "2025-10-21T10:00:00Z")
        ));
        HistoryPanel panel = new HistoryPanel(dummyService, dummyFrame);

        // WHEN — on change le mock et on simule le clic sur "↻ Rafraîchir"
        JsonStorage.setMockHistory(List.of(
                new HistoryEntry("Inception", "liked", "2025-10-21T18:00:00Z")
        ));
        JButton refreshBtn = (JButton) getPrivateField(panel, "refresh");
        refreshBtn.doClick(); // simule un clic utilisateur

        // THEN — la table est mise à jour
        JTable table = (JTable) getPrivateField(panel, "table");
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        assertEquals("Inception", model.getValueAt(0, 0));
    }

    @Test
    void givenUserClicksBackButton_whenActionPerformed_thenCallsShowCardHome() {
        // GIVEN — création du panneau
        HistoryPanel panel = new HistoryPanel(dummyService, dummyFrame);
        JButton backBtn = (JButton) getPrivateField(panel, "backBtn");

        // WHEN — clic sur le bouton retour
        backBtn.doClick();

        // THEN — la frame doit afficher la carte "home"
        assertEquals("home", dummyFrame.lastShownCard);
    }

    // Utilitaire : récupère un champ privé pour vérifier son contenu
    private Object getPrivateField(Object obj, String name) {
        try {
            var f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) {
            fail("Erreur d'accès au champ privé " + name + ": " + e.getMessage());
            return null;
        }
    }
}
