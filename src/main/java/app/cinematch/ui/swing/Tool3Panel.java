package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.util.JsonStorage;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public final class Tool3Panel extends JPanel {

    private final MovieRecommenderService service;
    private final Consumer<String> navigator;

    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JEditorPane descPane = new JEditorPane("text/html", "");

    private final JButton refresh = new JButton("Rafraîchir");
    private final JButton describe = new JButton("Générer description");
    private final JButton remove = new JButton("Retirer");
    private final JButton backBtn = new JButton("Retour");

    private static final Color NEON_PINK = new Color(255, 64, 160);
    private static final Color NEON_PINK_DARK = new Color(200, 30, 120);
    private static final Color HOVER_PINK_TXT = new Color(255, 210, 230);
    private static final Color BASE_CARD_BG = new Color(30, 30, 40);
    private static final Color HOVER_CARD_BG = new Color(50, 40, 60);
    private static final Color BG_TOP = new Color(18, 18, 24);
    private static final Color BG_BOTTOM = new Color(35, 20, 40);
    private static final Color TEXT_DIM = new Color(220, 220, 220);

    public Tool3Panel(final MovieRecommenderService service,
                      final Consumer<String> navigator) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.navigator = Objects.requireNonNull(navigator, "navigator must not be null");

        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 20, 20, 20));

        final JPanel topBar = new JPanel(new BorderLayout(12, 12));
        topBar.setOpaque(false);

        final JPanel leftTop = new JPanel();
        leftTop.setOpaque(false);
        styleBackOutlined(backBtn);
        backBtn.addActionListener(e -> this.navigator.accept("home"));
        leftTop.add(backBtn);

        final JPanel actions = new JPanel();
        actions.setOpaque(false);
        styleNeon(refresh);
        styleNeon(describe);
        styleNeon(remove);
        actions.add(refresh);
        actions.add(describe);
        actions.add(remove);

        topBar.add(leftTop, BorderLayout.WEST);
        topBar.add(actions, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(BASE_CARD_BG);
        list.setForeground(Color.WHITE);
        list.setFixedCellHeight(36);
        list.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        list.setCellRenderer(new NeonListCellRenderer());

        final JScrollPane leftScroll = new JScrollPane(list);
        styleScrollAsCard(leftScroll, "MA LISTE");

        descPane.setEditable(false);
        descPane.setOpaque(false);
        setDescHtml("<i>Sélectionnez un film pour voir la description.</i>");
        final JScrollPane rightScroll = new JScrollPane(descPane);
        styleScrollAsCard(rightScroll, "DESCRIPTION");

        final JSplitPane split =
                new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        split.setOpaque(false);
        split.setBorder(null);
        split.setDividerSize(8);
        split.setResizeWeight(0.36);
        add(split, BorderLayout.CENTER);

        refresh.addActionListener(e -> loadWishlist());
        describe.addActionListener(e -> generateForSelection());
        remove.addActionListener(e -> removeSelection());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                setDescHtml("<i>Génération possible avec le bouton ci-dessus.</i>");
            }
        });

        loadWishlist();
    }

    private void loadWishlist() {
        model.clear();
        final List<String> envies = JsonStorage.getByStatus("envie");
        for (String t : envies) {
            model.addElement(stripQuotes(t));
        }
    }

    private String stripQuotes(final String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("[\"“”«»]", "");
    }

    private void generateForSelection() {
        final String t = list.getSelectedValue();
        if (t == null) {
            return;
        }
        setBusy(true);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return service.generateDescription(t);
            }
            @Override
            protected void done() {
                try {
                    final String txt = get();
                    setDescHtml(escape(txt).replace("\n", "<br/>"));
                } catch (final Exception ex) {
                    setDescHtml("<span style='color:#ff8ab8;'>Erreur :</span> "
                            + escape(ex.getMessage()));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void removeSelection() {
        final String t = list.getSelectedValue();
        if (t == null) {
            return;
        }
        JsonStorage.addOrUpdate(t, "pas_interesse");
        loadWishlist();
        setDescHtml("<i>Retiré de la liste.</i>");
    }

    private void setBusy(final boolean busy) {
        refresh.setEnabled(!busy);
        describe.setEnabled(!busy);
        remove.setEnabled(!busy);
        list.setEnabled(!busy);
        backBtn.setEnabled(!busy);
    }

    private void styleNeon(final JButton b) {
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(BASE_CARD_BG);
        b.setOpaque(true);
        final EmptyBorder pad = new EmptyBorder(10, 16, 10, 16);
        b.setBorder(new CompoundBorder(
                new LineBorder(NEON_PINK_DARK, 2, true),
                new CompoundBorder(new LineBorder(NEON_PINK, 1, true), pad)
        ));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(final java.awt.event.MouseEvent e) {
                b.setBackground(HOVER_CARD_BG);
                b.setForeground(HOVER_PINK_TXT);
                b.setBorder(new CompoundBorder(
                        new LineBorder(NEON_PINK, 2, true),
                        new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad)
                ));
            }
            @Override
            public void mouseExited(final java.awt.event.MouseEvent e) {
                b.setBackground(BASE_CARD_BG);
                b.setForeground(Color.WHITE);
                b.setBorder(new CompoundBorder(
                        new LineBorder(NEON_PINK_DARK, 2, true),
                        new CompoundBorder(new LineBorder(NEON_PINK, 1, true), pad)
                ));
            }
        });
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
    }

    private void styleBackOutlined(final JButton b) {
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setForeground(TEXT_DIM);
        final EmptyBorder pad = new EmptyBorder(6, 12, 6, 12);
        b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(final java.awt.event.MouseEvent e) {
                b.setForeground(Color.WHITE);
                b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 2, true), pad));
            }
            @Override
            public void mouseExited(final java.awt.event.MouseEvent e) {
                b.setForeground(TEXT_DIM);
                b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
            }
        });
    }

    private void styleScrollAsCard(final JScrollPane sp, final String title) {
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(new CompoundBorder(
                new LineBorder(NEON_PINK_DARK, 3, true),
                new CompoundBorder(new LineBorder(NEON_PINK, 2, true),
                        new EmptyBorder(8, 8, 8, 8))
        ));
        final JLabel head = new JLabel(title, SwingConstants.CENTER);
        head.setOpaque(true);
        head.setBackground(new Color(20, 20, 28, 180));
        head.setForeground(Color.WHITE);
        head.setFont(new Font("Segoe UI", Font.BOLD, 16));
        head.setBorder(new EmptyBorder(10, 12, 10, 12));
        sp.setColumnHeaderView(head);
    }

    private void setDescHtml(final String inner) {
        // Pas de String.format / formatted -> pas d’avertissement VA_FORMAT_STRING_USES_NEWLINE
        final String html =
                "<html>" + System.lineSeparator()
                        + "  <body style=\"margin:0;padding:0;font-family:'Segoe UI',sans-serif;color:#f5f5f5;\">"
                        + System.lineSeparator()
                        + "    <div style=\"padding:4px 6px; min-height:220px;\">"
                        + System.lineSeparator()
                        + "      <div style=\"font-size:15px;line-height:1.5;\">"
                        + inner
                        + "</div>" + System.lineSeparator()
                        + "    </div>" + System.lineSeparator()
                        + "  </body>" + System.lineSeparator()
                        + "</html>";
        descPane.setText(html);
        descPane.setCaretPosition(0);
    }

    private static String escape(final String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static final class NeonListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(final JList<?> jl, final Object value,
                                                      final int index, final boolean isSelected,
                                                      final boolean cellHasFocus) {
            final JLabel c =
                    (JLabel) super.getListCellRendererComponent(jl, value, index, isSelected, cellHasFocus);
            final String text = c.getText() == null ? "" : c.getText().replaceAll("[\"“”«»]", "");
            c.setText(text);
            c.setOpaque(true);
            c.setBackground(isSelected ? HOVER_CARD_BG : BASE_CARD_BG);
            c.setForeground(isSelected ? HOVER_PINK_TXT : Color.WHITE);
            c.setBorder(new EmptyBorder(8, 12, 8, 12));
            c.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            return c;
        }
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        final int w = getWidth();
        final int h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }
}
