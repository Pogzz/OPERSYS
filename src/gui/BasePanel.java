package gui;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public abstract class BasePanel extends JPanel {

    public static final Color HEADER = new Color(0x5B6785);
    public static final Color SIDEBAR = new Color(0x120078);
    public static final Color CARD = new Color(0xE9EBEF);
    public static final Color CONTENT_BG = new Color(0xDCDFE5);

    protected final JPanel content = new Rounded(22, CONTENT_BG);

    protected BasePanel(String title) {

        setLayout(new BorderLayout(0, 10));
        setOpaque(false);

        JLabel lbl = new JLabel(title, icon("/resources/folder.png", 46, 40), SwingConstants.LEFT);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lbl.setForeground(Color.WHITE);
        lbl.setHorizontalTextPosition(SwingConstants.RIGHT);
        lbl.setVerticalTextPosition(SwingConstants.CENTER);
        lbl.setIconTextGap(14);

        JPanel header = new Rounded(20, HEADER);
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        header.add(lbl, BorderLayout.WEST);

        content.setLayout(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }
   public static ImageIcon icon(String path, int w, int h) {
        URL url = BasePanel.class.getResource(path);
        if (url == null) {
            System.err.println("Missing resource: " + path);
            return null;
        }
        Image img = new ImageIcon(url).getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    public static class Rounded extends JPanel {
        private final int radius;

        public Rounded(int radius, Color bg) {
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