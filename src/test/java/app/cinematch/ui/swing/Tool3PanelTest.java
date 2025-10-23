package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests stables et synchrones de Tool3Panel.
 * Aucune dépendance au fichier JSON réel.
 * Vérifie l'état de l'UI et le comportement de base.
 */
final class Tool3PanelTest {

    @BeforeAll
    static void setupEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {}); // force l'initialisation EDT
    }

    @AfterAll
    static void teardownEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {}); // vide la file EDT
    }

    // ---------------------------------------------------------------
    // TEST 1 : Construction basique
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Construction : composants présents et JEditorPane HTML configuré")
    void constructor_buildsUI_andHtmlPaneConfigured() throws Exception {
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final Consumer<String> navigator = mock(Consumer.class);

        final Tool3Panel[] ref = new Tool3Panel[1];
        SwingUtilities.invokeAndWait(() -> ref[0] = new Tool3Panel(service, navigator));
        final Tool3Panel panel = ref[0];

        final JEditorPane descPane = (JEditorPane) getField(panel, "descPane");
        final JList<?> list = (JList<?>) getField(panel, "list");

        assertNotNull(descPane, "Le panneau de description doit exister.");
        assertEquals("text/html", descPane.getContentType(),
                "Le panneau de description doit être configuré en text/html.");
        assertNotNull(list, "La JList de films doit exister.");
    }

    // ---------------------------------------------------------------
    // TEST 2 : Vérifie stripQuotes
    // ---------------------------------------------------------------
    @Test
    @DisplayName("stripQuotes retire tous les guillemets et doubles quotes")
    void stripQuotes_removesAllQuoteTypes() throws Exception {
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final Consumer<String> nav = mock(Consumer.class);
        final Tool3Panel panel = new Tool3Panel(service, nav);

        final Method stripQuotes = Tool3Panel.class.getDeclaredMethod("stripQuotes", String.class);
        stripQuotes.setAccessible(true);

        final String input = "\"«“Film Test”»\"";
        final String result = (String) stripQuotes.invoke(panel, input);

        assertEquals("Film Test", result);
    }

    // ---------------------------------------------------------------
    // TEST 3 : Vérifie escape() et setDescHtml()
    // ---------------------------------------------------------------
    @Test
    @DisplayName("escape() échappe bien les caractères spéciaux HTML")
    void escape_and_setDescHtml_workProperly() throws Exception {
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final Consumer<String> nav = mock(Consumer.class);
        final Tool3Panel panel = new Tool3Panel(service, nav);

        final Method escape = Tool3Panel.class.getDeclaredMethod("escape", String.class);
        escape.setAccessible(true);
        final Method setDescHtml = Tool3Panel.class.getDeclaredMethod("setDescHtml", String.class);
        setDescHtml.setAccessible(true);

        final String escaped = (String) escape.invoke(null, "A & B < C > D");
        assertEquals("A &amp; B &lt; C &gt; D", escaped);

        SwingUtilities.invokeAndWait(() -> {
            try {
                setDescHtml.invoke(panel, "Test HTML");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        final JEditorPane descPane = (JEditorPane) getField(panel, "descPane");
        assertTrue(descPane.getText().contains("Test HTML"));
    }

    // ---------------------------------------------------------------
    // TEST 4 : Vérifie le clic sur le bouton "Retour"
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Clic sur Retour appelle navigator.accept('home')")
    void backButton_triggersNavigatorHome() throws Exception {
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final Consumer<String> navigator = mock(Consumer.class);

        final Tool3Panel[] ref = new Tool3Panel[1];
        SwingUtilities.invokeAndWait(() -> ref[0] = new Tool3Panel(service, navigator));
        final Tool3Panel panel = ref[0];

        final JButton backBtn = (JButton) getField(panel, "backBtn");
        SwingUtilities.invokeAndWait(() -> backBtn.doClick());

        verify(navigator, atLeastOnce()).accept("home");
    }

    // ---------------------------------------------------------------
    // TEST 5 : Vérifie removeSelection (synchrone)
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Remove : affiche un message pertinent après suppression")
    void remove_showsRemovalMessage_withoutSizeAssumption() throws Exception {
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final Consumer<String> navigator = mock(Consumer.class);

        final Tool3Panel[] ref = new Tool3Panel[1];
        SwingUtilities.invokeAndWait(() -> ref[0] = new Tool3Panel(service, navigator));
        final Tool3Panel panel = ref[0];

        final JList<?> list = (JList<?>) getField(panel, "list");
        final JEditorPane descPane = (JEditorPane) getField(panel, "descPane");

        // On ajoute un élément fictif pour simuler une sélection
        final DefaultListModel<String> model = (DefaultListModel<String>) list.getModel();
        SwingUtilities.invokeAndWait(() -> {
            model.addElement("Titre A");
            list.setSelectedIndex(0);
        });

        // Appel direct à removeSelection()
        final Method removeSelection = Tool3Panel.class.getDeclaredMethod("removeSelection");
        removeSelection.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                removeSelection.invoke(panel);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Lecture du HTML affiché
        final String html = descPane.getText();

        // ✅ Accepte toutes les variantes possibles (Unicode ou entités HTML)
        final boolean removed =
                html.contains("Retiré de la liste.")
                        || html.contains("Retir&#233; de la liste.")
                        || html.contains("Retir&eacute; de la liste.");
        final boolean alt =
                html.contains("Génération possible avec le bouton ci-dessus.")
                        || html.contains("G&#233;n&#233;ration possible avec le bouton ci-dessus.")
                        || html.contains("G&eacute;n&eacute;ration possible avec le bouton ci-dessus.");

        assertTrue(removed || alt,
                () -> "Le panneau doit afficher un message pertinent après la suppression. Texte actuel : " + html);
    }

    // ---------------------------------------------------------------
    // Helpers reflection
    // ---------------------------------------------------------------
    private static Object getField(final Object target, final String name) throws Exception {
        final Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }
}
