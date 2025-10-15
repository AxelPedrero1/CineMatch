package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.model.Recommendation;
import app.cinematch.util.ImageLoader;

import javax.swing.*;
import java.awt.*;

public class Tool1Panel extends JPanel {

    private final MovieRecommenderService service;
    private final MainFrame parentFrame;

    private final JTextField input = new JTextField();
    private final JButton propose = new JButton("Proposer");
    private final JLabel poster = new JLabel("", SwingConstants.CENTER);
    private final JLabel title = new JLabel("—", SwingConstants.CENTER);
    private final JLabel reason = new JLabel("—", SwingConstants.CENTER);
    private final JLabel platform = new JLabel("—", SwingConstants.CENTER);
    private final JButton addWishlist = new JButton("Ajouter à ma liste ❤️");
    private final JButton descBtn = new JButton("Générer description");
    private final JButton backBtn = new JButton("⬅ Retour au menu");

    private Recommendation current;

    public Tool1Panel(MovieRecommenderService service, MainFrame parent) {
        this.service = service;
        this.parentFrame = parent;
        setLayout(new BorderLayout(10,10));

        // --- Barre du haut ---
        JPanel topBar = new JPanel(new BorderLayout(8,8));
        topBar.add(backBtn, BorderLayout.WEST);
        JPanel topInput = new JPanel(new BorderLayout(8,8));
        topInput.add(new JLabel("Film aimé : "), BorderLayout.WEST);
        topInput.add(input, BorderLayout.CENTER);
        topInput.add(propose, BorderLayout.EAST);
        topBar.add(topInput, BorderLayout.CENTER);
        add(topBar, BorderLayout.NORTH);

        // --- Zone centrale ---
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JPanel center = new JPanel(new BorderLayout(8,8));
        center.add(title, BorderLayout.NORTH);
        center.add(poster, BorderLayout.CENTER);

        JPanel info = new JPanel(new GridLayout(2,1));
        info.add(reason);
        info.add(platform);
        center.add(info, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);

        // --- Bas de page ---
        JPanel bottom = new JPanel();
        bottom.add(addWishlist);
        bottom.add(descBtn);
        add(bottom, BorderLayout.SOUTH);

        // --- Actions ---
        propose.addActionListener(e -> onPropose());
        addWishlist.addActionListener(e -> onAdd());
        descBtn.addActionListener(e -> onDescribe());
        backBtn.addActionListener(e -> parentFrame.showCard("home"));
    }

    private void onPropose() {
        String liked = input.getText().trim();
        if (liked.isEmpty()) return;
        setBusy(true);
        new SwingWorker<Recommendation, Void>() {
            @Override protected Recommendation doInBackground() {
                return service.recommendFromLike(liked);
            }
            @Override protected void done() {
                try {
                    current = get();
                    title.setText("🎥 " + current.title());
                    reason.setText("💬 " + current.reason());
                    platform.setText("📺 " + current.platform());
                    if (current.posterUrl() != null) {
                        poster.setIcon(ImageLoader.loadPoster(current.posterUrl(), 400, 500));
                    } else {
                        poster.setIcon(null);
                    }
                } catch (Exception ex) {
                    title.setText("Erreur: " + ex.getMessage());
                } finally { setBusy(false); }
            }
        }.execute();
    }

    private void onAdd() {
        if (current == null) return;
        service.mark(current.title(), "envie");
        JOptionPane.showMessageDialog(this, "Ajouté à la liste 'envie'.");
    }

    private void onDescribe() {
        if (current == null) return;
        setBusy(true);
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() { return service.generateDescription(current.title()); }
            @Override protected void done() {
                try { JOptionPane.showMessageDialog(Tool1Panel.this, get(), "Description", JOptionPane.INFORMATION_MESSAGE); }
                catch (Exception ex) { JOptionPane.showMessageDialog(Tool1Panel.this, ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE); }
                finally { setBusy(false); }
            }
        }.execute();
    }

    private void setBusy(boolean b) {
        propose.setEnabled(!b);
        addWishlist.setEnabled(!b);
        descBtn.setEnabled(!b);
        input.setEnabled(!b);
        backBtn.setEnabled(!b);
    }
}
