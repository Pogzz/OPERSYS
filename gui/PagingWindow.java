package gui;

import LOGICS.PagingLogic;
import LOGICS.PagingLogic.PagingResult;
import LOGICS.PagingLogic.TranslationRow;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.CubicCurve2D;
import java.util.*;
import java.util.List;

public class PagingWindow extends BasePanel {

    // Color palette used across cards, buttons, and the byte-level memory diagram
    private static final Color NAVY   = new Color(0x120078);
    private static final Color BLUE   = new Color(0x3B6FD4);
    private static final Color DARK   = new Color(0x0F1B2D);
    private static final Color DARK2  = new Color(0x13243D);
    private static final Color GREEN  = new Color(0x2E9E4F);
    private static final Color RED    = new Color(0xE74C3C);
    private static final Color ORANGE = new Color(0xE8A010);
    private static final Color RUN    = new Color(0x1F57E0);
    private static final Color ACCENT = new Color(0x3EA0FF);

    // Input fields for page size, total physical frames, and logical addresses
    private final JTextField pageSizeField = new JTextField("4", 4);
    private final JTextField framesField   = new JTextField("8", 4);
    private final JTextField addrField     = new JTextField("0 3 4 13");

    // Editable Page -> Frame mapping table
    private final DefaultTableModel pageModel = new DefaultTableModel(
            new String[]{"Page", "Frame"}, 0);
    private final JTable pageTable = new JTable(pageModel);

    // Read-only translation results table
    private final DefaultTableModel resModel = new DefaultTableModel(
            new String[]{"Logical", "Page", "Offset", "Frame", "Physical"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable resTable = new JTable(resModel);

    // Byte size summary label and custom logical-to-physical memory diagram
    private final JLabel memSummaryLabel = new JLabel("16 B logical  \u00B7  32 B physical");
    private final ByteMemoryMapPanel mapPanel = new ByteMemoryMapPanel();

    // Initializes the Paging window and loads the default sample page table
    public PagingWindow() {
        super("Paging");

        content.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;

        gc.gridx = 0; gc.weightx = 0.36;
        gc.insets = new Insets(0, 0, 0, 14);
        content.add(buildLeft(), gc);

        gc.gridx = 1; gc.weightx = 0.64;
        gc.insets = new Insets(0, 0, 0, 0);
        content.add(buildRight(), gc);

        loadSampleData();
    }

    // Builds the left column containing Memory setup, Page table, and Addresses cards
    private JPanel buildLeft() {
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);

        // Card 1: Memory setup (Page size & Frames)
        RoundedPanel setupCard = new RoundedPanel(18, CARD);
        setupCard.setLayout(new BorderLayout(0, 6));
        setupCard.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel setupTitle = new JLabel("Memory setup");
        setupTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        setupTitle.setForeground(NAVY);

        JPanel setupRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        setupRow.setOpaque(false);
        JLabel psLbl = new JLabel("Page size");
        psLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        styleInput(pageSizeField, 46, 26);

        JLabel frLbl = new JLabel("Frames");
        frLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        styleInput(framesField, 46, 26);

        pageSizeField.addActionListener(e -> refreshPreview());
        framesField.addActionListener(e -> refreshPreview());

        setupRow.add(psLbl);
        setupRow.add(pageSizeField);
        setupRow.add(Box.createHorizontalStrut(6));
        setupRow.add(frLbl);
        setupRow.add(framesField);

        setupCard.add(setupTitle, BorderLayout.NORTH);
        setupCard.add(setupRow, BorderLayout.CENTER);

        // Card 2: Page table + Add/Remove/Randomize/Clear buttons
        RoundedPanel tableCard = new RoundedPanel(20, CARD);
        tableCard.setLayout(new BorderLayout(0, 8));
        tableCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel ptTitle = new JLabel("Page table");
        ptTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        ptTitle.setForeground(NAVY);

        styleTable(pageTable, false);
        JScrollPane sp = new JScrollPane(pageTable);
        sp.setBorder(BorderFactory.createLineBorder(BLUE, 1));
        sp.getViewport().setBackground(Color.WHITE);

        JButton addBtn  = makeButton("+ Add page", GREEN);
        JButton remBtn  = makeButton("- Remove page", RED);
        JButton randBtn = makeButton("Randomize", BLUE );
        JButton clrBtn  = makeButton("x Clear all", ORANGE);

        addBtn.addActionListener(e -> addRow());
        remBtn.addActionListener(e -> removeRow());
        randBtn.addActionListener(e -> randomizeData());
        clrBtn.addActionListener(e -> clearAll());

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 8, 6));
        btnGrid.setOpaque(false);
        btnGrid.add(addBtn);
        btnGrid.add(remBtn);
        btnGrid.add(randBtn);
        btnGrid.add(clrBtn);

