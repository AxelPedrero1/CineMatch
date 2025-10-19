package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.model.Recommendation;
import app.cinematch.util.ImageLoader;
import app.cinematch.util.JsonStorage;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class Tool2Panel extends JPanel {

    private final MovieRecommenderService service;
    private final MainFrame parentFrame;

    private final JEditorPane descPane = new JEditorPane("text/html", "");
    private SwingWorker<String, Void> descWorker;

    private final JLabel poster = new JLabel("", SwingConstants.CENTER);
    private final JLabel title = new JLabel("—", SwingConstants.CENTER);
    private final JLabel reason = new JLabel("—", SwingConstants.CENTER);
    private final JLabel platform = new JLabel("—", SwingConstants.CENTER);

    private final JButton likeBtn = new JButton("Je veux voir");
    private final JButton nopeBtn = new JButton("Pas intéressé");
    private final JButton seenBtn = new JButton("Déjà vu");
    private final JButton nextBtn = new JButton("Proposer autre");
    private final JButton backBtn = new JButton("Retour");

    private Recommendation current;

    private static final Color NEON_PINK      = new Color(255, 64, 160);
    private static final Color NEON_PINK_DARK = new Color(200, 30, 120);
    private static final Color HOVER_PINK_TXT = new Color(255, 210, 230);
    private static final Color BASE_CARD_BG   = new Color(30, 30, 40);
    private static final Color HOVER_CARD_BG  = new Color(50, 40, 60);
    private static final Color BG_TOP         = new Color(18, 18, 24);
    private static final Color BG_BOTTOM      = new Color(35, 20, 40);

    public Tool2Panel(MovieRecommenderService service, MainFrame parent) {
        this.service = service;
        this.parentFrame = parent;
        setLayout(new BorderLayout(10,10));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 20, 20, 20));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JPanel leftTop = new JPanel(); leftTop.setOpaque(false);
        styleBackOutlined(backBtn);
        backBtn.addActionListener(e -> parentFrame.showCard("home"));
        leftTop.add(backBtn);
        topBar.add(leftTop, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);

        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Color.WHITE);

        JPanel center = new JPanel(new BorderLayout(8,8));
        center.setOpaque(false);
        center.add(title, BorderLayout.NORTH);

        descPane.setEditable(false);
        descPane.setOpaque(false);
        descPane.setBorder(new EmptyBorder(10, 24, 10, 24));
        JScrollPane descScroll = new JScrollPane(descPane);
        descScroll.setBorder(null);
        descScroll.getViewport().setOpaque(false);
        descScroll.setOpaque(false);
        center.add(descScroll, BorderLayout.CENTER);

        JPanel info = new JPanel(new GridLayout(2,1,0,4));
        info.setOpaque(false);
        styleInfoLabel(reason);
        styleInfoLabel(platform);
        info.add(reason);
        info.add(platform);
        center.add(info, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        styleNeonButton(likeBtn);
        styleNeonButton(nopeBtn);
        styleNeonButton(seenBtn);
        styleNeonButton(nextBtn);
        actions.add(likeBtn);
        actions.add(nopeBtn);
        actions.add(seenBtn);
        actions.add(nextBtn);
        add(actions, BorderLayout.SOUTH);

        nextBtn.addActionListener(e -> proposeNext());
        likeBtn.addActionListener(e -> onLike());
        nopeBtn.addActionListener(e -> onNope());
        seenBtn.addActionListener(e -> onSeen());

        proposeNext();
    }

    private void proposeNext() {
        setBusy(true);
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
                    title.setText(current.title());
                    reason.setText(current.reason());
                    platform.setText(current.platform());
                    poster.setIcon(current.posterUrl() != null
                            ? ImageLoader.loadPoster(current.posterUrl(), 400, 500)
                            : null);
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

    private void onLike() {
        if (current == null) return;
        service.mark(current.title(), "envie");
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
        String html = """
        <html>
          <body style="margin:0;padding:0;">
            <div style="display:flex;align-items:center;justify-content:center;min-height:240px;">
              <div style="
                  text-align:center;
                  font-size:18px;
                  line-height:1.5;
                  border:2px solid rgba(255,255,255,0.35);
                  border-radius:12px;
                  padding:20px 40px;
                  max-width:84%%;
                  background-color:rgba(255,255,255,0.05);
                  box-shadow:0 0 15px rgba(0,0,0,0.35);
                  color:#f5f5f5;
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

    private static String htmlCenterBig(String text) { return text.replace("\n", "<br/>"); }
    private static String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private void styleNeonButton(JButton b) {
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(BASE_CARD_BG);
        b.setOpaque(true);
        EmptyBorder pad = new EmptyBorder(10,16,10,16);
        b.setBorder(new CompoundBorder(
                new LineBorder(NEON_PINK_DARK, 2, true),
                new CompoundBorder(new LineBorder(NEON_PINK, 1, true), pad)
        ));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(HOVER_CARD_BG);
                b.setForeground(HOVER_PINK_TXT);
                b.setBorder(new CompoundBorder(
                        new LineBorder(NEON_PINK, 2, true),
                        new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad)
                ));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(BASE_CARD_BG);
                b.setForeground(Color.WHITE);
                b.setBorder(new CompoundBorder(
                        new LineBorder(NEON_PINK_DARK, 2, true),
                        new CompoundBorder(new LineBorder(NEON_PINK, 1, true), pad)
                ));
            }
        });
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
    }

    private void styleBackOutlined(JButton b) {
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setForeground(new Color(220,220,220));
        EmptyBorder pad = new EmptyBorder(6,12,6,12);
        b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setForeground(Color.WHITE);
                b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 2, true), pad));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setForeground(new Color(220,220,220));
                b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
            }
        });
    }

    private void styleInfoLabel(JLabel l) {
        l.setForeground(new Color(235,235,235));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        l.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }
}
