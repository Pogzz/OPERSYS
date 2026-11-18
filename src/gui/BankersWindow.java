package gui;

import LOGICS.BankersLogic;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.Random;

public class BankersWindow extends BasePanel {

    private static final Color NAVY   = new Color(0x120078);
    private static final Color BLUE   = new Color(0x3B6FD4);
    private static final Color DARK   = new Color(0x0F1B2D);
    private static final Color DARK2  = new Color(0x13243D);
    private static final Color GREEN  = new Color(0x2E9E4F);
    private static final Color RED    = new Color(0xE74C3C);
    private static final Color ORANGE = new Color(0xE8A010);
    private static final Color RUN    = new Color(0x1F57E0);
    private static final Color ACCENT = new Color(0x3EA0FF);

    private int nProc = 5;
    private int mRes  = 4;

    private final JTextField procField = new JTextField("5", 3);
    private final JTextField resField  = new JTextField("4", 3);
    private final JPanel totalResPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
    private JTextField[] totalFields   = new JTextField[0];

    private DefaultTableModel model;
    private final JTable table = new JTable();
    private final JLabel statusBadge = new JLabel("READY", SwingConstants.CENTER);
    private final JTextArea resultArea = new JTextArea();

    public BankersWindow() {
        super("Banker's Algorithm");

        content.setLayout(new BorderLayout(0, 10));
        content.add(buildTopControls(), BorderLayout.NORTH);
        content.add(buildCenterTable(), BorderLayout.CENTER);
        content.add(buildBottomResult(), BorderLayout.SOUTH);

        rebuildGrid(5, 4);
        randomizeData();
    }

