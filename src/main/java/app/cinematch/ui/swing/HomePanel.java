package app.cinematch.ui.swing;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class HomePanel extends JPanel {

    private Consumer<String> navigator;

    private static final Color NEON_PINK = new Color(255, 64, 160);
    private static final Color NEON_PINK_DARK = new Color(200, 30, 120);
    private static final Color HOVER_PINK_TEXT = new Color(255, 210, 230);
    private static final Color BASE_CARD_BG = new Color(30, 30, 40);
    private static final Color HOVER_CARD_BG = new Color(50, 40, 60);
    private static final Color BG_TOP = new Color(18, 18, 24);
    private static final Color BG_BOTTOM = new Color(35, 20, 40);

    public HomePanel(MainFrame frame) {
        setPreferredSize(new Dimension(1080, 1920));
        setMinimumSize(new Dimension(1080, 1920));
        setMaximumSize(new Dimension(1080, 1920));
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(56, 24, 8, 24));

        JLabel title = new JLabel(
                "<html><div style='text-align:center; white-space:nowrap;'>"
                        + "<span style='color:#FFFFFF;'>CineMatch 🎬</span>&nbsp;"
                        + "<span style='color:rgb(255,64,160); text-shadow:0 0 14px rgba(255,64,160,0.65);'>Deluxe</span>"
                        + "</div></html>",
                SwingConstants.CENTER
        );
        title.setFont(pickFont(new String[]{"Segoe UI Emoji","Segoe UI","Dialog"}, Font.BOLD, 56));

        JLabel subtitle = new JLabel("Trouvez votre prochain film en un swipe.", SwingConstants.CENTER);
        subtitle.setFont(pickFont(new String[]{"Segoe UI","Dialog"}, Font.PLAIN, 22));
        subtitle.setForeground(new Color(220, 220, 220));
        subtitle.setBorder(new EmptyBorder(12, 0, 0, 0));

        header.add(title, BorderLayout.CENTER);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(new EmptyBorder(24, 32, 48, 32));

        JButton swipeBtn = makeNeonCard("⚡  Swipe", Size.LARGE, true);
        swipeBtn.addActionListener(e -> navigate("t2"));
        swipeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(swipeBtn);

        column.add(Box.createVerticalStrut(28));

        JPanel row = new JPanel(new GridLayout(1, 2, 28, 0));
        row.setOpaque(false);
        JButton listBtn = makeNeonCard("❤️  Ma liste", Size.SMALL, false);
        listBtn.addActionListener(e -> navigate("t3"));
        JButton similarBtn = makeNeonCard("🎞️  Film similaire", Size.SMALL, false);
        similarBtn.addActionListener(e -> navigate("t1"));
        JButton chatBtn = makeNeonCard("💬  Parler à l'IA", Size.SMALL, false);
        chatBtn.addActionListener(e -> navigate("chat"));
        row.add(listBtn);
        row.add(similarBtn);
        row.add(chatBtn);
        column.add(row);

        add(column, BorderLayout.CENTER);
    }

    private enum Size { LARGE, SMALL }

    private JButton makeNeonCard(String text, Size size, boolean fullWidth) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);

        if (size == Size.LARGE) {
            btn.setFont(pickFont(new String[]{"Segoe UI Emoji","Segoe UI","Dialog"}, Font.BOLD, 40));
            btn.setPreferredSize(new Dimension(0, 360));
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
        } else {
            btn.setFont(pickFont(new String[]{"Segoe UI Emoji","Segoe UI","Dialog"}, Font.BOLD, 28));
            btn.setPreferredSize(new Dimension(0, 220));
            if (fullWidth) btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        }

        LineBorder outer = new LineBorder(NEON_PINK_DARK, 3, true);
        LineBorder inner = new LineBorder(NEON_PINK, 2, true);
        int padV = (size == Size.LARGE) ? 24 : 18;
        int padH = (size == Size.LARGE) ? 28 : 22;
        EmptyBorder pad = new EmptyBorder(padV, padH, padV, padH);
        btn.setBorder(new CompoundBorder(outer, new CompoundBorder(inner, pad)));

        btn.setBackground(BASE_CARD_BG);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(HOVER_CARD_BG);
                btn.setForeground(HOVER_PINK_TEXT);
                btn.setBorder(new CompoundBorder(
                        new LineBorder(NEON_PINK, 3, true),
                        new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad)
                ));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BASE_CARD_BG);
                btn.setForeground(Color.WHITE);
                btn.setBorder(new CompoundBorder(outer, new CompoundBorder(inner, pad)));
            }
        });

        if (fullWidth) {
            Dimension pref = btn.getPreferredSize();
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        return btn;
    }

    private static Font pickFont(String[] families, int style, int size) {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
        ));
        for (String f : families) {
            if (available.contains(f)) return new Font(f, style, size);
        }
        return new Font("Dialog", style, size);
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

    public void onNavigate(Consumer<String> nav) { this.navigator = nav; }
    private void navigate(String id) {
        if (navigator != null) navigator.accept(id);
    }
}
