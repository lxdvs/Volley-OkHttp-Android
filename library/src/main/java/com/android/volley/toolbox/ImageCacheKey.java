package com.android.volley.toolbox;

public class ImageCacheKey {

    public String url;
    public int width;
    public int height;

    public ImageCacheKey(String url, int width, int height) {
        this.url = url;
        this.width = width;
        this.height = height;
    }
}
