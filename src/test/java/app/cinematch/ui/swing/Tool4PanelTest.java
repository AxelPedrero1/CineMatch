package app.cinematch.ui.swing;

import app.cinematch.agent.ChatAgent;
import org.junit.jupiter.api.*;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests de Tool4Panel avec style "given / when / then".
 * Objectif : 100% de couverture JaCoCo (chemins normal + erreur, styles, paint, 2 constructeurs).
 */
public class Tool4PanelTest {

    @BeforeAll
    static void headless() {
        // Exécuter les tests Swing en mode headless pour CI
        System.setProperty("java.awt.headless", "true");
    }

    /**
     * Utilitaire : lecture champ privé par réflexion.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getPrivate(Object target, String fieldName, Class<T> type) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new AssertionError("Impossible d'accéder au champ " + fieldName, e);
        }
    }

    /**
     * Utilitaire : appel méthode privée par réflexion.
     */
    private static Object callPrivate(Object target, String methodName, Class<?>[] sig, Object... args) {
        try {
            Method m = target.getClass().getDeclaredMethod(methodName, sig);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Exception e) {
            throw new AssertionError("Impossible d'appeler " + methodName, e);
        }
    }

    /**
     * Utilitaire : exécuter un bloc et attendre que l'EDT ait fini.
     */
    private static void onEDTAndWait(Runnable r) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeAndWait(r);
        }
    }

    /**
     * Utilitaire : attendre une condition avec timeout simple (sans libs externes).
     */
    private static void waitUntil(String msg, long timeoutMs, Condition cond) throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (cond.ok()) return;
            Thread.sleep(10);
        }
        fail("Timeout: " + msg);
    }

    @FunctionalInterface
    private interface Condition { boolean ok(); }

    @Test
    void givenConstructorWithFunction_whenBuilds_thenUIConnected() throws Exception {
        // given — une askFn qui renvoie immédiatement une réponse
        Function<String, String> askFn = s -> "Réponse à: " + s;
        AtomicReference<String> navTarget = new AtomicReference<>();
        Consumer<String> navigator = navTarget::set;

        Tool4Panel panel = new Tool4Panel(askFn, navigator);

        // when — on récupère les composants privés
        JTextField input = getPrivate(panel, "inputField", JTextField.class);
        JButton send = getPrivate(panel, "sendButton", JButton.class);
        JButton back = getPrivate(panel, "backButton", JButton.class);
        JTextArea area = getPrivate(panel, "conversationArea", JTextArea.class);
        JLabel thinking = getPrivate(panel, "thinkingLabel", JLabel.class);
        JProgressBar bar = getPrivate(panel, "loadingBar", JProgressBar.class);

        // then — les composants existent
        assertNotNull(input);
        assertNotNull(send);
        assertNotNull(back);
        assertNotNull(area);
        assertNotNull(thinking);
        assertNotNull(bar);

        // given — un message utilisateur non vide
        onEDTAndWait(() -> input.setText("Bonjour"));

        // when — on simule un click sur "Envoyer"
        onEDTAndWait(send::doClick);

        // then — le message de l'utilisateur doit apparaître immédiatement
        waitUntil("message utilisateur visible", 500, () -> area.getText().contains("Vous"));
        assertTrue(thinking.isVisible(), "Le label de réflexion doit être visible pendant le traitement");
        assertTrue(bar.isVisible(), "La barre de chargement doit être visible pendant le traitement");
        assertFalse(send.isEnabled(), "Le bouton doit être désactivé pendant l'appel");

        // then — la réponse de l'IA apparaît, le loader se cache, et le bouton se réactive
        waitUntil("réponse IA visible et loader caché", 2000, () ->
                area.getText().contains("IA") && !bar.isVisible() && send.isEnabled());
        assertTrue(area.getText().contains("Réponse à: Bonjour"));
    }

    @Test
    void givenAskThrows_whenSend_thenErrorBranchIsShownAndLoaderHides() throws Exception {
        // given — une askFn qui lève une exception
        Function<String, String> failingAsk = s -> { throw new RuntimeException("Boom"); };
        Tool4Panel panel = new Tool4Panel(failingAsk, s -> {});

        JTextField input = getPrivate(panel, "inputField", JTextField.class);
        JButton send = getPrivate(panel, "sendButton", JButton.class);
        JTextArea area = getPrivate(panel, "conversationArea", JTextArea.class);
        JProgressBar bar = getPrivate(panel, "loadingBar", JProgressBar.class);

        onEDTAndWait(() -> input.setText("Test erreur"));
        onEDTAndWait(send::doClick);

        // then — le message d'erreur apparaît et le loader est masqué
        waitUntil("message d'erreur visible", 2000, () ->
                area.getText().contains("Erreur") && !bar.isVisible() && send.isEnabled());
        assertTrue(area.getText().contains("Boom"), "Le message d'erreur doit contenir l'exception");
    }

    @Test
    void givenEmptyInput_whenSend_thenDoesNothing() throws Exception {
        // given — askFn qui échouerait si appelée
        Function<String, String> askFn = s -> { throw new AssertionError("Ne devrait pas être appelée"); };
        Tool4Panel panel = new Tool4Panel(askFn, s -> {});
        JTextField input = getPrivate(panel, "inputField", JTextField.class);
        JButton send = getPrivate(panel, "sendButton", JButton.class);
        JTextArea area = getPrivate(panel, "conversationArea", JTextArea.class);

        // when — champ vide puis click
        onEDTAndWait(() -> input.setText("   "));
        onEDTAndWait(send::doClick);

        // then — rien ne change
        assertEquals("", area.getText().trim(), "Aucun texte ne doit être ajouté quand l'entrée est vide");
        assertTrue(send.isEnabled(), "Le bouton ne doit pas rester désactivé");
    }

    @Test
    void givenBackButton_whenClicked_thenNavigatorCalled() throws Exception {
        // given
        AtomicReference<String> navTarget = new AtomicReference<>();
        Tool4Panel panel = new Tool4Panel(s -> "ok", navTarget::set);
        JButton back = getPrivate(panel, "backButton", JButton.class);

        // when
        onEDTAndWait(back::doClick);

        // then
        assertEquals("home", navTarget.get(), "Le bouton Retour doit appeler navigator.accept(\"home\")");
    }

    @Test
    void givenButtons_whenHover_thenStyleMouseListenersApply() throws Exception {
        // given
        Tool4Panel panel = new Tool4Panel(s -> "ok", s -> {});
        JButton send = getPrivate(panel, "sendButton", JButton.class);
        JButton back = getPrivate(panel, "backButton", JButton.class);

        // when — on envoie des events de mouse enter/exit
        Component src = send;
        MouseEvent enterSend = new MouseEvent(src, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 1,1,0,false);
        MouseEvent exitSend  = new MouseEvent(src, MouseEvent.MOUSE_EXITED,  System.currentTimeMillis(), 0, 1,1,0,false);
        for (var l : send.getMouseListeners()) { l.mouseEntered(enterSend); }
        Color afterEnterBg = send.getBackground();
        for (var l : send.getMouseListeners()) { l.mouseExited(exitSend); }
        Color afterExitBg = send.getBackground();

        // then — couleurs modifiées (on vérifie un changement)
        assertNotEquals(afterEnterBg, afterExitBg, "Le style du bouton 'Envoyer' doit changer au survol");

        // when — idem pour back (foreground)
        MouseEvent enterBack = new MouseEvent(back, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 1,1,0,false);
        MouseEvent exitBack  = new MouseEvent(back, MouseEvent.MOUSE_EXITED,  System.currentTimeMillis(), 0, 1,1,0,false);
        Color fgBefore = back.getForeground();
        for (var l : back.getMouseListeners()) { l.mouseEntered(enterBack); }
        Color fgEnter = back.getForeground();
        for (var l : back.getMouseListeners()) { l.mouseExited(exitBack); }
        Color fgExit = back.getForeground();

        // then — foreground varie au survol puis revient
        assertNotEquals(fgBefore, fgEnter, "Le texte du bouton 'Retour' doit éclaircir au survol");
        assertEquals(fgExit, back.getForeground(), "Le texte du bouton 'Retour' doit revenir à la couleur normale après sortie");
    }

    @Test
    void givenPanel_whenPaintComponent_thenGradientIsRenderedWithoutError() {
        // given
        Tool4Panel panel = new Tool4Panel(s -> "ok", s -> {});
        panel.setSize(320, 180);

        // when — peindre sur une image offscreen
        BufferedImage img = new BufferedImage(320, 180, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        assertDoesNotThrow(() -> panel.paint(g2));
        g2.dispose();

        // then — quelques pixels non transparents attendus (on vérifie un pixel central)
        int argb = img.getRGB(160, 90);
        assertNotEquals(0, (argb >>> 24), "Le dégradé doit effectivement peindre le fond (alpha non nul)");
    }

    @Test
    void givenPrivateCompound_whenCalledByReflection_thenReturnsBorder() {
        // given
        Tool4Panel panel = new Tool4Panel(s -> "ok", s -> {});
        // when
        Object border = callPrivate(
                panel,
                "compound",
                new Class<?>[]{Color.class, int.class, EmptyBorder.class},
                Color.WHITE, 2, new EmptyBorder(1,1,1,1)
        );
        // then
        assertTrue(border instanceof CompoundBorder, "La méthode utilitaire doit renvoyer une CompoundBorder");
    }

    @Test
    void givenSecondConstructorWithChatAgent_whenAsk_thenResponseShown() throws Exception {
        // given — utilisation du 2e constructeur pour couvrir ces lignes
        ChatAgent agent = mock(ChatAgent.class);
        when(agent.ask("Ping")).thenReturn("Pong");

        Tool4Panel panel = new Tool4Panel(agent, s -> {});
        JTextField input = getPrivate(panel, "inputField", JTextField.class);
        JButton send = getPrivate(panel, "sendButton", JButton.class);
        JTextArea area = getPrivate(panel, "conversationArea", JTextArea.class);

        // when
        onEDTAndWait(() -> input.setText("Ping"));
        onEDTAndWait(send::doClick);

        // then
        waitUntil("réponse via agent visible", 2000, () -> area.getText().contains("Pong"));
        verify(agent, times(1)).ask("Ping");
    }
}
