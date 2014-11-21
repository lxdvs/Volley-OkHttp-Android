package com.android.volley.toolbox;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

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
        this.client = client;

        /*
        // force http
        // no spdy https://github.com/square/okhttp/issues/963
        ArrayList<Protocol> list = new ArrayList< Protocol >();
        list.add(Protocol.HTTP_1_1);
        client.setProtocols(list);
        */
    }

    public OkHttpClient getClient() {
        return client;
    }

    @Override
    protected HttpURLConnection createConnection(URL url) throws IOException {
        return new OkUrlFactory(client).open(url);
    }
}