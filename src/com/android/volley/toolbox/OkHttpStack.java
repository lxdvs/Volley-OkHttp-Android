package com.android.volley.toolbox;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.internal.tls.OkHostnameVerifier;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * An {@link com.android.volley.toolbox.HttpStack HttpStack} implementation which uses OkHttp as its transport.
 */
public class OkHttpStack extends HurlStack {
    private final OkHttpClient client;

    public OkHttpStack() {
        this(new OkHttpClient());
    }

    public OkHttpStack(OkHttpClient client) {
        if (client == null) {
            throw new NullPointerException("Client must not be null.");
        }

        client.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                final OkHostnameVerifier underlying = OkHostnameVerifier.INSTANCE;
                return underlying.verify(hostname, session) ||
                        underlying.verify("api.airbnb.com", session);
            }
        });
        this.client = client;

        // No SPDY :( https://github.com/square/okhttp/issues/184
        // tried URL.setURLStreamHandlerFactory(new OkHttpClient()); but not working atm
        ArrayList< String > list = new ArrayList< String >();
        list.add("http/1.1");
        client.setTransports(list);
    }

    public OkHttpClient getClient() {
        return client;
    }

    @Override
    protected HttpURLConnection createConnection(URL url) throws IOException {
        return client.open(url);
    }
}