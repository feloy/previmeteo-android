package fr.elol.meteo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import fr.elol.meteo.data.ForecastMap;
import fr.elol.meteo.helpers.GetJSONData;
import fr.elol.meteo.helpers.WeatherIcon;

public class MapsActivity extends FragmentActivity {

    private static final int PLUS_TEMPE = 0;
    private static final int PLUS_PRESSION = 1;
    private static final int PLUS_VENT = 2;
    private int displayPlus;
    private int displayedPlus;

    private ProgressBar loadbar;
    private TextView infoText;

    private SeekBar sb2;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private ArrayList<ForecastMarker> forecastMarkers;

    private static final String[] dayParts = { "nuit", "matin", "après-midi", "soirée"};

    private String latestDlJson = null;

    private static final String linkPrefix = "android-app://fr.elol.meteo/http/meteo.elol.fr/carte";
    private GoogleApiClient mClient;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("displayPlus", displayPlus);
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Tracker t = ((MeteoApp) getApplication()).getTracker(
                MeteoApp.TrackerName.APP_TRACKER);
        t.setScreenName("MapsActivity");
        t.send(new HitBuilders.AppViewBuilder().build());

        if (savedInstanceState != null) {
            displayPlus = savedInstanceState.getInt("displayPlus", PLUS_TEMPE);
        } else {
            displayPlus = PLUS_TEMPE;
        }
        displayedPlus = displayPlus;

        setContentView(R.layout.activity_maps);

        infoText = (TextView) findViewById (R.id.infoTop);

        loadbar = (ProgressBar) findViewById (R.id.loadbar);
        loadbar.setVisibility(View.GONE);

        forecastMarkers = null;

