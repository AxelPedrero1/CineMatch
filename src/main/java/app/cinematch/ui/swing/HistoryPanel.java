package app.cinematch.ui.swing;

import app.cinematch.model.HistoryEntry;
import app.cinematch.util.JsonStorage;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class HistoryPanel extends JPanel {

    private final JTable table = new JTable();
    private final JButton refresh = new JButton("↻ Rafraîchir");
    private final JButton backBtn = new JButton("⬅ Retour au menu");

    private final MainFrame parentFrame;

    public HistoryPanel(app.cinematch.MovieRecommenderService service, MainFrame parent) {
        this.parentFrame = parent;
        setLayout(new BorderLayout(10,10));

        // --- Barre du haut ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(backBtn, BorderLayout.WEST);
        JLabel title = new JLabel("Historique des actions", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        topBar.add(title, BorderLayout.CENTER);
        add(topBar, BorderLayout.NORTH);

        // --- Table des historiques ---
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Barre du bas ---
        JPanel bottom = new JPanel();
        bottom.add(refresh);
        add(bottom, BorderLayout.SOUTH);

        // --- Actions ---
        backBtn.addActionListener(e -> parentFrame.showCard("home"));
        refresh.addActionListener(e -> loadHistory());

        // --- Chargement initial ---
        loadHistory();
    }

    private void loadHistory() {
        List<HistoryEntry> all = JsonStorage.loadAll();
        all.sort(Comparator.comparing(HistoryEntry::dateTimeIso).reversed());

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Titre", "Statut", "Date"}, 0);
        for (HistoryEntry e : all) {
            model.addRow(new Object[]{e.title(), e.status(), e.dateTimeIso()});
        }

        table.setModel(model);
    }
}
