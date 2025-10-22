package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.model.Recommendation;
import app.cinematch.util.JsonStorage;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.LayerUI;

public final class Tool2Panel extends JPanel {

    private final MovieRecommenderService service;
    private final Consumer<String> navigator;

    private final JEditorPane descPane = new JEditorPane("text/html", "");
    private SwingWorker<String, Void> descWorker;

    private final JLabel title = new JLabel("—", SwingConstants.CENTER);
    private final JLabel reason = new JLabel("—", SwingConstants.CENTER);
    private final JLabel platform = new JLabel("—", SwingConstants.CENTER);

    private final JButton likeBtn = new JButton("Je veux voir");
    private final JButton nopeBtn = new JButton("Pas intéressé");
    private final JButton seenBtn = new JButton("Déjà vu");
    private final JButton backBtn = new JButton("Retour");

    private final PopSparkLayerUI popUI = new PopSparkLayerUI();
    private final ShakeLayerUI shakeUI = new ShakeLayerUI();
    private final JLayer<JComponent> likeLayer = new JLayer<>(likeBtn, popUI);
    private final JLayer<JComponent> nopeLayer = new JLayer<>(nopeBtn, shakeUI);

    private Recommendation current;

    private static final Color NEON_PINK = new Color(255, 64, 160);
    private static final Color NEON_PINK_DARK = new Color(200, 30, 120);
    private static final Color HOVER_PINK_TXT = new Color(255, 210, 230);
    private static final Color BASE_CARD_BG = new Color(30, 30, 40);
    private static final Color HOVER_CARD_BG = new Color(50, 40, 60);
    private static final Color BG_TOP = new Color(18, 18, 24);
    private static final Color BG_BOTTOM = new Color(35, 20, 40);

