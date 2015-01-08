package com.android.volley.toolbox;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;

import com.android.volley.NetworkResponse;

import java.util.ArrayList;
import java.util.List;

public class NetworkMonitor {

    // Bandwidths expressed in kB/s
    public static final int BANDWIDTH_HYSTERETIC_LOWER = 40; // Just above the upper limit of 2G
    public static final int BANDWIDTH_HYSTERETIC_UPPER = 80; // Listing Image loads ~4s apiece

    private static final boolean ENABLE_LOGGING = false;

    public static final int RING_SIZE = 4;

    public static boolean lowBandwidth;
    private static boolean mIsRoaming;

    private static RingQueue<Pair<Integer, Long>> mImageTimings = new RingQueue<Pair<Integer, Long>>(RING_SIZE);

    public static void initialize(Context context) {
        NetworkClass netclass = getNetworkClass(context);
        log("NETMON", "Net class: " + netclass.name());
        if (netclass.ordinal() <= NetworkClass.TYPE_2G.ordinal()) {
            log("NETMON", "Low-Bandwidth flag On");
            lowBandwidth = true;
        }
    }

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

    public static boolean isRoaming() {
        return mIsRoaming;
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

    public static enum NetworkClass {
        TYPE_ROAMING("roaming"),
        TYPE_2G("2G"),
        TYPE_3G("3G"),
        TYPE_4G("4G"),
        TYPE_WIFI("Wifi"),
        Unknown("Unknown");

        public final String description;

        NetworkClass(String description) {
            this.description = description;
        }
    }

    public static NetworkClass getNetworkClass(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager != null) {
            NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (wifi != null && wifi.isConnected()) {
                mIsRoaming = false;
                return NetworkClass.TYPE_WIFI;
            }
        }

        TelephonyManager telephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            if (telephonyManager.isNetworkRoaming()) {
                mIsRoaming = true;
                return NetworkClass.TYPE_ROAMING;
            }
            mIsRoaming = false;

            int networkType = telephonyManager.getNetworkType();
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return NetworkClass.TYPE_2G;
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                    return NetworkClass.TYPE_3G;
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return NetworkClass.TYPE_4G;
                default:
                    return NetworkClass.Unknown;
            }
        }
        return NetworkClass.Unknown;
    }
}
