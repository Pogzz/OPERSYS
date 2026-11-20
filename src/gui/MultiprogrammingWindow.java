package gui;

import LOGICS.MultiprogrammingLogic;
import LOGICS.MultiprogrammingLogic.EventRow;
import LOGICS.MultiprogrammingLogic.SimulationResult;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MultiprogrammingWindow extends BasePanel {

    // Color palette used across cards, buttons, and event tables
    private static final Color NAVY   = new Color(0x120078);
    private static final Color BLUE   = new Color(0x3B6FD4);
    private static final Color DARK   = new Color(0x0F1B2D);
    private static final Color DARK2  = new Color(0x13243D);
    private static final Color GREEN  = new Color(0x2E9E4F);
    private static final Color RED    = new Color(0xE74C3C);
    private static final Color ORANGE = new Color(0xE8A010);
    private static final Color RUN    = new Color(0x1F57E0);
    private static final Color ACCENT = new Color(0x3EA0FF);

    // Job queue table model (Jobs ID is read-only; Arrival and Burst are editable)
    private final DefaultTableModel jobModel = new DefaultTableModel(
            new String[]{"Jobs", "Arrival Time", "Burst Time"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return c == 1 || c == 2;
        }
    };
    private final JTable jobTable = new JTable(jobModel);

    // Timeline text area logging arrival and termination timestamps
    private final JTextArea timelineArea = new JTextArea();

    // Event-driven processor-sharing simulation table
    private final DefaultTableModel simModel = new DefaultTableModel(
            new String[]{"Time", "Event", "Jobs", "%CPU/Job", "Elapsed", "Headway/Job", "Jobs", "Left"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable simTable = new JTable(simModel);

    // Summary metrics table (total jobs, finish time, avg turnaround, avg waiting)
    private final DefaultTableModel sumModel = new DefaultTableModel(
            new String[]{"Measure", "Value"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable sumTable = new JTable(sumModel);

    // Initializes the Multiprogramming window and runs the initial sample workload
    public MultiprogrammingWindow() {
        super("Multiprogramming");

        content.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;

        // Left column: Job Queue, action buttons, Run button, and Timeline box
        gc.gridx = 0; gc.weightx = 0.34;
        gc.insets = new Insets(0, 0, 0, 14);
        content.add(buildLeftPanel(), gc);

        // Right column: Simulation event log table and Summary metrics table
        gc.gridx = 1; gc.weightx = 0.66;
        gc.insets = new Insets(0, 0, 0, 0);
        content.add(buildRightPanel(), gc);

        loadReferenceSample();
        runSimulation();
    }

    // Builds the left input column containing Job Queue, control buttons, and Timeline
    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(false);

        // Top section: Job Queue label, table, 2x2 buttons, and full-width Run button
        JPanel queueSection = new JPanel(new BorderLayout(0, 8));
        queueSection.setOpaque(false);

        JLabel jqTitle = new JLabel("Job Queue:");
        jqTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        jqTitle.setForeground(NAVY);

        styleLightTable(jobTable);
        JScrollPane jqScroll = new JScrollPane(jobTable);
        jqScroll.setBorder(BorderFactory.createLineBorder(BLUE, 1));
        jqScroll.getViewport().setBackground(Color.WHITE);
        jqScroll.setPreferredSize(new Dimension(240, 185));

        JPanel controlsWrap = new JPanel(new BorderLayout(0, 8));
        controlsWrap.setOpaque(false);

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 10, 8));
        btnGrid.setOpaque(false);

        JButton addBtn   = makeButton("+ Add", GREEN);
        JButton remBtn   = makeButton("- Remove", RED);
        JButton randBtn  = makeButton("Randomize", ACCENT);
        JButton clearBtn = makeButton("x Clear All", ORANGE);

        addBtn.addActionListener(e -> addJobRow());
        remBtn.addActionListener(e -> removeJobRow());
        randBtn.addActionListener(e -> randomizeJobs());
        clearBtn.addActionListener(e -> clearAllJobs());

        btnGrid.add(addBtn);
        btnGrid.add(remBtn);
        btnGrid.add(randBtn);
        btnGrid.add(clearBtn);

        JButton runBtn = makeButton("  Run", RUN);
        runBtn.setPreferredSize(new Dimension(120, 32));
        runBtn.addActionListener(e -> runSimulation());

        controlsWrap.add(btnGrid, BorderLayout.NORTH);
        controlsWrap.add(runBtn, BorderLayout.SOUTH);

        queueSection.add(jqTitle, BorderLayout.NORTH);
        queueSection.add(jqScroll, BorderLayout.CENTER);
        queueSection.add(controlsWrap, BorderLayout.SOUTH);

        // Bottom section: Timeline event summary box
        JPanel timelineSection = new JPanel(new BorderLayout(0, 6));
        timelineSection.setOpaque(false);
        timelineSection.setPreferredSize(new Dimension(240, 145));

        JLabel tlTitle = new JLabel("Timeline:");
        tlTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tlTitle.setForeground(NAVY);

        timelineArea.setEditable(false);
        timelineArea.setBackground(DARK2);
        timelineArea.setForeground(new Color(0xCFE3FF));
        timelineArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        timelineArea.setMargin(new Insets(8, 10, 8, 10));

        JScrollPane tlScroll = new JScrollPane(timelineArea);
        tlScroll.setBorder(BorderFactory.createLineBorder(BLUE, 1));

        timelineSection.add(tlTitle, BorderLayout.NORTH);
        timelineSection.add(tlScroll, BorderLayout.CENTER);

        left.add(queueSection, BorderLayout.CENTER);
        left.add(timelineSection, BorderLayout.SOUTH);
        return left;
    }

    // Builds the right dark cards for Simulation events and Summary measures
    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setOpaque(false);

        // Top card: Simulation table
        RoundedPanel simCard = new RoundedPanel(22, DARK);
        simCard.setLayout(new BorderLayout(0, 8));
        simCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel simTitle = new JLabel("Simulation:");
        simTitle.setForeground(ACCENT);
        simTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));

        styleDarkTable(simTable, true);
        JScrollPane simScroll = new JScrollPane(simTable);
        simScroll.setBorder(BorderFactory.createLineBorder(new Color(0x2A3B55)));
        simScroll.getViewport().setBackground(DARK2);

        simCard.add(simTitle, BorderLayout.NORTH);
        simCard.add(simScroll, BorderLayout.CENTER);

        // Bottom card: Summary table
        RoundedPanel sumCard = new RoundedPanel(22, DARK);
        sumCard.setLayout(new BorderLayout(0, 8));
        sumCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        sumCard.setPreferredSize(new Dimension(100, 185));

        JLabel sumTitle = new JLabel("Summary:");
        sumTitle.setForeground(ACCENT);
        sumTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));

        styleDarkTable(sumTable, false);
        JScrollPane sumScroll = new JScrollPane(sumTable);
        sumScroll.setBorder(BorderFactory.createLineBorder(new Color(0x2A3B55)));
        sumScroll.getViewport().setBackground(DARK2);

        sumCard.add(sumTitle, BorderLayout.NORTH);
        sumCard.add(sumScroll, BorderLayout.CENTER);

        right.add(simCard, BorderLayout.CENTER);
        right.add(sumCard, BorderLayout.SOUTH);
        return right;
    }

    // Applies light theme styling to the left Job Queue table
    private void styleLightTable(JTable t) {
        t.setRowHeight(26);
        t.setGridColor(BLUE);
        t.setShowGrid(true);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setFillsViewportHeight(true);
        t.setBackground(Color.WHITE);
        t.setSelectionBackground(new Color(0xCFE0FF));
        t.setSelectionForeground(Color.BLACK);

        DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object v,
                    boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(table, v, sel, false, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createLineBorder(BLUE, 1));
                if (!sel && c == 0) {
                    setBackground(new Color(0xEEF3FC));
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                } else if (!sel) {
                    setBackground(Color.WHITE);
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
                setBackground(NAVY);
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
                return this;
            }
        };
        t.getTableHeader().setDefaultRenderer(hr);
        t.getTableHeader().setPreferredSize(new Dimension(60, 28));
        t.getTableHeader().setReorderingAllowed(false);
    }

    // Applies dark theme styling and column widths to the Simulation and Summary tables
    private void styleDarkTable(JTable t, boolean isEventTable) {
        t.setRowHeight(26);
        t.setGridColor(new Color(0x24385A));
        t.setShowGrid(true);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        t.setFillsViewportHeight(true);
        t.setBackground(DARK2);
        t.setForeground(Color.WHITE);
        t.setSelectionBackground(new Color(0x1E3557));
        t.setSelectionForeground(Color.WHITE);

        if (isEventTable) {
            t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            t.getColumnModel().getColumn(0).setPreferredWidth(58);
            t.getColumnModel().getColumn(1).setPreferredWidth(135);
            t.getColumnModel().getColumn(2).setPreferredWidth(46);
            t.getColumnModel().getColumn(3).setPreferredWidth(76);
            t.getColumnModel().getColumn(4).setPreferredWidth(64);
            t.getColumnModel().getColumn(5).setPreferredWidth(88);
            t.getColumnModel().getColumn(6).setPreferredWidth(140);
            t.getColumnModel().getColumn(7).setPreferredWidth(180);
        }

        DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object v,
                    boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(table, v, sel, false, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createLineBorder(new Color(0x24385A), 1));
                String text = String.valueOf(v);
                if (isEventTable && c == 1) {
                    setForeground(text.contains("Terminated") ? new Color(0xFF8A98) : new Color(0x7DD3FC));
                } else {
                    setForeground(Color.WHITE);
                }
                setToolTipText(text);
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
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createLineBorder(new Color(0x2A3B55), 1));
                return this;
            }
        };
        t.getTableHeader().setDefaultRenderer(hr);
        t.getTableHeader().setPreferredSize(new Dimension(60, 26));
        t.getTableHeader().setReorderingAllowed(false);
    }

    // Expands Jobs and Left columns dynamically so long active lists are never truncated
    private void adjustEventColumnWidths() {
        FontMetrics fm = simTable.getFontMetrics(simTable.getFont());
        int maxJobsW = 140;
        int maxLeftW = 180;

        for (int r = 0; r < simModel.getRowCount(); r++) {
            String jobsStr = String.valueOf(simModel.getValueAt(r, 6));
            String leftStr = String.valueOf(simModel.getValueAt(r, 7));
            maxJobsW = Math.max(maxJobsW, fm.stringWidth(jobsStr) + 24);
            maxLeftW = Math.max(maxLeftW, fm.stringWidth(leftStr) + 24);
        }

        simTable.getColumnModel().getColumn(6).setPreferredWidth(maxJobsW);
        simTable.getColumnModel().getColumn(7).setPreferredWidth(maxLeftW);

        // Reset horizontal scroll back to the leftmost column (Time)
        if (simModel.getRowCount() > 0) {
            simTable.scrollRectToVisible(simTable.getCellRect(0, 0, true));
        }
    }

    // Populates the Job Queue with 4 default sample jobs (J1 to J4)
    private void loadReferenceSample() {
        jobModel.setRowCount(0);
        jobModel.addRow(new Object[]{"J1", "12.000", "5.000"});
        jobModel.addRow(new Object[]{"J2", "8.000", "8.000"});
        jobModel.addRow(new Object[]{"J3", "13.000", "14.000"});
        jobModel.addRow(new Object[]{"J4", "4.000", "6.855"});
    }

    // Appends a new job row to the Job Queue
    private void addJobRow() {
        stopEditing();
        int next = jobModel.getRowCount() + 1;
        jobModel.addRow(new Object[]{"J" + next, "0.000", "4.000"});
    }

    // Removes the selected job row (or the last row) and renumbers remaining jobs
    private void removeJobRow() {
        stopEditing();
        int count = jobModel.getRowCount();
        if (count == 0) return;
        int row = jobTable.getSelectedRow();
        if (row < 0) row = count - 1;
        jobModel.removeRow(row);
        for (int i = 0; i < jobModel.getRowCount(); i++) {
            jobModel.setValueAt("J" + (i + 1), i, 0);
        }
    }

    // Fills all current job rows with random arrival and burst times and runs simulation
    private void randomizeJobs() {
        stopEditing();
        int rows = jobModel.getRowCount();
        if (rows == 0) {
            rows = 4;
            for (int i = 0; i < rows; i++) {
                jobModel.addRow(new Object[]{"J" + (i + 1), "0", "1"});
            }
        }
        Random rand = new Random();
        for (int i = 0; i < rows; i++) {
            double arr = rand.nextInt(15);
            double bst = 2 + rand.nextInt(13);
            jobModel.setValueAt(String.format("%.3f", arr), i, 1);
            jobModel.setValueAt(String.format("%.3f", bst), i, 2);
        }
        runSimulation();
    }

    // Clears all job values, simulation rows, summary metrics, and timeline text
    private void clearAllJobs() {
        stopEditing();
        jobModel.setRowCount(0);
        for (int i = 0; i < 4; i++) {
            jobModel.addRow(new Object[]{"J" + (i + 1), "", ""});
        }
        simModel.setRowCount(0);
        sumModel.setRowCount(0);
        timelineArea.setText("");
    }

    // Commits any active cell edit in the Job Queue table
    private void stopEditing() {
        if (jobTable.isEditing()) jobTable.getCellEditor().stopCellEditing();
    }

    // Validates job inputs and updates Simulation, Summary, and Timeline panels
    private void runSimulation() {
        stopEditing();

        List<String> ids = new ArrayList<>();
        List<Double> arrivals = new ArrayList<>();
        List<Double> bursts = new ArrayList<>();

        for (int i = 0; i < jobModel.getRowCount(); i++) {
            String id = String.valueOf(jobModel.getValueAt(i, 0)).trim();
            String arrStr = String.valueOf(jobModel.getValueAt(i, 1)).trim();
            String bstStr = String.valueOf(jobModel.getValueAt(i, 2)).trim();

            if (arrStr.isEmpty() && bstStr.isEmpty()) continue;

            try {
                double arr = Double.parseDouble(arrStr);
                double bst = Double.parseDouble(bstStr);
                if (arr < 0 || bst <= 0) {
                    JOptionPane.showMessageDialog(this,
                            "Invalid values on " + id + ": Arrival must be >= 0 and Burst > 0.");
                    return;
                }
                ids.add(id);
                arrivals.add(arr);
                bursts.add(bst);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter valid numeric values on row " + (i + 1) + ".");
                return;
            }
        }

        if (ids.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter at least one job in the Job Queue.");
            return;
        }

        SimulationResult res = MultiprogrammingLogic.simulateProcessorSharing(ids, arrivals, bursts);

        // Populate Simulation event table
        simModel.setRowCount(0);
        for (EventRow r : res.events) {
            simModel.addRow(new Object[]{
                String.format("%.3f", r.time),
                r.event,
                r.jobs,
                r.cpuPerJob,
                r.elapsed,
                r.headwayPerJob,
                r.activeJobs,
                r.remainingWork
            });
        }

        // Populate Summary metrics table
        sumModel.setRowCount(0);
        sumModel.addRow(new Object[]{"Jobs", res.totalJobs});
        sumModel.addRow(new Object[]{"Finish time", String.format("%.3f", res.finishTime)});
        sumModel.addRow(new Object[]{"Avg turnaround", String.format("%.3f", res.avgTurnaround)});
        sumModel.addRow(new Object[]{"Avg waiting", String.format("%.3f", res.avgWaiting)});

        // Populate Timeline box
        timelineArea.setText(String.join("\n", res.timelineLogs));
        timelineArea.setCaretPosition(0);

        adjustEventColumnWidths();
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
        b.setMargin(new Insets(0, 6, 0, 6));
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