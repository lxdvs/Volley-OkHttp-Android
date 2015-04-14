/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.volley.toolbox;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;

/**
 * A canned request for getting an image at a given URL and calling
 * back with a decoded Bitmap.
 */
public class ImageRequest extends Request<CacheableBitmapDrawable> {
    /** Socket timeout in milliseconds for image requests */
    private static final int IMAGE_TIMEOUT_MS = 10000;

    /** Default number of retries for image requests */
    private static final int IMAGE_MAX_RETRIES = 3;

    /** Default backoff multiplier for image requests */
    private static final float IMAGE_BACKOFF_MULT = 2f;

    private final Response.Listener<CacheableBitmapDrawable> mListener;
    private final Config mDecodeConfig;
    private final int mMaxWidth;
    private final int mMaxHeight;
    private final boolean mBackgroundFetch;

    /** Decoding lock so that we don't decode more than one image at a time (to avoid OOM's) */
    private static final Object sDecodeLock = new Object();

    private Context mContext;

    private ImageLoader.ImageCache mCache;
    private long mTtl = 0;

    /**
     * Creates a new image request, decoding to a maximum specified width and
     * height. If both width and height are zero, the image will be decoded to
     * its natural size. If one of the two is nonzero, that dimension will be
     * clamped and the other one will be set to preserve the image's aspect
     * ratio. If both width and height are nonzero, the image will be decoded to
     * be fit in the rectangle of dimensions width x height while keeping its
     * aspect ratio.
     *
     * @param url URL of the image
     * @param listener Listener to receive the decoded bitmap
     * @param maxWidth Maximum width to decode this bitmap to, or zero for none
     * @param maxHeight Maximum height to decode this bitmap to, or zero for
     *            none
     * @param decodeConfig Format to decode the bitmap to
     * @param errorListener Error listener, or null to ignore errors
     */
    public ImageRequest(Context context, String url, ImageLoader.ImageCache imageCache, Response.Listener<CacheableBitmapDrawable> listener,
            int maxWidth, int maxHeight, boolean backgroundFetch,
            Config decodeConfig, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        setRetryPolicy(
                new DefaultRetryPolicy(IMAGE_TIMEOUT_MS, IMAGE_MAX_RETRIES, IMAGE_BACKOFF_MULT));
        mListener = listener;
        mDecodeConfig = decodeConfig;
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
        mContext = context;
        mCache = imageCache;
        mBackgroundFetch = backgroundFetch;
    }

    @Override
    public Priority getPriority() {
        return mBackgroundFetch ? Priority.BACKGROUND : Priority.LOW;
    }

