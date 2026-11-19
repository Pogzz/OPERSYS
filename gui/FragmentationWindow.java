package gui;

import LOGICS.FragmentationLogic;
import LOGICS.FragmentationLogic.BlockState;
import LOGICS.FragmentationLogic.Placement;
import LOGICS.FragmentationLogic.Report;
import LOGICS.FragmentationLogic.Strategy;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

public class FragmentationWindow extends BasePanel {

    private static final Color DARK         = new Color(0x0B1220);
    private static final Color ACCENT       = new Color(0x4DA3FF);
    private static final Color TITLE_PURPLE = new Color(0x2A1B8F);
    private static final Color TOGGLE_ON    = new Color(0x1D4ED8);
    private static final Color TOGGLE_OFF   = new Color(0x4DA3FF);
    private static final Color LIGHT_BLUE   = new Color(0x4DA3FF);
    private static final Color RUN_BLUE     = new Color(0x1D4ED8);
    private static final Color BAR_FREE     = new Color(0x1B2740);
    private static final Color BAR_INTERNAL = new Color(0x2A3550);
    private static final Color CELL_BG      = new Color(0x0F1B2D);
    private static final Color GRID         = new Color(0x2A3B55);
    private static final Color TXT_LIGHT    = new Color(0xDDE6F5);
    private static final Color TXT_GREEN    = new Color(0x4ADE80);
    private static final Color TXT_ORANGE   = new Color(0xFBA23C);
    private static final Color TXT_RED      = new Color(0xFF5C5C);

    private static final Color[] PROC_COLORS = {
        new Color(0x3AA0FF), new Color(0x2EA043), new Color(0xE5483B),
        new Color(0xE8A030), new Color(0x9B59B6), new Color(0x1ABC9C)
    };
    private Strategy strategy = Strategy.FIRST_FIT;
    private boolean dynamic = false;
    private boolean hasRun = false;
    private int bestRow = -1;

    private HintField blocksField, procsField;
    private JLabel errorLabel;
    private MapPanel mapPanel;
    private StatCard internalCard, externalCard;
    private DefaultTableModel compareModel, resultsModel;

    public FragmentationWindow() {
        super("Fragmentation");       

        content.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        JPanel left = buildConfig();
        left.setPreferredSize(new Dimension(220, 10));
        c.gridx = 0; c.weightx = 0; c.insets = new Insets(0, 0, 0, 10);
        content.add(left, c);

        JPanel mid = buildMap();
        mid.setPreferredSize(new Dimension(10, 10));
        c.gridx = 1; c.weightx = 0.55;
        content.add(mid, c);

        JPanel right = buildRight();
        right.setPreferredSize(new Dimension(10, 10));
        c.gridx = 2; c.weightx = 0.45; c.insets = new Insets(0, 0, 0, 0);
        content.add(right, c);
    }

