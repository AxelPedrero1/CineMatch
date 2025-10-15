package app.cinematch.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class HomePanel extends JPanel {

    private Consumer<String> navigator;

    public HomePanel(MainFrame frame) {
        setLayout(new BorderLayout(10,10));

        JLabel title = new JLabel("CineMatch 🎬 Deluxe", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        JButton b1 = new JButton("1️⃣  Film similaire");
        JButton b2 = new JButton("2️⃣  Tinder infini");
        JButton b3 = new JButton("3️⃣  Ma liste ❤️");
        JButton b4 = new JButton("🕒 Historique");

        grid.add(b1); grid.add(b2); grid.add(b3); grid.add(b4);
        add(grid, BorderLayout.CENTER);

        b1.addActionListener(e -> navigate("t1"));
        b2.addActionListener(e -> navigate("t2"));
        b3.addActionListener(e -> navigate("t3"));
        b4.addActionListener(e -> navigate("hist"));
    }

    public void onNavigate(Consumer<String> nav) { this.navigator = nav; }
    private void navigate(String id) { if (navigator != null) navigator.accept(id); }
}
