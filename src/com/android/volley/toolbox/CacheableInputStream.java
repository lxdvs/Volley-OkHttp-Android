package com.android.volley.toolbox;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;

import org.apache.http.HttpStatus;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CacheableInputStream extends InputStream {

    private InputStream mStream;
    private FileOutputStream mFos;
    private Cache mCache;
    private Request mRequest;
    private boolean mShouldCache;

    public CacheableInputStream(Request request, NetworkResponse response, Cache cache) {
        mStream = response.inputStream;
        mCache = cache;
        mRequest = request;
        mShouldCache = request.shouldCache() && response.statusCode == HttpStatus.SC_OK;
    }

    @Override
    public int read() throws IOException {
        if (mFos == null && mShouldCache) {
            mFos = mCache.prepareEntry(mRequest.getCacheKey(), mRequest.getCacheEntry(), mStream.available());

            if (mFos == null) {
                mShouldCache = false;
            }
        }

        int c = mStream.read();

        if (mFos != null && c >= 0) {
            mFos.write(c);
        }

        return c;
    }

    @Override
    public void close() throws IOException {
        super.close();

        if (mFos != null) {
            mFos.close();
            mCache.putEntry(mRequest.getCacheKey(), mRequest.getCacheEntry());
        }
    }
}