    private JPanel buildConfig() {
        Rounded card = new Rounded(18, CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("Configuration & Inputs");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(TITLE_PURPLE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(12));

        card.add(fieldLabel("Strategy:"));
        card.add(Box.createVerticalStrut(4));
        card.add(toggleRow(new String[]{"First Fit", "Best Fit", "Worst Fit"}, 0, i -> {
            strategy = Strategy.values()[i];
            if (hasRun) doRun();
        }));
        card.add(Box.createVerticalStrut(10));
        card.add(fieldLabel("Memory Type:"));
        card.add(Box.createVerticalStrut(4));
        card.add(toggleRow(new String[]{"Fixed", "Dynamic"}, 0, i -> {
            dynamic = (i == 1);
            if (hasRun) doRun();
        }));

        card.add(fieldLabel("Memory Blocks (KB):"));
        card.add(Box.createVerticalStrut(4));
        blocksField = new HintField("e.g. 100, 500, 200, 300, 600");
        blocksField.addActionListener(e -> doRun());
        card.add(blocksField);
        card.add(Box.createVerticalStrut(10));

        card.add(fieldLabel("Processes (KB):"));
        card.add(Box.createVerticalStrut(4));
        procsField = new HintField("e.g. 212, 417, 112, 426");
        procsField.addActionListener(e -> doRun());
        card.add(procsField);

        card.add(Box.createVerticalGlue());

        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        errorLabel.setForeground(new Color(0xD32F2F));
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setPreferredSize(new Dimension(10, 30));
        errorLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        errorLabel.setVerticalAlignment(SwingConstants.TOP);
        card.add(errorLabel);

        RoundButton rnd = new RoundButton("Randomize", LIGHT_BLUE);
        RoundButton run = new RoundButton("Run", RUN_BLUE);
        for (RoundButton b : new RoundButton[]{rnd, run}) {
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            b.setPreferredSize(new Dimension(10, 32));
        }
        rnd.addActionListener(e -> randomize());
        run.addActionListener(e -> doRun());
        card.add(rnd);
        card.add(Box.createVerticalStrut(8));
        card.add(run);
        return card;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(0x1A1A3A));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel toggleRow(String[] names, int selected, IntConsumer onSelect) {
        JPanel row = new JPanel(new GridLayout(3, names.length, 3, 5));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setPreferredSize(new Dimension(15, 90));
        ButtonGroup g = new ButtonGroup();
        for (int i = 0; i < names.length; i++) {
            ToggleBtn b = new ToggleBtn(names[i]);
            b.setSelected(i == selected);
            final int idx = i;
            b.addActionListener(e -> onSelect.accept(idx));
            g.add(b);
            row.add(b);
        }
        return row;
    }

    // =====================================================================
    // MIDDLE: Memory Map + stat cards
    // =====================================================================
    private JPanel buildMap() {
        Rounded card = new Rounded(18, DARK);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel t = darkTitle("Memory Map");
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(t);
        head.add(Box.createVerticalStrut(4));
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        legend.setOpaque(false);
        legend.setAlignmentX(Component.LEFT_ALIGNMENT);
        legend.add(legendItem(PROC_COLORS[0], "Process"));
        legend.add(legendItem(BAR_INTERNAL, "Internal Fragmentation"));
        legend.add(legendItem(BAR_FREE, "Free"));
        head.add(legend);
        card.add(head, BorderLayout.NORTH);

        mapPanel = new MapPanel();
        JScrollPane sp = new JScrollPane(mapPanel);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        card.add(sp, BorderLayout.CENTER);

        JPanel stats = new JPanel(new GridLayout(1, 2, 10, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(10, 84));
        internalCard = new StatCard("Internal Fragmentation");
        externalCard = new StatCard("External Fragmentation");
        stats.add(internalCard);
        stats.add(externalCard);
        card.add(stats, BorderLayout.SOUTH);
        return card;
    }

    private static JLabel darkTitle(String s) {
        JLabel l = new JLabel(s);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(ACCENT);
        return l;
    }

    private static JPanel legendItem(Color c, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JComponent dot = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillOval(0, 0, 10, 10);
                g2.setColor(new Color(0x4A5A7A));
                g2.drawOval(0, 0, 9, 9);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(11, 11));
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        l.setForeground(TXT_LIGHT);
        p.add(dot);
        p.add(l);
        return p;
    }

    // =====================================================================
    // RIGHT: Compare Strategies + Allocation Results
    // =====================================================================
    private JPanel buildRight() {
        JPanel col = new JPanel(new GridLayout(2, 1, 0, 10));
        col.setOpaque(false);

        // --- Compare Strategies
        compareModel = new DefaultTableModel(new Object[]{"Strategy", "Placed", "Internal", "External"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Strategy s : Strategy.values()) compareModel.addRow(new Object[]{s.label, "", "", ""});
        JTable compareTable = new JTable(compareModel);
        styleDarkTable(compareTable, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v,
                    boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, r, c);
                boolean best = hasRun && r == bestRow;
                l.setHorizontalAlignment(CENTER);
                l.setOpaque(true);
                l.setBackground(r == strategy.ordinal() ? new Color(0x16294A) : CELL_BG);
                l.setForeground(best ? TXT_GREEN : TXT_LIGHT);
                l.setFont(l.getFont().deriveFont(best ? Font.BOLD : Font.PLAIN));
                return l;
            }
        });
        JPanel compareBody = new JPanel(new BorderLayout(0, 4));
        compareBody.setOpaque(false);
        compareBody.add(tableScroll(compareTable), BorderLayout.CENTER);
        JLabel note = new JLabel("Green = best (most placed, least waste)");
        note.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        note.setForeground(new Color(0x7F90AD));
        compareBody.add(note, BorderLayout.SOUTH);
        col.add(darkCard("Compare Strategies", compareBody));

