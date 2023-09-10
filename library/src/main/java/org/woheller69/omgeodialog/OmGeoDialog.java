package org.woheller69.omgeodialog;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ConfigurationCompat;
import androidx.fragment.app.DialogFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OmGeoDialog extends DialogFragment {

    public interface OmGeoDialogResult {
        void onOmGeoDialogResult(City city);
    }
        public OmGeoDialogResult mOmGeoDialogResult;
        Activity activity;
        View rootView;

        private AutoCompleteTextView autoCompleteTextView;
        City selectedCity;

        private ArrayList<String> countryList=null;
        private String title="Title";
        private String negativeButtonText= "OK";
        private String positiveButtonText= "Cancel";
        private String userAgentString = null;

        private static final int TRIGGER_AUTO_COMPLETE = 100;
        private static final long AUTO_COMPLETE_DELAY = 300;
        private Handler handler;
        private AutoSuggestAdapter autoSuggestAdapter;
        String url="https://geocoding-api.open-meteo.com/v1/search?name=";
        String lang = "en";

        @Override
        public void onAttach (@NonNull Context context){
            super.onAttach(context);
            if (context instanceof Activity) {
                this.activity = (Activity) context;
                mOmGeoDialogResult = (OmGeoDialogResult) activity;

            }
        }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) dismiss();
    }

        @NonNull
        @SuppressLint("SetJavaScriptEnabled")
        @Override
        public Dialog onCreateDialog (Bundle savedInstanceState){

            Locale locale = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0);
            if (locale != null) lang=locale.getLanguage();

            LayoutInflater inflater = activity.getLayoutInflater();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            View view = inflater.inflate(R.layout.omgeo_dialog, null);

            rootView = view;

            builder.setView(view);
            builder.setTitle(title);

            final WebView webview =  rootView.findViewById(R.id.mapView);
            webview.getSettings().setJavaScriptEnabled(true);
            if (userAgentString!=null) {
                webview.getSettings().setUserAgentString(userAgentString);
            }

            webview.setBackgroundColor(0x00000000);

            autoCompleteTextView = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextView);
            //Setting up the adapter for AutoSuggest
            autoSuggestAdapter = new AutoSuggestAdapter(requireContext(),
                    R.layout.omgeo_list_item);
            autoCompleteTextView.setThreshold(2);
            autoCompleteTextView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            autoCompleteTextView.setAdapter(autoSuggestAdapter);

            autoCompleteTextView.setOnItemClickListener(
                    (parent, view1, position, id) -> {
                        selectedCity = autoSuggestAdapter.getObject(position);
                        //Hide keyboard to have more space
                        final InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                        //Show city on map
                        webview.setVisibility(View.VISIBLE);
                        webview.loadUrl("file:///android_asset/map.html?lat=" + selectedCity.getLatitude() + "&lon=" + selectedCity.getLongitude());
                    });

            autoCompleteTextView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int
                        count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before,
                                          int count) {
                    handler.removeMessages(TRIGGER_AUTO_COMPLETE);
                    handler.sendEmptyMessageDelayed(TRIGGER_AUTO_COMPLETE,
                            AUTO_COMPLETE_DELAY);
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            handler = new Handler(Looper.getMainLooper(), msg -> {
                if (msg.what == TRIGGER_AUTO_COMPLETE) {
                    if (!TextUtils.isEmpty(autoCompleteTextView.getText())) {
                        try {
                            makeApiCall(URLEncoder.encode(autoCompleteTextView.getText().toString(), "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return false;
            });

            builder.setPositiveButton(positiveButtonText, (dialog, which) -> performDone());

            builder.setNegativeButton(negativeButtonText, null);

            return builder.create();

        }
        private void makeApiCall (String text){
            omGeoApiCall.make(getContext(), text, url, lang, userAgentString, response -> {
                //parsing logic, please change it as per your requirement
                List<String> stringList = new ArrayList<>();
                List<City> cityList = new ArrayList<>();
                try {
                    JSONObject responseObject = new JSONObject(response);

                    JSONArray array = responseObject.getJSONArray("results");
                    for (int i = 0; i < array.length(); i++) {
                        City city =new City();
                        String citystring="";
                        JSONObject jsonFeatures = array.getJSONObject(i);
                        String name="";
                        if (jsonFeatures.has("name")) {
                            name=jsonFeatures.getString("name");
                            citystring=citystring+name;
                        }

                        String countrycode="";
                        if (jsonFeatures.has("country_code")) {
                            countrycode=jsonFeatures.getString("country_code");
                            citystring=citystring+", "+countrycode;
                        }
                        String admin1="";
                        if (jsonFeatures.has("admin1")) {
                            admin1=jsonFeatures.getString("admin1");
                            citystring=citystring+", "+admin1;
                        }

                        String admin2="";
                        if (jsonFeatures.has("admin2")) {
                            admin2=jsonFeatures.getString("admin2");
                            citystring=citystring+", "+admin2;
                        }

                        String admin3="";
                        if (jsonFeatures.has("admin3")) {
                            admin3=jsonFeatures.getString("admin3");
                            citystring=citystring+", "+admin3;
                        }

                        String admin4="";
                        if (jsonFeatures.has("admin4")) {
                            admin4=jsonFeatures.getString("admin4");
                            citystring=citystring+", "+admin4;
                        }

                        city.setCityName(name);
                        city.setCountryCode(countrycode);
                        city.setLatitude((float) jsonFeatures.getDouble("latitude"));
                        city.setLongitude((float) jsonFeatures.getDouble("longitude"));
                        if (countryList==null){
                            cityList.add(city);
                            stringList.add(citystring);
                        } else {
                            for(String country:countryList){
                                if (country.equals(countrycode)){
                                    cityList.add(city);
                                    stringList.add(citystring);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //IMPORTANT: set data here and notify
                autoSuggestAdapter.setData(stringList, cityList);
                autoSuggestAdapter.notifyDataSetChanged();
            }, error -> {
                Handler h = new Handler(activity.getMainLooper());
                h.post(() -> Toast.makeText(activity, error.toString(), Toast.LENGTH_LONG).show());
            });
        }


        private void performDone () {
            if (selectedCity == null) {
                Toast.makeText(activity, "Not found", Toast.LENGTH_SHORT).show();
            } else {
                mOmGeoDialogResult.onOmGeoDialogResult(selectedCity);
                dismiss();
            }
        }


    public void setTitle(String title) {
            this.title = title;
    }

    public void setPositiveButtonText(String positiveButtonText) {
        this.positiveButtonText = positiveButtonText;
    }

    public void setNegativeButtonText(String negativeButtonText) {
        this.negativeButtonText = negativeButtonText;
    }

    public void setCountryList(ArrayList<String> countryList) {
            this.countryList=countryList;
    }

    public void setUserAgentString(String userAgentString) { this.userAgentString=userAgentString;}

}

class AutoSuggestAdapter extends ArrayAdapter<String> implements Filterable {
    private final List<String> mlistData;
    private final List<City> mlistCity;

    public AutoSuggestAdapter(@NonNull Context context, int resource) {
        super(context, resource);
        mlistData = new ArrayList<>();
        mlistCity = new ArrayList<>();
    }

    public void setData(List<String> list, List<City> cityList) {
        mlistData.clear();
        mlistCity.clear();
        mlistData.addAll(list);
        mlistCity.addAll(cityList);
    }

    @Override
    public int getCount() {
        return mlistData.size();
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return mlistData.get(position);
    }

    /**
     * Used to Return the full object directly from adapter.
     *
     * @param position
     * @return
     */
    public City getObject(int position) {
        return mlistCity.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        Filter dataFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    filterResults.values = mlistData;
                    filterResults.count = mlistData.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && (results.count > 0)) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return dataFilter;
    }
}

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
        url = url + query+"&language="+lang;

        StringRequest stringRequest;
        if (userAgent!=null){
            stringRequest = new StringRequest(Request.Method.GET, url,
                    listener, errorListener) {
                @Override
                public Map<String, String> getHeaders() {  //from https://stackoverflow.com/questions/17049473/how-to-set-custom-header-in-volley-request
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("User-Agent", userAgent);
                    return params;
                }
            };
        }else{
            stringRequest = new StringRequest(Request.Method.GET, url,
                    listener, errorListener);
        }

        omGeoApiCall.getInstance(ctx).addToRequestQueue(stringRequest);
    }
}
