package gui;

import LOGICS.DiskLogic;
import LOGICS.DiskLogic.DiskResult;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DiskWindow extends BasePanel {

    // Color palette used across inputs, buttons, dark cards, and the seek graph
    private static final Color NAVY   = new Color(0x120078);
    private static final Color BLUE   = new Color(0x3B6FD4);
    private static final Color DARK   = new Color(0x0F1B2D);
    private static final Color DARK2  = new Color(0x13243D);
    private static final Color ORANGE = new Color(0xE8A010);
    private static final Color RUN    = new Color(0x1F57E0);
    private static final Color ACCENT = new Color(0x3EA0FF);

    // Supported disk scheduling algorithms shown in the selector and comparison table
    private static final String[] ALGOS = {
        "SSTF", "LOOK", "Elevator (SCAN)", "C-SCAN", "FIFO (FCFS)",
    };

    // Input fields for request queue, starting head position, max cylinder, and random count
    private final JTextField queueField = new JTextField("98 183 37 122 14 124 65 67");
    private final JTextField headField  = new JTextField("53", 4);
    private final JTextField maxField   = new JTextField("199", 4);
    private final JTextField countField = new JTextField("8", 4);
    private final JComboBox<String> algoBox = new JComboBox<>(ALGOS);

    // Visualization canvas and bottom summary metric labels
    private final SeekGraphPanel graphPanel = new SeekGraphPanel();
    private final JLabel totalValLabel = new JLabel("0 tracks", SwingConstants.CENTER);
    private final JLabel avgValLabel   = new JLabel("0.00", SwingConstants.CENTER);

    // Read-only table comparing total and average seek distances across all algorithms
    private final DefaultTableModel compModel = new DefaultTableModel(
            new String[]{"Algorithm", "Total", "Avg/seek"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable compTable = new JTable(compModel);

    // Read-only table listing step-by-step track visits and distance traversed
    private final DefaultTableModel seqModel = new DefaultTableModel(
            new String[]{"Seq", "Track", "Traversed"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable seqTable = new JTable(seqModel);

    // Initializes the Disk Scheduling window and runs the default sample queue
    public DiskWindow() {
        super("Disk Scheduling");

        content.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;

        // Left column: input controls, head movement graph, and metric cards
        gc.gridx = 0; gc.weightx = 0.65;
        gc.insets = new Insets(0, 0, 0, 14);
        content.add(buildLeftArea(), gc);

        // Right column: algorithm comparison table and step sequence table
        gc.gridx = 1; gc.weightx = 0.35;
        gc.insets = new Insets(0, 0, 0, 0);
        content.add(buildRightArea(), gc);

        algoBox.addActionListener(e -> runSimulation());
        runSimulation();
    }

    // Builds the left workspace containing input controls and the seek graph card
    private JPanel buildLeftArea() {
        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(false);

        left.add(buildInputHeader(), BorderLayout.NORTH);
        left.add(buildGraphCard(), BorderLayout.CENTER);
        return left;
    }

    // Builds the two-row header for request queue, algorithm selector, head/max/count, and action buttons
    private JPanel buildInputHeader() {
        JPanel wrap = new JPanel(new GridLayout(2, 1, 0, 8));
        wrap.setOpaque(false);

        // Row 1: Queue input field and Algorithm dropdown
        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        row1.setOpaque(false);

        JPanel qWrap = new JPanel(new BorderLayout(8, 0));
        qWrap.setOpaque(false);
        JLabel qLbl = new JLabel("Queue:");
        qLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        queueField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        queueField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BLUE, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        qWrap.add(qLbl, BorderLayout.WEST);
        qWrap.add(queueField, BorderLayout.CENTER);

        JPanel aWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        aWrap.setOpaque(false);
        JLabel aLbl = new JLabel("Algorithm:");
        aLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        algoBox.setFont(new Font("Segoe UI", Font.BOLD, 12));
        algoBox.setPreferredSize(new Dimension(145, 30));
        aWrap.add(aLbl);
        aWrap.add(algoBox);

        row1.add(qWrap, BorderLayout.CENTER);
        row1.add(aWrap, BorderLayout.EAST);

        // Row 2: Head, Max track, Count fields, Randomize button, and Run button
        JPanel row2 = new JPanel(new BorderLayout(8, 0));
        row2.setOpaque(false);

        JPanel numFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        numFields.setOpaque(false);
        numFields.add(makeFieldLabel("Head:"));
        numFields.add(styleNumBox(headField));
        numFields.add(makeFieldLabel("Max:"));
        numFields.add(styleNumBox(maxField));
        numFields.add(makeFieldLabel("Count:"));
        numFields.add(styleNumBox(countField));

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionBtns.setOpaque(false);
        JButton randBtn = makeButton("Randomize", ORANGE, 108, 30);
        JButton runBtn  = makeButton(" Run", RUN, 96, 30);

        randBtn.addActionListener(e -> generateRandomQueue());
        runBtn.addActionListener(e -> runSimulation());

        actionBtns.add(randBtn);
        actionBtns.add(runBtn);

        row2.add(numFields, BorderLayout.WEST);
        row2.add(actionBtns, BorderLayout.EAST);

        wrap.add(row1);
        wrap.add(row2);
        return wrap;
    }

    // Creates a bold input label for numeric fields
    private JLabel makeFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return l;
    }

    // Applies centered bold font and blue border to small numeric text boxes
    private JTextField styleNumBox(JTextField tf) {
        tf.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tf.setHorizontalAlignment(SwingConstants.CENTER);
        tf.setPreferredSize(new Dimension(52, 28));
        tf.setBorder(BorderFactory.createLineBorder(BLUE, 1));
        return tf;
    }

    // Builds the dark card holding the head movement chart and bottom stat boxes
    private JPanel buildGraphCard() {
        RoundedPanel card = new RoundedPanel(24, DARK);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel("Disk Head Movement Graph");
        title.setForeground(ACCENT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JPanel statsRow = new JPanel(new GridLayout(1, 2, 14, 0));
        statsRow.setOpaque(false);
        statsRow.setPreferredSize(new Dimension(100, 74));
        statsRow.add(makeStatBox("Total Head Movement", totalValLabel));
        statsRow.add(makeStatBox("Avg / Seek", avgValLabel));

        card.add(title, BorderLayout.NORTH);
        card.add(graphPanel, BorderLayout.CENTER);
        card.add(statsRow, BorderLayout.SOUTH);
        return card;
    }

    // Creates a dark rounded stat box displaying a metric caption and value
    private JPanel makeStatBox(String caption, JLabel valueLbl) {
        RoundedPanel box = new RoundedPanel(16, DARK2);
        box.setLayout(new BorderLayout(0, 2));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2A3B55), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        JLabel cap = new JLabel(caption);
        cap.setForeground(Color.WHITE);
        cap.setFont(new Font("Segoe UI", Font.BOLD, 12));

        valueLbl.setForeground(ACCENT);
        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));

        box.add(cap, BorderLayout.NORTH);
        box.add(valueLbl, BorderLayout.CENTER);
        return box;
    }

    // Builds the right column stacked with Comparison (top) and Sequence (bottom) cards
    private JPanel buildRightArea() {
        JPanel right = new JPanel(new GridLayout(2, 1, 0, 12));
        right.setOpaque(false);

        right.add(wrapTableCard("Comparison", compTable));
        right.add(wrapTableCard("Sequence", seqTable));
        return right;
    }

    // Wraps a styled dark table inside a titled dark rounded card
    private JPanel wrapTableCard(String heading, JTable table) {
        RoundedPanel card = new RoundedPanel(22, DARK);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel lbl = new JLabel(heading);
        lbl.setForeground(ACCENT);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));

        styleDarkTable(table);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(new Color(0x2A3B55)));
        sp.getViewport().setBackground(DARK2);

        card.add(lbl, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // Applies dark theme colors, cell centering, and header styling to a JTable
    private void styleDarkTable(JTable t) {
        t.setRowHeight(26);
        t.setGridColor(new Color(0x24385A));
        t.setShowGrid(true);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setFillsViewportHeight(true);
        t.setBackground(DARK2);
        t.setForeground(Color.WHITE);
        t.setSelectionBackground(new Color(0x1E3557));
        t.setSelectionForeground(Color.WHITE);

        DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object v,
                    boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(table, v, sel, false, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createLineBorder(new Color(0x24385A), 1));
                setForeground(c == 0 ? new Color(0xCFE3FF) : Color.WHITE);
                if (c == 0) setFont(new Font("Segoe UI", Font.BOLD, 12));
                return this;
            }
        };
        t.setDefaultRenderer(Object.class, cell);

        DefaultTableCellRenderer hr = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object v,
                    boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(table, v, sel, foc, r, c);
                setBackground(new Color(0x0B1320));
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createLineBorder(new Color(0x2A3B55), 1));
                return this;
            }
        };
        t.getTableHeader().setDefaultRenderer(hr);
        t.getTableHeader().setPreferredSize(new Dimension(60, 26));
        t.getTableHeader().setReorderingAllowed(false);
    }

    // Generates a random initial head and request queue within [0, maxTrack] and runs simulation
    private void generateRandomQueue() {
        int maxTrack, count;
        try {
            maxTrack = Integer.parseInt(maxField.getText().trim());
            count = Integer.parseInt(countField.getText().trim());
            if (maxTrack < 1 || count < 1) {
                JOptionPane.showMessageDialog(this, "Max track and Count must be at least 1.");
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid whole numbers for Max and Count.");
            return;
        }

        Random rand = new Random();
        headField.setText(String.valueOf(rand.nextInt(maxTrack + 1)));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(" ");
            sb.append(rand.nextInt(maxTrack + 1));
        }
        queueField.setText(sb.toString());
        runSimulation();
    }

    // Validates inputs, simulates all algorithms for comparison, and plots the active trajectory
    private void runSimulation() {
        int head, maxTrack;
        try {
            head = Integer.parseInt(headField.getText().trim());
            maxTrack = Integer.parseInt(maxField.getText().trim());
            if (maxTrack < 1 || head < 0 || head > maxTrack) {
                JOptionPane.showMessageDialog(this, "Head must be between 0 and Max (" + maxTrack + ").");
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid whole numbers for Head and Max.");
            return;
        }

        String raw = queueField.getText().trim();
        if (raw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter at least one track request in the Queue.");
            return;
        }

        List<Integer> reqs = new ArrayList<>();
        for (String tok : raw.split("[,\\s]+")) {
            if (tok.isEmpty()) continue;
            try {
                int track = Integer.parseInt(tok.trim());
                if (track < 0 || track > maxTrack) {
                    JOptionPane.showMessageDialog(this,
                            "Track " + track + " is outside valid range [0, " + maxTrack + "].");
                    return;
                }
                reqs.add(track);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid track number: \"" + tok + "\"");
                return;
            }
        }

        countField.setText(String.valueOf(reqs.size()));

        // Run all algorithms to populate the Comparison table
        compModel.setRowCount(0);
        String selectedAlgo = (String) algoBox.getSelectedItem();
        DiskResult activeResult = null;

        for (String algo : ALGOS) {
            DiskResult res = DiskLogic.simulate(algo, head, reqs, maxTrack);
            compModel.addRow(new Object[]{
                algo,
                res.totalMovement,
                String.format("%.2f", res.avgSeek)
            });
            if (algo.equals(selectedAlgo)) {
                activeResult = res;
            }
        }

        if (activeResult == null) return;

        // Update bottom summary metrics
        totalValLabel.setText(activeResult.totalMovement + " tracks");
        avgValLabel.setText(String.format("%.2f", activeResult.avgSeek));

        // Update step-by-step Sequence breakdown table
        seqModel.setRowCount(0);
        for (int i = 0; i < activeResult.sequence.size(); i++) {
            String stepLabel = (i == 0) ? "Start" : String.valueOf(i);
            seqModel.addRow(new Object[]{
                stepLabel,
                activeResult.sequence.get(i),
                activeResult.steps.get(i)
            });
        }

        // Redraw the seek trajectory graph
        graphPanel.setTrajectory(activeResult.sequence, maxTrack);
    }

    // Custom canvas plotting the disk arm seek path across cylinders (X) and steps (Y)
    private static class SeekGraphPanel extends JPanel {
        private List<Integer> sequence = new ArrayList<>();
        private int maxTrack = 199;

        SeekGraphPanel() {
            setOpaque(false);
        }

        // Updates the seek sequence and cylinder scale, then triggers a repaint
        void setTrajectory(List<Integer> sequence, int maxTrack) {
            this.sequence = new ArrayList<>(sequence);
            this.maxTrack = Math.max(1, maxTrack);
            repaint();
        }

        // Renders cylinder grid lines, orange seek path lines, and cylinder node markers
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            g2.setColor(DARK2);
            g2.fillRoundRect(0, 0, w, h, 16, 16);

            int padL = 42, padR = 24, padT = 28, padB = 18;
            int plotW = Math.max(40, w - padL - padR);
            int plotH = Math.max(40, h - padT - padB);

            // Draw top cylinder axis scale ticks (0 .. maxTrack)
            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            int ticks = 5;
            for (int t = 0; t <= ticks; t++) {
                int trackVal = (int) Math.round((t / (double) ticks) * maxTrack);
                int x = padL + (int) Math.round((trackVal / (double) maxTrack) * plotW);

                g2.setColor(new Color(0x1E3352));
                g2.drawLine(x, padT, x, padT + plotH);

                g2.setColor(new Color(0x9BB0D1));
                String lbl = String.valueOf(trackVal);
                g2.drawString(lbl, x - g2.getFontMetrics().stringWidth(lbl) / 2, padT - 8);
            }

            if (sequence.size() < 2) {
                g2.dispose();
                return;
            }

            int n = sequence.size();
            double stepY = (double) plotH / (n - 1);

            // Draw orange line segments connecting consecutive head positions
            g2.setStroke(new BasicStroke(2.2f));
            for (int i = 0; i < n - 1; i++) {
                int x1 = padL + (int) Math.round((sequence.get(i) / (double) maxTrack) * plotW);
                int y1 = padT + (int) Math.round(i * stepY);
                int x2 = padL + (int) Math.round((sequence.get(i + 1) / (double) maxTrack) * plotW);
                int y2 = padT + (int) Math.round((i + 1) * stepY);

                g2.setColor(new Color(0xFF8C42));
                g2.drawLine(x1, y1, x2, y2);
            }

            // Draw circular dots and cylinder number labels at each visited track
            for (int i = 0; i < n; i++) {
                int val = sequence.get(i);
                int x = padL + (int) Math.round((val / (double) maxTrack) * plotW);
                int y = padT + (int) Math.round(i * stepY);

                g2.setColor(i == 0 ? new Color(0x4ADE80) : ACCENT);
                g2.fillOval(x - 4, y - 4, 8, 8);

                // Skip intermediate labels on very large queues to prevent text overlap
                if (n <= 25 || i == 0 || i == n - 1 || i % (n / 20 + 1) == 0) {
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    g2.setColor(Color.WHITE);
                    g2.drawString(String.valueOf(val), x + 7, y + 4);
                }
            }

            g2.dispose();
        }
    }

    // Creates a rounded button with custom background color and dimensions
    private JButton makeButton(String text, Color bg, int w, int h) {
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
        b.setMargin(new Insets(0, 6, 0, 6));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(w, h));
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