package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.model.Recommendation;
import app.cinematch.util.ImageLoader;
import app.cinematch.util.JsonStorage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class Tool2Panel extends JPanel {

    private final MovieRecommenderService service;
    private final MainFrame parentFrame;

    // Zone description centrée (HTML) + worker annulable
    private final JEditorPane descPane = new JEditorPane("text/html", "");
    private SwingWorker<String, Void> descWorker;

    private final JLabel poster = new JLabel("", SwingConstants.CENTER);   // (non affiché, conservé si besoin futur)
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

        // --- Barre du haut ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(backBtn, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);

        // --- Zone centrale ---
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JPanel center = new JPanel(new BorderLayout(8,8));
        center.add(title, BorderLayout.NORTH);

        // Description centrée au milieu (HTML + gros)
        descPane.setEditable(false);
        descPane.setOpaque(false);
        descPane.setBorder(new EmptyBorder(10, 24, 10, 24));
        JScrollPane descScroll = new JScrollPane(descPane);
        descScroll.setBorder(null);
        descScroll.getViewport().setOpaque(false);
        descScroll.setOpaque(false);

        // On n’ajoute PAS l’affiche dans le layout pour laisser la place au texte centré
        // center.add(poster, BorderLayout.CENTER);
        center.add(descScroll, BorderLayout.CENTER);

        // Bloc en dessous : Suggestion IA + Plateforme
        JPanel info = new JPanel(new GridLayout(2,1,0,4));
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

        // --- Première proposition ---
        proposeNext();
    }

    private void proposeNext() {
        setBusy(true);

        // annule une éventuelle génération précédente et nettoie l'affichage
        if (descWorker != null && !descWorker.isDone()) descWorker.cancel(true);
        setDescHtml("<i>Génération de la proposition…</i>");

        new SwingWorker<Recommendation, Void>() {
            @Override protected Recommendation doInBackground() {
                Recommendation rec;
                int guard = 0;
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

                    // Génère la description tout de suite
                    startDescriptionForCurrent();
                } catch (Exception ex) {
                    title.setText("Erreur: " + ex.getMessage());
                    setDescHtml("<i>Description indisponible.</i>");
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void startDescriptionForCurrent() {
        if (current == null) return;
        final String titleAtStart = current.title();

        setDescHtml("<i>Génération de la description…</i>");

        if (descWorker != null && !descWorker.isDone()) descWorker.cancel(true);

        descWorker = new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return service.generateDescription(titleAtStart);
            }
            @Override protected void done() {
                // si l'utilisateur a déjà swipé, on n'écrit pas une description obsolète
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

    private void onLike() {
        if (current == null) return;
        service.mark(current.title(), "envie");
        // feedback léger inline
        reason.setText("💬 Ajouté à ma liste ❤️ — " + current.reason());
        // enchaîne directement
        proposeNext();
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
        // conserver la mise en forme et les retours à la ligne
        return text.replace("\n", "<br/>");
    }

    private static String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
