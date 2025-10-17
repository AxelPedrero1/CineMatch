package app.cinematch.ui.swing;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.function.Consumer;

public class HomePanel extends JPanel {

    private Consumer<String> navigator;


    private static final Color NEON_BORDER = new Color(255, 64, 160);          // rose fluo (bord)
    private static final Color NEON_BORDER_DARK = new Color(200, 30, 120);     // rose plus sombre
    private static final Color HOVER_PINK = new Color(255, 182, 193);          // rose clair au survol
    private static final float HOVER_BLEND = 0.16f;                             // intensité du hover

    public HomePanel(MainFrame frame) {
        setLayout(new BorderLayout(10, 10));
        setOpaque(true);


        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 16, 8, 16));


        String titleHtml =
                "<html>" +
                        "<div style='text-align:center; white-space:nowrap;'>" +
                        "<span style=\"color:#FFFFFF;\">CineMatch 🎬</span>&nbsp;" +
                        "<span style=\"color:rgb(255,64,160);\">Deluxe</span>" +
                        "</div>" +
                        "</html>";

        JLabel title = new JLabel(titleHtml, SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 36f));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitle = new JLabel("Trouvez votre prochain film en un swipe.", SwingConstants.CENTER);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 15f));
        subtitle.setBorder(new EmptyBorder(6, 0, 0, 0));
        subtitle.setForeground(new Color(220, 220, 220));

        header.add(title, BorderLayout.CENTER);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);


        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setOpaque(false);
        gridWrapper.setBorder(new EmptyBorder(12, 16, 16, 16));

        JPanel grid = new JPanel(new GridLayout(2, 2, 18, 18));
        grid.setOpaque(false);

        JButton b1 = makeNeonCard("Film similaire");
        JButton b2 = makeNeonCard("Swipe");
        JButton b3 = makeNeonCard("Ma liste ❤️");
        JButton b4 = makeNeonCard("🕒 Historique"); // on garde l'emoji

        grid.add(b1);
        grid.add(b2);
        grid.add(b3);
        grid.add(b4);
        gridWrapper.add(grid, BorderLayout.CENTER);
        add(gridWrapper, BorderLayout.CENTER);


        b1.addActionListener(e -> navigate("t1"));
        b2.addActionListener(e -> navigate("t2"));
        b3.addActionListener(e -> navigate("t3"));
        b4.addActionListener(e -> navigate("hist"));
    }


    private JButton makeNeonCard(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 20f));
        btn.setForeground(new Color(245, 245, 245));


        Color baseBg = UIManager.getColor("Panel.background");
        if (baseBg == null) baseBg = new Color(25, 28, 34);
        Color normalBg = blend(baseBg, Color.BLACK, 0.10f);


        LineBorder outerNeon = new LineBorder(NEON_BORDER_DARK, 2, true);
        EmptyBorder innerPad = new EmptyBorder(22, 26, 22, 26);
        LineBorder innerNeon = new LineBorder(NEON_BORDER, 1, true);
        btn.setBorder(new CompoundBorder(outerNeon, new CompoundBorder(innerNeon, innerPad)));


        btn.setBackground(normalBg);
        btn.setOpaque(true);


        Color hoverBg = blend(normalBg, HOVER_PINK, HOVER_BLEND);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(hoverBg); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(normalBg); }
            @Override public void mousePressed(java.awt.event.MouseEvent e) { btn.setBackground(hoverBg); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) {
                if (btn.getBounds().contains(e.getPoint())) btn.setBackground(hoverBg);
            }
        });

        return btn;
    }


    private static Color blend(Color c1, Color c2, float ratio) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        int r = Math.round(c1.getRed()   * (1 - ratio) + c2.getRed()   * ratio);
        int g = Math.round(c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
        int b = Math.round(c1.getBlue()  * (1 - ratio) + c2.getBlue()  * ratio);
        return new Color(r, g, b, c1.getAlpha());
    }

    public void onNavigate(Consumer<String> nav) { this.navigator = nav; }
    private void navigate(String id) { if (navigator != null) navigator.accept(id); }
}
