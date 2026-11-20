package gui;

import LOGICS.CPULogic;
import LOGICS.Process;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CPUScheduling extends BasePanel {

    // Color palette used across buttons, tables, metric cards, and the Gantt chart
    private static final Color NAVY   = new Color(0x120078);
    private static final Color BLUE   = new Color(0x3B6FD4);
    private static final Color DARK   = new Color(0x0F1B2D);
    private static final Color DARK2  = new Color(0x13243D);
    private static final Color GREEN  = new Color(0x2E9E4F);
    private static final Color RED    = new Color(0xE74C3C);
    private static final Color ORANGE = new Color(0xE8A010);
    private static final Color RUN    = new Color(0x1F57E0);
    private static final Color ACCENT = new Color(0x3EA0FF);

    // Supported CPU scheduling algorithms shown in the dropdown
    private static final String[] ALGOS = {
            "FCFS",
            "SJF (Non-Preemptive)",
            "SRTF (Preemptive)",
            "Priority (Non-Preemptive)",
            "Priority (Preemptive)",
            "Round Robin"
    };

    // Top algorithm selector and Round Robin time quantum controls
    private final JComboBox<String> algoBox = new JComboBox<>(ALGOS);
    private final JLabel quantumLbl = new JLabel("Quantum:");
    private final JTextField quantumField = new JTextField("2", 3);

    // Process input table model (Job column is locked; AT, BT, and Priority are editable)
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Job", "AT", "BT", "Priority"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return c != 0; }
    };
    private final JTable table = new JTable(model);

    // Visualization and output components on the right panel
    private final GanttPanel gantt = new GanttPanel();
    private final JLabel awtLabel = bigNumber();
    private final JLabel atatLabel = bigNumber();
    private final JTextArea log = new JTextArea();

    // Initializes the CPU Scheduling window with left inputs and right results panel
    public CPUScheduling() {
        super("CPU Scheduling");

        content.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;

        gc.gridx = 0; gc.weightx = 0.42;
        gc.insets = new Insets(0, 0, 0, 14);
        content.add(buildLeft(), gc);

        gc.gridx = 1; gc.weightx = 0.58;
        gc.insets = new Insets(0, 0, 0, 0);
        content.add(buildRight(), gc);

        resetTable();
        updateQuantumVisibility();
    }

    // Builds the left panel containing algorithm dropdown, process table, and action buttons
    private JPanel buildLeft() {
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);

        // Top bar for selecting the scheduling algorithm and Round Robin quantum
        JPanel algoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        algoRow.setOpaque(false);
        JLabel al = new JLabel("Algorithm");
        al.setFont(new Font("Segoe UI", Font.BOLD, 16));
        algoBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        algoBox.setPreferredSize(new Dimension(190, 28));
        algoBox.addActionListener(e -> updateQuantumVisibility());
        quantumLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        algoRow.add(al);
        algoRow.add(algoBox);
        algoRow.add(quantumLbl);
        algoRow.add(quantumField);

        // Card holding the process table and control buttons
        RoundedPanel card = new RoundedPanel(24, CARD);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        styleTable();
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(CARD);

        JButton add = makeButton("+ Add", GREEN);
        JButton remove = makeButton("- Remove", RED);
        JButton rand = makeButton("Randomize", BLUE);
        JButton clear = makeButton("x Clear All", ORANGE);
        add.addActionListener(e -> addRow());
        remove.addActionListener(e -> removeRow());
        rand.addActionListener(e -> randomizeCurrentRows());
        clear.addActionListener(e -> clearAll());

        JPanel row = new JPanel(new GridLayout(1, 4, 6, 0));
        row.setOpaque(false);
        row.add(add);
        row.add(remove);
        row.add(rand);
        row.add(clear);

        JButton run = makeButton(" Run Simulator", RUN);
        run.setPreferredSize(new Dimension(100, 40));
        run.addActionListener(e -> runSimulator());

        JPanel south = new JPanel(new GridLayout(2, 1, 0, 8));
        south.setOpaque(false);
        south.add(row);
        south.add(run);

        card.add(sp, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);

        left.add(algoRow, BorderLayout.NORTH);
        left.add(card, BorderLayout.CENTER);
        return left;
    }

    // Builds the right dark panel containing the Gantt chart, AWT/ATAT cards, and execution log
    private JPanel buildRight() {
        RoundedPanel panel = new RoundedPanel(24, DARK);
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel("Visualization & Results");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JPanel cards = new JPanel(new GridLayout(1, 2, 12, 0));
        cards.setOpaque(false);
        cards.add(metricCard("Average Waiting Time (AWT)", awtLabel));
        cards.add(metricCard("Average Turnaround Time (ATAT)", atatLabel));
        cards.setPreferredSize(new Dimension(100, 110));

        log.setEditable(false);
        log.setBackground(new Color(0x0B1320));
        log.setForeground(new Color(0xCFE3FF));
        log.setFont(new Font("Consolas", Font.PLAIN, 12));
        log.setMargin(new Insets(6, 8, 6, 8));
        log.setText("Press \"Run Simulator\" to start.");
        JScrollPane logSp = new JScrollPane(log);
        logSp.setBorder(BorderFactory.createLineBorder(new Color(0x2A3B55)));
        logSp.setPreferredSize(new Dimension(100, 120));

        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.setOpaque(false);
        south.add(cards, BorderLayout.NORTH);
        south.add(logSp, BorderLayout.CENTER);

        JScrollPane ganttSp = new JScrollPane(gantt);
        ganttSp.setBorder(BorderFactory.createEmptyBorder());
        ganttSp.setOpaque(false);
        ganttSp.getViewport().setOpaque(false);
        ganttSp.getVerticalScrollBar().setUnitIncrement(16);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(ganttSp, BorderLayout.CENTER);
        center.add(south, BorderLayout.SOUTH);

        panel.add(title, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    // Creates a rounded metric card displaying a caption and large numeric readout
    private JPanel metricCard(String title, JLabel value) {
        RoundedPanel c = new RoundedPanel(16, DARK2);
        c.setLayout(new BorderLayout());
        c.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JLabel t = new JLabel(title);
        t.setForeground(Color.WHITE);
        t.setFont(new Font("Segoe UI", Font.BOLD, 11));
        c.add(t, BorderLayout.NORTH);
        c.add(value, BorderLayout.CENTER);
        return c;
    }

    // Creates a large centered light-blue label for AWT and ATAT metrics
    private static JLabel bigNumber() {
        JLabel l = new JLabel("0.00", SwingConstants.CENTER);
        l.setForeground(ACCENT);
        l.setFont(new Font("Segoe UI", Font.BOLD, 34));
        return l;
    }

    // Applies row height, grid borders, cell centering, and navy header styling to the process table
    private void styleTable() {
        table.setRowHeight(30);
        table.setGridColor(BLUE);
        table.setShowGrid(true);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setFillsViewportHeight(true);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(0xCFE0FF));
        table.setSelectionForeground(Color.BLACK);

        DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, false, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createLineBorder(BLUE, 1));
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, cell);

        DefaultTableCellRenderer hr = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setBackground(NAVY);
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
                return this;
            }
        };
        table.getTableHeader().setDefaultRenderer(hr);
        table.getTableHeader().setPreferredSize(new Dimension(100, 28));
        table.getTableHeader().setReorderingAllowed(false);
    }

    // Resets the process table to 5 empty rows labeled P1 through P5
    private void resetTable() {
        model.setRowCount(0);
        for (int i = 0; i < 5; i++) model.addRow(new Object[]{"P" + (i + 1), "", "", ""});
    }

    // Renumbers all Job IDs sequentially (P1, P2, ...) after a row is removed
    private void renumber() {
        for (int i = 0; i < model.getRowCount(); i++) model.setValueAt("P" + (i + 1), i, 0);
    }

    // Commits any active cell edit in the process table
    private void stopEditing() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
    }

    // Appends a new process row to the table and scrolls it into view
    private void addRow() {
        stopEditing();
        model.addRow(new Object[]{"P" + (model.getRowCount() + 1), "", "", ""});
        int lastRow = model.getRowCount() - 1;
        table.scrollRectToVisible(table.getCellRect(lastRow, 0, true));
    }

    // Removes the selected process row (or the last row) and renumbers remaining rows
    private void removeRow() {
        stopEditing();
        if (model.getRowCount() == 0) return;
        int r = table.getSelectedRow();
        if (r < 0) r = model.getRowCount() - 1;
        model.removeRow(r);
        renumber();
    }

    // Fills all current rows with random AT, BT, and Priority values and runs the simulator
    private void randomizeCurrentRows() {
        stopEditing();
        int rows = model.getRowCount();
        if (rows == 0) {
            resetTable();
            rows = model.getRowCount();
        }
        Random rand = new Random();
        for (int i = 0; i < rows; i++) {
            int at = rand.nextInt(Math.max(5, rows * 2));
            int bt = 1 + rand.nextInt(12);
            int pr = 1 + rand.nextInt(Math.max(5, rows));
            model.setValueAt(String.valueOf(at), i, 1);
            model.setValueAt(String.valueOf(bt), i, 2);
            model.setValueAt(String.valueOf(pr), i, 3);
        }
        runSimulator();
    }

    // Clears all table inputs, resets the Gantt chart, and resets AWT/ATAT metrics
    private void clearAll() {
        stopEditing();
        resetTable();
        gantt.setData(new ArrayList<>(), new ArrayList<>(), 0);
        awtLabel.setText("0.00");
        atatLabel.setText("0.00");
        log.setText("Cleared.");
    }

    // Shows the Time Quantum input only when Round Robin is selected
    private void updateQuantumVisibility() {
        boolean rr = "Round Robin".equals(algoBox.getSelectedItem());
        quantumLbl.setVisible(rr);
        quantumField.setVisible(rr);
    }

    // Validates inputs, runs the selected CPU scheduling algorithm, and updates results
    private void runSimulator() {
        stopEditing();
        List<Process> ps = readProcs();
        if (ps == null) return;
        if (ps.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one job (AT and BT).");
            return;
        }

        String algo = (String) algoBox.getSelectedItem();
        int q = 1;
        if ("Round Robin".equals(algo)) {
            try {
                q = Integer.parseInt(quantumField.getText().trim());
                if (q < 1) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Quantum must be a whole number of 1 or more.");
                return;
            }
        }

        CPULogic.SimResult res = CPULogic.run(ps, algo, q);

        List<String> names = new ArrayList<>();
        for (Process p : ps) names.add(p.pid);

        awtLabel.setText(String.format("%.2f", res.awt));
        atatLabel.setText(String.format("%.2f", res.atat));
        log.setText(res.logText);
        log.setCaretPosition(0);
        gantt.setData(names, res.segments, res.maxTime);
    }

    // Parses valid process rows from the table, returning null if any input is invalid
    private List<Process> readProcs() {
        List<Process> list = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            String name = String.valueOf(model.getValueAt(i, 0));
            String at = cell(i, 1), bt = cell(i, 2), pr = cell(i, 3);
            if (at.isEmpty() && bt.isEmpty() && pr.isEmpty()) continue;
            try {
                int arrival = at.isEmpty() ? 0 : Integer.parseInt(at);
                int burst = Integer.parseInt(bt);
                int priority = pr.isEmpty() ? 0 : Integer.parseInt(pr);
                if (arrival < 0 || burst < 1 || priority < 0)
                    throw new NumberFormatException();
                list.add(new Process(name, arrival, burst, priority));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Invalid input in " + name + ".\nAT & Priority: 0 or higher. BT: 1 or higher (whole numbers).");
                return null;
            }
        }
        return list;
    }

    // Safely reads and trims a cell string value
    private String cell(int r, int c) {
        Object o = model.getValueAt(r, c);
        return o == null ? "" : o.toString().trim();
    }

    // Custom scrollable panel that renders the multi-row CPU execution Gantt chart
    private static class GanttPanel extends JPanel {
        private static final Color[] PALETTE = {
                new Color(0x4DA3FF), new Color(0x3DD6A8), new Color(0xFFB454),
                new Color(0xFF6B8A), new Color(0xB48CFF), new Color(0x5CE1E6),
                new Color(0xF2D45C), new Color(0x8BE36B)
        };
        private List<String> names = new ArrayList<>();
        private List<int[]> segs = new ArrayList<>();
        private int maxTime = 0;

        GanttPanel() {
            setOpaque(false);
            setData(new ArrayList<>(), new ArrayList<>(), 0);
        }

        // Updates process rows, execution segments, and total timeline duration
        void setData(List<String> n, List<int[]> s, int max) {
            names = new ArrayList<>(n);
            for (int i = names.size(); i < 5; i++) names.add("P" + (i + 1));
            segs = s;
            maxTime = max;
            int prefH = Math.max(210, names.size() * 28 + 46);
            setPreferredSize(new Dimension(300, prefH));
            revalidate();
            repaint();
        }

        // Draws time grid lines, process row labels, and colored execution blocks
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            g2.setColor(DARK2);
            g2.fillRoundRect(0, 0, w, h, 16, 16);

            int left = 44, right = 16, top = 12, bottom = 34;
            int pw = w - left - right, ph = h - top - bottom;
            if (pw < 20 || ph < 20) { g2.dispose(); return; }

            int rows = names.size();
            int rowH = Math.max(16, ph / rows);

            int step, axisMax;
            if (maxTime == 0) { step = 5; axisMax = 20; }
            else {
                step = Math.max(1, (int) Math.ceil(maxTime / 10.0));
                axisMax = ((maxTime + step - 1) / step) * step;
            }

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            FontMetrics fm = g2.getFontMetrics();

            // Draw vertical time ticks and bottom axis labels
            for (int tt = 0; tt <= axisMax; tt += step) {
                int x = left + (int) ((long) pw * tt / axisMax);
                g2.setColor(new Color(0x24385A));
                g2.drawLine(x, top, x, top + ph);
                g2.setColor(new Color(0xB8C7E0));
                String s = String.valueOf(tt);
                g2.drawString(s, x - fm.stringWidth(s) / 2, top + ph + 14);
            }
            g2.drawString("Time", left + pw / 2 - fm.stringWidth("Time") / 2, h - 6);

            // Draw horizontal process lanes and left PID labels
            for (int r = 0; r < rows; r++) {
                int y = top + r * rowH;
                g2.setColor(new Color(0x1C2E4D));
                g2.drawLine(left, y + rowH, left + pw, y + rowH);
                g2.setColor(new Color(0xB8C7E0));
                g2.drawString(names.get(r), 8, y + rowH / 2 + fm.getAscent() / 2 - 2);
            }

            // Draw colored execution bars for each scheduled time slice
            for (int[] s : segs) {
                int r = s[0];
                int x1 = left + (int) ((long) pw * s[1] / axisMax);
                int x2 = left + (int) ((long) pw * s[2] / axisMax);
                int y = top + r * rowH + 3;
                int bh = Math.max(8, rowH - 6);
                g2.setColor(PALETTE[r % PALETTE.length]);
                g2.fillRoundRect(x1, y, Math.max(2, x2 - x1), bh, 8, 8);

                String label = names.get(r);
                if (x2 - x1 > fm.stringWidth(label) + 6) {
                    g2.setColor(new Color(0x0B1320));
                    g2.drawString(label, x1 + (x2 - x1 - fm.stringWidth(label)) / 2,
                            y + bh / 2 + fm.getAscent() / 2 - 2);
                }
            }
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
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
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
        b.setPreferredSize(new Dimension(90, 34));
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