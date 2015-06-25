package com.raspberry;

import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.raspberry.ui.MyFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class GoogleAuth {

    /*Complete those data */
    private static String CLIENT_ID = "";
    private static String CLIENT_SECRET = "";
    private static String REDIRECT_URI = "";
    public String defaultFile = "";

    public String code = null;
    public String refresh_token;


    public String defaultName = "default.ods";
    private int RaspberryPiDiff = 2000;

    HttpTransport httpTransport;
    JsonFactory jsonFactory;
    private GoogleCredential credential;

    private GoogleAuthorizationCodeFlow flow;

    public GoogleAuth() {
        this.refresh_token = Parameters.loadTokenFromFile();
    }

    public String getURL() {
        httpTransport = new NetHttpTransport();
        jsonFactory = new JacksonFactory();

        flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, CLIENT_ID, CLIENT_SECRET, Arrays.asList(DriveScopes.DRIVE))
                .setAccessType("offline")
                .setApprovalPrompt("force").build();

        return flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
    }

    public void setCredential() {
        if (code != null) {
            GoogleAuthorizationCodeTokenRequest tokenRequest =
                    flow.newTokenRequest(code);
            tokenRequest.setRedirectUri(REDIRECT_URI);
            GoogleTokenResponse tokenResponse;
            try {
                tokenResponse = tokenRequest.execute();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // Create the OAuth2 credential.
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(new NetHttpTransport())
                    .setJsonFactory(new JacksonFactory())
                    .setClientSecrets(CLIENT_ID, CLIENT_SECRET)
                    .build();

            // Set authorized credentials.
            credential.setFromTokenResponse(tokenResponse);

            //can display the tokenResponse to debut. Contains a token and a refresh token

            refresh_token = credential.getRefreshToken();

            Parameters.saveTokenToFile(refresh_token);

            this.credential = credential;

        } else {
            System.out.println("Not token set");
        }
    }

    private Drive getService() {
        httpTransport = new NetHttpTransport();
        jsonFactory = new JacksonFactory();

        GoogleCredential credential = new GoogleCredential.Builder()
                .setJsonFactory(jsonFactory)
                .setTransport(httpTransport)
                .setClientSecrets(CLIENT_ID, CLIENT_SECRET)
                .build();
        credential.setRefreshToken(refresh_token);

        //Create a new authorized API client

        return new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), credential).setApplicationName("Raspberry PI").build();
    }


    public String send(ArrayList<MyFile> files) {

        if (!this.refresh_token.isEmpty()) {

            Drive service = getService();

            Iterator<MyFile> i = files.iterator();
            while (i.hasNext()) {
                MyFile f = i.next();
                if (f.publicId.length() != 0) {
                    if (!sync(service, f)) {
                        i.remove();
                        System.out.println("Error : file not valid. Removing it from the list...");
                    }

                } else {
                    System.out.println("Uploading " + f.name + " ...");
                    if (!upload(service, f)) {
                        System.out.println("Error : file not valid. Removing it from the list...");
                        i.remove();
                    }
                }
            }


            Parameters.saveFiles(files);

            return "success";
        }
        return "Erreur : aucun refresh_token n'est present";
    }

    private boolean upload(Drive service, MyFile file) {
        //Insert a file
        File body = new File();
        body.setTitle(file.name);
        body.setDescription("Raspberry pi doc");
        body.setMimeType("text/csv");

        java.io.File fileContent = new java.io.File(file.path);
        FileContent mediaContent = new FileContent("text/csv", fileContent);

        File onlineFile;
        try {
            onlineFile = service.files().insert(body, mediaContent).execute();
            file.setID(onlineFile.getId());
            System.out.println("The file has been uploaded");
            return true;
        } catch (UnknownHostException | SocketTimeoutException | SocketException e) {
            System.out.println("No internet connection detected. Nothing has been done.");
            return true;
        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            System.out.println("Error : either 404, file not found so remove or 503 network issue, nothing done.");
            return !(e.getDetails().getCode() == 404);
        } catch (IOException e) {
            System.out.println("The file does not exist in the system and has been removed");
            e.printStackTrace();
            return false;
        }
    }

    public boolean syncDefault(String onlineID, String localFilePath) {
        if (!this.refresh_token.isEmpty()) {


            java.io.File localFile = new java.io.File(localFilePath + defaultName);
            Drive service = getService();

            File onlineFile;
            try {
                onlineFile = service.files().get(onlineID).execute();
                if (!localFile.exists() || onlineFile.getModifiedDate().getValue() > localFile.lastModified() + RaspberryPiDiff) {
                    System.out.println("Refreshing local default document");
                    return downloadDefaultFile(service, localFilePath, onlineFile);
                }

                return true;
            } catch (UnknownHostException | SocketTimeoutException | SocketException e) {
                System.out.println("No internet connection detected. Nothing has been done.");
                return true;
            } catch (GoogleJsonResponseException e) {
                e.printStackTrace();
                return !(e.getDetails().getCode() == 404);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return false;
    }

    private boolean downloadDefaultFile(Drive service, String localFilePath, File onlineFile) {

        Map<String, String> links = onlineFile.getExportLinks();
        String downloadUrl = links.get("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        //tip to delete extension and get the ods file because the new google sheet does not support ods file.
        //see : http://stackoverflow.com/questions/28028031/google-drive-web-api-does-not-return-open-office-export-link-for-new-spreadsheet

        //remember to change the extension of the file with the right format, see at the top, defaultName variable

        downloadUrl = downloadUrl.substring(0, downloadUrl.lastIndexOf("xlsx")) + "ods";

        if (downloadUrl.length() > 0) {
            try {
                HttpResponse resp =
                        service.getRequestFactory().buildGetRequest(
                                new GenericUrl(downloadUrl))
                                .execute();

                InputStream in = resp.getContent();
                Files.copy(in, Paths.get(localFilePath + defaultName), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("local file at " + localFilePath + defaultName);
                java.io.File fileContent = new java.io.File(localFilePath + defaultName);
                if (!fileContent.setLastModified(onlineFile.getModifiedDate().getValue()))
                    System.out.println("Could not change modified date to the online date");


                System.out.println("Done!");
                return true;
            } catch (UnknownHostException | SocketTimeoutException | SocketException e) {
                System.out.println("No internet connection detected. Nothing has been done.");
                return true;
            } catch (GoogleJsonResponseException e) {
                e.printStackTrace();
                if (e.getDetails().getCode() == 404) {
                    System.out.println("File not found online...");
                    return false;
                }
                else {
                    System.out.println("File not found online but this might be temporary... (503 error, network issue)");
                    return true;
                }
            } catch (IOException e) {
                // An error occurred.
                e.printStackTrace();
                return false;
            }
        }

        return false;
    }


    private boolean sync(Drive service, MyFile file) {
        try {

            File onlineFile = service.files().get(file.publicId).execute();

            //get local file for information
            java.io.File localFile = new java.io.File(file.path);

            if (!localFile.exists())
                return false;


            long onlineDate = onlineFile.getModifiedDate().getValue();
            long localDate = localFile.lastModified();


            /*

            System.out.println("LDate: " + new Date(localDate).toString());
            System.out.println("ODate: " + new Date(onlineDate).toString());
            System.out.println("LDate: " + Long.toString(localDate));
            System.out.println("ODate: " + Long.toString(onlineDate));

            System.out.println("Download if : " + Long.toString(onlineDate) + ">");
            System.out.println("Download if : " + Long.toString(localDate + RaspberryPiDiff) + "");
            */

            if (onlineDate > localDate + RaspberryPiDiff) {
                //we download the file
                System.out.println("Downloading online file...");
                if (onlineFile.getDownloadUrl() != null && onlineFile.getDownloadUrl().length() > 0) {
                    try {
                        // uses alt=media query parameter to request content
                        InputStream in = service.files().get(file.publicId).executeMediaAsInputStream();
                        Files.copy(in, Paths.get(file.path), StandardCopyOption.REPLACE_EXISTING);
                        java.io.File fileContent = new java.io.File(file.path);
                        if (!fileContent.setExecutable(true, false))
                            System.out.println("Could no change permissions of the file (to executable)");
                        if (!fileContent.setWritable(true, false))
                            System.out.println("Could no change permissions of the file (to writable)");
                        if (!fileContent.setLastModified(onlineDate))
                            System.out.println("Could not change modified date to the online date");

                        System.out.println("Done!");
                    } catch (IOException e) {
                        // An error occurred.
                        e.printStackTrace();
                        return false;
                    }
                }
            } else if (localDate > onlineDate + RaspberryPiDiff) {
                System.out.println("Updating online file...");

                // File's new metadata.
                onlineFile.setTitle(file.name);
                onlineFile.setDescription("Raspberry pi Doc");
                onlineFile.setMimeType("text/csv");


                // File's new content.
                java.io.File fileContent = new java.io.File(file.path);
                FileContent mediaContent = new FileContent("text/csv", fileContent);

                // Send the request to the API.
                File updatedFile = service.files().update(file.publicId, onlineFile, mediaContent).execute();
                long newOnlineDate = updatedFile.getModifiedDate().getValue();

                localFile.setLastModified(newOnlineDate);
                System.out.println("Done!");
            }
            return true;

        } catch (UnknownHostException | SocketTimeoutException | SocketException e) {
            System.out.println("No internet connection detected. Nothing has been done.");
            return true;
        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            if (e.getDetails().getCode() == 404) {
                file.setID("");
                System.out.println("File with id " + file.publicId + " was not found online. The file will then be uploaded on Drive");
                return upload(service, file);
            } else {
                System.out.println("A problem occured. Nothing has been done. (Probably temporary internet connection not valid (503 error)).");
                return true;
            }
        } catch (IOException e) {
            System.out.println("A problem occured. Invalid settings");
            e.printStackTrace();
            return false;
        }

    }


}