        // --- Allocation Results
        resultsModel = new DefaultTableModel(
                new Object[]{"Process", "Size (KB)", "Block", "Wasted", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (int i = 1; i <= 3; i++) resultsModel.addRow(new Object[]{"P" + i, "", "", "", ""});
        JTable resultsTable = new JTable(resultsModel);
        styleDarkTable(resultsTable, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v,
                    boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, r, c);
                l.setHorizontalAlignment(CENTER);
                l.setOpaque(true);
                l.setBackground(CELL_BG);
                l.setForeground(TXT_LIGHT);
                l.setFont(l.getFont().deriveFont(Font.PLAIN));
                if (c == 4) {
                    String s = String.valueOf(v);
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                    if (s.startsWith("Allocated")) l.setForeground(TXT_GREEN);
                    else if (s.startsWith("Ext")) l.setForeground(TXT_ORANGE);
                    else if (s.startsWith("No")) l.setForeground(TXT_RED);
                }
                return l;
            }
        });
        col.add(darkCard("Allocation Results", tableScroll(resultsTable)));
        return col;
    }

    private static JPanel darkCard(String title, JComponent body) {
        Rounded card = new Rounded(18, DARK);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        card.add(darkTitle(title), BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private static JScrollPane tableScroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createLineBorder(GRID));
        sp.getViewport().setBackground(CELL_BG);
        return sp;
    }

    private static void styleDarkTable(JTable t, DefaultTableCellRenderer body) {
        t.setRowHeight(24);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setGridColor(GRID);
        t.setBackground(CELL_BG);
        t.setForeground(TXT_LIGHT);
        t.setFillsViewportHeight(true);
        t.setFocusable(false);
        t.setRowSelectionAllowed(false);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(10, 26));

        DefaultTableCellRenderer head = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v,
                    boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, s, f, r, c);
                l.setBackground(new Color(0x13233F));
                l.setForeground(Color.WHITE);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                l.setHorizontalAlignment(CENTER);
                l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, GRID));
                return l;
            }
        };
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(body);
            t.getColumnModel().getColumn(i).setHeaderRenderer(head);
        }
    }

    // =====================================================================
    // ACTIONS
    // =====================================================================
    private void doRun() {
        errorLabel.setText(" ");
        List<Integer> blocks, procs;
        try {
            blocks = FragmentationLogic.parseList(blocksField.getText(), "Memory Blocks");
            procs = FragmentationLogic.parseList(procsField.getText(), "Processes");
        } catch (IllegalArgumentException ex) {
            errorLabel.setText("<html>" + ex.getMessage() + "</html>");
            return;
        }
        hasRun = true;

        // Pangunahing resulta (napiling strategy)
        Report r = FragmentationLogic.run(strategy, dynamic, blocks, procs);
        mapPanel.setBlocks(r.blocks);
        internalCard.setValue(r.internal);
        externalCard.setValue(r.external);

        resultsModel.setRowCount(0);
        for (Placement p : r.placements) {
            resultsModel.addRow(new Object[]{
                    "P" + p.process, p.size,
                    p.isAllocated() ? "Block " + p.block : "\u2014",
                    p.isAllocated() ? p.wasted + " KB" : "\u2014",
                    p.status});
        }

        // Compare: patakbuhin ang lahat ng strategy sa parehong input
        Strategy[] all = Strategy.values();
        Report bestRep = null;
        bestRow = 0;
        for (int i = 0; i < all.length; i++) {
            Report x = FragmentationLogic.run(all[i], dynamic, blocks, procs);
            compareModel.setValueAt(x.placed + "/" + x.total, i, 1);
            compareModel.setValueAt(x.internal + " KB", i, 2);
            compareModel.setValueAt(x.external + " KB", i, 3);
            if (bestRep == null || x.placed > bestRep.placed
                    || (x.placed == bestRep.placed && x.internal + x.external < bestRep.internal + bestRep.external)) {
                bestRep = x;
                bestRow = i;
            }
        }
        repaint();
    }

    private void randomize() {
        blocksField.setText(join(FragmentationLogic.randomBlocks(5)));
        procsField.setText(join(FragmentationLogic.randomProcesses(4)));
        doRun();
    }

    private static String join(List<Integer> l) {
        return l.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }

    // =====================================================================
    // CUSTOM COMPONENTS
    // =====================================================================
    private static class HintField extends JTextField {
        private final String hint;
        HintField(String hint) {
            this.hint = hint;
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            setPreferredSize(new Dimension(10, 30));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x9AA7C7), 1, true),
                    new EmptyBorder(2, 8, 2, 8)));
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(0x9AA3B5));
                g2.setFont(getFont().deriveFont(Font.ITALIC, 11f));
                Insets in = getInsets();
                g2.drawString(hint, in.left, (getHeight() + g2.getFontMetrics().getAscent()) / 2 - 2);
                g2.dispose();
            }
        }
    }

    private static class ToggleBtn extends JToggleButton {
        ToggleBtn(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isSelected() ? TOGGLE_ON : TOGGLE_OFF);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundButton extends JButton {
        private final Color base; private boolean hover;
        RoundButton(String text, Color base) {
            super(text);
            this.base = base;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = getModel().isPressed() ? base.darker().darker() : hover ? base.darker() : base;
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 3, 12, 12);
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Outlined card na may title at malaking "12KB" value. */
    private static class StatCard extends JPanel {
        private final JLabel value = new JLabel("\u2014", SwingConstants.CENTER);
        StatCard(String title) {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(8, 12, 8, 12));
            JLabel t = new JLabel("<html>" + title.replace(" ", "<br>") + "</html>");
            t.setFont(new Font("Segoe UI", Font.BOLD, 11));
            t.setForeground(Color.WHITE);
            value.setFont(new Font("Segoe UI", Font.BOLD, 22));
            value.setForeground(new Color(0x4DA3FF));
            add(t, BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
        }
        void setValue(int kb) { value.setText(kb + "KB"); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0x101B2E));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(new Color(0x3A4F70));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Bawat block = isang row: label sa kaliwa, bar sa kanan (process / internal frag / free). */
    private static class MapPanel extends JPanel {
        private static final int ROW = 52;
        private List<BlockState> blocks = new java.util.ArrayList<>();

        MapPanel() { setOpaque(false); }

        void setBlocks(List<BlockState> b) {
            blocks = b;
            setPreferredSize(new Dimension(200, b.size() * ROW + 8));
            revalidate();
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();

            if (blocks.isEmpty()) {
                g.setColor(new Color(0x6B7C99));
                g.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                String m = "Enter blocks & processes, then press Run";
                g.drawString(m, Math.max(4, (w - g.getFontMetrics().stringWidth(m)) / 2), 40);
                g.dispose();
                return;
            }

            int y = 4;
            for (BlockState b : blocks) {
                drawLabel(g, b, y);
                drawBar(g, b, 76, y + 4, Math.max(40, w - 76 - 8), 40);
                y += ROW;
            }
            g.dispose();
        }

        private void drawLabel(Graphics2D g, BlockState b, int y) {
            g.setColor(new Color(0x3A4256));
            g.fillRoundRect(4, y + 4, 64, 40, 10, 10);
            g.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g.setColor(Color.WHITE);
            String a = "Block " + b.no;
            g.drawString(a, 4 + (64 - g.getFontMetrics().stringWidth(a)) / 2, y + 4 + 17);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g.setColor(new Color(0xC9D4E8));
            String s = b.size + "KB";
            g.drawString(s, 4 + (64 - g.getFontMetrics().stringWidth(s)) / 2, y + 4 + 31);
        }

        private void drawBar(Graphics2D g, BlockState b, int x, int y, int w, int h) {
            RoundRectangle2D shape = new RoundRectangle2D.Float(x, y, w, h, 12, 12);
            Shape old = g.getClip();
            g.setClip(shape);

            g.setColor(BAR_FREE);
            g.fillRect(x, y, w, h);

            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fm = g.getFontMetrics();
            int cum = 0;
            for (int[] p : b.procs) {
                int x1 = x + (int) ((long) w * cum / b.size);
                cum += p[1];
                int x2 = x + (int) ((long) w * cum / b.size);
                g.setColor(PROC_COLORS[(p[0] - 1) % PROC_COLORS.length]);
                g.fillRect(x1, y, x2 - x1, h);
                g.setColor(new Color(0, 0, 0, 70));
                g.drawLine(x2 - 1, y, x2 - 1, y + h);

                String full = "P" + p[0] + " \u00B7 " + p[1] + "KB", shortS = "P" + p[0];
                String lbl = fm.stringWidth(full) + 8 <= x2 - x1 ? full
                           : fm.stringWidth(shortS) + 6 <= x2 - x1 ? shortS : null;
                if (lbl != null) {
                    g.setColor(Color.WHITE);
                    g.drawString(lbl, x1 + (x2 - x1 - fm.stringWidth(lbl)) / 2, y + h / 2 + fm.getAscent() / 2 - 2);
                }
            }

            int x1 = x + (int) ((long) w * cum / b.size);
            int restW = x + w - x1;
            if (restW > 0) {
                if (b.internalFrag() > 0) {
                    // hatched = internal fragmentation
                    g.setColor(BAR_INTERNAL);
                    g.fillRect(x1, y, restW, h);
                    Shape c2 = g.getClip();
                    g.clipRect(x1, y, restW, h);
                    g.setColor(new Color(0x4A5A7A));
                    for (int i = x1 - h; i < x + w; i += 7) g.drawLine(i, y + h, i + h, y);
                    g.setClip(c2);
                    drawCentered(g, "Internal", x1, restW, y, h, new Color(0xC9D4E8));
                } else {
                    drawCentered(g, "Free", x1, restW, y, h, new Color(0x5E6F8C));
                }
            }

            g.setClip(old);
            g.setColor(new Color(0x2A3B55));
            g.draw(shape);
        }

        private void drawCentered(Graphics2D g, String s, int x, int w, int y, int h, Color c) {
            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fm = g.getFontMetrics();
            if (fm.stringWidth(s) + 8 > w) return;
            g.setColor(c);
            g.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + h / 2 + fm.getAscent() / 2 - 2);
        }
    }
}