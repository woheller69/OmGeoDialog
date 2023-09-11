package org.woheller69.omgeodialog;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

class omGeoApiCall {
    private static omGeoApiCall mInstance;
    private RequestQueue mRequestQueue;
    private static Context mCtx;

    public omGeoApiCall(Context ctx) {
        mCtx = ctx.getApplicationContext();
        mRequestQueue = getRequestQueue();
    }

    public static synchronized omGeoApiCall getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new omGeoApiCall(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public static void make(Context ctx, String query, String url, String lang, String userAgent, Response.Listener<String>
            listener, Response.ErrorListener errorListener) {
        url = url + query + "&language=" + lang;

        StringRequest stringRequest;
        if (userAgent != null) {
            stringRequest = new StringRequest(Request.Method.GET, url,
                    listener, errorListener) {
                @Override
                public Map<String, String> getHeaders() {  //from https://stackoverflow.com/questions/17049473/how-to-set-custom-header-in-volley-request
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("User-Agent", userAgent);
                    return params;
                }
            };
        } else {
            stringRequest = new StringRequest(Request.Method.GET, url,
                    listener, errorListener);
        }

        omGeoApiCall.getInstance(ctx).addToRequestQueue(stringRequest);
    }
}
