
package minios;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class MainBg extends JFrame {

    private JLayeredPane mainPanel;
    private final CardLayout screens = new CardLayout();   // BAGO
    private final JPanel root = new JPanel(screens);       // BAGO
    private gui.AppPage app;                               // BAGO

    public MainBg() {

        setTitle("Mini OS Simulator");
        setSize(1250, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel = new JLayeredPane();
        mainPanel.setPreferredSize(new Dimension(1100, 700));
        mainPanel.setLayout(null);

        URL backgroundURL = getClass().getResource("/resources/HOME.png");
        ImageIcon originalBackground = new ImageIcon(backgroundURL);
        Image backgroundImage = originalBackground.getImage()
                .getScaledInstance(1250, 800
                		, Image.SCALE_SMOOTH);
        JLabel background = new JLabel(new ImageIcon(backgroundImage));
        background.setBounds(0, 0, 1250, 800);
        mainPanel.add(background, JLayeredPane.DEFAULT_LAYER);

        
        URL folderURL = getClass().getResource("/resources/folder.png");
        ImageIcon originalFolder = new ImageIcon(folderURL);
        Image folderImage = originalFolder.getImage()
                .getScaledInstance(80, 80, Image.SCALE_SMOOTH);
        ImageIcon folderIcon = new ImageIcon(folderImage);
        
        
        //folders
        JButton cpuButton = new JButton("<html><center> CPU<br>Scheduling</center></html>");
        cpuButton.setFont(new Font("Tahoma", Font.BOLD, 14));

        cpuButton.setIcon(folderIcon);
        cpuButton.setHorizontalTextPosition(SwingConstants.CENTER);
        cpuButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        cpuButton.setBounds(46, 66, 140, 130);

        cpuButton.setContentAreaFilled(false);
        cpuButton.setBorderPainted(false);
        cpuButton.setFocusPainted(false);
        cpuButton.setForeground(new Color(255, 255, 255));

        mainPanel.add(cpuButton, JLayeredPane.PALETTE_LAYER);

        JButton bankersButton = new JButton("<html><center> Banker's<br>Algorithm</center></html>");
        bankersButton.setFont(new Font("Tahoma", Font.BOLD, 14));

        bankersButton.setIcon(folderIcon);
        bankersButton.setHorizontalTextPosition(SwingConstants.CENTER);
        bankersButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        bankersButton.setBounds(46, 234, 140, 130);

        bankersButton.setContentAreaFilled(false);
        bankersButton.setBorderPainted(false);
        bankersButton.setFocusPainted(false);
        bankersButton.setForeground(new Color(255, 255, 255));

        mainPanel.add(bankersButton, JLayeredPane.PALETTE_LAYER);

        JButton pagingButton = new JButton("Paging");
        pagingButton.setFont(new Font("Tahoma", Font.BOLD, 14));

        pagingButton.setIcon(folderIcon);
        pagingButton.setHorizontalTextPosition(SwingConstants.CENTER);
        pagingButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        pagingButton.setBounds(46, 398, 140, 130);

        pagingButton.setContentAreaFilled(false);
        pagingButton.setBorderPainted(false);
        pagingButton.setFocusPainted(false);
        pagingButton.setForeground(new Color(255, 255, 255));

        mainPanel.add(pagingButton, JLayeredPane.PALETTE_LAYER);

        JButton prButton = new JButton("<html><center> Page<br>Replacement</center></html>");
        prButton.setFont(new Font("Tahoma", Font.BOLD, 14));

        prButton.setIcon(folderIcon);
        prButton.setHorizontalTextPosition(SwingConstants.CENTER);
        prButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        prButton.setBounds(46, 568, 140, 130);

        prButton.setContentAreaFilled(false);
        prButton.setBorderPainted(false);
        prButton.setFocusPainted(false);
        prButton.setForeground(new Color(255, 255, 255));

        mainPanel.add(prButton, JLayeredPane.PALETTE_LAYER);
        
        JButton segButton = new JButton("Segmentation");
        segButton.setFont(new Font("Tahoma", Font.BOLD, 14));

        segButton.setIcon(folderIcon);
        segButton.setHorizontalTextPosition(SwingConstants.CENTER);
        segButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        segButton.setBounds(207, 57, 140, 130);

        segButton.setContentAreaFilled(false);
        segButton.setBorderPainted(false);
        segButton.setFocusPainted(false);
        segButton.setForeground(new Color(255, 255, 255));

        mainPanel.add(segButton, JLayeredPane.PALETTE_LAYER);
        
        JButton fragButton = new JButton("Fragmentation");
        fragButton.setFont(new Font("Tahoma", Font.BOLD, 14));

        fragButton.setIcon(folderIcon);
        fragButton.setHorizontalTextPosition(SwingConstants.CENTER);
        fragButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        fragButton.setBounds(207, 225, 140, 130);

        fragButton.setContentAreaFilled(false);
        fragButton.setBorderPainted(false);
        fragButton.setFocusPainted(false);
        fragButton.setForeground(new Color(255, 255, 255));

        mainPanel.add(fragButton, JLayeredPane.PALETTE_LAYER);
        
        JButton dsButton = new JButton("<html><center> Disk<br>Scheduling</center></html>");
        dsButton.setFont(new Font("Tahoma", Font.BOLD, 14));

        dsButton.setIcon(folderIcon);
        dsButton.setHorizontalTextPosition(SwingConstants.CENTER);
        dsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        dsButton.setBounds(207, 407, 140, 130);

        dsButton.setContentAreaFilled(false);
        dsButton.setBorderPainted(false);
        dsButton.setFocusPainted(false);
        dsButton.setForeground(new Color(255, 255, 255));

        mainPanel.add(dsButton, JLayeredPane.PALETTE_LAYER);
        
        JButton multiButton = new JButton("Multiprogramming");
        multiButton.setFont(new Font("Tahoma", Font.BOLD, 14));

        multiButton.setIcon(folderIcon);
        multiButton.setHorizontalTextPosition(SwingConstants.CENTER);
        multiButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        multiButton.setBounds(189, 574, 180, 100);

        multiButton.setContentAreaFilled(false);
        multiButton.setBorderPainted(false);
        multiButton.setFocusPainted(false);
        multiButton.setForeground(new Color(255, 255, 255));

        mainPanel.add(multiButton, JLayeredPane.PALETTE_LAYER);
        
        URL windowURL = getClass().getResource("/resources/window1.png");
        ImageIcon originalWindow = new ImageIcon(windowURL);
        Image windowImage = originalWindow.getImage()
                .getScaledInstance(1200, 800, Image.SCALE_SMOOTH);
        ImageIcon windowIcon = new ImageIcon(windowImage);
        JLabel windowLabel = new JLabel(windowIcon);
        windowLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        windowLabel.setBounds(240, 102, 963, 596);

        mainPanel.add(windowLabel, JLayeredPane.PALETTE_LAYER);
        
        JLabel lblStarts = new JLabel("Starts here...");
        lblStarts.setFont(new Font("Tahoma", Font.ITALIC, 40));
        lblStarts.setHorizontalAlignment(SwingConstants.CENTER);
        lblStarts.setBounds(433, 517, 300, 80);
        mainPanel.add(lblStarts, JLayeredPane.DRAG_LAYER);
        
        URL startURL = getClass().getResource("/resources/butt.png");
        ImageIcon originalStart = new ImageIcon(startURL);
        Image startImage = originalStart.getImage()
                .getScaledInstance(100, 80, Image.SCALE_SMOOTH);
        ImageIcon startIcon = new ImageIcon(startImage);
        
        JButton startButton = new JButton("");
        startButton.setFont(new Font("Tahoma", Font.BOLD, 14));

        startButton.setIcon(startIcon);
        startButton.setHorizontalTextPosition(SwingConstants.CENTER);
        startButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        startButton.setBounds(965, 517, 150, 93);

        startButton.setContentAreaFilled(false);
        startButton.setBorderPainted(false);
        startButton.setFocusPainted(false);
        startButton.setForeground(new Color(255, 255, 255));

        mainPanel.add(startButton, JLayeredPane.DRAG_LAYER);

     cpuButton.addActionListener(e -> openApp("CPU Scheduling"));
     bankersButton.addActionListener(e -> openApp("Banker's Algorithm"));
     pagingButton.addActionListener(e -> openApp("Paging"));
     prButton.addActionListener(e -> openApp("Page Replacement"));
     segButton.addActionListener(e -> openApp("Segmentation"));
     fragButton.addActionListener(e -> openApp("Fragmentation"));
     dsButton.addActionListener(e -> openApp("Disk Scheduling"));
     multiButton.addActionListener(e -> openApp("Multiprogramming"));
     startButton.addActionListener(e -> openApp("CPU Scheduling")); // ang ">" button

     // =================================
     // SCREENS: HOME at APP
     // =================================
     app = new gui.AppPage(() -> screens.show(root, "HOME"));
     root.add(mainPanel, "HOME");
     root.add(app, "APP");
     setContentPane(root);
     screens.show(root, "HOME");
   

    }
    private void openApp(String page) {
        app.show(page);	
        screens.show(root, "APP");
    }
}
