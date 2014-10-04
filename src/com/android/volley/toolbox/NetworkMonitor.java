package com.android.volley.toolbox;

import android.util.Log;
import android.util.Pair;

import com.android.volley.NetworkResponse;

import java.util.ArrayList;
import java.util.List;

public class NetworkMonitor {

    // Bandwidths expressed in kB/s
    public static final int BANDWIDTH_HYSTERETIC_LOWER = 30;
    public static final int BANDWIDTH_HYSTERETIC_UPPER = 50;

    private static final boolean ENABLE_LOGGING = false;

    public static final int RING_SIZE = 4;

    public static boolean lowBandwidth;

    private static RingQueue<Pair<Integer, Long>> mImageTimings = new RingQueue<Pair<Integer, Long>>(RING_SIZE);

    public static void add(NetworkResponse response, ImageRequest request) {
        if (request.getRequestTime() == -1) {
            return;
        }

        mImageTimings.add(Pair.create(response.data.length, request.getRequestTime()));
        log("NETMON", "Added image request : " + response.data.length + " bytes with time " + request.getRequestTime());

        int byteSum = 0;
        int timeSum = 0;
        for (Pair<Integer, Long> entry : mImageTimings.getElements()) {
            log("NETMON", "Queue entry:: bytes:" + entry.first + " time(ms):" + entry.second);
            byteSum += entry.first;
            timeSum += entry.second;
        }

        if (mImageTimings.isFull()) {
            int bandwidthKBPS = (byteSum / timeSum);
            log("NETMON", "Derived bandwidth: " + bandwidthKBPS + "kB/s");
            if (bandwidthKBPS < BANDWIDTH_HYSTERETIC_LOWER) {
                lowBandwidth = true;
                log("NETMON", "Low-Bandwidth flag On");
            } else if (bandwidthKBPS > BANDWIDTH_HYSTERETIC_UPPER) {
                lowBandwidth = false;
                log("NETMON", "Low-Bandwidth flag Off");
            }
        }

    }

    private static final void log(String tag, String content) {
        if (ENABLE_LOGGING) {
            Log.i(tag, content);
        }
    }

    /**
     * ArrayList-backed circular FIFO store of <E>s
     * @param <E>
     */
    public static class RingQueue<E> {

        private ArrayList<E> mBackingArray;
        private final int capacity;

        public RingQueue(int capacity) {
            this.capacity = capacity;
            mBackingArray = new ArrayList<E>(capacity);
        }

        public synchronized void add(E element) {
            if (mBackingArray.size() >= capacity) {
                mBackingArray.remove(0);
            }
            mBackingArray.add(element);
        }

        public synchronized List<E> getElements() {
            return new ArrayList<E>(mBackingArray);
        }

        public boolean isFull() {
            return mBackingArray.size() == capacity;
        }
    }
}
