package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.util.JsonStorage;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Tool3Panel extends JPanel {

    private final MovieRecommenderService service;
    private final MainFrame parentFrame;

    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JTextArea desc = new JTextArea(10, 40);

    private final JButton refresh = new JButton("↻ Rafraîchir");
    private final JButton describe = new JButton("📝 Générer description");
    private final JButton remove = new JButton("🗑️ Retirer de la liste");
    private final JButton backBtn = new JButton("⬅ Retour au menu");

    public Tool3Panel(MovieRecommenderService service, MainFrame parent) {
        this.service = service;
        this.parentFrame = parent;
        setLayout(new BorderLayout(10,10));

        // --- Bouton retour ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(backBtn, BorderLayout.WEST);

        JPanel topButtons = new JPanel();
        topButtons.add(refresh);
        topButtons.add(describe);
        topButtons.add(remove);
        topBar.add(topButtons, BorderLayout.CENTER);

        add(topBar, BorderLayout.NORTH);

        // --- Zone de gauche : liste des films ---
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel left = new JPanel(new BorderLayout());
        left.add(new JLabel("Ma liste ❤️", SwingConstants.CENTER), BorderLayout.NORTH);
        left.add(new JScrollPane(list), BorderLayout.CENTER);

        // --- Zone de droite : description ---
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setEditable(false);
        JPanel right = new JPanel(new BorderLayout());
        right.add(new JLabel("Description", SwingConstants.CENTER), BorderLayout.NORTH);
        right.add(new JScrollPane(desc), BorderLayout.CENTER);

        // --- Conteneur principal ---
        add(left, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);

        // --- Actions des boutons ---
        refresh.addActionListener(e -> loadWishlist());
        describe.addActionListener(e -> generateForSelection());
        remove.addActionListener(e -> removeSelection());
        backBtn.addActionListener(e -> parentFrame.showCard("home"));

        list.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) desc.setText(""); });

        loadWishlist();
    }

    private void loadWishlist() {
        model.clear();
        List<String> envies = JsonStorage.getByStatus("envie");
        for (String t : envies) model.addElement(t);
    }

    private void generateForSelection() {
        String t = list.getSelectedValue();
        if (t == null) return;
        setBusy(true);
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() { return service.generateDescription(t); }
            @Override protected void done() {
                try { desc.setText(get()); }
                catch (Exception ex) { desc.setText("Erreur: " + ex.getMessage()); }
                finally { setBusy(false); }
            }
        }.execute();
    }

    private void removeSelection() {
        String t = list.getSelectedValue();
        if (t == null) return;
        JsonStorage.addOrUpdate(t, "pas_interesse");
        loadWishlist();
    }

    private void setBusy(boolean b) {
        refresh.setEnabled(!b);
        describe.setEnabled(!b);
        remove.setEnabled(!b);
        list.setEnabled(!b);
        backBtn.setEnabled(!b);
    }
}