        tableCard.add(ptTitle, BorderLayout.NORTH);
        tableCard.add(sp, BorderLayout.CENTER);
        tableCard.add(btnGrid, BorderLayout.SOUTH);

        // Card 3: Logical Addresses input + Run Simulation button
        RoundedPanel addrCard = new RoundedPanel(18, CARD);
        addrCard.setLayout(new BorderLayout(0, 6));
        addrCard.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel addrTitle = new JLabel("Addresses");
        addrTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addrTitle.setForeground(NAVY);

        JPanel addrRow = new JPanel(new BorderLayout(8, 0));
        addrRow.setOpaque(false);
        addrField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addrField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BLUE, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        JButton runBtn = makeButton("Run Simulation", RUN);
        runBtn.setPreferredSize(new Dimension(135, 32));
        runBtn.addActionListener(e -> runSimulation());

        addrRow.add(addrField, BorderLayout.CENTER);
        addrRow.add(runBtn, BorderLayout.EAST);

        addrCard.add(addrTitle, BorderLayout.NORTH);
        addrCard.add(addrRow, BorderLayout.CENTER);

        left.add(setupCard, BorderLayout.NORTH);
        left.add(tableCard, BorderLayout.CENTER);
        left.add(addrCard, BorderLayout.SOUTH);
        return left;
    }

    // Builds the right dark panel containing the byte-level Memory map and Results table
    private JPanel buildRight() {
        RoundedPanel panel = new RoundedPanel(24, DARK);
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Header bar showing total logical and physical bytes
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        topBar.setOpaque(false);
        JLabel title = new JLabel("Memory map");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));

        memSummaryLabel.setForeground(ACCENT);
        memSummaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        topBar.add(title);
        topBar.add(memSummaryLabel);

        // Center: Byte-level Memory Map + color legend
        JPanel mapWrap = new JPanel(new BorderLayout(0, 6));
        mapWrap.setOpaque(false);
        mapWrap.add(mapPanel, BorderLayout.CENTER);
        mapWrap.add(buildLegendBar(), BorderLayout.SOUTH);

        // Bottom: Results Table (Logical | Page | Offset | Frame | Physical)
        JPanel resWrap = new JPanel(new BorderLayout(0, 6));
        resWrap.setOpaque(false);
        resWrap.setPreferredSize(new Dimension(100, 165));

        JLabel resTitle = new JLabel("Results");
        resTitle.setForeground(Color.WHITE);
        resTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));

        styleTable(resTable, true);
        JScrollPane resSp = new JScrollPane(resTable);
        resSp.setBorder(BorderFactory.createLineBorder(new Color(0x2A3B55)));
        resSp.getViewport().setBackground(DARK2);

        resWrap.add(resTitle, BorderLayout.NORTH);
        resWrap.add(resSp, BorderLayout.CENTER);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(mapWrap, BorderLayout.CENTER);
        panel.add(resWrap, BorderLayout.SOUTH);
        return panel;
    }

    // Builds the legend bar explaining colors and P/F/d notation in the diagram
    private JPanel buildLegendBar() {
        RoundedPanel leg = new RoundedPanel(12, new Color(0x0B1320));
        leg.setLayout(new GridLayout(2, 1, 0, 2));
        leg.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row1.setOpaque(false);
        row1.add(legendChip(ACCENT, "Translated address"));
        row1.add(legendChip(RED, "Page fault"));
        row1.add(legendChip(new Color(0xFFB454), "Loaded frame"));
        row1.add(legendChip(new Color(0x2B4266), "Free frame"));

        JLabel row2 = new JLabel("  P = page   F = frame   d = offset   P\u00B7d = page-sized line");
        row2.setForeground(new Color(0x9BB0D1));
        row2.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        leg.add(row1);
        leg.add(row2);
        return leg;
    }

    // Creates a small colored square and label for the legend bar
    private JPanel legendChip(Color c, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setOpaque(false);
        JPanel box = new JPanel();
        box.setBackground(c);
        box.setPreferredSize(new Dimension(10, 10));
        JLabel lbl = new JLabel(text);
        lbl.setForeground(new Color(0xCFE3FF));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        p.add(box);
        p.add(lbl);
        return p;
    }

    // Applies centered bold styling to small setup text fields
    private void styleInput(JTextField tf, int w, int h) {
        tf.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tf.setHorizontalAlignment(SwingConstants.CENTER);
        tf.setPreferredSize(new Dimension(w, h));
        tf.setBorder(BorderFactory.createLineBorder(BLUE, 1));
    }

    // Styles either the light Page table or the dark Results table
    private void styleTable(JTable t, boolean darkTheme) {
        t.setRowHeight(26);
        t.setGridColor(darkTheme ? new Color(0x24385A) : BLUE);
        t.setShowGrid(true);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setFillsViewportHeight(true);
        t.setBackground(darkTheme ? DARK2 : Color.WHITE);
        t.setForeground(darkTheme ? Color.WHITE : Color.BLACK);
        t.setSelectionBackground(darkTheme ? new Color(0x1E3557) : new Color(0xCFE0FF));
        t.setSelectionForeground(darkTheme ? Color.WHITE : Color.BLACK);

        DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object v,
                    boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(table, v, sel, false, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createLineBorder(darkTheme ? new Color(0x24385A) : BLUE, 1));
                if (darkTheme && c == 4 && "PAGE FAULT".equals(String.valueOf(v))) {
                    setForeground(new Color(0xFF7B89));
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                } else if (darkTheme) {
                    setForeground(Color.WHITE);
                }
                return this;
            }
        };
        t.setDefaultRenderer(Object.class, cell);

        DefaultTableCellRenderer hr = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object v,
                    boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(table, v, sel, foc, r, c);
                setBackground(darkTheme ? new Color(0x0B1320) : NAVY);
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createLineBorder(darkTheme ? new Color(0x2A3B55) : Color.WHITE, 1));
                return this;
            }
        };
        t.getTableHeader().setDefaultRenderer(hr);
        t.getTableHeader().setPreferredSize(new Dimension(60, 26));
        t.getTableHeader().setReorderingAllowed(false);

        if (!darkTheme) {
            pageModel.addTableModelListener(e -> SwingUtilities.invokeLater(this::refreshPreview));
        }
    }

    // Loads a default sample (Page size 4, Frames 8, Pages 0..3 -> 2,5,1,7) and runs simulation
    private void loadSampleData() {
        pageSizeField.setText("4");
        framesField.setText("8");
        addrField.setText("0 3 4 13");
        pageModel.setRowCount(0);
        pageModel.addRow(new Object[]{"0", "2"});
        pageModel.addRow(new Object[]{"1", "5"});
        pageModel.addRow(new Object[]{"2", "1"});
        pageModel.addRow(new Object[]{"3", "7"});
        runSimulation();
    }

    // Assigns unique random frames to all current page rows, generates sample addresses, and runs
    private void randomizeData() {
        stopEditing();
        int ps = parsePos(pageSizeField.getText(), 4);
        int nf = parsePos(framesField.getText(), 8);

        // Use current row count in the Page table (defaulting to 4 if empty)
        int numPages = pageModel.getRowCount();
        if (numPages <= 0) {
            numPages = Math.min(4, nf);
        }

        // Expand Frames count automatically if there are more pages than available frames
        if (numPages > nf) {
            nf = numPages;
            framesField.setText(String.valueOf(nf));
        }

        List<Integer> framePool = new ArrayList<>();
        for (int f = 0; f < nf; f++) framePool.add(f);
        Collections.shuffle(framePool);

        // Keep existing page numbers and assign unique shuffled frames
        List<Integer> pagesInTable = new ArrayList<>();
        for (int i = 0; i < numPages; i++) {
            String pStr = cell(i, 0);
            int pageNum;
            try {
                pageNum = pStr.isEmpty() ? i : Integer.parseInt(pStr);
            } catch (NumberFormatException e) {
                pageNum = i;
            }
            pagesInTable.add(pageNum);
            pageModel.setValueAt(String.valueOf(pageNum), i, 0);
            pageModel.setValueAt(String.valueOf(framePool.get(i)), i, 1);
        }

        Random rand = new Random();
        int maxPage = pagesInTable.isEmpty() ? (numPages - 1) : Collections.max(pagesInTable);
        int maxLogical = Math.max(1, (maxPage + 1) * ps);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) sb.append(" ");
            sb.append(rand.nextInt(maxLogical));
        }
        addrField.setText(sb.toString());
        runSimulation();
    }

    // Commits any active cell edit in the Page table
    private void stopEditing() {
        if (pageTable.isEditing()) pageTable.getCellEditor().stopCellEditing();
    }

    // Appends a new page row to the Page table
    private void addRow() {
        stopEditing();
        pageModel.addRow(new Object[]{String.valueOf(pageModel.getRowCount()), ""});
    }

    // Removes the selected page row (or the last row) and refreshes the preview
    private void removeRow() {
        stopEditing();
        if (pageModel.getRowCount() == 0) return;
        int r = pageTable.getSelectedRow();
        if (r < 0) r = pageModel.getRowCount() - 1;
        pageModel.removeRow(r);
        refreshPreview();
    }

    // Resets the Page table to 4 blank mappings and clears results and addresses
    private void clearAll() {
        stopEditing();
        pageModel.setRowCount(0);
        for (int i = 0; i < 4; i++) pageModel.addRow(new Object[]{String.valueOf(i), ""});
        resModel.setRowCount(0);
        addrField.setText("");
        refreshPreview();
    }

    // Updates the memory map preview when setup fields or page table cells change
    private void refreshPreview() {
        int ps = parsePos(pageSizeField.getText(), 4);
        int nf = parsePos(framesField.getText(), 8);
        Map<Integer, Integer> map = readPageTableSilently();
        int numPages = Math.max(4, map.isEmpty() ? 4 : Collections.max(map.keySet()) + 1);
        memSummaryLabel.setText((numPages * ps) + " B logical  \u00B7  " + (nf * ps) + " B physical");
        mapPanel.setData(ps, numPages, nf, map,
                Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptyMap());
    }

    // Validates inputs, translates logical addresses, and updates the Results table and diagram
    private void runSimulation() {
        stopEditing();

        int pageSize, numFrames;
        try {
            pageSize = Integer.parseInt(pageSizeField.getText().trim());
            numFrames = Integer.parseInt(framesField.getText().trim());
            if (pageSize < 1 || numFrames < 1) {
                JOptionPane.showMessageDialog(this, "Page size and Frames must be at least 1.");
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid whole numbers for Page size and Frames.");
            return;
        }

        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pageModel.getRowCount(); i++) {
            String pStr = cell(i, 0);
            String fStr = cell(i, 1);
            if (pStr.isEmpty() && fStr.isEmpty()) continue;
            try {
                int p = Integer.parseInt(pStr);
                int f = Integer.parseInt(fStr);
                if (p < 0 || f < 0) throw new NumberFormatException();

                if (map.containsKey(p)) {
                    JOptionPane.showMessageDialog(this, "Duplicate Page " + p + " in page table.");
                    return;
                }
                if (map.containsValue(f)) {
                    JOptionPane.showMessageDialog(this, "Frame " + f + " is already assigned to another page.");
                    return;
                }
                map.put(p, f);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid Page or Frame on row " + (i + 1) + ".");
                return;
            }
        }

        String raw = addrField.getText().trim();
        if (raw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter at least one logical address.");
            return;
        }

        List<Integer> addresses = new ArrayList<>();
        for (String tok : raw.split("[,\\s]+")) {
            if (tok.isEmpty()) continue;
            try {
                int a = Integer.parseInt(tok.trim());
                if (a < 0) throw new NumberFormatException();
                addresses.add(a);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid address: \"" + tok + "\"");
                return;
            }
        }

        // Perform address translation via PagingLogic
        PagingResult res = PagingLogic.translateDetailed(pageSize, numFrames, map, addresses);

        // Populate the 5-column Results table
        resModel.setRowCount(0);
        for (TranslationRow r : res.rows) {
            resModel.addRow(new Object[]{
                    r.logical,
                    r.page,
                    r.offset,
                    (r.frame == null) ? "-" : r.frame,
                    r.statusNote
            });
        }

        int maxPageFromMap = map.isEmpty() ? 3 : Collections.max(map.keySet());
        int maxPageFromAddr = addresses.isEmpty() ? 0 : (Collections.max(addresses) / pageSize);
        int numPages = Math.max(pageModel.getRowCount(), Math.max(maxPageFromMap, maxPageFromAddr) + 1);

        memSummaryLabel.setText((numPages * pageSize) + " B logical  \u00B7  " + (numFrames * pageSize) + " B physical");
        mapPanel.setData(pageSize, numPages, numFrames, map,
                res.translatedLogicalBytes, res.faultedLogicalBytes, res.translatedPhysicalBytes, res.byteArrows);
    }

    // Reads valid Page -> Frame entries without showing validation dialogs
    private Map<Integer, Integer> readPageTableSilently() {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pageModel.getRowCount(); i++) {
            try {
                int p = Integer.parseInt(cell(i, 0));
                int f = Integer.parseInt(cell(i, 1));
                if (p >= 0 && f >= 0) map.put(p, f);
            } catch (NumberFormatException ignored) {}
        }
        return map;
    }

    // Parses a positive integer or falls back to a default value
    private int parsePos(String s, int def) {
        try {
            int v = Integer.parseInt(s.trim());
            return v > 0 ? v : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // Safely reads and trims a cell string value
    private String cell(int r, int c) {
        Object o = pageModel.getValueAt(r, c);
        return o == null ? "" : o.toString().trim();
    }

    // Custom panel drawing byte-level Logical and Physical memory columns with curved translation arrows
    private static class ByteMemoryMapPanel extends JPanel {
        private int pageSize = 4;
        private int numPages = 4;
        private int numFrames = 8;
        private Map<Integer, Integer> pageTable = new LinkedHashMap<>();
        private Set<Integer> transLog = Collections.emptySet();
        private Set<Integer> faultLog = Collections.emptySet();
        private Set<Integer> transPhys = Collections.emptySet();
        private Map<Integer, Integer> arrows = Collections.emptyMap();

        ByteMemoryMapPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(360, 260));
        }

        // Updates memory dimensions, highlights, and arrow mappings, then repaints
        void setData(int pageSize, int numPages, int numFrames, Map<Integer, Integer> pageTable,
                     Set<Integer> transLog, Set<Integer> faultLog, Set<Integer> transPhys,
                     Map<Integer, Integer> arrows) {
            this.pageSize = pageSize;
            this.numPages = Math.max(1, numPages);
            this.numFrames = Math.max(1, numFrames);
            this.pageTable = new LinkedHashMap<>(pageTable);
            this.transLog = transLog;
            this.faultLog = faultLog;
            this.transPhys = transPhys;
            this.arrows = arrows;
            repaint();
        }

        // Renders Logical memory slots, Physical memory slots, and curved mapping arrows
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            g2.setColor(DARK2);
            g2.fillRoundRect(0, 0, w, h, 16, 16);

            int totalLogBytes  = numPages * pageSize;
            int totalPhysBytes = numFrames * pageSize;

            int topY = 26;
            int botY = h - 10;
            int availH = Math.max(40, botY - topY);

            int colW = Math.min(130, (w - 130) / 2);
            int leftX = 34;
            int rightX = w - colW - 22;

            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            g2.drawString("Logical memory", leftX, 17);
            g2.drawString("Physical memory", rightX, 17);

            double logSlotH  = (double) availH / totalLogBytes;
            double physSlotH = (double) availH / totalPhysBytes;

            // Reverse map: frame -> page
            Map<Integer, Integer> frameToPage = new HashMap<>();
            for (Map.Entry<Integer, Integer> e : pageTable.entrySet()) {
                frameToPage.put(e.getValue(), e.getKey());
            }

            // Draw Logical Memory Byte Slots (0 .. totalLogBytes - 1)
            for (int b = 0; b < totalLogBytes; b++) {
                int p = b / pageSize;
                int d = b % pageSize;
                int y1 = topY + (int) Math.round(b * logSlotH);
                int y2 = topY + (int) Math.round((b + 1) * logSlotH);
                int sh = Math.max(1, y2 - y1);

                boolean isFault = faultLog.contains(b);
                boolean isTrans = transLog.contains(b);
                boolean isMapped = pageTable.containsKey(p);

                if (isFault) g2.setColor(RED);
                else if (isTrans) g2.setColor(ACCENT);
                else if (isMapped) g2.setColor(new Color(0x1E3557));
                else g2.setColor(new Color(0x111E33));

                g2.fillRect(leftX, y1, colW, sh);

                // Draw thicker page boundary lines vs thinner byte lines
                if (d == 0) {
                    g2.setColor(new Color(0x6C92CC));
                    g2.drawLine(leftX, y1, leftX + colW, y1);
                } else if (sh >= 4) {
                    g2.setColor(new Color(0x284066));
                    g2.drawLine(leftX, y1, leftX + colW, y1);
                }
                g2.setColor(new Color(0x6C92CC));
                g2.drawLine(leftX, y1, leftX, y2);
                g2.drawLine(leftX + colW, y1, leftX + colW, y2);

                if (sh >= 10) {
                    g2.setFont(new Font("Consolas", Font.PLAIN, 10));
                    g2.setColor(new Color(0x9BB0D1));
                    String idxStr = String.valueOf(b);
                    g2.drawString(idxStr, leftX - g2.getFontMetrics().stringWidth(idxStr) - 5, y1 + sh / 2 + 4);

                    g2.setColor((isFault || isTrans) ? Color.BLACK : new Color(0xDCE8FF));
                    g2.drawString("P" + p + " \u00B7 d=" + d, leftX + 8, y1 + sh / 2 + 4);
                }
            }
            g2.setColor(new Color(0x6C92CC));
            g2.drawLine(leftX, topY + availH, leftX + colW, topY + availH);

            // Draw Physical Memory Byte Slots (0 .. totalPhysBytes - 1)
            for (int b = 0; b < totalPhysBytes; b++) {
                int f = b / pageSize;
                int d = b % pageSize;
                int y1 = topY + (int) Math.round(b * physSlotH);
                int y2 = topY + (int) Math.round((b + 1) * physSlotH);
                int sh = Math.max(1, y2 - y1);

                boolean isTrans = transPhys.contains(b);
                boolean isLoaded = frameToPage.containsKey(f);

                if (isTrans) g2.setColor(ACCENT);
                else if (isLoaded) g2.setColor(new Color(0x3B2E1E)); // Loaded frame tint
                else g2.setColor(new Color(0x111E33));               // Free frame

                g2.fillRect(rightX, y1, colW, sh);

                if (d == 0) {
                    g2.setColor(new Color(0x6C92CC));
                    g2.drawLine(rightX, y1, rightX + colW, y1);
                } else if (sh >= 4) {
                    g2.setColor(new Color(0x284066));
                    g2.drawLine(rightX, y1, rightX + colW, y1);
                }
                g2.setColor(new Color(0x6C92CC));
                g2.drawLine(rightX, y1, rightX, y2);
                g2.drawLine(rightX + colW, y1, rightX + colW, y2);

                if (sh >= 9) {
                    g2.setFont(new Font("Consolas", Font.PLAIN, 9));
                    g2.setColor(new Color(0x9BB0D1));
                    String idxStr = String.valueOf(b);
                    g2.drawString(idxStr, rightX - g2.getFontMetrics().stringWidth(idxStr) - 4, y1 + sh / 2 + 3);

                    g2.setColor(isTrans ? Color.BLACK : (isLoaded ? new Color(0xFFD599) : new Color(0x7B93B8)));
                    g2.drawString("F" + f + " \u00B7 d=" + d, rightX + 8, y1 + sh / 2 + 3);
                }
            }
            g2.setColor(new Color(0x6C92CC));
            g2.drawLine(rightX, topY + availH, rightX + colW, topY + availH);

            // Draw smooth cubic Bezier arrows connecting translated logical and physical bytes
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(1.8f));
            for (Map.Entry<Integer, Integer> entry : arrows.entrySet()) {
                int logB = entry.getKey();
                int physB = entry.getValue();
                if (logB < 0 || logB >= totalLogBytes || physB < 0 || physB >= totalPhysBytes) continue;

                double y1 = topY + (logB + 0.5) * logSlotH;
                double y2 = topY + (physB + 0.5) * physSlotH;
                double x1 = leftX + colW;
                double x2 = rightX - 24;
                double midX = (x1 + x2) / 2.0;

                g2.setColor(ACCENT);
                CubicCurve2D curve = new CubicCurve2D.Double(x1, y1, midX, y1, midX, y2, x2, y2);
                g2.draw(curve);

                int ax = (int) Math.round(x2);
                int ay = (int) Math.round(y2);
                g2.fillPolygon(new int[]{ax + 4, ax - 3, ax - 3}, new int[]{ay, ay - 3, ay + 3}, 3);
            }
            g2.setStroke(old);
            g2.dispose();
        }
    }

    // Creates a rounded button with custom background color
    private JButton makeButton(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(100, 30));
        return b;
    }

    // JPanel with anti-aliased rounded corners
    private static class RoundedPanel extends JPanel {
        private final int radius;
        RoundedPanel(int radius, Color bg) {
            this.radius = radius;
            setBackground(bg);
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}