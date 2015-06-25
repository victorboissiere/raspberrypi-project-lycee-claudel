package com.raspberry;

import com.raspberry.ui.Alert;
import com.raspberry.ui.MyFile;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class Raspberry implements com.raspberry.ui.Window.WindowAction {

    private GoogleAuth auth;
    private Timer timer;
    private JLabel text_files;
    private int refreshTimer;//in milliseconds
    private String[] settings;

    private Alert alert;

    ArrayList<MyFile> myFiles = new ArrayList<>();

    public Raspberry() {

        //load parameters
        settings = Parameters.loadSettings();

        //the file does not exists, set default parameters
        if (settings == null) {
            refreshTimer = 300000;//default timer : every 5 minutes

            settings = new String[3];
            settings[1] = "";
            settings[2] = "";
        } else {
            refreshTimer = Integer.parseInt(settings[0]) * 60 * 1000;
        }


        com.raspberry.ui.Window window = new com.raspberry.ui.Window(400, 500, this);
        JPanel pan = window.createPanel();

        window.addButton(pan, "Autoriser Google API", "Google_auth");
        window.addButton(pan, "Param\u00e8tres", "settings");

        window.addButton(pan, "Ajouter un fichier", "add_file");
        window.addButton(pan, "Supprimer la liste", "remove_list");

        JPanel pan_text = window.createPanel();

        text_files = window.addLabel(pan_text, "");


        JPanel pan_sync = window.createPanel();
        window.addButton(pan_sync, "Synchroniser maintenant", "sync");

        alert = new Alert(window.addLabel(window.createPanel(), ""));


        window.display(true);

        //create object
        auth = new GoogleAuth();
        this.myFiles = Parameters.loadFiles();
        displayFiles();

        ActionListener taskPerformer = evt -> refresh();

        timer = new Timer(refreshTimer, taskPerformer);

        timer.start();


    }

    public void refresh() {
        if (auth != null && !auth.refresh_token.isEmpty()) {
            alert.show("Synchronisation en cours...");
            Runnable r = this::sync;
            new Thread(r).start();
        }
    }

    private void setUp() {
        openWebPage(auth.getURL());
        auth.code = JOptionPane.showInputDialog(null, "Google Auth ID : ");
        auth.setCredential();
    }

    public static void openWebPage(String url) {
        URI uri = URI.create(url);
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void addFile() {

        JFileChooser fc = new JFileChooser();
        int returnVal = fc.showOpenDialog(fc);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            myFiles.add(new MyFile(file.getAbsolutePath(), file.getName()));
            displayFiles();
            Parameters.saveFiles(myFiles);
        }
    }

    public void displayFiles() {
        if (myFiles.isEmpty()) {
            text_files.setText("Vous n'avez aucun fichier");
        } else {
            String text = "<html>";
            for (MyFile myFile : myFiles) text += myFile.name + "<br>";
            text += "</html>";
            text_files.setText(text);
        }
    }

    public void forceGoogleAuth() {
        JOptionPane.showMessageDialog(null, "Vous devez d'abord autoriser l'API Google");
    }

    private void sync() {

        String message = auth.send(myFiles);

        if (!settings[1].isEmpty() && !settings[2].isEmpty()) {
            boolean getDefaultFile = auth.syncDefault(settings[1], settings[2]);
            if (!getDefaultFile) {
                settings[1] = "";
                settings[2] = "";
                Parameters.setSettings(settings);
                System.out.println("Local file details or online ID are invalid so they have been removed from the settings");
            }
        }

        if (message.equals("success"))
            alert.show("Derni\u00e8re synchronisation le : " + frenchDate());
        else
            alert.show(message);

        displayFiles();

    }

    public static String frenchDate() {
        return new SimpleDateFormat("EEE dd MMM hh:mm:ss yyyy", Locale.FRANCE).format(new Date());
    }




    /*
     * Interface for action listener
     */

    @Override
    public void buttonActions(String label) {

        if (!Objects.equals(label, "Google_auth") && (this.auth == null || this.auth.refresh_token.isEmpty())) {
            forceGoogleAuth();
            return;
        }

        switch (label) {
            case "Google_auth":
                //JOptionPane.showMessageDialog(null, "Eggs are not supposed to be green.");
                this.setUp();
                break;
            case "add_file":
                addFile();
                break;
            case "remove_list":

                Parameters.removeLoadedFiles();
                myFiles = new ArrayList<>();
                displayFiles();
                JOptionPane.showMessageDialog(null, "La liste des fichiers synchronis\u00e9s a \u00e9t\u00e9 supprim\u00e9 ");
                break;
            case "sync":
                alert.show("Synchronisation en cours...");
                Runnable r = this::sync;
                new Thread(r).start();
                break;
            case "settings":
                JTextField TextBoxDelay = new JTextField();
                JTextField TextBoxDefaultId = new JTextField();
                JTextField TextBoxDefaultLocalFolder = new JTextField();

                //init with default current value
                TextBoxDelay.setText(Integer.toString(refreshTimer / (60 * 1000)));
                TextBoxDefaultId.setText(settings[1]);
                TextBoxDefaultLocalFolder.setText(settings[2]);


                Object[] fields = {
                        "D\u00e9lai entre chaque synchronisation en minute : ", TextBoxDelay,
                        "ID du fichier Google Drive à importer en readOnly : ", TextBoxDefaultId,
                        "Chemin du dossier de récupération de ce fichier : ", TextBoxDefaultLocalFolder
                };
                int selection = JOptionPane.showConfirmDialog(null, fields, "Paramètres", JOptionPane.OK_CANCEL_OPTION);

                if (selection == JOptionPane.OK_OPTION) {
                    //default
                    int delayInt;

                    String delay = TextBoxDelay.getText();
                    if (!delay.isEmpty()) {
                        delayInt = Integer.parseInt(delay);
                        if (delayInt <= 0 || delayInt > 200) {
                            settingsError("Le format du d\u00e9lai est invalide");
                            return;
                        }
                    } else {
                        settingsError("Vous devez indiquer un d\u00e9lai");
                        return;
                    }

                    String TextDefaultId = TextBoxDefaultId.getText();
                    String TextDefaultLocalFolder = TextBoxDefaultLocalFolder.getText();

                    File folder = new File(TextDefaultLocalFolder);

                    if (!TextDefaultLocalFolder.isEmpty()) {
                        if (Files.notExists(folder.toPath())) {
                            settingsError("Le chemin du dossier n'est pas valide");
                            return;
                        } else if (!folder.isDirectory()) {
                            settingsError("le chemin correspond à un fichier et non à un dossier");
                            return;
                        }
                    }

                    if ((TextDefaultId.isEmpty() && !TextDefaultLocalFolder.isEmpty()) || (!TextDefaultId.isEmpty() && TextDefaultLocalFolder.isEmpty())) {
                        settingsError("Vous devez compléter tous les champs ou laisser vide à la fois l'ID et le chemin du dossier");
                        return;
                    }


                    settings[0] = TextBoxDelay.getText();
                    settings[1] = TextDefaultId;
                    settings[2] = TextDefaultLocalFolder;

                    int newTimer = delayInt * 60 * 1000;
                    if (newTimer != this.refreshTimer) {
                        this.refreshTimer = newTimer;
                        System.out.println("New timer in milliseconds : " + refreshTimer);
                        this.timer.stop();
                        this.timer.setInitialDelay(refreshTimer);
                        this.timer.setDelay(refreshTimer);
                        this.timer.start();
                    }

                    Parameters.setSettings(settings);


                    return;
                }

                break;
        }
    }

    private void settingsError(String message) {
        JOptionPane.showMessageDialog(null, message);
        buttonActions("settings");
    }


}
