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

    public Strap kv(String k, long v) {
        return this.kv(k, Long.toString(v));
    }

    public Strap kv(String k, int v) {
        return this.kv(k, Integer.toString(v));
    }

    public Strap kv(String k, boolean v) {
        return this.kv(k, Boolean.toString(v));
    }

    public Strap kv(String k, float v) {
        return this.kv(k, Float.toString(v));
    }

    public Strap kv(String k, double v) {
        return this.kv(k, Double.toString(v));
    }

    public Strap mix(Strap strap) {
        for (Entry< String, String > entry : strap.entrySet()) {
            kv(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public String getString(String key) {
        String value = super.get(key);
        return value != null ? value : "";
    }
}
