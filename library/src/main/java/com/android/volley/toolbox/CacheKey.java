package com.android.volley.toolbox;

public class CacheKey {

    public CacheKey(String url, int width, int height) {
        this.url = url;
        this.width = width;
        this.height = height;
    }

    public String url;
    public int width;
    public int height;
}
