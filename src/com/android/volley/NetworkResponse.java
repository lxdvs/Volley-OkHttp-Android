/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.volley;

import org.apache.http.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;

/**
 * Data and headers returned from {@link Network#performRequest(Request)}.
 */
public class NetworkResponse {
    /**
     * Creates a new network response.
     * @param statusCode the HTTP status code
     * @param data Response body
     * @param headers Headers returned with this response, or null for none
     * @param notModified True if the server returned a 304 and the data was already in cache
     */
    public NetworkResponse(int statusCode, byte[] data, Map<String, String> headers,
            boolean notModified) {
        this.statusCode = statusCode;
        this.data = data;
        this.headers = headers;
        this.notModified = notModified;
        this.inputStream = null;

        // errorResponseString not needed if response is success
        if (statusCode != HttpStatus.SC_OK) {
            String inter;
            try {
                inter = new String(data, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                inter = "<error parse failed>";
            }
            errorResponseString = inter;
        } else {
            errorResponseString = null;
        }

        throw new IllegalStateException("you can't do this");
    }

    /**
     * Creates a new network response.
     * @param statusCode the HTTP status code
     * @param stream Response stream
     * @param headers Headers returned with this response, or null for none
     * @param notModified True if the server returned a 304 and the data was already in cache
     */
    public NetworkResponse(int statusCode, InputStream stream, Map<String, String> headers,
            boolean notModified) {
        this.statusCode = statusCode;
        this.data = new byte[0];
        this.inputStream = stream;
        this.headers = headers;
        this.notModified = notModified;

        // errorResponseString not needed if response is success or 304
        if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_NOT_MODIFIED) {
            errorResponseString = parseStream(stream);
        } else {
            errorResponseString = null;
        }
    }

    public NetworkResponse(byte[] data) {
        this(HttpStatus.SC_OK, data, Collections.<String, String>emptyMap(), false);
    }

    public NetworkResponse(byte[] data, Map<String, String> headers) {
        this(HttpStatus.SC_OK, data, headers, false);
    }

    public NetworkResponse(InputStream stream, Map<String, String> headers) {
        this(HttpStatus.SC_OK, stream, headers, false);
    }

    /** The HTTP status code. */
    public final int statusCode;

    public boolean isImage = false;

    /** Raw data from this response. */
    public final byte[] data;
    public InputStream inputStream;

    /** Response headers. */
    public final Map<String, String> headers;

    /** True if the server returned a 304 (Not Modified). */
    public final boolean notModified;
    
    /** Attempted parse of the returned string data */
    public final String errorResponseString;

    private static String parseStream(InputStream stream) {
        InputStreamReader is = new InputStreamReader(stream);
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(is);
        String read;
        try {
            read = br.readLine();

            while(read != null) {
                sb.append(read);
                read = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public String getDataAsString() {
        return parseStream(inputStream);
    }
}