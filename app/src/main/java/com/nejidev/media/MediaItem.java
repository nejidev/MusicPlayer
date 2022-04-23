package com.nejidev.media;

public class MediaItem {
    private String name;
    private String path;

    public MediaItem(String name, String path){
        this.name = name;
        this.path = path;
    }

    public MediaItem(String path){
        int lastPathPos = path.lastIndexOf("/");
        this.path = path;
        this.name = path;

        if(0 <= lastPathPos && 1 < path.length()){
            this.name = path.substring(lastPathPos + 1);
        }
    }

    public String getName(){
        return name;
    }
    public String getPath(){
        return path;
    }
}
