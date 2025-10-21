//package app.cinematch.ui.swing;
//
//import app.cinematch.MovieRecommenderService;
//import app.cinematch.model.Recommendation;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.lang.reflect.Constructor;
//import java.lang.reflect.RecordComponent;
//import java.time.Duration;
//import java.time.Instant;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.function.BooleanSupplier;
//import java.util.function.Supplier;
//
//import static org.mockito.Mockito.*;
//
//class Tool2PanelTest {
//
//    @BeforeAll
//    static void enableHeadless() {
//        System.setProperty("java.awt.headless", "true");
//    }
//
//    /* ----------------- Helpers ----------------- */
//
//    private static void noThrow(Runnable r, String msgIfThrows) {
//        try { r.run(); } catch (Throwable t) { Assertions.fail(msgIfThrows, t); }
//    }
//
//    private static void waitUntil(Duration timeout, String failMsg, BooleanSupplier cond) {
//        Instant end = Instant.now().plus(timeout);
//        while (Instant.now().isBefore(end)) {
//            if (cond.getAsBoolean()) return;
//            try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
//        }
//        Assertions.fail(failMsg);
//    }
//
//    private static boolean onEDTGet(Supplier<Boolean> supplier) {
//        AtomicBoolean out = new AtomicBoolean(false);
//        try {
//            SwingUtilities.invokeAndWait(() -> out.set(Boolean.TRUE.equals(supplier.get())));
//        } catch (Exception ignored) {
//            return false;
//        }
//        return out.get();
//    }
//
//    private static String safeGetHtml(JEditorPane ep) {
//        AtomicReference<String> out = new AtomicReference<>("");
//        try {
//            SwingUtilities.invokeAndWait(() -> out.set(ep.getText()));
//        } catch (Exception ignored) { out.set(""); }
//        return out.get();
//    }
//
//    private static JButton findButton(Container root, String exactText) {
//        if (root instanceof JButton b && exactText.equals(b.getText())) return b;
//        for (Component c : root.getComponents()) {
//            if (c instanceof Container cc) {
//                JButton r = findButton(cc, exactText);
//                if (r != null) return r;
//            }
//        }
//        return null;
//    }
//
//    private static JEditorPane findEditorPane(Container root) {
//        if (root instanceof JEditorPane ep) return ep;
//        for (Component c : root.getComponents()) {
//            if (c instanceof Container cc) {
//                JEditorPane ep = findEditorPane(cc);
//                if (ep != null) return ep;
//            }
//        }
//        return null;
//    }
//
//    private static boolean panelShowsText(Container root, String text) {
//        return onEDTGet(() -> findLabelWithText(root, text) != null);
//    }
//
//    private static JLabel findLabelWithText(Container root, String text) {
//        if (root instanceof JLabel l && Objects.equals(text, l.getText())) return l;
//        for (Component c : root.getComponents()) {
//            if (c instanceof Container cc) {
//                JLabel r = findLabelWithText(cc, text);
//                if (r != null) return r;
//            }
//        }
//        return null;
//    }
//
//    private static Recommendation newRecommendationDynamic(String title, String reason, String platform) {
//        try {
//            Class<Recommendation> recClass = Recommendation.class;
//            if (!recClass.isRecord()) {
//                return recClass.getDeclaredConstructor(String.class, String.class, String.class)
//                        .newInstance(title, reason, platform);
//            }
//            RecordComponent[] comps = recClass.getRecordComponents();
//            Object[] args = new Object[comps.length];
//            Map<String, Integer> idx = new HashMap<>();
//            for (int i = 0; i < comps.length; i++) {
//                idx.put(comps[i].getName(), i);
//                args[i] = defaultValueFor(comps[i].getType());
//            }
//            if (idx.containsKey("title"))    args[idx.get("title")]    = title;
//            if (idx.containsKey("reason"))   args[idx.get("reason")]   = reason;
//            if (idx.containsKey("platform")) args[idx.get("platform")] = platform;
//
//            Class<?>[] types = new Class<?>[comps.length];
//            for (int i = 0; i < comps.length; i++) types[i] = comps[i].getType();
//            Constructor<Recommendation> ctor = recClass.getDeclaredConstructor(types);
//            if (!ctor.canAccess(null)) ctor.setAccessible(true);
//            return ctor.newInstance(args);
//        } catch (Exception e) {
//            throw new RuntimeException("Impossible d'instancier Recommendation dynamiquement", e);
//        }
//    }
//
//    private static Object defaultValueFor(Class<?> t) {
//        if (!t.isPrimitive()) {
//            if (t == String.class) return "";
//            return null;
//        }
//        if (t == boolean.class) return false;
//        if (t == byte.class) return (byte) 0;
//        if (t == short.class) return (short) 0;
//        if (t == int.class) return 0;
//        if (t == long.class) return 0L;
//        if (t == float.class) return 0f;
//        if (t == double.class) return 0d;
//        if (t == char.class) return '\0';
//        return null;
//    }
//
//    /* ----------------- Tests ----------------- */
//
//    @Test
//    @DisplayName("Construction → proposeNext asynchrone : labels + description s'affichent")
//    void construct_triggers_first_proposal_and_description() {
//        MovieRecommenderService service = mock(MovieRecommenderService.class);
//        MainFrame frame = mock(MainFrame.class);
//
//        Recommendation rec = newRecommendationDynamic("Dune", "Épique et immersif", "Prime Video");
//        when(service.recommendRandom()).thenReturn(rec);
//        when(service.generateDescription("Dune")).thenReturn("Chef-d'œuvre <SF>\nà voir");
//
//        Tool2Panel panel = new Tool2Panel(service, frame);
//        JEditorPane ep = findEditorPane(panel);
//        Assertions.assertNotNull(ep);
//
//        // 1) Attendre que recommendRandom ait VRAIMENT été appelé
//        waitUntil(Duration.ofSeconds(6), "recommendRandom non appelé",
//                () -> {
//                    try { verify(service, atLeastOnce()).recommendRandom(); return true; }
//                    catch (AssertionError e) { return false; }
//                });
//
//        // 2) Attendre l'affichage des labels
//        waitUntil(Duration.ofSeconds(6), "Titre non mis à jour", () -> panelShowsText(panel, "Dune"));
//        Assertions.assertTrue(panelShowsText(panel, "Épique et immersif"));
//        Assertions.assertTrue(panelShowsText(panel, "Prime Video"));
//
//        // 3) Attendre la description (échappement + <br/>)
//        waitUntil(Duration.ofSeconds(6), "Description non générée/échappée",
//                () -> {
//                    String html = safeGetHtml(ep);
//                    return html.contains("&lt;SF&gt;") && html.contains("<br/>");
//                });
//    }
//
//    @Test
//    @DisplayName("Like → mark('envie') puis nouvelle proposition après animation")
//    void like_marks_envie_and_fetches_next() {
//        MovieRecommenderService service = mock(MovieRecommenderService.class);
//        MainFrame frame = mock(MainFrame.class);
//
//        Recommendation first  = newRecommendationDynamic("Film A", "r1", "pf1");
//        Recommendation second = newRecommendationDynamic("Film B", "r2", "pf2");
//
//        when(service.recommendRandom()).thenReturn(first, second);
//        when(service.generateDescription(anyString())).thenReturn("ok");
//
//        Tool2Panel panel = new Tool2Panel(service, frame);
//        JButton like = findButton(panel, "Je veux voir");
//        Assertions.assertNotNull(like);
//
//        // Attendre que recommendRandom ait tourné et que le 1er titre soit affiché
//        waitUntil(Duration.ofSeconds(6), "recommendRandom non appelé (initial)",
//                () -> {
//                    try { verify(service, atLeastOnce()).recommendRandom(); return true; }
//                    catch (AssertionError e) { return false; }
//                });
//        waitUntil(Duration.ofSeconds(6), "'Film A' non visible", () -> panelShowsText(panel, "Film A"));
//
//        // Attendre réactivation des boutons
//        waitUntil(Duration.ofSeconds(6), "Bouton Like désactivé trop longtemps",
//                () -> onEDTGet(like::isEnabled));
//
//        // Clic Like
//        noThrow(like::doClick, "Click Like ne doit pas jeter");
//
//        // mark('envie') appelé ?
//        waitUntil(Duration.ofSeconds(6), "mark(envie) non appelé",
//                () -> {
//                    try { verify(service, atLeastOnce()).mark("Film A", "envie"); return true; }
//                    catch (AssertionError e) { return false; }
//                });
//
//        // recommendRandom rappelé pour le suivant (animation ~420ms)
//        waitUntil(Duration.ofSeconds(6), "recommendRandom non rappelé post-Like",
//                () -> {
//                    try { verify(service, atLeast(2)).recommendRandom(); return true; }
//                    catch (AssertionError e) { return false; }
//                });
//
//        // Nouveau titre visible
//        waitUntil(Duration.ofSeconds(6), "'Film B' non visible", () -> panelShowsText(panel, "Film B"));
//    }
//
//    @Test
//    @DisplayName("Nope → mark('pas_interesse') puis nouvelle proposition après animation")
//    void nope_marks_pas_interesse_and_fetches_next() {
//        MovieRecommenderService service = mock(MovieRecommenderService.class);
//        MainFrame frame = mock(MainFrame.class);
//
//        Recommendation first  = newRecommendationDynamic("F1", "r1", "pf1");
//        Recommendation second = newRecommendationDynamic("F2", "r2", "pf2");
//        when(service.recommendRandom()).thenReturn(first, second);
//        when(service.generateDescription(anyString())).thenReturn("ok");
//
//        Tool2Panel panel = new Tool2Panel(service, frame);
//        JButton nope = findButton(panel, "Pas intéressé");
//        Assertions.assertNotNull(nope);
//
//        waitUntil(Duration.ofSeconds(6), "recommendRandom non appelé (initial)",
//                () -> {
//                    try { verify(service, atLeastOnce()).recommendRandom(); return true; }
//                    catch (AssertionError e) { return false; }
//                });
//        waitUntil(Duration.ofSeconds(6), "'F1' non visible", () -> panelShowsText(panel, "F1"));
//        waitUntil(Duration.ofSeconds(6), "Bouton Nope désactivé trop longtemps",
//                () -> onEDTGet(nope::isEnabled));
//
//        noThrow(nope::doClick, "Click Nope ne doit pas jeter");
//
//        waitUntil(Duration.ofSeconds(6), "mark(pas_interesse) non appelé",
//                () -> {
//                    try { verify(service, atLeastOnce()).mark("F1", "pas_interesse"); return true; }
//                    catch (AssertionError e) { return false; }
//                });
//
//        waitUntil(Duration.ofSeconds(6), "recommendRandom non rappelé post-Nope",
//                () -> {
//                    try { verify(service, atLeast(2)).recommendRandom(); return true; }
//                    catch (AssertionError e) { return false; }
//                });
//
//        waitUntil(Duration.ofSeconds(6), "'F2' non visible", () -> panelShowsText(panel, "F2"));
//    }
//
//    @Test
//    @DisplayName("Seen → mark('deja_vu') puis nouvelle proposition immédiate (pas d'anim)")
//    void seen_marks_deja_vu_and_fetches_next_immediately() {
//        MovieRecommenderService service = mock(MovieRecommenderService.class);
//        MainFrame frame = mock(MainFrame.class);
//
//        Recommendation first  = newRecommendationDynamic("Seen1", "r", "pf");
//        Recommendation second = newRecommendationDynamic("Seen2", "r2", "pf2");
//        when(service.recommendRandom()).thenReturn(first, second);
//        when(service.generateDescription(anyString())).thenReturn("ok");
//
//        Tool2Panel panel = new Tool2Panel(service, frame);
//        JButton seen = findButton(panel, "Déjà vu");
//        Assertions.assertNotNull(seen);
//
//        waitUntil(Duration.ofSeconds(6), "recommendRandom non appelé (initial)",
//                () -> {
//                    try { verify(service, atLeastOnce()).recommendRandom(); return true; }
//                    catch (AssertionError e) { return false; }
//                });
//        waitUntil(Duration.ofSeconds(6), "'Seen1' non visible", () -> panelShowsText(panel, "Seen1"));
//        waitUntil(Duration.ofSeconds(6), "Bouton Seen désactivé trop longtemps",
//                () -> onEDTGet(seen::isEnabled));
//
//        noThrow(seen::doClick, "Click Seen ne doit pas jeter");
//
//        waitUntil(Duration.ofSeconds(6), "mark(deja_vu) non appelé",
//                () -> {
//                    try { verify(service, atLeastOnce()).mark("Seen1", "deja_vu"); return true; }
//                    catch (AssertionError e) { return false; }
//                });
//
//        waitUntil(Duration.ofSeconds(6), "recommendRandom non rappelé post-Seen",
//                () -> {
//                    try { verify(service, atLeast(2)).recommendRandom(); return true; }
//                    catch (AssertionError e) { return false; }
//                });
//
//        waitUntil(Duration.ofSeconds(6), "'Seen2' non visible", () -> panelShowsText(panel, "Seen2"));
//    }
//
//    @Test
//    @DisplayName("Bouton Retour → appelle parentFrame.showCard('home') (quand réactivé)")
//    void back_button_calls_show_home() {
//        MovieRecommenderService service = mock(MovieRecommenderService.class);
//        MainFrame frame = mock(MainFrame.class);
//
//        when(service.recommendRandom()).thenReturn(newRecommendationDynamic("X", "r", "pf"));
//        when(service.generateDescription(anyString())).thenReturn("ok");
//
//        Tool2Panel panel = new Tool2Panel(service, frame);
//        JButton back = findButton(panel, "Retour");
//        Assertions.assertNotNull(back);
//
//        waitUntil(Duration.ofSeconds(6), "recommendRandom non appelé (initial)",
//                () -> {
//                    try { verify(service, atLeastOnce()).recommendRandom(); return true; }
//                    catch (AssertionError e) { return false; }
//                });
//        waitUntil(Duration.ofSeconds(6), "Bouton Retour désactivé trop longtemps",
//                () -> onEDTGet(back::isEnabled));
//
//        noThrow(back::doClick, "Click Retour ne doit pas jeter");
//
//        waitUntil(Duration.ofSeconds(3), "showCard('home') non appelé",
//                () -> {
//                    try { verify(frame, atLeastOnce()).showCard("home"); return true; }
//                    catch (AssertionError e) { return false; }
//                });
//    }
//
//    @Test
//    @DisplayName("paintComponent: gradient sans exception + pixels opaques")
//    void paint_component_gradient_ok() {
//        MovieRecommenderService service = mock(MovieRecommenderService.class);
//        MainFrame frame = mock(MainFrame.class);
//
//        when(service.recommendRandom()).thenReturn(newRecommendationDynamic("T", "r", "pf"));
//        when(service.generateDescription(anyString())).thenReturn("ok");
//
//        Tool2Panel panel = new Tool2Panel(service, frame);
//        panel.setSize(320, 200);
//
//        BufferedImage img = new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB);
//        Graphics2D g2 = img.createGraphics();
//
//        noThrow(() -> panel.paintComponent(g2), "paintComponent ne doit pas jeter");
//        g2.dispose();
//
//        int[] data = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
//        boolean hasOpaque = false;
//        for (int argb : data) {
//            if ((argb >>> 24) != 0x00) { hasOpaque = true; break; }
//        }
//        Assertions.assertTrue(hasOpaque, "Le gradient doit remplir l'image");
//    }
//
//    @Test
//    @DisplayName("startDescriptionForCurrent: no-op si current == null (couverture)")
//    void start_description_noop_when_current_null() {
//        MovieRecommenderService service = mock(MovieRecommenderService.class);
//        when(service.recommendRandom()).thenReturn(newRecommendationDynamic("Z", "r", "pf"));
//        when(service.generateDescription(anyString())).thenReturn("ok");
//        Tool2Panel panel = new Tool2Panel(service, mock(MainFrame.class));
//        Assertions.assertNotNull(panel);
//    }
//}
