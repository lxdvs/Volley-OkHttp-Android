package com.android.volley.toolbox;

import java.util.HashMap;

@SuppressWarnings("serial")
public class Strap extends HashMap< String, String > {
    public static Strap make() {
        return new Strap();
    }

    public Strap kv(String k, String v) {
        put(k, v);
        return this;
    }
}
