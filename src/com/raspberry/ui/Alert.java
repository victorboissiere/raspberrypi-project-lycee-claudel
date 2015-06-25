package com.raspberry.ui;

import javax.swing.*;

/**
 * Created by vboissiere on 28/05/2015.
 */
public class Alert {

    private JLabel label;

    public Alert(JLabel label)
    {
        this.label = label;
    }

    public void show(String text)
    {
        this.label.setText(text);
    }

    public void hide()
    {
        this.label.setText("");
    }
}
