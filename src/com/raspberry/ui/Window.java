package com.raspberry.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class Window extends JFrame implements ActionListener {

    private WindowAction listener;
    JPanel container;

    public Window(int width, int height, WindowAction listener) {
        this.setTitle("Raspberry PI project");
        this.setSize(width, height);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE)
        ;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                String ObjButtons[] = {"Yes", "No"};
                int PromptResult = JOptionPane.showOptionDialog(null, "\u00cates-vous s\u00fbr de vouloir quitter le logiciel ?", "Raspberry PI project",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, ObjButtons, ObjButtons[1]);

                if (PromptResult == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });


        this.listener = listener;
        container = new JPanel();
        container.setLayout(
                new BoxLayout(container, BoxLayout.Y_AXIS)
        );

        this.add(container);

    }


    public JPanel createPanel() {
        JPanel pan = new JPanel();
        container.add(pan);
        return pan;
    }

    public void addButton(JPanel pan, String text, String label) {

        com.raspberry.ui.Button button = new com.raspberry.ui.Button(text);
        button.setActionCommand(label);
        button.addActionListener(this);
        pan.add(button);

    }

    public JLabel addLabel(JPanel pan, String text) {
        JLabel label = new JLabel(text);
        //label.setPreferredSize(new Dimension(200, 50)); //only work for width
        pan.add(label);
        return label;
    }

    public void display(Boolean status) {
        this.setVisible(status);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = ((JButton) e.getSource()).getActionCommand();
        listener.buttonActions(actionCommand);
    }

    public interface WindowAction {
        public void buttonActions(String label);
    }
}

