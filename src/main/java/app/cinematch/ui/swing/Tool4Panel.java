package app.cinematch.ui.swing;

import app.cinematch.agent.ChatAgent;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Panneau de discussion directe avec l'IA (ChatAgent)
 */
public class Tool4Panel extends JPanel {

    private final Consumer<String> navigator;   // au lieu de stocker MainFrame
    private final ChatAgent agent;              // injecté directement

    private final JTextArea conversationArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Envoyer");
    private final JButton backButton = new JButton("Retour");

    private final Color NEON_PINK = new Color(255, 64, 160);
    private final Color NEON_PINK_DARK = new Color(200, 30, 120);
    private final Color HOVER_PINK_TXT = new Color(255, 210, 230);
    private final Color BASE_CARD_BG = new Color(30, 30, 40);
    private final Color HOVER_CARD_BG = new Color(50, 40, 60);
    private final Color BG_TOP = new Color(18, 18, 24);
    private final Color BG_BOTTOM = new Color(35, 20, 40);
    private final Color TEXT_DIM = new Color(220, 220, 220);

    // Nouveau constructeur : injecte l’agent et un callback de navigation
    public Tool4Panel(ChatAgent agent, Consumer<String> navigator) {
        this.agent = agent;
        this.navigator = navigator;

        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 20, 20, 20));

        // Barre supérieure
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        styleBackOutlined(backButton);
        backButton.addActionListener(e -> {
            if (navigator != null) navigator.accept("home");
        });
        JLabel title = new JLabel("💬 Discussion IA", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI Emoji", Font.BOLD, 20));
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

        JScrollPane scroll = new JScrollPane(conversationArea);
        scroll.setBorder(new LineBorder(new Color(80, 80, 100), 1, true));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        // Zone d’entrée
        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
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

    private void sendMessage() {
        String userText = inputField.getText().trim();
        if (userText.isEmpty() || agent == null) return;

        appendMessage("👤 Vous : " + userText + "\n");
        inputField.setText("");
        sendButton.setEnabled(false);

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return agent.ask(userText);
            }
            @Override protected void done() {
                try {
                    String response = get();
                    appendMessage("🤖 IA : " + response + "\n\n");
                } catch (Exception ex) {
                    appendMessage("⚠️ Erreur : " + ex.getMessage() + "\n\n");
                } finally {
                    sendButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void appendMessage(String msg) {
        conversationArea.append(msg);
        conversationArea.setCaretPosition(conversationArea.getDocument().getLength());
    }


    private void styleNeonButton(JButton b) {
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(BASE_CARD_BG);
        b.setOpaque(true);
        EmptyBorder pad = new EmptyBorder(10, 16, 10, 16);
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
        b.setForeground(TEXT_DIM);
        EmptyBorder pad = new EmptyBorder(6, 12, 6, 12);
        b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setForeground(Color.WHITE);
                b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 2, true), pad));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setForeground(TEXT_DIM);
                b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
            }
        });
    }

    private void styleTextField(JTextField tf) {
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBackground(new Color(25, 25, 32));
        tf.setBorder(new CompoundBorder(
                new LineBorder(new Color(120, 120, 140), 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
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