    /**
     * Scales one side of a rectangle to fit aspect ratio.
     *
     * @param maxPrimary Maximum size of the primary dimension (i.e. width for
     *        max width), or zero to maintain aspect ratio with secondary
     *        dimension
     * @param maxSecondary Maximum size of the secondary dimension, or zero to
     *        maintain aspect ratio with primary dimension
     * @param actualPrimary Actual size of the primary dimension
     * @param actualSecondary Actual size of the secondary dimension
     */
    private static int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary,
            int actualSecondary) {
        // If no dominant value at all, just return the actual.
        if (maxPrimary == 0 && maxSecondary == 0) {
            return actualPrimary;
        }

        // If primary is unspecified, scale primary to match secondary's scaling ratio.
        if (maxPrimary == 0) {
            double ratio = (double) maxSecondary / (double) actualSecondary;
            return (int) (actualPrimary * ratio);
        }

        if (maxSecondary == 0) {
            return maxPrimary;
        }

        double ratio = (double) actualSecondary / (double) actualPrimary;
        int resized = maxPrimary;
        if (resized * ratio > maxSecondary) {
            resized = (int) (maxSecondary / ratio);
        }
        return resized;
    }

    @Override
    protected Response<CacheableBitmapDrawable> parseNetworkResponse(NetworkResponse response) {
        // Serialize all decode on a global lock to reduce concurrent heap usage.
        synchronized (sDecodeLock) {
            try {
                CacheableBitmapDrawable image = doParse(response);
                if (image == null) {
                    return Response.error(new ParseError(response));
                } else {
                    Cache.Entry entry = HttpHeaderParser.parseCacheHeaders(response);
                    if (entry != null && getTTL() > 0) {
                        entry.setTTL(System.currentTimeMillis() + getTTL());
                        if (isOfflineCache()) {
                            entry.keepUntil = entry.ttl;
                        }
                    } else if (getTTL() != 0) {
                        Log.w(getClass().getSimpleName(), getClass().getSimpleName() + " has a TTL, but will not be cached due to network response's cache policy");
                    }
                    return Response.success(image, entry);
                }
            } catch (OutOfMemoryError e) {
                VolleyLog.e("Caught OOM for %d byte image, url=%s", response.data.length, getUrl());
                return Response.error(new ParseError(e));
            }
        }
    }

    @Override
    protected boolean isMarkerLogEnabled() {
        return false;
    }

    /**
     * The real guts of parseNetworkResponse. Broken out for readability.
     */
    private CacheableBitmapDrawable doParse(NetworkResponse response) {
        NetworkMonitor.add(response, this);
        response.isImage = true;
        byte[] data = response.data;
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inPreferredConfig = mDecodeConfig;

        Bitmap bitmap;

        // If we have to resize this image, first get the natural bounds.
        decodeOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        int actualWidth = decodeOptions.outWidth;
        int actualHeight = decodeOptions.outHeight;

        int sampleSize = 1;

        if (mMaxWidth > 0 && mMaxHeight > 0) {
            // Then compute the dimensions we would ideally like to decode to.
            int desiredWidth = getResizedDimension(mMaxWidth, mMaxHeight,
                    actualWidth, actualHeight);
            int desiredHeight = getResizedDimension(mMaxHeight, mMaxWidth,
                    actualHeight, actualWidth);

            // Decode to the nearest power of two scaling factor.
            sampleSize = findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
        }

        decodeOptions.inSampleSize = sampleSize;

        decodeOptions.inJustDecodeBounds = false;
        // TODO(ficus): Do we need this or is it okay since API 8 doesn't support it?
        // decodeOptions.inPreferQualityOverSpeed = PREFER_QUALITY_OVER_SPEED;

        decodeOptions.inMutable = true;
        Bitmap inBitmap = mCache.getOldestUnused(actualWidth / sampleSize, actualHeight / sampleSize, decodeOptions.inPreferredConfig, sampleSize);
        if (inBitmap != null) {
            decodeOptions.inBitmap = inBitmap;
        }

        try {
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        } catch (IllegalArgumentException e) {
            decodeOptions.inBitmap = null;
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        }

        if (bitmap == null) {
            return null;
        } else {
            return new CacheableBitmapDrawable(mContext.getResources(), bitmap, sampleSize > 1);
        }
    }

    @Override
    protected void deliverResponse(CacheableBitmapDrawable response) {
        mListener.onResponse(response);
    }

    /**
     * Returns the largest power-of-two divisor for use in downscaling a bitmap.
     * may result in image being slightly larger than desired dimensions
     *
     * @param actualWidth Actual width of the bitmap
     * @param actualHeight Actual height of the bitmap
     * @param desiredWidth Desired width of the bitmap
     * @param desiredHeight Desired height of the bitmap
     */
    // Visible for testing.
    static int findBestSampleSize(
            int actualWidth, int actualHeight, int desiredWidth, int desiredHeight) {
        double wr = (double) actualWidth / desiredWidth;
        double hr = (double) actualHeight / desiredHeight;
        double ratio = Math.min(wr, hr);
        float n = 1.0f;
        while (n < ratio) {
            n *= 2;
        }

        return (int) Math.ceil(n / 2f);
    }

    @Override
    protected boolean isFifoProcessed() {
        return false;
    }

    public long getTTL() {
        return mTtl;
    }

    public void setTtl(long ttl) {
        mTtl = ttl;
        setOfflineCache(ttl > 0);
    }

    public boolean isImageRequest() {
        return true;
    }
}
