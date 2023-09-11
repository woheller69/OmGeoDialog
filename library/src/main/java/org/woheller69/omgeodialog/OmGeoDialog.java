package org.woheller69.omgeodialog;

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
import android.widget.AutoCompleteTextView;

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
import java.util.List;
import java.util.Locale;

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
        private String negativeButtonText= "Cancel";
        private String positiveButtonText= "OK";
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

