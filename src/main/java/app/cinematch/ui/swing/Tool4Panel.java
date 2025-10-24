package app.cinematch.ui.swing;

import app.cinematch.agent.ChatAgent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

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
        super();
        this.askFn = Objects.requireNonNull(askFunction, "askFunction must not be null");
        this.navigator = Objects.requireNonNull(navigationCallback, "navigationCallback must not be null");
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
        this(Objects.requireNonNull(agent, "agent must not be null")::ask,
                Objects.requireNonNull(navigationCallback, "navigationCallback must not be null"));
    }

    /**
     * Construit l’interface : barre supérieure, zone de conversation et zone d’entrée.
     * Applique les styles et connecte les actions.
     */
    private void buildUi() {
        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 20, 20, 20));

        // Barre supérieure
        final JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        styleBackOutlined(backButton);
        backButton.addActionListener(e -> navigator.accept("home"));

        final javax.swing.JLabel title =
                new javax.swing.JLabel("Discussion IA", SwingConstants.CENTER);
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
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // Actions
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
    }

    /**
     * Envoie le message utilisateur de manière asynchrone et affiche la réponse de l’IA.
     * Protège l’UI en désactivant temporairement le bouton d’envoi.
     */
    private void sendMessage() {
        final String userText = inputField.getText().trim();
        if (userText.isEmpty()) {
            return;
        }

        appendMessage("Vous : " + userText + System.lineSeparator());
        inputField.setText("");
        sendButton.setEnabled(false);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return askFn.apply(userText);
            }

            @Override
            protected void done() {
                try {
                    final String response = get();
                    appendMessage("IA : " + response + System.lineSeparator() + System.lineSeparator());
                } catch (final Exception ex) {
                    appendMessage("Erreur : " + ex.getMessage()
                            + System.lineSeparator() + System.lineSeparator());
                } finally {
                    sendButton.setEnabled(true);
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
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                button.setBackground(HOVER_CARD_BG);
                button.setForeground(HOVER_PINK_TXT);
                button.setBorder(new CompoundBorder(
                        new LineBorder(NEON_PINK, 2, true),
                        new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad)
                ));
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                button.setBackground(BASE_CARD_BG);
                button.setForeground(Color.WHITE);
                button.setBorder(new CompoundBorder(
                        new LineBorder(NEON_PINK_DARK, 2, true),
                        new CompoundBorder(new LineBorder(NEON_PINK, 1, true), pad)
                ));
            }
        });
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
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
            public void mouseEntered(final MouseEvent e) {
                button.setForeground(Color.WHITE);
                button.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 2, true), pad));
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                button.setForeground(TEXT_DIM);
                button.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
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
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        final int w = getWidth();
        final int h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
        g2.fillRect(0, 0, w, h);
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