 // ================= TOP CONTROLS =================
    private JPanel buildTopControls() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);

        // ROW 1: Dimensions + Set + Randomize (Left) | Run Simulation (Right)
        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        row1.setOpaque(false);

        JPanel dimPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dimPanel.setOpaque(false);

        JLabel pLbl = new JLabel("Processes (n):");
        pLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        styleInputField(procField);

        JLabel rLbl = new JLabel("Resource types (m):");
        rLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        styleInputField(resField);

        JButton setBtn  = makeButton("Set", BLUE, 64, 30);
        JButton randBtn = makeButton("Randomize", ORANGE, 110, 30);

        setBtn.addActionListener(e -> applyDimensions());
        randBtn.addActionListener(e -> randomizeData());

        dimPanel.add(pLbl);
        dimPanel.add(procField);
        dimPanel.add(rLbl);
        dimPanel.add(resField);
        dimPanel.add(setBtn);
        dimPanel.add(randBtn);

        JButton runBtn = makeButton("\u25B6  Run Simulation", RUN, 155, 32);
        runBtn.addActionListener(e -> runSimulation());

        row1.add(dimPanel, BorderLayout.WEST);
        row1.add(runBtn, BorderLayout.EAST);

        // ROW 2: Total Resources (Left) | + / - Process & Resource buttons (Right)
        JPanel row2 = new JPanel(new BorderLayout(10, 0));
        row2.setOpaque(false);

        totalResPanel.setOpaque(false);

        JPanel rowColActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rowColActions.setOpaque(false);

        JButton addProcBtn = makeButton("+ Process", GREEN, 100, 28);
        JButton delProcBtn = makeButton("- Process", RED, 98, 28);
        JButton addResBtn  = makeButton("+ Resource", BLUE, 106, 28);
        JButton delResBtn  = makeButton("- Resource", RED, 104, 28);

        addProcBtn.addActionListener(e -> {
            nProc++;
            procField.setText(String.valueOf(nProc));
            rebuildGrid(nProc, mRes);
            recalculateTable();
        });

        delProcBtn.addActionListener(e -> {
            if (nProc > 1) {
                nProc--;
                procField.setText(String.valueOf(nProc));
                rebuildGrid(nProc, mRes);
                recalculateTable();
            }
        });

        addResBtn.addActionListener(e -> {
            mRes++;
            resField.setText(String.valueOf(mRes));
            rebuildGrid(nProc, mRes);
            recalculateTable();
        });

        delResBtn.addActionListener(e -> {
            if (mRes > 1) {
                mRes--;
                resField.setText(String.valueOf(mRes));
                rebuildGrid(nProc, mRes);
                randomizeData();
            }
        });

        rowColActions.add(addProcBtn);
        rowColActions.add(delProcBtn);
        rowColActions.add(addResBtn);
        rowColActions.add(delResBtn);

        row2.add(totalResPanel, BorderLayout.CENTER);
        row2.add(rowColActions, BorderLayout.EAST);

        wrapper.add(row1, BorderLayout.NORTH);
        wrapper.add(row2, BorderLayout.SOUTH);
        return wrapper;
    }
    private void styleInputField(JTextField tf) {
        tf.setHorizontalAlignment(SwingConstants.CENTER);
        tf.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tf.setPreferredSize(new Dimension(42, 28));
        tf.setBorder(BorderFactory.createLineBorder(BLUE, 1));
    }

    // ================= CENTER TABLE =================
    private JPanel buildCenterTable() {
        RoundedPanel card = new RoundedPanel(24, CARD);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);

        JLabel title = new JLabel("Banker's Table");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(NAVY);

        JLabel hint = new JLabel("Edit Allocation & Max cells (Available & Need auto-calculate)");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(new Color(0x4A5B75));

        headerBar.add(title, BorderLayout.WEST);
        headerBar.add(hint, BorderLayout.EAST);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(BLUE, 1));
        sp.getViewport().setBackground(Color.WHITE);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        card.add(headerBar, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ================= BOTTOM RESULT =================
    private JPanel buildBottomResult() {
        RoundedPanel panel = new RoundedPanel(24, DARK);
        panel.setLayout(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        panel.setPreferredSize(new Dimension(100, 145));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = new JLabel("Result");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));

        statusBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusBadge.setForeground(ACCENT);

        top.add(title, BorderLayout.WEST);
        top.add(statusBadge, BorderLayout.EAST);

        resultArea.setEditable(false);
        resultArea.setBackground(new Color(0x0B1320));
        resultArea.setForeground(new Color(0xCFE3FF));
        resultArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        resultArea.setMargin(new Insets(6, 10, 6, 10));
        resultArea.setText("Set n and m, click Randomize, then click Run Simulation.");

        JScrollPane sp = new JScrollPane(resultArea);
        sp.setBorder(BorderFactory.createLineBorder(new Color(0x2A3B55)));

        panel.add(top, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    // ================= GRID BUILDER =================
    private void applyDimensions() {
        try {
            int p = Integer.parseInt(procField.getText().trim());
            int r = Integer.parseInt(resField.getText().trim());
            if (p < 1 || r < 1) {
                JOptionPane.showMessageDialog(this, "Processes (n) and Resources (m) must be at least 1.");
                return;
            }
            nProc = p;
            mRes = r;
            rebuildGrid(nProc, mRes);
            recalculateTable();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid whole numbers for n and m.");
        }
    }

    private String resourceName(int idx) {
        if (idx < 26) return String.valueOf((char) ('A' + idx));
        return "R" + (idx + 1);
    }

    private void rebuildGrid(int pCount, int rCount) {
        // 1. Rebuild Total Resources fields
        totalResPanel.removeAll();
        JLabel trLbl = new JLabel("Total resources:");
        trLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        totalResPanel.add(trLbl);

        totalFields = new JTextField[rCount];
        for (int j = 0; j < rCount; j++) {
            JLabel rName = new JLabel(resourceName(j));
            rName.setFont(new Font("Segoe UI", Font.BOLD, 13));
            JTextField tf = new JTextField("10", 3);
            styleInputField(tf);
            tf.setPreferredSize(new Dimension(40, 28));
            tf.addActionListener(e -> recalculateTable());
            totalFields[j] = tf;
            totalResPanel.add(rName);
            totalResPanel.add(tf);
        }
        totalResPanel.revalidate();
        totalResPanel.repaint();

        // 2. Build Unified Table: Process | Allocation | Max | Available | Need
        int totalCols = 1 + (rCount * 4);
        String[] cols = new String[totalCols];
        cols[0] = "";
        for (int sec = 0; sec < 4; sec++) {
            for (int j = 0; j < rCount; j++) {
                cols[1 + sec * rCount + j] = resourceName(j);
            }
        }

        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                // Only Allocation and Max are editable
                return c >= 1 && c <= 2 * mRes;
            }
        };

        for (int i = 0; i < pCount; i++) {
            Object[] row = new Object[totalCols];
            row[0] = "P" + (i + 1);
            for (int c = 1; c < totalCols; c++) {
                row[c] = (c > 2 * mRes && c <= 3 * mRes && i > 0) ? "" : "0";
            }
            model.addRow(row);
        }

        model.addTableModelListener(e -> {
            if (e.getColumn() >= 1 && e.getColumn() <= 2 * mRes) {
                SwingUtilities.invokeLater(this::recalculateTable);
            }
        });

        table.setModel(model);
        styleUnifiedTable();
    }

    private void styleUnifiedTable() {
        table.setRowHeight(30);
        table.setGridColor(BLUE);
        table.setShowGrid(true);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setFillsViewportHeight(true);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(0xCFE0FF));
        table.setSelectionForeground(Color.BLACK);

        // If there are many resource columns, enable horizontal scrolling so columns don't squish
        int totalCols = table.getColumnCount();
        if (totalCols > 17) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.getColumnModel().getColumn(0).setPreferredWidth(70);
            for (int c = 1; c < totalCols; c++) {
                table.getColumnModel().getColumn(c).setPreferredWidth(44);
            }
        } else {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        }

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, false, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createLineBorder(BLUE, 1));
                if (!sel) {
                    if (c == 0) {
                        setBackground(new Color(0xEEF3FC));
                        setFont(new Font("Segoe UI", Font.BOLD, 13));
                    } else if (c <= mRes) {
                        setBackground(Color.WHITE);              // Allocation
                    } else if (c <= 2 * mRes) {
                        setBackground(new Color(0xF7FAFF));      // Max
                    } else if (c <= 3 * mRes) {
                        setBackground(new Color(0xEFF8F4));      // Available
                    } else {
                        setBackground(new Color(0xFFF9EE));      // Need
                    }
                }
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(100, 48));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new GroupedHeaderRenderer());
    }

    /** 2-row grouped header: Process | Allocation | Max | Available | Need over A, B, C, D... */
    private class GroupedHeaderRenderer extends JComponent implements TableCellRenderer {
        private int col;
        private String subLabel = "";

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) {
            this.col = c;
            this.subLabel = (v == null) ? "" : v.toString();
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int halfH = h / 2;

            Color topBg = NAVY;
            Color botBg = new Color(0x1D3B8B);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();

            if (col == 0) {
                g2.setColor(topBg);
                g2.fillRect(0, 0, w, h);
                g2.setColor(Color.WHITE);
                g2.drawRect(0, 0, w - 1, h - 1);
                String title = "Process";
                g2.drawString(title, (w - fm.stringWidth(title)) / 2, h / 2 + fm.getAscent() / 2 - 2);
            } else {
                int secIdx = (col - 1) / mRes;
                int posInSec = (col - 1) % mRes;
                String groupTitle = (secIdx == 0) ? "Allocation"
                                  : (secIdx == 1) ? "Max"
                                  : (secIdx == 2) ? "Available"
                                  : "Need";

                g2.setColor(topBg);
                g2.fillRect(0, 0, w, halfH);

                // Compute exact pixel span across the mRes columns in this group
                TableColumnModel cm = table.getColumnModel();
                int groupStartCol = 1 + secIdx * mRes;
                int groupWidth = 0;
                int xOffset = 0;
                for (int k = 0; k < mRes; k++) {
                    int cw = cm.getColumn(groupStartCol + k).getWidth();
                    if (k < posInSec) xOffset -= cw;
                    groupWidth += cw;
                }

                g2.setColor(Color.WHITE);
                int textX = xOffset + (groupWidth - fm.stringWidth(groupTitle)) / 2;
                g2.drawString(groupTitle, textX, halfH / 2 + fm.getAscent() / 2 - 2);

                g2.drawLine(0, 0, w, 0);
                g2.drawLine(0, halfH - 1, w, halfH - 1);
                if (posInSec == 0) g2.drawLine(0, 0, 0, halfH);
                if (posInSec == mRes - 1) g2.drawLine(w - 1, 0, w - 1, halfH);

                g2.setColor(botBg);
                g2.fillRect(0, halfH, w, h - halfH);
                g2.setColor(Color.WHITE);
                g2.drawRect(0, halfH, w - 1, h - halfH - 1);
                g2.drawString(subLabel, (w - fm.stringWidth(subLabel)) / 2,
                        halfH + (h - halfH) / 2 + fm.getAscent() / 2 - 2);
            }
            g2.dispose();
        }
    }

    // ================= RANDOMIZE & AUTO-CALC =================
    private void randomizeData() {
        stopEditing();
        Random rand = new Random();
        int[] totals = new int[mRes];
        int[] sumAlloc = new int[mRes];

        for (int j = 0; j < mRes; j++) {
            totals[j] = Math.max(5, nProc + 2 + rand.nextInt(Math.max(4, nProc)));
            totalFields[j].setText(String.valueOf(totals[j]));
        }

        for (int i = 0; i < nProc; i++) {
            for (int j = 0; j < mRes; j++) {
                int maxVal = 1 + rand.nextInt(Math.max(2, totals[j] / 2 + 1));
                int remainingCap = Math.max(0, totals[j] - sumAlloc[j]);
                int allocVal = (remainingCap == 0) ? 0 : rand.nextInt(Math.min(maxVal, remainingCap) + 1);
                sumAlloc[j] += allocVal;

                model.setValueAt(allocVal, i, 1 + j);
                model.setValueAt(maxVal, i, 1 + mRes + j);
            }
        }
        recalculateTable();
        statusBadge.setText("READY");
        statusBadge.setForeground(ACCENT);
        resultArea.setText("Set n and m, click Randomize, then click Run Simulation.");
    }

    private void recalculateTable() {
        int[] totals = new int[mRes];
        int[] sumAlloc = new int[mRes];

        for (int j = 0; j < mRes; j++) {
            totals[j] = parseSafe(totalFields[j].getText());
        }

        for (int i = 0; i < nProc; i++) {
            for (int j = 0; j < mRes; j++) {
                int a = parseSafe(model.getValueAt(i, 1 + j));
                int m = parseSafe(model.getValueAt(i, 1 + mRes + j));
                sumAlloc[j] += a;
                int need = m - a;
                model.setValueAt(need, i, 1 + 3 * mRes + j);
            }
        }

        for (int j = 0; j < mRes; j++) {
            int avail = totals[j] - sumAlloc[j];
            model.setValueAt(avail, 0, 1 + 2 * mRes + j);
        }
    }

    private int parseSafe(Object o) {
        if (o == null) return 0;
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void stopEditing() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
    }

    // ================= RUN SIMULATION =================
    private void runSimulation() {
        stopEditing();
        recalculateTable();

        int[][] alloc = new int[nProc][mRes];
        int[][] max   = new int[nProc][mRes];
        int[] avail   = new int[mRes];

        try {
            for (int j = 0; j < mRes; j++) {
                int total = Integer.parseInt(totalFields[j].getText().trim());
                if (total < 0) {
                    JOptionPane.showMessageDialog(this, "Total resources cannot be negative.");
                    return;
                }
            }

            for (int i = 0; i < nProc; i++) {
                for (int j = 0; j < mRes; j++) {
                    alloc[i][j] = Integer.parseInt(model.getValueAt(i, 1 + j).toString().trim());
                    max[i][j]   = Integer.parseInt(model.getValueAt(i, 1 + mRes + j).toString().trim());
                    if (alloc[i][j] < 0 || max[i][j] < 0) {
                        JOptionPane.showMessageDialog(this, "Values in Allocation and Max cannot be negative.");
                        return;
                    }
                    if (alloc[i][j] > max[i][j]) {
                        JOptionPane.showMessageDialog(this,
                                "Error in P" + (i + 1) + " (" + resourceName(j) + "): Allocation cannot exceed Max.");
                        return;
                    }
                }
            }

            for (int j = 0; j < mRes; j++) {
                avail[j] = Integer.parseInt(model.getValueAt(0, 1 + 2 * mRes + j).toString().trim());
                if (avail[j] < 0) {
                    JOptionPane.showMessageDialog(this,
                            "Error in Resource " + resourceName(j) + ": Total Allocation exceeds Total Resources!");
                    return;
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please make sure all cells contain valid whole numbers.");
            return;
        }

        BankersLogic.BankersResult res = BankersLogic.analyze(alloc, max, avail);

        StringBuilder out = new StringBuilder();
        if (res.isSafe) {
            statusBadge.setText("SAFE STATE");
            statusBadge.setForeground(new Color(0x4ADE80));
            out.append("SYSTEM IS SAFE!  Safe Sequence: ");
            for (int k = 0; k < res.safeSequence.size(); k++) {
                out.append("P").append(res.safeSequence.get(k) + 1);
                if (k < res.safeSequence.size() - 1) out.append(" -> ");
            }
            out.append("\n\n").append(res.logText);
        } else {
            statusBadge.setText("UNSAFE STATE (DEADLOCK)");
            statusBadge.setForeground(RED);
            out.append("SYSTEM IS UNSAFE (Deadlock possible)!\n\n")
               .append(res.logText);
        }

        resultArea.setText(out.toString());
        resultArea.setCaretPosition(0);
    }

    // ================= HELPERS =================
    private JButton makeButton(String text, Color bg, int w, int h) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setMargin(new Insets(0, 6, 0, 6));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(w, h));
        return b;
    }

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