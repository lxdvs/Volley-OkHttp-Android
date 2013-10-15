package com.android.volley.toolbox;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache.Entry;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyLog;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Volley adapter for JSON requests that will be parsed into Java objects by Gson.
 */
public class JacksonRequest< T extends JacksonRequest > extends Request< T > {
    private static ObjectMapper mapper;
    private static String TAG = JacksonRequest.class.getSimpleName();
    static {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.setSerializationInclusion(Include.NON_NULL);
    }
    private final Map< String, String > headers;
    private final Listener< T > listener;
    private Properties mPostFields;
    private String mJsonPost;
    private boolean mIntermediate;
    private boolean mPrintJson;

    /**
     * Make a GET request and return a parsed object from JSON.
     * 
     * @param url
     *            URL of the request to make
     * @param clazz
     *            Relevant class object, for Gson's reflection
     * @param headers
     *            Map of request headers
     */
    public JacksonRequest(String url, Map< String, String > headers,
            Listener< T > listener, ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        this.headers = headers;
        this.listener = listener;
    }

    @Override
    public Map< String, String > getHeaders() throws AuthFailureError {
        return headers != null ? headers : super.getHeaders();
    }

    @Override
    protected void deliverResponse(T response, boolean intermediate) {
        preProcess();
        mIntermediate = intermediate;
        if (listener != null) {
            listener.onResponse(response);
        }
    }

    protected void preProcess() {
    }

    /**
     * Override for POST and set postparams with {@link #getBody()}
     */
    @Override
    public int getMethod() {
        return Method.GET;
    }

    /**
     * Milliseconds to live in cache
     * 
     * @return
     */
    public long getTTL() {
        return 0;
    }

    public void setPostParams(Properties props) {
        mPostFields = props;
    }

    public void setPostParamsAsStrap(Strap strap) {
        if (strap != null) {
            mPostFields = new Properties();
            mPostFields.putAll(strap);
        }
    }

    @Override
    protected Map< String, String > getParams() throws AuthFailureError {
        // Java Properties suck.
        if (mPostFields == null) {
            return null;
        }
        HashMap< String, String > map = new HashMap< String, String >();
        for (Map.Entry< Object, Object > entry : mPostFields.entrySet()) {
            map.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Response< T > parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, "utf-8");
            mapper.readerForUpdating(this).withView(((T) this).getClass()).readValue(json);
            if (mPrintJson) {
                try {
                    Log.d(TAG, getUrl() + " response: " + (new JSONObject(json)).toString(4));
                } catch (JSONException e) {
                    Log.d(TAG, getUrl() + " response(parse exception!): " + json);
                }
            }
            Entry entry = HttpHeaderParser.parseCacheHeaders(response);
            entry.setTTL(isPermaCache() ? Long.MAX_VALUE : System.currentTimeMillis() + getTTL());
            return Response.success((T) this, entry);
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonProcessingException e) {
            return Response.error(new ParseError(e));
        } catch (IOException e) {
            return Response.error(new ParseError(e));
        }
    }

    public static void setObjectMapper(ObjectMapper newMapper) {
        mapper = newMapper;
    }

    @Override
    public byte[] getBody() {
        if (TextUtils.isEmpty(mJsonPost)) {
            try {
                return super.getBody();
            } catch (AuthFailureError e) {
                e.printStackTrace();
            }
        }

        try {
            return mJsonPost == null ? null : mJsonPost.getBytes("utf-8");
        } catch (UnsupportedEncodingException uee) {
            VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s",
                    mJsonPost, "utf-8");
            return null;
        }
    }

    @Override
    public String getBodyContentType() {
        if (!TextUtils.isEmpty(mJsonPost)) {
            return "application/json; charset=" + getParamsEncoding();
        } else {
            return super.getBodyContentType();
        }
    }

    public void setJsonPost(String json) {
        this.mJsonPost = json;
    }

    public boolean getIntermediate() {
        return mIntermediate;
    }

	public void setPrintJson(boolean mPrintJson) {
		this.mPrintJson = mPrintJson;
	}

}