package gui;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class AppPage extends JPanel {

    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final Map<String, JPanel> pages = new LinkedHashMap<>();
    private final Map<String, MenuItem> items = new LinkedHashMap<>();
    private final Image background;

    public AppPage(Runnable onBackHome) {
        setLayout(new BorderLayout(12, 0));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        URL bgURL = getClass().getResource("/resources/HOME.png");
        background = (bgURL != null) ? new ImageIcon(bgURL).getImage() : null;

        pages.put("CPU Scheduling", new CPUScheduling());
        pages.put("Banker's Algorithm", new BankersWindow());
        pages.put("Paging", placeholder("Paging"));
        pages.put("Page Replacement", placeholder("Page Replacement"));
        pages.put("Segmentation", placeholder("Segmentation"));
        pages.put("Fragmentation", placeholder("Fragmentation"));
        pages.put("Disk Scheduling", placeholder("Disk Scheduling"));
        pages.put("Multiprogramming", placeholder("Multiprogramming"));

        JPanel sidebar = new BasePanel.Rounded(24, BasePanel.SIDEBAR);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(16, 10, 14, 10));

        JLabel logo = new JLabel("<html>Mini OS<br>Simulator</html>",
                BasePanel.icon("/resources/mini.png", 48, 48), SwingConstants.LEFT);
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        logo.setHorizontalTextPosition(SwingConstants.RIGHT);
        logo.setVerticalTextPosition(SwingConstants.CENTER);
        logo.setIconTextGap(8);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(18));

        for (Map.Entry<String, JPanel> e : pages.entrySet()) {
            center.add(e.getValue(), e.getKey());
            MenuItem item = makeItem(e.getKey());
            items.put(e.getKey(), item);
            sidebar.add(item);
            sidebar.add(Box.createVerticalStrut(4));
        }

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(makeBackButton(onBackHome));

        center.setOpaque(false);
        add(sidebar, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);

        show("CPU Scheduling");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(new Color(0x6A9BD1));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private static JPanel placeholder(String name) {
        return new BasePanel(name) {{
            JLabel l = new JLabel(name + " - coming soon", SwingConstants.CENTER);
            l.setFont(new Font("Segoe UI", Font.BOLD, 20));
            l.setForeground(BasePanel.HEADER);
            content.add(l, BorderLayout.CENTER);
        }};
    }

    private MenuItem makeItem(String name) {
        MenuItem b = new MenuItem(name, BasePanel.icon("/resources/folder.png", 30, 26));
        b.addActionListener(e -> show(name));
        return b;
    }

    private JButton makeBackButton(Runnable onBackHome) {
        JButton back = new JButton("BACK TO HOME", BasePanel.icon("/resources/house.png", 30, 22)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(0xE6E6F2) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        back.setForeground(Color.BLACK);
        back.setFont(new Font("Segoe UI", Font.BOLD, 12));
        back.setHorizontalTextPosition(SwingConstants.RIGHT);
        back.setVerticalTextPosition(SwingConstants.CENTER);
        back.setHorizontalAlignment(SwingConstants.CENTER);
        back.setIconTextGap(10);
        back.setContentAreaFilled(false);
        back.setOpaque(false);
        back.setBorderPainted(false);
        back.setFocusPainted(false);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.setAlignmentX(Component.LEFT_ALIGNMENT);
        back.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        back.setPreferredSize(new Dimension(150, 40));
        back.addActionListener(e -> onBackHome.run());
        return back;
    }

    public void show(String name) {
        cards.show(center, name);
        for (Map.Entry<String, MenuItem> e : items.entrySet())
            e.getValue().setActive(e.getKey().equals(name));
    }

    private static class MenuItem extends JButton {
        private boolean active;

        MenuItem(String text, Icon icon) {
            super(text, icon);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setHorizontalTextPosition(SwingConstants.RIGHT);
            setVerticalTextPosition(SwingConstants.CENTER);
            setHorizontalAlignment(SwingConstants.LEFT);
            setIconTextGap(10);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMargin(new Insets(2, 8, 2, 8));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setPreferredSize(new Dimension(150, 42));
        }

        void setActive(boolean a) {
            active = a;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (active || getModel().isRollover()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, active ? 45 : 25));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}