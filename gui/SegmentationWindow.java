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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

public class SegmentationWindow extends BasePanel {

    // Color palette used across cards, buttons, and table themes
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
    private static final Color BACK        = new Color(0xf0f5fe);

    // Distinct colors assigned to segments on the physical memory map
    private static final Color[] SEG_COLORS = {
        new Color(0x3DAA5C), new Color(0xE8A030), new Color(0x2E86D1),
        new Color(0x9B59B6), new Color(0xE0556A), new Color(0x1ABC9C)
    };

    // Stores valid segments mapped by Segment ID
    private final Map<Integer, Segment> segMap = new LinkedHashMap<>();
    // Prevents recursive listener triggers while updating table cells programmatically
    private boolean updating = false;

    // UI Components
    private JTextField addressField;
    private JLabel errorLabel;
    private DefaultTableModel segModel, resultsModel;
    private JTable segTable, resultsTable;
    private MemoryMapPanel memoryMap;

    // Initializes the Segmentation module layout and default rows
    public SegmentationWindow() {
        super("Segmentation");
        content.setLayout(new BorderLayout(0, 10));

        content.add(buildTop(), BorderLayout.NORTH);
        content.add(buildCenter(), BorderLayout.CENTER);
        content.add(buildResults(), BorderLayout.SOUTH);
    }

    // Builds the top logical address input bar and Translate button
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

