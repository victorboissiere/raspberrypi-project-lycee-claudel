package com.raspberry;


import com.raspberry.ui.MyFile;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class Parameters {

    private static File getOrCreateFile(String filename) {
        File parameters = new File(filename);

        if (!parameters.exists()) {
            try {
                parameters.createNewFile();
                return parameters;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return parameters;
    }

    public static void removeLoadedFiles() {
        try {

            String tempFile = "files.txt";
            File fileTemp = new File(tempFile);
            if (fileTemp.exists()) {
                fileTemp.delete();
            }
        } catch (Exception e) {
            // if any error occurs
            e.printStackTrace();
        }
    }

    public static ArrayList<MyFile> loadFiles() {
        ArrayList<MyFile> files = new ArrayList<>();
        File file = getOrCreateFile("files.txt");

        if (file != null) {
            BufferedReader br;

            try {
                br = new BufferedReader(new FileReader(file));
                String line;
                int nb = 0;

                String[] data = {"", "", ""};

                while ((line = br.readLine()) != null) {
                    data[nb] = line;
                    nb++;

                    if (nb == 3) {
                        nb = 0;
                        MyFile new_ = new MyFile(data[1], data[0]);
                        new_.publicId = data[2];
                        files.add(new_);
                    }
                }
                br.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return files;
    }

    public static void saveFiles(ArrayList<MyFile> files) {
        File file = getOrCreateFile("files.txt");

        if (file != null) {
            try {
                FileOutputStream filetowrite = new FileOutputStream(file, false);
                String str = "";
                for (MyFile myFile : files) {
                    str += myFile.name + "\n" + myFile.path + "\n" + myFile.publicId;
                }
                str += "\n";

                filetowrite.write(str.getBytes(Charset.forName("UTF-8")));
                filetowrite.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String[] loadSettings() {
        String[] settings = new String[3];
        File file = new File("settings.txt");

        if (!file.exists()) return null;
        BufferedReader br;

        try {
            br = new BufferedReader(new FileReader(file));
            String line;

            int pos = 0;
            while ((line = br.readLine()) != null) {
                settings[pos] = line;
                pos++;
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return settings;
    }

    public static void setSettings(String[] settings) {
        File file = new File("settings.txt");

        if (!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        FileOutputStream filetowrite = null;
        try {
            filetowrite = new FileOutputStream(file, false);
            String str = settings[0] + "\n" + settings[1] + "\n" + settings[2];
            filetowrite.write(str.getBytes(Charset.forName("UTF-8")));
            filetowrite.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //load the file, if file does not exist return null, else return the refresh_token
    public static String loadTokenFromFile() {
        File parameters = getOrCreateFile("drive.txt");

        if (parameters == null)
            return "";

        BufferedReader br;

        try {
            br = new BufferedReader(new FileReader(parameters));
            String line = br.readLine();
            br.close();
            return line == null ? "" : line;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void saveTokenToFile(String token) {
        File parameters = getOrCreateFile("drive.txt");

        if (parameters != null) {
            try {
                FileOutputStream filetowrite = new FileOutputStream(parameters, false);
                filetowrite.write(token.getBytes(Charset.forName("UTF-8")));
                filetowrite.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
