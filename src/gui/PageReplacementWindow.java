package gui;

import LOGICS.PageReplacementLogic;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.Arrays;

public class PageReplacementWindow extends BasePanel {

    private static final Color NAVY = new Color(0x120078);
    private static final Color BLUE = new Color(0x3B6FD4);
    private static final Color ORANGE = new Color(0xE8A010);
    private static final Color RUN    = new Color(0x1F57E0);

    private final java.util.Random rng = new java.util.Random();

    private final JTextField refField = new RoundField("", 24, "e.g. 0 1 2 3 4 5 6");

    private final JTextField lengthField = numberField("10");
    private final JTextField framesField = numberField("3");
    private final JComboBox<String> algo = new JComboBox<>(new String[]{"FIFO", "LRU", "Optimal"});
    private final DefaultTableModel model = new DefaultTableModel();

    private final JTable table = new JTable(model) {
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() == null || getPreferredSize().width < getParent().getWidth();
        }
    };

    private final JLabel lblAlgo = statValue("-"), lblRefs = statValue("-"),
                         lblFaults = statValue("-"), lblHits = statValue("-");

    public PageReplacementWindow() {
        super("Page Replacement");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        top.setOpaque(false);
        top.add(new JLabel("Length:"));
        top.add(lengthField);
        top.add(new JLabel("Reference:"));
        top.add(refField);
        top.add(new JLabel("Frames:"));
        top.add(framesField);
        top.add(new JLabel("Algorithm:"));
        top.add(algo);

        JButton run = new RoundButton("Run", RUN, Color.WHITE, null);
        top.add(run);

        JButton random = new RoundButton("Randomize", ORANGE, Color.WHITE, null);
        top.add(random);

        // gitna: table
        table.setRowHeight(28);
        table.setTableHeader(null);
        table.setGridColor(new Color(0xB8BCC8));
        table.setDefaultRenderer(Object.class, new CellRenderer());
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createTitledBorder("Page Replacement Table"));
        sp.setOpaque(false);
        sp.getViewport().setBackground(CONTENT_BG);

        // baba: stats
        JPanel stats = new Rounded(18, NAVY);
        stats.setLayout(new GridLayout(1, 4, 10, 0));
        stats.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        stats.add(statBox("Algorithm:", lblAlgo));
        stats.add(statBox("References:", lblRefs));
        stats.add(statBox("Total Page Faults:", lblFaults));
        stats.add(statBox("Total Page Hits:", lblHits));

        content.add(top, BorderLayout.NORTH);
        content.add(sp, BorderLayout.CENTER);
        content.add(stats, BorderLayout.SOUTH);

        run.addActionListener(e -> runSimulation());
        random.addActionListener(e -> randomize());

        // Habang nagta-type sa Reference, sumusunod agad ang Length sa bilang ng numero
        refField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { syncLength(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { syncLength(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { syncLength(); }
        });
    }

    // Binibilang ang mga numero sa Reference at inilalagay sa Length
    private void syncLength() {
        String t = refField.getText().trim();
        if (t.isEmpty()) return;   // kung blangko, iwan ang huling Length
        int count = (int) Arrays.stream(t.split("[\\s,]+")).filter(x -> !x.isEmpty()).count();
        lengthField.setText(String.valueOf(count));
    }


    private void randomize() {
        int len = readInt(lengthField, "Length", 10000);
        if (len < 0) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i > 0) sb.append(' ');
            sb.append(rng.nextInt(10));              // pages 0 hanggang 9
        }
        refField.setText(sb.toString());
        runSimulation();
    }

    private void runSimulation() {
        // 1. blangko
        if (refField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Invalid input");
            return;
        }

        // 1b. kung iba ang Length sa bilang ng reference, ayusin ang reference:
        //     mas maliit = puputulin, mas malaki = dadagdagan ng random na numero
        int wanted = readInt(lengthField, "Length", 10000);
        if (wanted < 0) return;
        String[] tok = Arrays.stream(refField.getText().trim().split("[\\s,]+"))
                             .filter(x -> !x.isEmpty()).toArray(String[]::new);
        if (wanted != tok.length) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < wanted; i++) {
                if (i > 0) sb.append(' ');
                sb.append(i < tok.length ? tok[i] : String.valueOf(rng.nextInt(10)));
            }
            refField.setText(sb.toString());
        }

        // 2. basahin ang input (kahit ilang numero)
        int[] refs;
        try {
            refs = Arrays.stream(refField.getText().trim().split("[\\s,]+"))
                         .mapToInt(Integer::parseInt).toArray();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Whole number, add spaces in between");
            return;
        }

        // 3. bawal ang negatibo
        for (int x : refs) {
            if (x < 0) {
                JOptionPane.showMessageDialog(this, "No negative");
                return;
            }
        }

        // 4. frames
        int nFrames = readInt(framesField, "Frames", 100);
        if (nFrames < 0) return;

        lengthField.setText(String.valueOf(refs.length));   // sundan ng Length ang na-type
        String a = (String) algo.getSelectedItem();

        PageReplacementLogic.Result r = PageReplacementLogic.simulate(a, refs, nFrames);

        // row 0 = Ref #, rows 1..n = F1..Fn, huling row = Results
        int cols = refs.length + 1;
        Object[][] data = new Object[nFrames + 2][cols];
        data[0][0] = "Ref #";
        data[nFrames + 1][0] = "Results";
        for (int f = 0; f < nFrames; f++) data[f + 1][0] = "F" + (f + 1);
        for (int i = 0; i < refs.length; i++) {
            data[0][i + 1] = refs[i];
            for (int f = 0; f < nFrames; f++)
                data[f + 1][i + 1] = r.frames[i][f] < 0 ? "" : String.valueOf(r.frames[i][f]);
            data[nFrames + 1][i + 1] = r.fault[i] ? "Fault" : "Hit";
        }
        model.setDataVector(data, new Object[cols]);
        for (int c = 0; c < cols; c++)
            table.getColumnModel().getColumn(c).setPreferredWidth(c == 0 ? 80 : 54);

        lblAlgo.setText(a);
        lblRefs.setText(String.valueOf(refs.length));
        lblFaults.setText(String.valueOf(r.faults));
        lblHits.setText(String.valueOf(refs.length - r.faults));
    }

    // Length at Frames: nasa gitna ang numero
    private static JTextField numberField(String text) {
        RoundField f = new RoundField(text, 3, null);
        f.setHorizontalAlignment(SwingConstants.CENTER);
        f.setFont(new Font("Segoe UI", Font.BOLD, 14));
        f.setForeground(new Color(0x1F3FBF));
        return f;
    }


    private static class RoundButton extends JButton {
        private static final int RADIUS = 14;   // ← border radius ng button
        private static final int HEIGHT = 34;   // ← taas ng button
        private final Color base;
        private final Color outline;            // null = walang outline

        RoundButton(String text, Color bg, Color fg, Color outline) {
            super(text);
            this.base = bg;
            this.outline = outline;
            setForeground(fg);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(6, 20, 6, 20));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.height = HEIGHT;
            return d;
        }

        private static Color darken(Color c, double amount) {
            return new Color((int) (c.getRed() * (1 - amount)),
                             (int) (c.getGreen() * (1 - amount)),
                             (int) (c.getBlue() * (1 - amount)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = base;
            if (getModel().isPressed()) fill = darken(base, 0.18);
            else if (getModel().isRollover()) fill = darken(base, 0.08);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
            if (outline != null) {
                g2.setColor(outline);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, RADIUS, RADIUS);
            }
            g2.dispose();
            super.paintComponent(g);   // ipinipinta ang text
        }
    }

    // Text field na may rounded na background at border (border radius)
    private static class RoundField extends JTextField {
        private static final int RADIUS = 0;   // ← border radius, palitan ang numero
        private static final int HEIGHT = 34;   // ← taas ng field
        private final String hint;

        RoundField(String text, int columns, String hint) {
            super(text, columns);
            this.hint = hint;
            setOpaque(false);                                   // para hindi lumabas ang kanto
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.height = HEIGHT;
            return d;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
            g2.dispose();

            super.paintComponent(g);

            if (hint != null && getText().isEmpty()) {
                Graphics2D h = (Graphics2D) g.create();
                h.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                   RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                h.setColor(Color.GRAY);
                FontMetrics fm = h.getFontMetrics();
                h.drawString(hint, getInsets().left,
                             (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                h.dispose();
            }
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BLUE);
            g2.setStroke(new BasicStroke(1));                // ← kapal ng border
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, RADIUS, RADIUS);
            g2.dispose();
        }
    }

    // Binabasa ang buong numero (1 hanggang max). Nagbabalik ng -1 kung mali.
    private int readInt(JTextField f, String label, int max) {
        try {
            int v = Integer.parseInt(f.getText().trim());
            if (v < 1 || v > max) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    label + " positive number only " + max + ".");
            return -1;
        }
    }

    private static JLabel statValue(String t) {
        JLabel l = new JLabel(t, SwingConstants.CENTER);
        l.setForeground(new Color(0x5BB8FF));
        l.setFont(new Font("Segoe UI", Font.BOLD, 16));
        return l;
    }

    private static JPanel statBox(String title, JLabel value) {
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.setOpaque(false);
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setForeground(Color.WHITE);
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(t);
        p.add(value);
        return p;
    }

    private static class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setHorizontalAlignment(CENTER);
            boolean last = row == t.getRowCount() - 1;
            if (row == 0 || col == 0 || last) {
                setBackground(NAVY);
                setForeground(Color.WHITE);
                setFont(getFont().deriveFont(Font.BOLD));
                if (last && col > 0)
                    setForeground("Fault".equals(v) ? new Color(0xFF8A80) : new Color(0x7CF0A8));
            } else {
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);
                setFont(getFont().deriveFont(Font.PLAIN));
            }
            return this;
        }
    }
}