        // Text field accepting single or semicolon-separated (segment, offset) pairs
        addressField = new JTextField();
        addressField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        addressField.setToolTipText("e.g. 1, 250   or   0,100; 2,50");
        addressField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x3B6FD8), 1, true),
                new EmptyBorder(2, 8, 2, 8)));
        addressField.addActionListener(e -> doTranslate());

        // Triggers logical-to-physical address translation
        RoundButton translateBtn = new RoundButton(" Translate", TRANSLATE);
        translateBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        translateBtn.setPreferredSize(new Dimension(200, 60));
        translateBtn.addActionListener(e -> doTranslate());

        row.add(addressField, BorderLayout.CENTER);
        row.add(translateBtn, BorderLayout.EAST);
        top.add(row);

        // Displays validation or parsing error messages
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        errorLabel.setForeground(RED);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(Box.createVerticalStrut(2));
        top.add(errorLabel);

        return top;
    }

    // Splits the middle workspace into the Segment Table (left) and Physical Memory Map (right)
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

    // Builds the left card containing the editable Segment Table and control buttons
    private JPanel buildSegmentCard() {
        RoundPanel card = new RoundPanel(BACK, 16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel t = new JLabel("Segment Table:");
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(new Color(0x1E3A8A));
        card.add(t, BorderLayout.NORTH);

        // Starts with 4 default segment rows (0 to 3) matching the other modules
        segModel = new DefaultTableModel(new Object[]{"Segment", "Limit", "Base"}, 0);
        for (int i = 0; i < 4; i++) segModel.addRow(new Object[]{i, "", ""});

        segTable = new JTable(segModel);
        styleTable(segTable, NAVY, Color.WHITE, Color.WHITE, Color.BLACK, GRID_LIGHT);

        // Re-validates and updates the memory map whenever the user edits a cell
        segModel.addTableModelListener(e -> { if (!updating) syncSegments(); });

        JScrollPane sp = new JScrollPane(segTable);
        sp.setBorder(BorderFactory.createLineBorder(GRID_LIGHT));
        sp.getViewport().setBackground(Color.WHITE);
        card.add(sp, BorderLayout.CENTER);

        // 2x2 action button grid: Add, Randomize, Remove, Clear All
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

    // Builds the right dark card displaying the visual Physical Memory Map
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

    // Builds the bottom dark card showing the Translation Results table
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

        // Color-codes the Status column (Green = VALID, Orange = OUT OF BOUNDS, Red = FAULT)
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

    // Stops any active cell editor so typed values are committed before button actions
    private void stopEditing() {
        if (segTable.isEditing()) segTable.getCellEditor().stopCellEditing();
    }

    // Parses logical address queries, translates them using segMap, and updates results & map
    private void doTranslate() {
        stopEditing();
        syncSegments();
        errorLabel.setForeground(RED);
        errorLabel.setText(" ");
        resultsModel.setRowCount(0);

        if (segMap.isEmpty()) {
            errorLabel.setText("Add at least one valid segment first.");
            return;
        }
        if (addressField.getText().trim().isEmpty()) {
            errorLabel.setText("Enter a logical address (e.g. 0, 100).");
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

    // Appends a new segment row with the next available Segment ID and focuses its Limit cell
    private void addRow() {
        stopEditing();
        updating = true;

        // Find the next unused Segment ID from both existing table rows and segMap
        int nextId = 0;
        for (int i = 0; i < segModel.getRowCount(); i++) {
            String val = String.valueOf(segModel.getValueAt(i, 0)).trim();
            try {
                if (!val.isEmpty()) nextId = Math.max(nextId, Integer.parseInt(val) + 1);
            } catch (NumberFormatException ignored) {}
        }
        for (int id : segMap.keySet()) {
            nextId = Math.max(nextId, id + 1);
        }

        segModel.addRow(new Object[]{nextId, "", ""});
        int newRow = segModel.getRowCount() - 1;
        updating = false;

        // Scroll to the newly added row and begin editing the Limit column
        segTable.scrollRectToVisible(segTable.getCellRect(newRow, 0, true));
        segTable.changeSelection(newRow, 1, false, false);
        segTable.requestFocusInWindow();
        segTable.editCellAt(newRow, 1);
    }

    // Removes the selected row (or the last row if none is selected) and updates the memory map
    private void removeRow() {
        stopEditing();
        int rowCount = segModel.getRowCount();
        if (rowCount == 0) return;

        int row = segTable.getSelectedRow();
        if (row < 0) row = rowCount - 1;

        updating = true;
        segModel.removeRow(row);
        updating = false;

        // Select an adjacent row if rows remain
        if (segModel.getRowCount() > 0) {
            int nextSel = Math.min(row, segModel.getRowCount() - 1);
            segTable.setRowSelectionInterval(nextSel, nextSel);
        }
        syncSegments();
    }

    // Generates random non-overlapping segments for all current rows and auto-runs translation
    private void randomize() {
        stopEditing();
        int count = segModel.getRowCount();
        if (count == 0) count = 4; // Default to 4 rows if the table is completely empty

        Random r = new Random();
        // Scale segment sizes dynamically so all rows fit within MEMORY_SIZE
        int slotSize = Math.max(20, SegmentationLogic.MEMORY_SIZE / count);
        int cursor = 0;

        updating = true;
        segModel.setRowCount(0);
        List<Segment> generated = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int remainingSlots = count - i;
            int remainingMem = SegmentationLogic.MEMORY_SIZE - cursor;
            if (remainingMem < remainingSlots * 10) break;

            int maxSlice = remainingMem / remainingSlots;
            int gap = r.nextInt(Math.max(1, maxSlice / 4));
            int base = cursor + gap;
            int maxLimit = Math.max(10, maxSlice - gap);
            int limit = Math.max(10, (maxLimit / 2) + r.nextInt(Math.max(1, maxLimit / 2)));

            if (base + limit > SegmentationLogic.MEMORY_SIZE) {
                limit = SegmentationLogic.MEMORY_SIZE - base;
            }
            if (limit <= 0) break;

            Segment s = new Segment(i, limit, base);
            generated.add(s);
            segModel.addRow(new Object[]{s.id, s.limit, s.base});
            cursor = base + limit;
        }
        updating = false;

        syncSegments();

        // Populate sample logical addresses using the generated segments and run translation
        if (!generated.isEmpty()) {
            Segment first = generated.get(0);
            Segment last = generated.get(generated.size() - 1);
            int validOffset = Math.max(0, first.limit / 2);
            int oobOffset = last.limit + 50; // Deliberate out-of-bounds example
            addressField.setText(first.id + ", " + validOffset + "; " + last.id + ", " + oobOffset);
            doTranslate();
        }
    }

    // Resets the Segment Table to 4 empty rows, clears results, and resets the memory map
    private void clearAll() {
        stopEditing();
        updating = true;
        segModel.setRowCount(0);
        for (int i = 0; i < 4; i++) segModel.addRow(new Object[]{i, "", ""});
        updating = false;

        resultsModel.setRowCount(0);
        addressField.setText("");
        errorLabel.setText(" ");
        syncSegments();
    }

    // Checks whether all 3 cells in a given table row are blank
    private boolean isBlankRow(int r) {
        for (int c = 0; c < 3; c++) {
            Object val = segModel.getValueAt(r, c);
            if (val != null && !val.toString().trim().isEmpty()) return false;
        }
        return true;
    }

    // Reads all rows from the Segment Table, validates them, and updates segMap and the Memory Map
    private void syncSegments() {
        segMap.clear();
        StringBuilder err = new StringBuilder();
        for (int i = 0; i < segModel.getRowCount(); i++) {
            if (isBlankRow(i)) continue;
            String a = String.valueOf(segModel.getValueAt(i, 0)).trim();
            String l = String.valueOf(segModel.getValueAt(i, 1)).trim();
            String b = String.valueOf(segModel.getValueAt(i, 2)).trim();
            if (a.isEmpty() || l.isEmpty() || b.isEmpty()) continue; // Skip incomplete rows while typing
            try {
                Segment s = new Segment(Integer.parseInt(a), Integer.parseInt(l), Integer.parseInt(b));
                String e = SegmentationLogic.validate(segMap, s);
                if (e != null) {
                    err.append("Row ").append(i + 1).append(": ").append(e).append("  ");
                    continue;
                }
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

    // Table Styling

    // Applies consistent row height, colors, alignment, and header styling to a JTable
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

    // Custom Components

    // JPanel with anti-aliased rounded corners
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

    // JButton with rounded corners and hover/press color transitions
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
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Draws the horizontal physical memory bar, segment blocks, address ticks, and PA pointer
    private static class MemoryMapPanel extends JPanel {
        private Map<Integer, Segment> segs = new LinkedHashMap<>();
        private Result hl;

        MemoryMapPanel() { setOpaque(false); }

        // Updates the segments shown on the memory bar and repaints
        void setSegments(Map<Integer, Segment> m) { segs = new LinkedHashMap<>(m); repaint(); }

        // Highlights the segment and physical address from the latest translation result
        void setHighlight(Result r) { hl = r; repaint(); }

        // Converts a memory byte address into an X pixel coordinate along the memory bar
        private int px(int value, int barW, int pad) {
            return pad + (int) ((long) value * barW / SegmentationLogic.MEMORY_SIZE);
        }

        // Renders the memory bar, colored segments, boundary address ticks, and translation marker
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int pad = 20, barW = Math.max(1, w - 2 * pad), barH = 56;
            int y = Math.max(34, h / 2 - barH / 2 - 8);
            int mem = SegmentationLogic.MEMORY_SIZE;

            // Draw empty physical memory background bar
            g.setColor(new Color(0x16213A));
            g.fillRoundRect(pad, y, barW, barH, 8, 8);
            g.setColor(new Color(0x2F4366));
            g.drawRoundRect(pad, y, barW - 1, barH - 1, 8, 8);

            // Collect boundary addresses for axis tick marks
            TreeSet<Integer> ticks = new TreeSet<>();
            ticks.add(0); ticks.add(mem);

            // Draw each allocated segment block inside the memory bar
            Font bold = new Font("Segoe UI", Font.BOLD, 14);
            for (Segment s : segs.values()) {
                int x = px(s.base, barW, pad);
                int sw = Math.max(2, px(s.limit, barW, 0));
                g.setColor(SEG_COLORS[Math.abs(s.id) % SEG_COLORS.length]);
                g.fillRect(x, y, sw, barH);
                g.setColor(new Color(0, 0, 0, 80));
                g.drawRect(x, y, sw, barH);

                // Draw Segment ID centered inside the block if wide enough
                g.setFont(bold);
                g.setColor(Color.WHITE);
                String t = String.valueOf(s.id);
                FontMetrics fm = g.getFontMetrics();
                if (sw >= fm.stringWidth(t) + 2)
                    g.drawString(t, x + (sw - fm.stringWidth(t)) / 2, y + barH / 2 + fm.getAscent() / 2 - 2);
                ticks.add(s.base);
                ticks.add(s.end());
            }

            // Draw boundary address labels staggered across 2 rows to prevent text overlap
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

            // Highlight the target segment and draw the physical address pointer arrow
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

            // Show helper prompt when no segments are defined yet
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