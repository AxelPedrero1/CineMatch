package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.model.Recommendation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Panneau Swing pour recommander un film à partir d'un film aimé et
 * générer une description, avec un style d'interface personnalisé.
 *
 * <p>Conforme Checkstyle (imports, indentation, javadoc, lignes courtes)
 * et évite les flags SpotBugs classiques.</p>
 */
public final class Tool1Panel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Couleurs de thème. */
    private static final Color NEON_PINK = new Color(255, 64, 160);
    private static final Color NEON_PINK_DARK = new Color(200, 30, 120);
    private static final Color HOVER_PINK_TXT = new Color(255, 210, 230);
    private static final Color BASE_CARD_BG = new Color(30, 30, 40);
    private static final Color HOVER_CARD_BG = new Color(50, 40, 60);
    private static final Color BG_TOP = new Color(18, 18, 24);
    private static final Color BG_BOTTOM = new Color(35, 20, 40);
    private static final Color TEXT_DIM = new Color(220, 220, 220);

    private final MovieRecommenderService service;
    private final Consumer<String> navigator;

    private final JTextField input = new JTextField();
    private final JButton propose = new JButton("Proposer");
    private final JLabel title = new JLabel("—", SwingConstants.CENTER);
    private final JLabel reason = new JLabel("—", SwingConstants.CENTER);
    private final JLabel platform = new JLabel("—", SwingConstants.CENTER);
    private final JEditorPane descPane = new JEditorPane("text/html", "");

    private SwingWorker<String, Void> descWorker;
    private final JButton addWishlist = new JButton("Ajouter à ma liste");
    private final JButton descBtn = new JButton("Régénérer description");
    private final JButton backBtn = new JButton("Retour");

    private Recommendation current;

    /**
     * Constructeur principal.
     *
     * @param service   service de recommandation (non null)
     * @param navigator callback de navigation (ex. "home") (non null)
     * @throws NullPointerException si un paramètre est null
     */
    public Tool1Panel(final MovieRecommenderService service,
                      final Consumer<String> navigator) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.navigator = Objects.requireNonNull(navigator, "navigator must not be null");

        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 20, 20, 20));

        final JPanel topBar = new JPanel(new BorderLayout(8, 8));
        topBar.setOpaque(false);

        final JPanel left = new JPanel();
        left.setOpaque(false);
        styleBackOutlined(backBtn);
        backBtn.addActionListener(e -> this.navigator.accept("home"));
        left.add(backBtn);
        topBar.add(left, BorderLayout.WEST);

        final JPanel topInput = new JPanel(new BorderLayout(8, 8));
        topInput.setOpaque(false);
        final JLabel lbl = new JLabel("Film aimé : ");
        lbl.setForeground(Color.WHITE);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 15f));
        topInput.add(lbl, BorderLayout.WEST);

        styleTextField(input);
        topInput.add(input, BorderLayout.CENTER);

        styleNeonButton(propose);
        topInput.add(propose, BorderLayout.EAST);
        topBar.add(topInput, BorderLayout.CENTER);

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

        final JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        styleNeonButton(addWishlist);
        styleNeonButton(descBtn);
        bottom.add(addWishlist);
        bottom.add(descBtn);
        add(bottom, BorderLayout.SOUTH);

        propose.addActionListener(e -> onPropose());
        addWishlist.addActionListener(e -> onAdd());
        descBtn.addActionListener(e -> startDescriptionForCurrent());
    }

    /**
     * Constructeur de compatibilité (ancien code/tests) acceptant un MainFrame.
     *
     * @param service service de recommandation (non null)
     * @param parent  frame parent, utilisé pour la navigation (non null)
     * @throws NullPointerException si un paramètre est null
     */
    public Tool1Panel(final MovieRecommenderService service,
                      final app.cinematch.ui.swing.MainFrame parent) {
        this(service,
                Objects.requireNonNull(parent, "parent must not be null")::showCard);
    }

    /**
     * Déclenche une proposition de film à partir du champ "Film aimé".
     * Gère l'état "busy" et lance une requête asynchrone.
     */
    private void onPropose() {
        final String liked = input.getText().trim();
        if (liked.isEmpty()) {
            return;
        }

        setBusy(true);

        if (descWorker != null && !descWorker.isDone()) {
            descWorker.cancel(true);
        }

        setDescHtml("<i>Recherche d’un film similaire…</i>");

        new SwingWorker<Recommendation, Void>() {
            @Override
            protected Recommendation doInBackground() {
                return service.recommendFromLike(liked);
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

    /**
     * Démarre la génération asynchrone de la description pour la
     * recommandation courante, si disponible.
     */
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

    /**
     * Ajoute le film courant à la liste d'envies.
     */
    private void onAdd() {
        if (current == null) {
            return;
        }
        service.mark(current.title(), "envie");
        JOptionPane.showMessageDialog(this, "Ajouté à la liste 'envie'.");
    }

    /**
     * Active/désactive les composants interactifs pendant les tâches asynchrones.
     *
     * @param busy true si l'UI doit être désactivée temporairement
     */
    private void setBusy(final boolean busy) {
        propose.setEnabled(!busy);
        addWishlist.setEnabled(!busy);
        descBtn.setEnabled(!busy);
        input.setEnabled(!busy);
        backBtn.setEnabled(!busy);
    }

    /**
     * Définit le HTML de la zone de description (sans {@code String.format}).
     *
     * @param htmlInner contenu HTML interne (échappé en amont si besoin)
     */
    private void setDescHtml(final String htmlInner) {
        final String ls = System.lineSeparator();

        final String part1 = "<html>" + ls
                + "  <body style=\"margin:0;padding:0;\">" + ls
                + "    <div style=\"display:flex;align-items:center;justify-content:"
                + "center;min-height:240px;\">" + ls
                + "      <div style=\"text-align:center;font-size:18px;line-height:1.5;"
                + "border:2px solid rgba(255,255,255,0.35);border-radius:12px;padding:"
                + "20px 40px;max-width:84%;background-color:rgba(255,255,255,0.05);"
                + "box-shadow:0 0 15px rgba(0,0,0,0.35);color:#f5f5f5;\">" + ls;

        final String part2 = htmlInner + ls
                + "      </div>" + ls
                + "    </div>" + ls
                + "  </body>" + ls
                + "</html>";

        descPane.setText(part1 + part2);
        descPane.setCaretPosition(0);
    }

    /**
     * Centre/agrandit : remplace les sauts de ligne par des {@code <br/>}.
     *
     * @param text texte initial
     * @return texte prêt pour l'injection HTML
     */
    private static String htmlCenterBig(final String text) {
        return text.replace("\n", "<br/>");
    }

    /**
     * Échappe minimalement le HTML.
     *
     * @param s texte source
     * @return texte échappé
     */
    private static String htmlEscape(final String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Style néon pour les boutons d'action.
     *
     * @param b bouton à styliser
     */
    private void styleNeonButton(final JButton b) {
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(BASE_CARD_BG);
        b.setOpaque(true);

        final EmptyBorder pad = new EmptyBorder(10, 16, 10, 16);
        b.setBorder(new CompoundBorder(
                new LineBorder(NEON_PINK_DARK, 2, true),
                new CompoundBorder(new LineBorder(NEON_PINK, 1, true), pad)));

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(final java.awt.event.MouseEvent e) {
                b.setBackground(HOVER_CARD_BG);
                b.setForeground(HOVER_PINK_TXT);
                b.setBorder(new CompoundBorder(
                        new LineBorder(NEON_PINK, 2, true),
                        new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad)));
            }

            @Override
            public void mouseExited(final java.awt.event.MouseEvent e) {
                b.setBackground(BASE_CARD_BG);
                b.setForeground(Color.WHITE);
                b.setBorder(new CompoundBorder(
                        new LineBorder(NEON_PINK_DARK, 2, true),
                        new CompoundBorder(new LineBorder(NEON_PINK, 1, true), pad)));
            }
        });

        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
    }

    /**
     * Style "outlined" pour le bouton retour.
     *
     * @param b bouton à styliser
     */
    private void styleBackOutlined(final JButton b) {
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setForeground(TEXT_DIM);

        final EmptyBorder pad = new EmptyBorder(6, 12, 6, 12);
        b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(final java.awt.event.MouseEvent e) {
                b.setForeground(Color.WHITE);
                b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 2, true), pad));
            }

            @Override
            public void mouseExited(final java.awt.event.MouseEvent e) {
                b.setForeground(TEXT_DIM);
                b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
            }
        });
    }

    /**
     * Style du champ texte.
     *
     * @param tf champ à styliser
     */
    private void styleTextField(final JTextField tf) {
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBackground(new Color(25, 25, 32));
        tf.setBorder(new CompoundBorder(
                new LineBorder(new Color(120, 120, 140), 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    }

    /**
     * Style des labels d'information.
     *
     * @param l label à styliser
     */
    private void styleInfoLabel(final JLabel l) {
        l.setForeground(new Color(235, 235, 235));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        l.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            final int w = getWidth();
            final int h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
            g2.fillRect(0, 0, w, h);
        } finally {
            g2.dispose();
        }
    }
}
