package com.example.music;


import android.net.Uri;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

class Music {

    private String name;
    private int imageId;
    private String author;
    private Uri musicUri;
    private int musicId;
    private long musicDuration;

    Music(String name, String author, int imageId, String musicUri, int musicId, long musicDuration){
        this.name = name;
        this.imageId = imageId;
        this.author = author;
        this.musicUri = Uri.parse(musicUri);
        this.musicId = musicId;
        this.musicDuration = musicDuration;
    }

    public String getName(){
        return name;
    }

    public int getImageId(){
        return imageId;
    }

    public String getAuthor(){
        return author;
    }

    public Uri getMusicUri(){
      return musicUri;
    }

    public int getMusicId(){
        return musicId;
    }

    public String getMusicDuration(){
        String musicTime;
        int musicMin = (int)musicDuration/1000/60;
        //感谢Android studio 可以检查拼写错误，我second拼写错误也可以发现 --!
        int musicSecond = (int)musicDuration/1000%60;
        musicTime = musicMin+":"+musicSecond;
        if (musicTime.length()<4){
            musicTime += "0";
        }
        return musicTime;
    }
}
