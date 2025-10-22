package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.agent.ChatAgent;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel container = new JPanel(cards);

    private final MovieRecommenderService service;

    /**
     * Nouveau constructeur principal : accepte à la fois
     * le service de recommandation et l'agent IA.
     */
    public MainFrame(MovieRecommenderService service, ChatAgent agent) {
        super("CineMatch 🎬 Deluxe");
        this.service = service;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        // Écrans principaux
        HomePanel home = new HomePanel(this);
        Tool1Panel t1 = new Tool1Panel(service, this);
        Tool2Panel t2 = new Tool2Panel(service, this);
        Tool3Panel t3 = new Tool3Panel(service, this);
        Tool4Panel chat = new Tool4Panel(agent, this::showCard);
        HistoryPanel hist = new HistoryPanel(service, this);

        // Ajouter les vues
        container.add(home, "home");
        container.add(t1, "t1");
        container.add(t2, "t2");
        container.add(t3, "t3");
        container.add(chat, "chat");
        container.add(hist, "hist");

        setContentPane(container);

        // Gestion de navigation depuis HomePanel
        home.onNavigate(id -> cards.show(container, id));
    }

    /**
     * Ancien constructeur conservé pour compatibilité :
     * (permet de ne pas casser le code existant)
     */
    public MainFrame(MovieRecommenderService service) {
        this(service, null);
    }

    /**
     * Navigation entre panneaux.
     */
    public void showCard(String id) {
        ((CardLayout) getContentPane().getLayout()).show(getContentPane(), id);
    }


    /**
     * Accès au service de recommandation
     */
    public MovieRecommenderService getService() {
        return service;
    }
}