        sb2 = (SeekBar) findViewById (R.id.seekBar2);
        sb2.setMax(2);
        sb2.setProgress(PLUS_TEMPE);
        sb2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int display, boolean fromUser) {
                displayPlus = display;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (displayPlus != displayedPlus) {
                    displayedPlus = displayPlus;
                    displayMap();
                }
            }
        });

        setUpMapIfNeeded();

        mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mClient.connect();
        // Define a title for your current page, shown in autocompletion UI
        String title = "Météo carte de France";
        Log.d("appIndex", "Start uri=" + Uri.parse(linkPrefix).toString());
        Action viewAction = Action.newAction(Action.TYPE_VIEW, title, Uri.parse(linkPrefix));
        AppIndex.AppIndexApi.start(mClient, viewAction);
    }

    @Override
    protected void onStop() {
        Log.d("appIndex", "End uri=" + Uri.parse(linkPrefix).toString());
        String title = "Météo carte de France";
        Action viewAction = Action.newAction(Action.TYPE_VIEW, title, Uri.parse(linkPrefix));
        AppIndex.AppIndexApi.end(mClient, viewAction);
        mClient.disconnect();
        super.onStop();
    }

    private void displayInfo () {
        sb2.setEnabled(true);
        switch (displayPlus) {
            case PLUS_PRESSION:
                infoText.setText("T° et pression atmosphérique");
                break;
            case PLUS_VENT:
                infoText.setText("Vent : direction et vitesse moyenne");
                break;
            case PLUS_TEMPE:
                infoText.setText("Température moyenne");
                break;

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #displayMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setupMap();
                displayMap();
                centerMap();
            }
        }
    }

    private void setupMap () {
        mMap.setMyLocationEnabled(false);
        mMap.setInfoWindowAdapter(new PopupAdapter(getLayoutInflater()));

        //                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (forecastMarkers == null)
                    return;
                Log.d("Meteo", "zoom: " + cameraPosition.zoom);
                for (int i = 0; i < forecastMarkers.size(); i++) {
                    forecastMarkers.get(i).refresh(cameraPosition.zoom);
                }
            }
        });
    }

    private void displayMap() {

        displayInfo();

        loadbar.setVisibility(View.VISIBLE);

        if (latestDlJson != null) {
            (new computeMarkers()).execute (latestDlJson);
            return;
        }

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String u = "https://meteo.android.elol.fr/ajax/carte.php?p=6";
            GetJSONData task = new GetJSONData(this, 0, new GetJSONData.CallBackListener() {

                @Override
                public void callback(int range, String str) {
                    latestDlJson = str;
                    (new computeMarkers()).execute (str);
                }
            });
            task.execute(u);
        } else {
            Toast.makeText(this, R.string.erreur_connexion_reseau,
                    Toast.LENGTH_SHORT)
                    .show();
            loadbar.setVisibility(View.GONE);
/*            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.INVISIBLE);
            }*/
        }

    }

    private BitmapDescriptor GetCustomBitmapDescriptor (ForecastMap fc)
    {
        LayoutInflater inflater =
                (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/weathericons.ttf");

        View tagView = null;
        switch (displayPlus) {
            case PLUS_TEMPE: {
                tagView = inflater.inflate(R.layout.map_marker, null);

                ImageView picto = (ImageView) tagView.findViewById(R.id.imgPicto);
                String str = "icon" + fc.mPicto + (fc.mIsDay ? "" : "n") + "_24";
                int res = getResources().getIdentifier(str, "drawable", getPackageName());
                picto.setImageResource(res);

                TextView tempe = (TextView) tagView.findViewById(R.id.textTempe);
                tempe.setTypeface(tf);
                tempe.setText(fc.mTempe + WeatherIcon.WI_CELSIUS);
            }
            break;
            case PLUS_PRESSION: {
                tagView = inflater.inflate(R.layout.map_marker_pression, null);

                ImageView picto = (ImageView) tagView.findViewById(R.id.imgPicto);
                String str = "icon" + fc.mPicto + (fc.mIsDay ? "" : "n") + "_24";
                int res = getResources().getIdentifier(str, "drawable", getPackageName());
                picto.setImageResource(res);

                TextView tempe = (TextView) tagView.findViewById(R.id.textTempe);
                tempe.setTypeface(tf);
                tempe.setText(fc.mTempe + WeatherIcon.WI_CELSIUS);

                TextView textDisplayPlus = (TextView) tagView.findViewById(R.id.textTempeRes);
                textDisplayPlus.setTypeface(tf);
                textDisplayPlus.setText(fc.mPression.toString());
            }
            break;
            case PLUS_VENT: {
                tagView = inflater.inflate(R.layout.map_marker_wind, null);

                TextView picto = (TextView) tagView.findViewById(R.id.textPicto);
                picto.setTypeface(tf);
                picto.setText(WeatherIcon.getDirVent(fc.mDirVent));

                TextView tempe = (TextView) tagView.findViewById(R.id.textTempe);
                tempe.setTypeface(tf);
                tempe.setText(WeatherIcon.getBeaufort(fc.mVentMoyen));

                TextView textDisplayPlus = (TextView) tagView.findViewById(R.id.textTempeRes);
                textDisplayPlus.setText(fc.mVentMoyen+"km/h");
            }
            break;
        }

        if (tagView != null) {
            //second, set the width and height of inflated view
            tagView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            tagView.layout(0, 0, tagView.getMeasuredWidth(), tagView.getMeasuredHeight());

            //third, finally conversion
            final Bitmap bitmap = Bitmap.createBitmap(tagView.getMeasuredWidth(),
                    tagView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            tagView.draw(canvas);

            return BitmapDescriptorFactory.fromBitmap(bitmap);
        } else
            return null;
    }

    private void cleanMap () {
        if (forecastMarkers == null)
            return;
        for (int i = 0; i < forecastMarkers.size(); i++) {
            forecastMarkers.get(i).removeMarker();
        }
        forecastMarkers.clear ();
    }

    private void centerMap () {
        LatLng center = new LatLng(46.8, 2.3);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 5));
    }

    private class ForecastMarker {
        private MarkerOptions mMarkerOptions;
        private Marker mMarker;
        private Integer mLevel;
        float initialZoom;

        public ForecastMarker (MarkerOptions markerOptions, int level, float zoom) {
            mMarkerOptions = markerOptions;
            mLevel = level;
            mMarker = null;
            initialZoom = zoom;
//            refresh(zoom);
        }

        public void addMarker (GoogleMap map) {
            mMarker = map.addMarker(mMarkerOptions);
            refresh(initialZoom);
        }

        public void refresh (float zoom) {
            if (mMarker == null)
                return;
            switch (mLevel) {
                case 0: // Paris
                    mMarker.setVisible(true);
                    break;
                case 1: // Région
                    mMarker.setVisible(true);
                    break;
                case 2: // Département
                    mMarker.setVisible(zoom > 6.5);
                    break;
                case 3: // Arrondissement
                    mMarker.setVisible(zoom >= 8);
                    break;
                case 4: // Commune
                    mMarker.setVisible(zoom >= 10);
                    break;
            }
        }

        public void removeMarker () {
            if (mMarker == null)
                return;
            mMarker.remove();
        }
    }

    private class computeMarkers extends AsyncTask<String /* params*/, Void /*progress*/, ArrayList<ForecastMarker>/*result*/> {

        String mPeriodText;

        CameraPosition cp;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            cp = mMap.getCameraPosition();
        }

        @Override
        protected ArrayList<ForecastMarker> doInBackground(String... strs) {
            try {
                JSONObject a = new JSONObject(strs[0]);
                JSONObject interval = (JSONObject) a.get("interval");

                String mStartDay = interval.getString("for_start_interval").substring(0, 10);
                SimpleDateFormat formatFrom = new SimpleDateFormat ("dd/MM/yyyy");
                SimpleDateFormat formatTo = new SimpleDateFormat ("EEEE d");
                String mStartHour = interval.getString("for_start_interval").substring(11, 13);
                mPeriodText = formatTo.format(formatFrom.parse(mStartDay))+ " " + dayParts[Integer.parseInt(mStartHour)/6];

                JSONArray forecasts = (JSONArray) a.get("forecast");
                ArrayList<ForecastMap> mEntries = new ArrayList<>();
                for (int i = 0; i < forecasts.length(); i++) {
                    try {
                        JSONObject o = (JSONObject) forecasts.get(i);
                        mEntries.add(new ForecastMap(Integer.parseInt(o.getString("geo_id")),
                                o.getString("geo_name"),
                                null,
                                Double.parseDouble(o.getString("geo_lat")),
                                Double.parseDouble(o.getString("geo_lng")),
                                Integer.parseInt(o.getString("geo_level")),
                                Integer.parseInt(o.getString("for_picto")),
                                o.getString("for_nebu_phrase"),
                                o.getString("for_precip_phrase"),
                                Integer.parseInt(o.getString("for_pression")),
                                Integer.parseInt(o.getString("for_tempe")),
                                Integer.parseInt(o.getString("for_tempe_res")),
                                Integer.parseInt(o.getString("for_vent_moy")),
                                Integer.parseInt(o.getString("for_dir")),
                                o.getBoolean("is_day"),
                                null, null
                        ));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Log.d("Meteo", "nEntries: " + mEntries.size());

                ArrayList<ForecastMarker> mForecastMarkers = new ArrayList<>();
                for (int i = 0; i < mEntries.size(); i++) {
                    ForecastMap fc = mEntries.get(i);
                    MarkerOptions mo = new MarkerOptions().icon(GetCustomBitmapDescriptor(fc))
                            .anchor(0.0f, 0.0f)
                            .position(new LatLng(fc.city.mLat, fc.city.mLng))
                            .title(fc.city.mName);
                    mo.snippet(fc.mNebuPhrase + "\n" + fc.mPrecipPhrase);
                    mForecastMarkers.add(new ForecastMarker(mo, fc.city.mLevel, cp.zoom));
                }

                return mForecastMarkers;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
/*                    mDrawerListView.setAdapter(new CityAdapter(getActivity(), mEntries));
                    if (mProgressBar != null) {
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }
*/
        }

        @Override
        protected void onPostExecute(ArrayList<ForecastMarker> fm) {
            super.onPostExecute(fm);

            cleanMap();

            if (fm != null) {
                for (int i = 0; i < fm.size(); i++) {
                    fm.get(i).addMarker(mMap);
                }
            }
            forecastMarkers = fm;

            TextView textTop = (TextView) findViewById(R.id.textTop);
            if (textTop != null) {
                textTop.setText(mPeriodText);
            }

            loadbar.setVisibility(View.GONE);
        }
    }

    class PopupAdapter implements GoogleMap.InfoWindowAdapter {

        private View popup = null;
        private LayoutInflater inflater = null;

        PopupAdapter(LayoutInflater inflater) {
            this.inflater=inflater;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return(null);
        }

        @SuppressLint("InflateParams")
        @Override
        public View getInfoContents(Marker marker) {
            if (popup == null) {
                popup = inflater.inflate(R.layout.map_popup, null);
            }

            TextView tv = (TextView) popup.findViewById(R.id.title);
            tv.setText(marker.getTitle());

            tv = (TextView) popup.findViewById(R.id.snippet);
            tv.setText(marker.getSnippet());
            return(popup);
        }
    }
}
