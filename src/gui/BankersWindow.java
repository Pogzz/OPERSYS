package gui;

import LOGICS.BankersLogic;
import javax.swing.*;
import java.awt.*;

public class BankersWindow extends BasePanel {
    public BankersWindow() {
        super("Banker's Algorithm");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(new JLabel("Processes:"));
        JTextField p = new JTextField(4);
        top.add(p);
        top.add(new JLabel("Resources:"));
        JTextField r = new JTextField(4);
        top.add(r);
        JButton check = new JButton("Check Safety");
        top.add(check);

        JTextArea result = new JTextArea(4, 20);
        result.setEditable(false);

        content.add(top, BorderLayout.NORTH);
        content.add(new JScrollPane(new JTable(5, 6)), BorderLayout.CENTER);
        content.add(result, BorderLayout.SOUTH);

        check.addActionListener(e -> {
            // TODO: kunin ang alloc, max, avail sa table
            // result.setText(BankersLogic.checkSafety(alloc, max, avail));
        });
    }
}