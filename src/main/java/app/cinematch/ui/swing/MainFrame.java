package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.agent.ChatAgent;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class MainFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel container = new JPanel(cards);

    private final MovieRecommenderService service;

    public MainFrame(MovieRecommenderService service, ChatAgent agent) {
        super("CineMatch 🎬 Deluxe");
        this.service = service;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        // Écrans principaux
        HomePanel home = new HomePanel(this);
        Tool1Panel t1 = new Tool1Panel(service, this::showCard);
        Tool2Panel t2 = new Tool2Panel(service, this::showCard);
        Tool3Panel t3 = new Tool3Panel(service, this::showCard);

        // ⚠️ agent peut être null → fallback fonctionnel
        Tool4Panel chat = (agent != null)
                ? new Tool4Panel(agent, this::showCard)
                : new Tool4Panel(
                q -> "Le chat IA est indisponible pour le moment.",
                this::showCard
        );

        // (Tu peux aussi migrer HistoryPanel pour accepter un Consumer<String>)
        HistoryPanel hist = new HistoryPanel(service, this);

        // Ajouter les vues
        container.add(home, "home");
        container.add(t1, "t1");
        container.add(t2, "t2");
        container.add(t3, "t3");
        container.add(chat, "chat");
        container.add(hist, "hist");

        setContentPane(container);

        // Navigation depuis HomePanel
        home.onNavigate(id -> cards.show(container, id));
    }

    /** Ancien constructeur : désormais sûr car le ctor ci-dessus gère agent==null. */
    public MainFrame(MovieRecommenderService service) {
        this(service, (ChatAgent) null);
    }

    public void showCard(String id) {
        ((CardLayout) getContentPane().getLayout()).show(getContentPane(), id);
    }

    public MovieRecommenderService getService() {
        return service;
    }
}
