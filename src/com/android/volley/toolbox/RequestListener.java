package com.android.volley.toolbox;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;

public interface RequestListener< T > extends Listener< T >, ErrorListener {
}
