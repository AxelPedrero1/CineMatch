package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel container = new JPanel(cards);

    public MainFrame(MovieRecommenderService service) {
        super("CineMatch 🎬 Deluxe");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        HomePanel home = new HomePanel(this);
        Tool1Panel t1 = new Tool1Panel(service, this);
        Tool2Panel t2 = new Tool2Panel(service, this);
        Tool3Panel t3 = new Tool3Panel(service, this);
        HistoryPanel hist = new HistoryPanel(service, this);


        container.add(home, "home");
        container.add(t1, "t1");
        container.add(t2, "t2");
        container.add(t3, "t3");
        container.add(hist, "hist");

        setContentPane(container);

        home.onNavigate(id -> cards.show(container, id));
    }
    public void showCard(String id) {
        ((CardLayout) getContentPane().getLayout()).show(getContentPane(), id);
    }

}
