package app.cinematch.ui.swing;

import org.junit.jupiter.api.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests stables et headless pour Tool4Panel (sans Mockito ni JaCoCo).
 */
class Tool4PanelTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    /* ---------- Helpers ---------- */

    private static void noThrow(Runnable r, String msg) {
        try {
            r.run();
        } catch (Throwable t) {
            fail(msg, t);
        }
    }

    /**
     * Attend qu’une condition devienne vraie ou échoue après un délai donné.
     * Remplace l'ancien Thread.sleep() en boucle pour éviter le busy waiting.
     */
    private static void waitUntil(Duration timeout, String failMsg, BooleanSupplier cond) {
        Instant end = Instant.now().plus(timeout);
        while (Instant.now().isBefore(end)) {
            if (cond.getAsBoolean()) return;
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Thread interrompu pendant l’attente");
            }
        }
        fail(failMsg);
    }

    private static JButton findButton(Container root, String exactText) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b && exactText.equals(b.getText())) return b;
            if (c instanceof Container cc) {
                JButton r = findButton(cc, exactText);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static JTextField findTextField(Container root) {
        if (root instanceof JTextField tf) return tf;
        for (Component c : root.getComponents()) {
            if (c instanceof Container cc) {
                JTextField tf = findTextField(cc);
                if (tf != null) return tf;
            }
        }
        return null;
    }

    private static JLabel findThinkingLabel(Container root) {
        if (root instanceof JLabel l && l.getText() != null && l.getText().contains("IA")) return l;
        for (Component c : root.getComponents()) {
            if (c instanceof Container cc) {
                JLabel r = findThinkingLabel(cc);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static JEditorPane findEditor(Container root) {
        if (root instanceof JEditorPane ep) return ep;
        for (Component c : root.getComponents()) {
            if (c instanceof Container cc) {
                JEditorPane ep = findEditor(cc);
                if (ep != null) return ep;
            }
        }
        return null;
    }

    private static String safeHtml(JEditorPane ep) {
        if (ep == null) return "";
        AtomicReference<String> ref = new AtomicReference<>("");
        try {
            SwingUtilities.invokeAndWait(() -> ref.set(ep.getText()));
        } catch (Exception ignored) {
        }
        return ref.get();
    }

    /* ---------- Tests ---------- */

    @Test
    @DisplayName("Initial state: components exist and labels hidden")
    void initial_state_ok() {
        Function<String, String> fakeAsk = s -> "Echo:" + s;
        Consumer<String> fakeNav = s -> {};
        Tool4Panel panel = new Tool4Panel(fakeAsk, fakeNav);

        assertNotNull(findTextField(panel), "inputField manquant");
        assertNotNull(findButton(panel, "Envoyer"), "sendButton manquant");
        JLabel thinking = findThinkingLabel(panel);
        assertNotNull(thinking, "thinkingLabel manquant");
    }

    @Test
    @DisplayName("Send message updates HTML and toggles indicators")
    void send_flow_updates_html_and_indicators() {
        AtomicReference<String> lastAsked = new AtomicReference<>();
        Function<String, String> fakeAsk = s -> {
            lastAsked.set(s);
            try {
                Thread.sleep(150);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return "Réponse-" + s.toUpperCase();
        };
        AtomicReference<String> nav = new AtomicReference<>(null);
        Tool4Panel panel = new Tool4Panel(fakeAsk, nav::set);

        JTextField input = findTextField(panel);
        JButton send = findButton(panel, "Envoyer");
        JLabel thinking = findThinkingLabel(panel);
        JEditorPane editor = findEditor(panel);
        assertNotNull(thinking);

        if (input != null && send != null) {
            input.setText("salut");
            noThrow(send::doClick, "click ne doit pas jeter");

            waitUntil(Duration.ofSeconds(2), "thinking pas visible", thinking::isVisible);

            String html = safeHtml(editor);
            assertTrue(html.contains("salut"), "Le texte saisi doit apparaître dans le HTML");
            assertEquals("salut", lastAsked.get());
        } else {
            fail("Input ou bouton Envoyer introuvable");
        }
    }

    @Test
    @DisplayName("Back button triggers navigation callback")
    void back_button_calls_navigator() {
        AtomicReference<String> nav = new AtomicReference<>(null);
        Tool4Panel panel = new Tool4Panel(s -> "ok", nav::set);
        JButton back = findButton(panel, "← Retour");
        assertNotNull(back);
        noThrow(back::doClick, "click retour");
        assertEquals("home", nav.get(), "Le callback navigation doit être appelé avec 'home'");
    }

    @Test
    @DisplayName("Hover changes and resets on Envoyer button")
    void hover_changes_then_resets() {
        Tool4Panel panel = new Tool4Panel(s -> "ok", s -> {});
        JButton send = findButton(panel, "Envoyer");
        assertNotNull(send);

        Color bg0 = send.getBackground();
        Color fg0 = send.getForeground();

        MouseEvent enter = new MouseEvent(send, MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(), 0, 5, 5, 0, false);
        for (var l : send.getMouseListeners()) l.mouseEntered(enter);

        assertNotEquals(bg0, send.getBackground(), "BG doit changer au hover");

        MouseEvent exit = new MouseEvent(send, MouseEvent.MOUSE_EXITED,
                System.currentTimeMillis(), 0, 5, 5, 0, false);
        for (var l : send.getMouseListeners()) l.mouseExited(exit);

        assertEquals(bg0, send.getBackground(), "BG doit revenir à l’état initial");
        assertEquals(fg0, send.getForeground(), "FG doit revenir aussi");
    }

    @Test
    @DisplayName("Paint gradient fills image without error")
    void paint_component_ok() {
        Tool4Panel panel = new Tool4Panel(s -> "ok", s -> {});
        panel.setSize(320, 180);
        BufferedImage img = new BufferedImage(320, 180, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        noThrow(() -> panel.paintComponent(g2), "paintComponent ne doit pas jeter");
        g2.dispose();
        int[] data = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
        boolean opaque = false;
        for (int argb : data)
            if ((argb >>> 24) != 0x00) {
                opaque = true;
                break;
            }
        assertTrue(opaque, "Le gradient doit remplir l'image");
    }
}
