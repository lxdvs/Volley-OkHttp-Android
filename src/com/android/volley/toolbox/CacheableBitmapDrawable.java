package com.android.volley.toolbox;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

public class CacheableBitmapDrawable extends BitmapDrawable {

    private int mUseCount;

    public CacheableBitmapDrawable(Resources resources, Bitmap bitmap) {
        super(resources, bitmap);
        mUseCount = -1;
    }

    public void incrementUseCount() {
        synchronized (this) {
            if (mUseCount < 0) {
                mUseCount = 0;
            }

            mUseCount++;
        }
    }

    public void decrementUseCount() {
        synchronized (this) {
            mUseCount--;
            if (mUseCount < 0) {
                Log.d(CacheableBitmapDrawable.class
                        .getSimpleName(), "decrement less than 0");
                mUseCount = 0;
            }
        }
    }

    public boolean isUnused() {
        synchronized (this) {
            return mUseCount == 0;
        }
    }
}
