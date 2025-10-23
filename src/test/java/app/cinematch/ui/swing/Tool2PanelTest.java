package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.model.Recommendation;
import app.cinematch.util.JsonStorage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Tests synchrones et stables pour Tool2Panel.
 * - Pas de SwingWorker/animations: on injecte l'état requis par réflexion
 * - Given / When / Then en commentaires
 */
final class Tool2PanelTest {

    @Test
    @DisplayName("Construction: composants principaux présents (titre, reason, platform, boutons)")
    void constructor_buildsMainComponents() throws Exception {
        // Given
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final java.util.function.Consumer<String> parent = mock(java.util.function.Consumer.class);


        try (MockedStatic<JsonStorage> ignored = mockStatic(JsonStorage.class)) {
            // When
            final Tool2Panel[] ref = new Tool2Panel[1];
            SwingUtilities.invokeAndWait(() -> ref[0] = new Tool2Panel(service, parent));
            final Tool2Panel panel = ref[0];

            // Then
            assertNotNull(getField(panel, "title"));
            assertNotNull(getField(panel, "reason"));
            assertNotNull(getField(panel, "platform"));
            assertNotNull(getField(panel, "likeBtn"));
            assertNotNull(getField(panel, "nopeBtn"));
            assertNotNull(getField(panel, "seenBtn"));
            assertNotNull(getField(panel, "backBtn"));
            assertNotNull(getField(panel, "descPane"));
        }
    }

    @Test
    @DisplayName("htmlEscape + setDescHtml: la description est correctement échappée et injectée")
    void html_isEscaped_andSetIntoPane() throws Exception {
        // Given
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final java.util.function.Consumer<String> parent = mock(java.util.function.Consumer.class);

        final Tool2Panel[] ref = new Tool2Panel[1];
        SwingUtilities.invokeAndWait(() -> ref[0] = new Tool2Panel(service, parent));
        final Tool2Panel panel = ref[0];

        final Method htmlEscape = Tool2Panel.class.getDeclaredMethod("htmlEscape", String.class);
        htmlEscape.setAccessible(true);
        final Method setDescHtml = Tool2Panel.class.getDeclaredMethod("setDescHtml", String.class);
        setDescHtml.setAccessible(true);

        // When on échappe puis on pousse dans setDescHtml via réflexion
        final String escaped = (String) htmlEscape.invoke(panel, "A & B < C > D");
        final String inner = escaped.replace("\n", "<br/>"); // équivalent de htmlCenterBig()
        SwingUtilities.invokeAndWait(() -> {
            try {
                setDescHtml.invoke(panel, inner);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        // Then le JEditorPane contient bien les entités HTML
        final JEditorPane descPane = (JEditorPane) getField(panel, "descPane");
        final String html = descPane.getText();
        assertTrue(html.contains("A &amp; B &lt; C &gt; D"));
    }

    @Test
    @DisplayName("Clic 'Je veux voir' -> service.mark(title,'envie')")
    void likeButton_marksEnvie() throws Exception {
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final java.util.function.Consumer<String> parent = mock(java.util.function.Consumer.class);
        final Recommendation rec = mock(Recommendation.class);
        when(rec.title()).thenReturn("Matrix");

        final Tool2Panel[] ref = new Tool2Panel[1];
        SwingUtilities.invokeAndWait(() -> ref[0] = new Tool2Panel(service, parent));
        final Tool2Panel panel = ref[0];

        setField(panel, "current", rec);
        final JLabel title = (JLabel) getField(panel, "title");
        SwingUtilities.invokeAndWait(() -> title.setText("Matrix"));
        enableButtonsForClick(panel); // <-- important

        final JButton likeBtn = (JButton) getField(panel, "likeBtn");
        SwingUtilities.invokeAndWait(likeBtn::doClick);

        verify(service, atLeastOnce()).mark("Matrix", "envie");
    }


    @Test
    @DisplayName("Clic 'Pas intéressé' -> service.mark(title,'pas_interesse')")
    void nopeButton_marksPasInteresse() throws Exception {
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final java.util.function.Consumer<String> parent = mock(java.util.function.Consumer.class);
        final Recommendation rec = mock(Recommendation.class);
        when(rec.title()).thenReturn("Film Random");

        final Tool2Panel[] ref = new Tool2Panel[1];
        SwingUtilities.invokeAndWait(() -> ref[0] = new Tool2Panel(service, parent));
        final Tool2Panel panel = ref[0];

        setField(panel, "current", rec);
        final JLabel title = (JLabel) getField(panel, "title");
        SwingUtilities.invokeAndWait(() -> title.setText("Film Random"));
        enableButtonsForClick(panel); // <-- important

        final JButton nopeBtn = (JButton) getField(panel, "nopeBtn");
        SwingUtilities.invokeAndWait(nopeBtn::doClick);

        verify(service, atLeastOnce()).mark("Film Random", "pas_interesse");
    }

    @Test
    @DisplayName("Clic 'Déjà vu' -> service.mark(title,'deja_vu')")
    void seenButton_marksDejaVu() throws Exception {
        // Given
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final java.util.function.Consumer<String> parent = mock(java.util.function.Consumer.class);

        final Recommendation rec = mock(Recommendation.class);
        when(rec.title()).thenReturn("Amélie");

        final Tool2Panel[] ref = new Tool2Panel[1];
        SwingUtilities.invokeAndWait(() -> ref[0] = new Tool2Panel(service, parent));
        final Tool2Panel panel = ref[0];

        // injecter l'état et réactiver les boutons
        setField(panel, "current", rec);
        final JLabel title = (JLabel) getField(panel, "title");
        SwingUtilities.invokeAndWait(() -> title.setText("Amélie"));
        enableButtonsForClick(panel); // <-- important

        final JButton seenBtn = (JButton) getField(panel, "seenBtn");

        // When
        SwingUtilities.invokeAndWait(seenBtn::doClick);

        // Then
        verify(service, atLeastOnce()).mark("Amélie", "deja_vu");
    }


    // -------- Helpers réflexion --------

    private static Object getField(final Object target, final String name) throws Exception {
        final Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void enableButtonsForClick(final Tool2Panel panel) throws Exception {
        final JButton likeBtn = (JButton) getField(panel, "likeBtn");
        final JButton nopeBtn = (JButton) getField(panel, "nopeBtn");
        final JButton seenBtn = (JButton) getField(panel, "seenBtn");
        final JButton backBtn = (JButton) getField(panel, "backBtn");
        SwingUtilities.invokeAndWait(() -> {
            likeBtn.setEnabled(true);
            nopeBtn.setEnabled(true);
            seenBtn.setEnabled(true);
            backBtn.setEnabled(true);
        });
    }
}
