package com.raspberry.ui;

/**
 * Created by vboissiere on 27/05/2015.
 */
public class MyFile {

    public String name;
    public String path;
    public String publicId;

    public MyFile(String path, String name) {
        this.name = name;
        this.path = path;
        publicId = "";
    }

    public void setID(String id)
    {
        this.publicId = id;
    }
}
