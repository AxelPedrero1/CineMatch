package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.model.Recommendation;
import app.cinematch.util.ImageLoader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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

    // Description centrée (HTML) + worker annulable (comme Tool2)
    private final JEditorPane descPane = new JEditorPane("text/html", "");
    private SwingWorker<String, Void> descWorker;

    private final JButton addWishlist = new JButton("Ajouter à ma liste ❤️");
    private final JButton descBtn = new JButton("Regénérer description");
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

        // Description centrée (HTML) comme Tool2
        descPane.setEditable(false);
        descPane.setOpaque(false);
        descPane.setBorder(new EmptyBorder(10, 24, 10, 24));
        JScrollPane descScroll = new JScrollPane(descPane);
        descScroll.setBorder(null);
        descScroll.getViewport().setOpaque(false);
        descScroll.setOpaque(false);

        // On n’ajoute PAS le poster au centre pour laisser la place à la description centrée
        // center.add(poster, BorderLayout.CENTER);
        center.add(descScroll, BorderLayout.CENTER);

        // Bloc en dessous : raison + plateforme
        JPanel info = new JPanel(new GridLayout(2,1,0,4));
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
        descBtn.addActionListener(e -> startDescriptionForCurrent()); // regénère
        backBtn.addActionListener(e -> parentFrame.showCard("home"));
    }

    private void onPropose() {
        String liked = input.getText().trim();
        if (liked.isEmpty()) return;
        setBusy(true);

        // annule éventuelle génération précédente et nettoie
        if (descWorker != null && !descWorker.isDone()) descWorker.cancel(true);
        setDescHtml("<i>Recherche d’un film similaire…</i>");

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
                        // le poster reste dispo si un jour tu veux le remettre dans le layout
                        poster.setIcon(ImageLoader.loadPoster(current.posterUrl(), 400, 500));
                    } else {
                        poster.setIcon(null);
                    }

                    // génération automatique de la description
                    startDescriptionForCurrent();

                } catch (Exception ex) {
                    title.setText("Erreur: " + ex.getMessage());
                    setDescHtml("<i>Description indisponible.</i>");
                } finally { setBusy(false); }
            }
        }.execute();
    }

    private void startDescriptionForCurrent() {
        if (current == null) return;
        final String titleAtStart = current.title();

        setDescHtml("<i>Génération de la description…</i>");

        if (descWorker != null && !descWorker.isDone()) descWorker.cancel(true);

        descWorker = new SwingWorker<String, Void>() {
            @Override protected String doInBackground() { return service.generateDescription(titleAtStart); }
            @Override protected void done() {
                if (current == null || !current.title().equals(titleAtStart)) return;
                try {
                    if (!isCancelled()) {
                        String txt = get();
                        setDescHtml(htmlCenterBig(htmlEscape(txt)));
                    }
                } catch (Exception ex) {
                    if (!isCancelled()) setDescHtml("<i>Description indisponible.</i>");
                }
            }
        };
        descWorker.execute();
    }

    private void onAdd() {
        if (current == null) return;
        service.mark(current.title(), "envie");
        JOptionPane.showMessageDialog(this, "Ajouté à la liste 'envie'.");
    }

    private void setBusy(boolean b) {
        propose.setEnabled(!b);
        addWishlist.setEnabled(!b);
        descBtn.setEnabled(!b);
        input.setEnabled(!b);
        backBtn.setEnabled(!b);
    }

    /* --------------------- Helpers HTML (identiques à Tool2) --------------------- */

    private void setDescHtml(String htmlInner) {
        // Style global: centré, police plus grande (~18px), CADRE arrondi semi-transparent
        String html = """
            <html>
              <body style="margin:0;padding:0;">
                <div style="display:flex;align-items:center;justify-content:center;min-height:220px;">
                  <div style="
                      text-align:center;
                      font-size:18px;
                      line-height:1.5;
                      border:2px solid rgba(255,255,255,0.3);
                      border-radius:12px;
                      padding:20px 40px;
                      max-width:80%%;
                      background-color:rgba(255,255,255,0.05);
                      box-shadow:0 0 15px rgba(0,0,0,0.3);
                  ">
                    %s
                  </div>
                </div>
              </body>
            </html>
            """.formatted(htmlInner);
        descPane.setText(html);
        descPane.setCaretPosition(0);
    }

    private static String htmlCenterBig(String text) {
        // converti les retours à la ligne en <br/>
        return text.replace("\n", "<br/>");
    }

    private static String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
