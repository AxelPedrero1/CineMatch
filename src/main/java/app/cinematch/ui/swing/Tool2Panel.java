package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.model.Recommendation;
import app.cinematch.util.ImageLoader;
import app.cinematch.util.JsonStorage;

import javax.swing.*;
import java.awt.*;

public class Tool2Panel extends JPanel {

    private final MovieRecommenderService service;
    private final MainFrame parentFrame;

    private final JLabel poster = new JLabel("", SwingConstants.CENTER);
    private final JLabel title = new JLabel("—", SwingConstants.CENTER);
    private final JLabel reason = new JLabel("—", SwingConstants.CENTER);
    private final JLabel platform = new JLabel("—", SwingConstants.CENTER);

    private final JButton likeBtn = new JButton("❤️ Je veux voir");
    private final JButton nopeBtn = new JButton("❌ Pas intéressé");
    private final JButton seenBtn = new JButton("👁️ Déjà vu");
    private final JButton nextBtn = new JButton("🔄 Proposer autre");
    private final JButton backBtn = new JButton("⬅ Retour au menu");

    private Recommendation current;

    public Tool2Panel(MovieRecommenderService service, MainFrame parent) {
        this.service = service;
        this.parentFrame = parent;
        setLayout(new BorderLayout(10,10));

        // --- Barre du haut avec bouton retour ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(backBtn, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);

        // --- Zone centrale ---
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JPanel center = new JPanel(new BorderLayout(8,8));
        center.add(title, BorderLayout.NORTH);
        center.add(poster, BorderLayout.CENTER);

        JPanel info = new JPanel(new GridLayout(2,1));
        info.add(reason);
        info.add(platform);
        center.add(info, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);

        // --- Boutons d’action ---
        JPanel actions = new JPanel();
        actions.add(likeBtn);
        actions.add(nopeBtn);
        actions.add(seenBtn);
        actions.add(nextBtn);
        add(actions, BorderLayout.SOUTH);

        // --- Actions des boutons ---
        backBtn.addActionListener(e -> parentFrame.showCard("home"));
        nextBtn.addActionListener(e -> proposeNext());
        likeBtn.addActionListener(e -> onLike());
        nopeBtn.addActionListener(e -> onNope());
        seenBtn.addActionListener(e -> onSeen());

        // --- Première proposition automatique ---
        proposeNext();
    }

    private void proposeNext() {
        setBusy(true);
        new SwingWorker<Recommendation, Void>() {
            @Override protected Recommendation doInBackground() {
                Recommendation rec;
                int guard = 0;
                // éviter de reproposer un film déjà dans la liste "envie"
                do {
                    rec = service.recommendRandom();
                    guard++;
                } while (JsonStorage.getByStatus("envie").contains(rec.title()) && guard < 6);
                return rec;
            }
            @Override protected void done() {
                try {
                    current = get();
                    title.setText("🎥 " + current.title());
                    reason.setText("💬 " + current.reason());
                    platform.setText("📺 " + current.platform());
                    poster.setIcon(current.posterUrl() != null
                            ? ImageLoader.loadPoster(current.posterUrl(), 400, 500)
                            : null);
                } catch (Exception ex) {
                    title.setText("Erreur: " + ex.getMessage());
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void onLike() {
        if (current == null) return;
        service.mark(current.title(), "envie");
        setBusy(true);
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() { return service.generateDescription(current.title()); }
            @Override protected void done() {
                try {
                    JOptionPane.showMessageDialog(Tool2Panel.this, get(),
                            "Ajouté à ma liste ❤️", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(Tool2Panel.this, ex.getMessage(),
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setBusy(false);
                    proposeNext();
                }
            }
        }.execute();
    }

    private void onNope() {
        if (current == null) return;
        service.mark(current.title(), "pas_interesse");
        proposeNext();
    }

    private void onSeen() {
        if (current == null) return;
        service.mark(current.title(), "deja_vu");
        proposeNext();
    }

    private void setBusy(boolean b) {
        likeBtn.setEnabled(!b);
        nopeBtn.setEnabled(!b);
        seenBtn.setEnabled(!b);
        nextBtn.setEnabled(!b);
        backBtn.setEnabled(!b);
    }
}
