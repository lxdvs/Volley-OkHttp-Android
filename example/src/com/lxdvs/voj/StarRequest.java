package com.lxdvs.voj;

import com.android.volley.toolbox.JacksonRequest;
import com.android.volley.toolbox.RequestListener;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StarRequest extends JacksonRequest< StarRequest > {

    public StarRequest(RequestListener< StarRequest > listener) {
        super("https://api.github.com/rate_limit", null, listener);
    }

    @JsonProperty("rate")
    Rate rate;

}
