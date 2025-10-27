package app.cinematch.ui.swing;

import app.cinematch.agent.ChatAgent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Panneau de discussion directe avec l’IA ({@link ChatAgent}) via une interface simple.
 *
 * <p>Le composant affiche une zone de conversation, un champ de saisie et un bouton
 * d’envoi. Les appels au modèle sont réalisés de manière asynchrone avec {@link SwingWorker}
 * afin de ne pas bloquer l’EDT.</p>
 *
 * <p>Conception : le panneau ne conserve pas d’instance mutable externe du domaine ;
 * il stocke uniquement une « capacité » fonctionnelle via {@link #askFn} et un
 * callback de navigation via {@link #navigator}.</p>
 *
 * <p>Exemple d’intégration :
 * <pre>{@code
 * Tool4Panel chat = new Tool4Panel(agent::ask, frame::showCard);
 * frame.setContentPane(chat);
 * }</pre>
 */
public final class Tool4Panel extends JPanel {

    // --- Thème (constantes immuables) ---

    /** Couleur néon rose principale. */
    private static final Color NEON_PINK = new Color(255, 64, 160);
    /** Variante sombre du néon rose. */
    private static final Color NEON_PINK_DARK = new Color(200, 30, 120);
    /** Couleur de texte au survol. */
    private static final Color HOVER_PINK_TXT = new Color(255, 210, 230);
    /** Couleur de fond par défaut des cartes. */
    private static final Color BASE_CARD_BG = new Color(30, 30, 40);
    /** Couleur de fond des cartes au survol. */
    private static final Color HOVER_CARD_BG = new Color(50, 40, 60);
    /** Couleur haute du dégradé d’arrière-plan. */
    private static final Color BG_TOP = new Color(18, 18, 24);
    /** Couleur basse du dégradé d’arrière-plan. */
    private static final Color BG_BOTTOM = new Color(35, 20, 40);
    /** Couleur de texte secondaire. */
    private static final Color TEXT_DIM = new Color(220, 220, 220);

    // --- Dépendances fonctionnelles ---

    /** Callback de navigation (ex. parent::showCard). */
    private final Consumer<String> navigator;
    /**
     * Capacité fonctionnelle pour poser une question et obtenir une réponse de l’IA.
     * Signature : {@code askFn.apply(input) -> réponse}.
     */
    private final Function<String, String> askFn;

    // --- UI ---

    /** Zone d’historique de la conversation. */
    private final JTextArea conversationArea = new JTextArea();
    /** Champ de saisie du message utilisateur. */
    private final JTextField inputField = new JTextField();
    /** Bouton d’envoi du message. */
    private final JButton sendButton = new JButton("Envoyer");
    /** Bouton retour vers l’écran d’accueil. */
    private final JButton backButton = new JButton("Retour");

    /**
     * Constructeur recommandé : accepte une fonction « ask » et un callback de navigation.
     *
     * @param askFunction        fonction qui prend l’entrée utilisateur et renvoie la réponse
     * @param navigationCallback callback pour changer d’écran (ex. id → {@code "home"})
     * @throws NullPointerException si un des paramètres est {@code null}
     */
    public Tool4Panel(final Function<String, String> askFunction,
                      final Consumer<String> navigationCallback) {
        this.askFn = Objects.requireNonNull(askFunction);
        this.navigator = Objects.requireNonNull(navigationCallback);
        buildUi();
    }

    /**
     * Constructeur de confort : accepte un {@link ChatAgent} mais ne le stocke pas.
     * La méthode de question/réponse est capturée via référence de méthode.
     *
     * @param agent              agent de discussion (référence non conservée)
     * @param navigationCallback callback pour changer d’écran
     * @throws NullPointerException si un des paramètres est {@code null}
     */
    public Tool4Panel(final ChatAgent agent, final Consumer<String> navigationCallback) {
        this(Objects.requireNonNull(agent)::ask,
                Objects.requireNonNull(navigationCallback));
    }

    /**
     * Construit l’interface : barre supérieure, zone de conversation et zone d’entrée.
     * Applique les styles et connecte les actions.
     */
    private void buildUi() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setOpaque(false);

        // --- Haut : titre et bouton retour ---
        final JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        styleBackButton(backButton);
        backButton.addActionListener(e -> navigator.accept("home"));

        final JLabel title = new JLabel("💬 Discussion avec l’IA", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        topBar.add(backButton, BorderLayout.WEST);
        topBar.add(title, BorderLayout.CENTER);
        add(topBar, BorderLayout.NORTH);

        // Zone de conversation
        conversationArea.setEditable(false);
        conversationArea.setWrapStyleWord(true);
        conversationArea.setLineWrap(true);
        conversationArea.setBackground(new Color(25, 25, 32));
        conversationArea.setForeground(new Color(235, 235, 235));
        conversationArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        conversationArea.setBorder(new EmptyBorder(16, 16, 16, 16));

        final JScrollPane scroll = new JScrollPane(conversationArea);
        scroll.setBorder(new LineBorder(new Color(80, 80, 100), 1, true));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        // Zone d’entrée
        final JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.setOpaque(false);
        styleTextField(inputField);
        styleNeonButton(sendButton);

        final JPanel inputPanel = new JPanel(new BorderLayout(6, 6));
        inputPanel.setOpaque(false);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Chargement
        thinkingLabel.setForeground(new Color(220, 220, 220));
        thinkingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        thinkingLabel.setVisible(false);

        loadingBar.setIndeterminate(true);
        loadingBar.setForeground(PINK_ACCENT);
        loadingBar.setBackground(new Color(40, 40, 50));
        loadingBar.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 120), 1, true));
        loadingBar.setPreferredSize(new Dimension(200, 6));
        loadingBar.setVisible(false);

        final JPanel loadingPanel = new JPanel(new BorderLayout());
        loadingPanel.setOpaque(false);
        loadingPanel.add(thinkingLabel, BorderLayout.NORTH);
        loadingPanel.add(loadingBar, BorderLayout.SOUTH);

        bottom.add(inputPanel, BorderLayout.CENTER);
        bottom.add(loadingPanel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        // Actions
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
    }

    /**
     * Envoie le message utilisateur de manière asynchrone et affiche la réponse de l’IA.
     * Protège l’UI en désactivant temporairement le bouton d’envoi.
     */
    private void sendMessage() {
        final String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        appendMessage("Vous", text, true);
        inputField.setText("");
        sendButton.setEnabled(false);

        thinkingLabel.setVisible(true);
        loadingBar.setVisible(true);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return askFn.apply(text);
            }

            @Override
            protected void done() {
                try {
                    appendMessage("IA", get(), false);
                } catch (Exception ex) {
                    appendMessage("Erreur", ex.getMessage(), false);
                } finally {
                    sendButton.setEnabled(true);
                    thinkingLabel.setVisible(false);
                    loadingBar.setVisible(false);
                }
            }
        }.execute();
    }

    /**
     * Ajoute un message à la zone de conversation et fait défiler jusqu’en bas.
     *
     * @param msg message à appendre (peut contenir des sauts de ligne)
     */
    private void appendMessage(final String msg) {
        conversationArea.append(msg);
        conversationArea.setCaretPosition(conversationArea.getDocument().getLength());
    }

    /**
     * Style « néon » pour un bouton (couleurs, bordures composées et effet au survol).
     *
     * @param button bouton à styliser
     */
    private void styleNeonButton(final JButton button) {
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBackground(BASE_CARD_BG);
        button.setOpaque(true);
        final EmptyBorder pad = new EmptyBorder(10, 16, 10, 16);
        button.setBorder(new CompoundBorder(
                new LineBorder(NEON_PINK_DARK, 2, true),
                new CompoundBorder(new LineBorder(NEON_PINK, 1, true), pad)
        ));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(PINK_ACCENT);
                btn.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(50, 40, 60));
                btn.setForeground(Color.WHITE);
            }
        });
    }

    /**
     * Style « outlined » pour le bouton Retour (texte pâle + contour).
     *
     * @param button bouton à styliser
     */
    private void styleBackOutlined(final JButton button) {
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setForeground(TEXT_DIM);
        final EmptyBorder pad = new EmptyBorder(6, 12, 6, 12);
        button.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(Color.LIGHT_GRAY);
            }
        });
    }

    /**
     * Style du champ texte (couleurs, bordure et police).
     *
     * @param tf champ à styliser
     */
    private void styleTextField(final JTextField tf) {
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBackground(new Color(25, 25, 32));
        tf.setBorder(new CompoundBorder(
                new LineBorder(new Color(120, 120, 140), 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    }

    /**
     * Dessine le fond en dégradé vertical du panneau.
     *
     * @param g contexte graphique
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, getHeight(), BG_BOTTOM));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    /**
     * Utilitaire de composition de bordure (exemple simple), si besoin futur.
     * Évite une dépendance directe à {@link BorderFactory} pour chaque appel.
     *
     * @param color     couleur du contour
     * @param thickness épaisseur du contour
     * @param pad       marge intérieure
     * @return une {@link CompoundBorder} composée
     */
    @SuppressWarnings("unused")
    private static CompoundBorder compound(final Color color, final int thickness,
                                           final EmptyBorder pad) {
        return new CompoundBorder(new LineBorder(color, thickness, true), pad);
    }
}