    public Tool2Panel(final MovieRecommenderService service,
                      final Consumer<String> navigator) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.navigator = Objects.requireNonNull(navigator, "navigator must not be null");

        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 20, 20, 20));

        final JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        final JPanel leftTop = new JPanel();
        leftTop.setOpaque(false);
        styleBackOutlined(backBtn);
        backBtn.addActionListener(e -> this.navigator.accept("home"));
        leftTop.add(backBtn);
        topBar.add(leftTop, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);

        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Color.WHITE);

        final JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setOpaque(false);
        center.add(title, BorderLayout.NORTH);

        descPane.setEditable(false);
        descPane.setOpaque(false);
        descPane.setBorder(new EmptyBorder(10, 24, 10, 24));
        final JScrollPane descScroll = new JScrollPane(descPane);
        descScroll.setBorder(null);
        descScroll.getViewport().setOpaque(false);
        descScroll.setOpaque(false);
        center.add(descScroll, BorderLayout.CENTER);

        final JPanel info = new JPanel(new GridLayout(2, 1, 0, 4));
        info.setOpaque(false);
        styleInfoLabel(reason);
        styleInfoLabel(platform);
        info.add(reason);
        info.add(platform);
        center.add(info, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        final JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        final JPanel midActions = new JPanel();
        midActions.setOpaque(false);
        styleNeonPrimary(likeBtn);
        styleNeonPrimary(nopeBtn);
        midActions.add(likeLayer);
        midActions.add(nopeLayer);
        footer.add(midActions, BorderLayout.CENTER);

        final JPanel bottomBar = new JPanel();
        bottomBar.setOpaque(false);
        styleNeonButton(seenBtn);
        bottomBar.add(seenBtn);
        footer.add(bottomBar, BorderLayout.SOUTH);

        add(footer, BorderLayout.SOUTH);

        likeBtn.addActionListener(e -> onLike());
        nopeBtn.addActionListener(e -> onNope());
        seenBtn.addActionListener(e -> onSeen());

        proposeNext();
    }

    private void proposeNext() {
        setBusy(true);
        if (descWorker != null && !descWorker.isDone()) {
            descWorker.cancel(true);
        }
        setDescHtml("<i>Génération de la proposition…</i>");
        new SwingWorker<Recommendation, Void>() {
            @Override
            protected Recommendation doInBackground() {
                Recommendation rec;
                int guard = 0;
                do {
                    rec = service.recommendRandom();
                    guard++;
                } while (JsonStorage.getByStatus("envie").contains(rec.title()) && guard < 6);
                return rec;
            }

            @Override
            protected void done() {
                try {
                    current = get();
                    title.setText(current.title());
                    reason.setText(current.reason());
                    platform.setText(current.platform());
                    startDescriptionForCurrent();
                } catch (final Exception ex) {
                    title.setText("Erreur: " + ex.getMessage());
                    setDescHtml("<i>Description indisponible.</i>");
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void startDescriptionForCurrent() {
        if (current == null) {
            return;
        }
        final String titleAtStart = current.title();
        setDescHtml("<i>Génération de la description…</i>");
        if (descWorker != null && !descWorker.isDone()) {
            descWorker.cancel(true);
        }
        descWorker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return service.generateDescription(titleAtStart);
            }

            @Override
            protected void done() {
                if (current == null || !current.title().equals(titleAtStart)) {
                    return;
                }
                try {
                    if (!isCancelled()) {
                        final String txt = get();
                        setDescHtml(htmlCenterBig(htmlEscape(txt)));
                    }
                } catch (final Exception ex) {
                    if (!isCancelled()) {
                        setDescHtml("<i>Description indisponible.</i>");
                    }
                }
            }
        };
        descWorker.execute();
    }

    private void onLike() {
        if (current == null) {
            return;
        }
        service.mark(current.title(), "envie");
        setBusy(true);
        runLikeAnimation(() -> {
            setBusy(false);
            proposeNext();
        });
    }

    private void onNope() {
        if (current == null) {
            return;
        }
        service.mark(current.title(), "pas_interesse");
        setBusy(true);
        runNopeAnimation(() -> {
            setBusy(false);
            proposeNext();
        });
    }

    private void onSeen() {
        if (current == null) {
            return;
        }
        service.mark(current.title(), "deja_vu");
        proposeNext();
    }

    private void setBusy(final boolean busy) {
        likeBtn.setEnabled(!busy);
        nopeBtn.setEnabled(!busy);
        seenBtn.setEnabled(!busy);
        backBtn.setEnabled(!busy);
    }

    /** Pas de String.format / formatted : portable et sans warning SpotBugs. */
    private void setDescHtml(final String htmlInner) {
        final String ls = System.lineSeparator();
        final String html =
                "<html>" + ls
                        + "  <body style=\"margin:0;padding:0;\">" + ls
                        + "    <div style=\"display:flex;align-items:center;justify-content:center;min-height:240px;\">" + ls
                        + "      <div style=\"text-align:center;font-size:18px;line-height:1.5;border:2px solid rgba(255,255,255,0.35);"
                        + "border-radius:12px;padding:20px 40px;max-width:84%;background-color:rgba(255,255,255,0.05);"
                        + "box-shadow:0 0 15px rgba(0,0,0,0.35);color:#f5f5f5;\">" + ls
                        + htmlInner + ls
                        + "      </div>" + ls
                        + "    </div>" + ls
                        + "  </body>" + ls
                        + "</html>";
        descPane.setText(html);
        descPane.setCaretPosition(0);
    }

    private static String htmlCenterBig(final String text) {
        return text.replace("\n", "<br/>");
    }

    private static String htmlEscape(final String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void styleNeonButton(final JButton b) {
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(BASE_CARD_BG);
        b.setOpaque(true);
        final EmptyBorder pad = new EmptyBorder(10, 16, 10, 16);
        b.setBorder(new CompoundBorder(
                new LineBorder(NEON_PINK_DARK, 2, true),
                new CompoundBorder(new LineBorder(NEON_PINK, 1, true), pad)
        ));
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
    }

    private void styleNeonPrimary(final JButton b) {
        styleNeonButton(b);
        b.setFont(new Font("Segoe UI", Font.BOLD, 18));
        b.setPreferredSize(new Dimension(220, 56));
    }

    private void styleBackOutlined(final JButton b) {
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setForeground(new Color(220, 220, 220));
        final EmptyBorder pad = new EmptyBorder(6, 12, 6, 12);
        b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    }

    private void styleInfoLabel(final JLabel l) {
        l.setForeground(new Color(235, 235, 235));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        l.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void runLikeAnimation(final Runnable onDone) {
        popUI.start(likeLayer, onDone);
    }

    private void runNopeAnimation(final Runnable onDone) {
        shakeUI.start(nopeLayer, onDone);
    }

    /** Animation de “pop” + étincelles. */
    private static final class PopSparkLayerUI extends LayerUI<JComponent>
            implements ActionListener {

        private static final int SPARK_COUNT = 10; // ← static final (perf)

        private javax.swing.Timer timer;
        private long start;
        private int duration = 420;
        private float progress;
        private JLayer<? extends JComponent> layer;
        private Runnable onDone;

        private final double[] angles = new double[SPARK_COUNT];
        private final double[] speed = new double[SPARK_COUNT];
        private final Random rng = new Random();

        void start(final JLayer<? extends JComponent> lyr, final Runnable done) {
            this.layer = lyr;
            this.onDone = done;
            this.start = System.currentTimeMillis();
            for (int i = 0; i < SPARK_COUNT; i++) {
                angles[i] = rng.nextDouble() * Math.PI * 2;
                speed[i] = 40 + rng.nextDouble() * 40;
            }
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            timer = new javax.swing.Timer(16, this);
            timer.start();
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final long t = System.currentTimeMillis() - start;
            progress = Math.min(1f, t / (float) duration);
            if (layer != null) {
                layer.repaint();
            }
            if (progress >= 1f) {
                timer.stop();
                if (onDone != null) {
                    SwingUtilities.invokeLater(onDone);
                }
            }
        }

        @Override
        public void paint(final Graphics g, final JComponent c) {
            final JLayer<?> jlayer = (JLayer<?>) c;
            final JComponent view = (JComponent) jlayer.getView();

            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            final int w = view.getWidth();
            final int h = view.getHeight();
            final int cx = w / 2;
            final int cy = h / 2;

            final double t = progress;
            final double s = t < 0.5 ? 1 + 0.1 * (t / 0.5) : 1.1 - 0.1 * ((t - 0.5) / 0.5);

            final AffineTransform old = g2.getTransform();
            g2.translate(cx, cy);
            g2.scale(s, s);
            g2.translate(-cx, -cy);
            view.paint(g2);
            g2.setTransform(old);

            final float alpha = (float) (1.0 - progress);
            for (int i = 0; i < SPARK_COUNT; i++) {
                final double ang = angles[i];
                final double radius = speed[i] * progress * 0.6;
                final int x = cx + (int) Math.round(Math.cos(ang) * radius);
                final int y = cy + (int) Math.round(Math.sin(ang) * radius);
                final int size = 4 + (int) (4 * (1.0 - progress));
                g2.setColor(new Color(255, 64, 160, (int) (200 * alpha)));
                g2.fillOval(x - size / 2, y - size / 2, size, size);
            }
            g2.dispose();
        }
    }

    /** Animation “shake”. */
    private static final class ShakeLayerUI extends LayerUI<JComponent>
            implements ActionListener {

        private javax.swing.Timer timer;
        private long start;
        private int duration = 280;
        private float progress;
        private JLayer<? extends JComponent> layer;
        private Runnable onDone;

        void start(final JLayer<? extends JComponent> lyr, final Runnable done) {
            this.layer = lyr;
            this.onDone = done;
            this.start = System.currentTimeMillis();
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            timer = new javax.swing.Timer(16, this);
            timer.start();
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final long t = System.currentTimeMillis() - start;
            progress = Math.min(1f, t / (float) duration);
            if (layer != null) {
                layer.repaint();
            }
            if (progress >= 1f) {
                timer.stop();
                if (onDone != null) {
                    SwingUtilities.invokeLater(onDone);
                }
            }
        }

        @Override
        public void paint(final Graphics g, final JComponent c) {
            final Graphics2D g2 = (Graphics2D) g.create();
            if (progress > 0f) {
                final double amp = 8.0 * (1.0 - progress);
                final double x = Math.sin(progress * Math.PI * 6) * amp;
                g2.translate(x, 0);
            }
            super.paint(g2, c);
            g2.dispose();
        }
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        final int w = getWidth();
        final int h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }
}
