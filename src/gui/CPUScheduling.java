package gui;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class CPUScheduling extends BasePanel {

    private static final Color NAVY   = new Color(0x120078);
    private static final Color BLUE   = new Color(0x3B6FD4);
    private static final Color DARK   = new Color(0x0F1B2D);
    private static final Color DARK2  = new Color(0x13243D);
    private static final Color GREEN  = new Color(0x2E9E4F);
    private static final Color RED    = new Color(0xE74C3C);
    private static final Color ORANGE = new Color(0xE8A010);
    private static final Color RUN    = new Color(0x1F57E0);
    private static final Color ACCENT = new Color(0x3EA0FF);

    private static final String[] ALGOS = {
            "FCFS",
            "SJF (Non-Preemptive)",
            "SRTF (Preemptive)",
            "Priority (Non-Preemptive)",
            "Priority (Preemptive)",
            "Round Robin"
    };

    private final JComboBox<String> algoBox = new JComboBox<>(ALGOS);
    private final JLabel quantumLbl = new JLabel("Quantum:");
    private final JTextField quantumField = new JTextField("2", 3);

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Job", "AT", "BT", "Priority"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return c != 0; }
    };
    private final JTable table = new JTable(model);

    private final GanttPanel gantt = new GanttPanel();
    private final JLabel awtLabel = bigNumber();
    private final JLabel atatLabel = bigNumber();
    private final JTextArea log = new JTextArea();

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

    // ================= LEFT =================
    private JPanel buildLeft() {
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);

        // algorithm row (outside the card, like the mockup)
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

        // card
        RoundedPanel card = new RoundedPanel(24, CARD);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        styleTable();
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(CARD);

        JButton add = makeButton("+ Add", GREEN);
        JButton remove = makeButton("- Remove", RED);
        JButton clear = makeButton("x Clear All", ORANGE);
        add.addActionListener(e -> addRow());
        remove.addActionListener(e -> removeRow());
        clear.addActionListener(e -> clearAll());

        JPanel row = new JPanel(new GridLayout(1, 3, 8, 0));
        row.setOpaque(false);
        row.add(add);
        row.add(remove);
        row.add(clear);

        JButton run = makeButton("\u25B6  Run Simulator", RUN);
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

    // ================= RIGHT =================
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

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(gantt, BorderLayout.CENTER);
        center.add(south, BorderLayout.SOUTH);

        panel.add(title, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

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

    private static JLabel bigNumber() {
        JLabel l = new JLabel("0.00", SwingConstants.CENTER);
        l.setForeground(ACCENT);
        l.setFont(new Font("Segoe UI", Font.BOLD, 34));
        return l;
    }

    // ================= TABLE =================
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

    private void resetTable() {
        model.setRowCount(0);
        for (int i = 0; i < 5; i++) model.addRow(new Object[]{"P" + (i + 1), "", "", ""});
    }

    private void renumber() {
        for (int i = 0; i < model.getRowCount(); i++) model.setValueAt("P" + (i + 1), i, 0);
    }

    private void stopEditing() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
    }

    private void addRow() {
        stopEditing();
        if (model.getRowCount() >= 15) {
            JOptionPane.showMessageDialog(this, "Maximum of 15 jobs.");
            return;
        }
        model.addRow(new Object[]{"P" + (model.getRowCount() + 1), "", "", ""});
    }

    private void removeRow() {
        stopEditing();
        if (model.getRowCount() == 0) return;
        int r = table.getSelectedRow();
        if (r < 0) r = model.getRowCount() - 1;
        model.removeRow(r);
        renumber();
    }

    private void clearAll() {
        stopEditing();
        resetTable();
        gantt.setData(new ArrayList<>(), new ArrayList<>(), 0);
        awtLabel.setText("0.00");
        atatLabel.setText("0.00");
        log.setText("Cleared.");
    }

    private void updateQuantumVisibility() {
        boolean rr = "Round Robin".equals(algoBox.getSelectedItem());
        quantumLbl.setVisible(rr);
        quantumField.setVisible(rr);
    }

    // ================= SIMULATION =================
    private static class Proc {
        String name;
        int at, bt, pr, rem, ct;
    }

    private void runSimulator() {
        stopEditing();
        List<Proc> ps = readProcs();
        if (ps == null) return;
        if (ps.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one job (AT and BT).");
            return;
        }

        String algo = (String) algoBox.getSelectedItem();
        List<int[]> segs = new ArrayList<>();   // {procIndex, start, end}

        if ("Round Robin".equals(algo)) {
            int q;
            try {
                q = Integer.parseInt(quantumField.getText().trim());
                if (q < 1) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Quantum must be a whole number of 1 or more.");
                return;
            }
            simulateRR(ps, q, segs);
        } else {
            simulate(ps, algo, segs);
        }

        // metrics
        double totalWt = 0, totalTat = 0;
        int maxTime = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("Algorithm : ").append(algo);
        if ("Round Robin".equals(algo)) sb.append(" (q=").append(quantumField.getText().trim()).append(")");
        sb.append("\n\nGantt Chart:\n  ");
        for (int k = 0; k < segs.size(); k++) {
            int[] s = segs.get(k);
            sb.append(ps.get(s[0]).name).append(" [").append(s[1]).append("-").append(s[2]).append("]");
            if (k < segs.size() - 1) sb.append(" => ");
            maxTime = Math.max(maxTime, s[2]);
        }
        sb.append("\n\n").append(String.format("%-5s %4s %4s %4s %5s %4s%n", "Job", "AT", "BT", "CT", "TAT", "WT"));
        List<String> names = new ArrayList<>();
        for (Proc p : ps) {
            int tat = p.ct - p.at;
            int wt = tat - p.bt;
            totalTat += tat;
            totalWt += wt;
            names.add(p.name);
            sb.append(String.format("%-5s %4d %4d %4d %5d %4d%n", p.name, p.at, p.bt, p.ct, tat, wt));
        }

        awtLabel.setText(String.format("%.2f", totalWt / ps.size()));
        atatLabel.setText(String.format("%.2f", totalTat / ps.size()));
        log.setText(sb.toString());
        log.setCaretPosition(0);
        gantt.setData(names, segs, maxTime);
    }

    /** Reads valid rows. Returns null if there is an input error. */
    private List<Proc> readProcs() {
        List<Proc> list = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            String name = String.valueOf(model.getValueAt(i, 0));
            String at = cell(i, 1), bt = cell(i, 2), pr = cell(i, 3);
            if (at.isEmpty() && bt.isEmpty() && pr.isEmpty()) continue;   // blank row
            try {
                Proc p = new Proc();
                p.name = name;
                p.at = at.isEmpty() ? 0 : Integer.parseInt(at);
                p.bt = Integer.parseInt(bt);
                p.pr = pr.isEmpty() ? 0 : Integer.parseInt(pr);
                if (p.at < 0 || p.bt < 1 || p.pr < 0 || p.bt > 1000 || p.at > 1000)
                    throw new NumberFormatException();
                p.rem = p.bt;
                list.add(p);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Invalid input in " + name + ".\nAT/Priority: 0 or higher. BT: 1 or higher (whole numbers).");
                return null;
            }
        }
        return list;
    }

    private String cell(int r, int c) {
        Object o = model.getValueAt(r, c);
        return o == null ? "" : o.toString().trim();
    }

    /** True if a is a better pick than b for the given algorithm. */
    private boolean better(Proc a, Proc b, String algo) {
        int ka, kb;
        if (algo.startsWith("SJF")) { ka = a.bt; kb = b.bt; }
        else if (algo.startsWith("SRTF")) { ka = a.rem; kb = b.rem; }
        else if (algo.startsWith("Priority")) { ka = a.pr; kb = b.pr; }
        else { ka = a.at; kb = b.at; }                     // FCFS
        if (ka != kb) return ka < kb;
        return a.at < b.at;
    }

    /** FCFS, SJF, SRTF, Priority (both kinds): unit-time simulation. */
    private void simulate(List<Proc> ps, String algo, List<int[]> segs) {
        boolean preemptive = algo.startsWith("SRTF") || algo.equals("Priority (Preemptive)");
        int n = ps.size(), done = 0, t = 0, cur = -1;

        while (done < n) {
            if (cur == -1 || preemptive) {
                int best = -1;
                for (int i = 0; i < n; i++) {
                    Proc p = ps.get(i);
                    if (p.rem > 0 && p.at <= t && (best == -1 || better(p, ps.get(best), algo))) best = i;
                }
                cur = best;
            }
            if (cur == -1) { t++; continue; }              // CPU idle

            Proc p = ps.get(cur);
            addSeg(segs, cur, t, t + 1);
            p.rem--;
            t++;
            if (p.rem == 0) { p.ct = t; done++; cur = -1; }
        }
    }

    private void simulateRR(List<Proc> ps, int quantum, List<int[]> segs) {
        int n = ps.size(), done = 0, t = 0, next = 0;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (x, y) -> Integer.compare(ps.get(x).at, ps.get(y).at));

        Deque<Integer> q = new ArrayDeque<>();
        while (done < n) {
            while (next < n && ps.get(order[next]).at <= t) q.add(order[next++]);
            if (q.isEmpty()) { t = ps.get(order[next]).at; continue; }   // idle jump

            int i = q.poll();
            Proc p = ps.get(i);
            int run = Math.min(quantum, p.rem);
            addSeg(segs, i, t, t + run);
            t += run;
            p.rem -= run;

            while (next < n && ps.get(order[next]).at <= t) q.add(order[next++]);
            if (p.rem > 0) q.add(i);
            else { p.ct = t; done++; }
        }
    }

    /** Adds a segment, merging with the previous one if it continues directly. */
    private void addSeg(List<int[]> segs, int idx, int start, int end) {
        if (!segs.isEmpty()) {
            int[] last = segs.get(segs.size() - 1);
            if (last[0] == idx && last[2] == start) { last[2] = end; return; }
        }
        segs.add(new int[]{idx, start, end});
    }

    // ================= GANTT CHART =================
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
            setPreferredSize(new Dimension(300, 220));
            setData(new ArrayList<>(), new ArrayList<>(), 0);
        }

        void setData(List<String> n, List<int[]> s, int max) {
            names = new ArrayList<>(n);
            for (int i = names.size(); i < 5; i++) names.add("P" + (i + 1));   // pad to 5 rows
            segs = s;
            maxTime = max;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            g2.setColor(DARK2);
            g2.fillRoundRect(0, 0, w, h, 16, 16);

            int left = 40, right = 16, top = 12, bottom = 34;
            int pw = w - left - right, ph = h - top - bottom;
            if (pw < 20 || ph < 20) { g2.dispose(); return; }

            int rows = names.size();
            int rowH = ph / rows;

            int step, axisMax;
            if (maxTime == 0) { step = 5; axisMax = 20; }
            else {
                step = Math.max(1, (int) Math.ceil(maxTime / 10.0));
                axisMax = ((maxTime + step - 1) / step) * step;
            }

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            FontMetrics fm = g2.getFontMetrics();

            // vertical grid + time labels
            for (int tt = 0; tt <= axisMax; tt += step) {
                int x = left + (int) ((long) pw * tt / axisMax);
                g2.setColor(new Color(0x24385A));
                g2.drawLine(x, top, x, top + ph);
                g2.setColor(new Color(0xB8C7E0));
                String s = String.valueOf(tt);
                g2.drawString(s, x - fm.stringWidth(s) / 2, top + ph + 14);
            }
            g2.drawString("Time", left + pw / 2 - fm.stringWidth("Time") / 2, h - 6);

            // row labels + horizontal lines
            for (int r = 0; r < rows; r++) {
                int y = top + r * rowH;
                g2.setColor(new Color(0x1C2E4D));
                g2.drawLine(left, y + rowH, left + pw, y + rowH);
                g2.setColor(new Color(0xB8C7E0));
                g2.drawString(names.get(r), 8, y + rowH / 2 + fm.getAscent() / 2 - 2);
            }

            // bars
            for (int[] s : segs) {
                int r = s[0];
                int x1 = left + (int) ((long) pw * s[1] / axisMax);
                int x2 = left + (int) ((long) pw * s[2] / axisMax);
                int y = top + r * rowH + 4;
                int bh = Math.max(8, rowH - 8);
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

    // ================= HELPERS =================
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
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(100, 34));
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