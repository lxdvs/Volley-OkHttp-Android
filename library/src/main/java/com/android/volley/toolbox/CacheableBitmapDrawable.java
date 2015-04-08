package com.android.volley.toolbox;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

public class CacheableBitmapDrawable extends BitmapDrawable {

    private int mUseCount;
    private boolean mForceUsing;
    private boolean mDead;

    private boolean scaled;

    public CacheableBitmapDrawable(Resources resources, Bitmap bitmap, boolean scaled) {
        super(resources, bitmap);
        // starts used=true prevents it from being instantly reclaimed
        mForceUsing = true;

        this.scaled = scaled;
    }

    public void incrementUseCount() {
        synchronized (this) {
            if (mUseCount < 0) {
                mUseCount = 0;
            }

            mUseCount++;

            if (mDead) {
                Log.e(CacheableBitmapDrawable.class.getSimpleName(), "Reusing an image that has already been inBitmapped. This is very bad. Tell Nick about this.");
            }
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

    /**
     * mark used when pulled from cached so its marked as used even before it actually gets displayed
     */
    public void markForceUsing(boolean using) {
        synchronized (this) {
            mForceUsing = using;
        }
    }

    public boolean isUnused() {
        synchronized (this) {
            return mUseCount == 0 && !mForceUsing;
        }
    }

    public void markDead() {
        mDead = true;
    }

    public boolean isDead() {
        return mDead;
    }

    public boolean isOriginalSize() {
        return !scaled;
    }
}
