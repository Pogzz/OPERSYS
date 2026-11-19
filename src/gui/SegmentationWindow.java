package gui;

import LOGICS.SegmentationLogic;
import LOGICS.SegmentationLogic.Result;
import LOGICS.SegmentationLogic.Segment;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class SegmentationWindow extends BasePanel {

    private static final Color CARD_LIGHT  = new Color(0xD9DEE8);
    private static final Color CARD_DARK   = new Color(0x0B1220);
    private static final Color ACCENT      = new Color(0x4DA3FF);
    private static final Color NAVY        = new Color(0x0F0F7A);
    private static final Color GRID_LIGHT  = new Color(0x8899DD);
    private static final Color GREEN       = new Color(0x28A745);
    private static final Color LIGHT_BLUE  = new Color(0x4DB6FF);
    private static final Color RED         = new Color(0xE74C3C);
    private static final Color ORANGE      = new Color(0xE69B0B);
    private static final Color TRANSLATE   = new Color(0x1F57E0);
    private static final Color BACK   = new Color(0xf0f5fe);
    

    private static final Color[] SEG_COLORS = {
        new Color(0x3DAA5C), new Color(0xE8A030), new Color(0x2E86D1),
        new Color(0x9B59B6), new Color(0xE0556A), new Color(0x1ABC9C)
    };

    // ===== State =====
    private final Map<Integer, Segment> segMap = new LinkedHashMap<>();
    private boolean updating = false;

    // ===== Components =====
    private JTextField addressField;
    private JLabel errorLabel;
    private DefaultTableModel segModel, resultsModel;
    private JTable segTable, resultsTable;
    private MemoryMapPanel memoryMap;

    public SegmentationWindow() {
        super("Segmentation");         
        content.setLayout(new BorderLayout(0, 10));

        content.add(buildTop(), BorderLayout.NORTH);
        content.add(buildCenter(), BorderLayout.CENTER);
        content.add(buildResults(), BorderLayout.SOUTH);
    }

    private JPanel buildTop() {
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel("Logical Address (segment, offset):");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(0x1A1A3A));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(lbl);
        top.add(Box.createVerticalStrut(4));

        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        addressField = new JTextField();
        addressField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        addressField.setToolTipText("e.g. 1, 250   or   0,100; 2,50");
        addressField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x3B6FD8), 1, true),
                new EmptyBorder(2, 8, 2, 8)));
        addressField.addActionListener(e -> doTranslate());

        RoundButton translateBtn = new RoundButton(" Translate", TRANSLATE);
        translateBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        translateBtn.setPreferredSize(new Dimension(200, 60));
        translateBtn.addActionListener(e -> doTranslate());

        row.add(addressField, BorderLayout.CENTER);
        row.add(translateBtn, BorderLayout.EAST);
        top.add(row);

        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        errorLabel.setForeground(RED);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(Box.createVerticalStrut(2));
        top.add(errorLabel);

        return top;
    }

    
    private JPanel buildCenter() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        c.gridx = 0; c.weightx = 0.36; c.insets = new Insets(0, 0, 0, 8);
        center.add(buildSegmentCard(), c);

        c.gridx = 1; c.weightx = 0.64; c.insets = new Insets(0, 0, 0, 0);
        center.add(buildMapCard(), c);
        return center;
    }

    private JPanel buildSegmentCard() {
        RoundPanel card = new RoundPanel(BACK, 16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel t = new JLabel("Segment Table:");
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(new Color(0x1E3A8A));
        card.add(t, BorderLayout.NORTH);

        segModel = new DefaultTableModel(new Object[]{"Segment", "Limit", "Base"}, 0);
        for (int i = 0; i < 5; i++) segModel.addRow(new Object[]{"", "", ""});
        segTable = new JTable(segModel);
        styleTable(segTable, NAVY, Color.WHITE, Color.WHITE, Color.BLACK, GRID_LIGHT);
        segModel.addTableModelListener(e -> { if (!updating) syncSegments(); });

        JScrollPane sp = new JScrollPane(segTable);
        sp.setBorder(BorderFactory.createLineBorder(GRID_LIGHT));
        sp.getViewport().setBackground(Color.WHITE);
        card.add(sp, BorderLayout.CENTER);

        JPanel btns = new JPanel(new GridLayout(2, 2, 12, 8));
        btns.setOpaque(false);
        RoundButton add = new RoundButton("+ Add", GREEN);
        RoundButton rnd = new RoundButton("Randomize", LIGHT_BLUE);
        RoundButton rem = new RoundButton("- Remove", RED);
        RoundButton clr = new RoundButton("x Clear All", ORANGE);
        add.addActionListener(e -> addRow());
        rnd.addActionListener(e -> randomize());
        rem.addActionListener(e -> removeRow());
        clr.addActionListener(e -> clearAll());
        btns.add(add); btns.add(rnd); btns.add(rem); btns.add(clr);
        btns.setPreferredSize(new Dimension(100, 72));
        card.add(btns, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildMapCard() {
        RoundPanel card = new RoundPanel(CARD_DARK, 16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel t = new JLabel("Physical Memory Map:");
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(ACCENT);
        card.add(t, BorderLayout.NORTH);

        memoryMap = new MemoryMapPanel();
        card.add(memoryMap, BorderLayout.CENTER);
        return card;
    }
    private JPanel buildResults() {
        RoundPanel card = new RoundPanel(CARD_DARK, 16);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(10, 12, 12, 12));
        card.setPreferredSize(new Dimension(100, 165));

        JLabel t = new JLabel("Translation Results:");
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(ACCENT);
        card.add(t, BorderLayout.NORTH);

        resultsModel = new DefaultTableModel(
                new Object[]{"Address", "Limit", "Base", "Physical Address", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        resultsTable = new JTable(resultsModel);
        Color cell = new Color(0x0F1B2D);
        styleTable(resultsTable, new Color(0x13233F), Color.WHITE, cell,
                new Color(0xDDE6F5), new Color(0x2A3B55));

        // kulay ng Status column
        resultsTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v,
                    boolean sel, boolean foc, int r, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, sel, foc, r, col);
                l.setHorizontalAlignment(CENTER);
                l.setBackground(cell);
                String s = String.valueOf(v);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                if (s.equals("VALID")) l.setForeground(new Color(0x4ADE80));
                else if (s.equals("OUT OF BOUNDS")) l.setForeground(new Color(0xFBA23C));
                else l.setForeground(new Color(0xFF5C5C));
                return l;
            }
        });

        JScrollPane sp = new JScrollPane(resultsTable);
        sp.setBorder(BorderFactory.createLineBorder(new Color(0x2A3B55)));
        sp.getViewport().setBackground(cell);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

   
    private void doTranslate() {
        errorLabel.setForeground(RED);
        errorLabel.setText(" ");
        resultsModel.setRowCount(0);
        if (segMap.isEmpty()) {
            errorLabel.setText("Add at least one segment first.");
            return;
        }
        try {
            Result last = null;
            for (int[] q : SegmentationLogic.parseQueries(addressField.getText())) {
                Result r = SegmentationLogic.translate(segMap, q[0], q[1]);
                resultsModel.addRow(new Object[]{
                        "(" + r.seg + ", " + r.offset + ")",
                        r.limit != null ? r.limit : "\u2014",
                        r.base != null ? r.base : "\u2014",
                        r.physical != null ? r.physical : "\u2014",
                        r.status});
                last = r;
            }
            memoryMap.setHighlight(last);
        } catch (NumberFormatException ex) {
            errorLabel.setText("Numbers only. Use format: segment, offset (e.g. 1, 250)");
        } catch (IllegalArgumentException ex) {
            errorLabel.setText(ex.getMessage());
        }
    }

    private void addRow() {
        updating = true;
        int next = 0;
        for (int id : segMap.keySet()) next = Math.max(next, id + 1);
        int target = -1;
        for (int i = 0; i < segModel.getRowCount(); i++) {
            if (isBlankRow(i)) { target = i; break; }
        }
        if (target == -1) {
            segModel.addRow(new Object[]{next, "", ""});
            target = segModel.getRowCount() - 1;
        } else {
            segModel.setValueAt(next, target, 0);
        }
        updating = false;
        segTable.changeSelection(target, 1, false, false);
        segTable.requestFocusInWindow();
        segTable.editCellAt(target, 1);
    }

    private void removeRow() {
        if (segTable.isEditing()) segTable.getCellEditor().stopCellEditing();
        int row = segTable.getSelectedRow();
        if (row < 0) {
            for (int i = segModel.getRowCount() - 1; i >= 0; i--)
                if (!isBlankRow(i)) { row = i; break; }
        }
        if (row < 0) return;
        updating = true;
        if (segModel.getRowCount() > 5) segModel.removeRow(row);
        else for (int c = 0; c < 3; c++) segModel.setValueAt("", row, c);
        updating = false;
        syncSegments();
    }

    private void randomize() {
        Map<Integer, Segment> rnd = SegmentationLogic.randomize(4);
        updating = true;
        for (int i = 0; i < segModel.getRowCount(); i++)
            for (int c = 0; c < 3; c++) segModel.setValueAt("", i, c);
        int i = 0;
        for (Segment s : rnd.values()) {
            if (i >= segModel.getRowCount()) segModel.addRow(new Object[]{"", "", ""});
            segModel.setValueAt(s.id, i, 0);
            segModel.setValueAt(s.limit, i, 1);
            segModel.setValueAt(s.base, i, 2);
            i++;
        }
        updating = false;
        syncSegments();
    }

    private void clearAll() {
        if (segTable.isEditing()) segTable.getCellEditor().stopCellEditing();
        updating = true;
        segModel.setRowCount(0);
        for (int i = 0; i < 5; i++) segModel.addRow(new Object[]{"", "", ""});
        updating = false;
        resultsModel.setRowCount(0);
        addressField.setText("");
        errorLabel.setText(" ");
        syncSegments();
    }

    private boolean isBlankRow(int r) {
        for (int c = 0; c < 3; c++)
            if (!String.valueOf(segModel.getValueAt(r, c)).trim().isEmpty()) return false;
        return true;
    }

    /** Binabasa ang Segment Table at ina-update ang segMap + memory map. */
    private void syncSegments() {
        segMap.clear();
        StringBuilder err = new StringBuilder();
        for (int i = 0; i < segModel.getRowCount(); i++) {
            if (isBlankRow(i)) continue;
            String a = String.valueOf(segModel.getValueAt(i, 0)).trim();
            String l = String.valueOf(segModel.getValueAt(i, 1)).trim();
            String b = String.valueOf(segModel.getValueAt(i, 2)).trim();
            if (a.isEmpty() || l.isEmpty() || b.isEmpty()) continue; // hindi pa kumpleto
            try {
                Segment s = new Segment(Integer.parseInt(a), Integer.parseInt(l), Integer.parseInt(b));
                String e = SegmentationLogic.validate(segMap, s);
                if (e != null) { err.append("Row ").append(i + 1).append(": ").append(e).append("  "); continue; }
                segMap.put(s.id, s);
            } catch (NumberFormatException ex) {
                err.append("Row ").append(i + 1).append(": numbers only.  ");
            }
        }
        errorLabel.setForeground(RED);
        errorLabel.setText(err.length() == 0 ? " " : err.toString().trim());
        memoryMap.setSegments(segMap);
        memoryMap.setHighlight(null);
    }

    // =====================================================================
    // TABLE STYLING
    // =====================================================================
    private static void styleTable(JTable t, Color headBg, Color headFg,
                                   Color cellBg, Color cellFg, Color grid) {
        t.setRowHeight(28);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setGridColor(grid);
        t.setShowGrid(true);
        t.setBackground(cellBg);
        t.setForeground(cellFg);
        t.setSelectionBackground(new Color(0x3B6FD8));
        t.setSelectionForeground(Color.WHITE);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setCellRenderer(center);

        DefaultTableCellRenderer head = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v,
                    boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, s, f, r, c);
                l.setBackground(headBg);
                l.setForeground(headFg);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                l.setHorizontalAlignment(CENTER);
                l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, grid));
                return l;
            }
        };
        for (int i = 0; i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setHeaderRenderer(head);
        t.getTableHeader().setPreferredSize(new Dimension(100, 26));
    }

    // =====================================================================
    // CUSTOM COMPONENTS
    // =====================================================================
    private static class RoundPanel extends JPanel {
        private final Color color; private final int arc;
        RoundPanel(Color color, int arc) { this.color = color; this.arc = arc; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
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
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

  
    private static class MemoryMapPanel extends JPanel {
        private Map<Integer, Segment> segs = new LinkedHashMap<>();
        private Result hl;

        MemoryMapPanel() { setOpaque(false); }

        void setSegments(Map<Integer, Segment> m) { segs = new LinkedHashMap<>(m); repaint(); }
        void setHighlight(Result r) { hl = r; repaint(); }

        private int px(int value, int barW, int pad) {
            return pad + (int) ((long) value * barW / SegmentationLogic.MEMORY_SIZE);
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int pad = 20, barW = Math.max(1, w - 2 * pad), barH = 56;
            int y = Math.max(34, h / 2 - barH / 2 - 8);
            int mem = SegmentationLogic.MEMORY_SIZE;

            // free-space background
            g.setColor(new Color(0x16213A));
            g.fillRoundRect(pad, y, barW, barH, 8, 8);
            g.setColor(new Color(0x2F4366));
            g.drawRoundRect(pad, y, barW - 1, barH - 1, 8, 8);

            TreeSet<Integer> ticks = new TreeSet<>();
            ticks.add(0); ticks.add(mem);

            Font bold = new Font("Segoe UI", Font.BOLD, 14);
            for (Segment s : segs.values()) {
                int x = px(s.base, barW, pad);
                int sw = Math.max(2, px(s.limit, barW, 0));
                g.setColor(SEG_COLORS[s.id % SEG_COLORS.length]);
                g.fillRect(x, y, sw, barH);
                g.setColor(new Color(0, 0, 0, 80));
                g.drawRect(x, y, sw, barH);

                g.setFont(bold);
                g.setColor(Color.WHITE);
                String t = String.valueOf(s.id);
                FontMetrics fm = g.getFontMetrics();
                if (sw >= fm.stringWidth(t) + 2)
                    g.drawString(t, x + (sw - fm.stringWidth(t)) / 2, y + barH / 2 + fm.getAscent() / 2 - 2);
                ticks.add(s.base);
                ticks.add(s.end());
            }

            // address labels (2 rows para hindi mag-overlap)
            g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            FontMetrics fm = g.getFontMetrics();
            int[] lastRight = {-100, -100};
            for (int t : ticks) {
                int x = px(t, barW, pad);
                String s = String.valueOf(t);
                int fw = fm.stringWidth(s);
                int left = Math.max(2, Math.min(w - fw - 2, x - fw / 2));
                int row = left > lastRight[0] + 4 ? 0 : 1;
                g.setColor(new Color(0x6B7C99));
                g.drawLine(x, y + barH, x, y + barH + 4 + row * 14);
                g.setColor(new Color(0xC9D4E8));
                g.drawString(s, left, y + barH + 16 + row * 14);
                lastRight[row] = left + fw;
            }

            // highlight ng huling translate
            if (hl != null) {
                Segment hs = segs.get(hl.seg);
                if (hs != null) {
                    int x = px(hs.base, barW, pad);
                    int sw = Math.max(2, px(hs.limit, barW, 0));
                    g.setStroke(new BasicStroke(3f));
                    g.setColor(hl.isValid() ? Color.WHITE : new Color(0xFF4D4D));
                    g.drawRect(x, y, sw, barH);
                }
                if (hl.isValid()) {
                    int mx = px(hl.physical, barW, pad);
                    g.setColor(new Color(0xFFE81A));
                    g.fillPolygon(new int[]{mx - 6, mx + 6, mx}, new int[]{y - 12, y - 12, y - 1}, 3);
                    g.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    String lbl = "PA " + hl.physical;
                    int lw = g.getFontMetrics().stringWidth(lbl);
                    g.drawString(lbl, Math.max(2, Math.min(w - lw - 2, mx - lw / 2)), y - 16);
                }
            }

            if (segs.isEmpty()) {
                g.setColor(new Color(0x6B7C99));
                g.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                String m = "No segments yet \u2014 fill the Segment Table or press Randomize";
                g.drawString(m, (w - g.getFontMetrics().stringWidth(m)) / 2, y + barH + 52);
            }
            g.dispose();
        }
    }
}