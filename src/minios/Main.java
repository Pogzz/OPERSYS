
package minios;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            MainBg desktop = new MainBg();
            desktop.setVisible(true);
        });

    }
}
