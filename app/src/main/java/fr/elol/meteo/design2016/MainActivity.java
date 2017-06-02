package fr.elol.meteo.design2016;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.elol.meteo.BuildConfig;
import fr.elol.meteo.InfoTabbedActivity;
import fr.elol.meteo.MapsActivity;
import fr.elol.meteo.R;
import fr.elol.meteo.data.City;
import fr.elol.meteo.data.EphemerisCity;
import fr.elol.meteo.data.ForecastCity;
import fr.elol.meteo.data.InfoCity;
import fr.elol.meteo.data.MenuEntry;
import fr.elol.meteo.helpers.Db;
import fr.elol.meteo.helpers.GetJSONData;
import fr.elol.meteo.helpers.Preferences;
import fr.elol.meteo.helpers.WeatherIcon;
import fr.elol.meteo.wear.WearListener;

/**
 * Created by philippe on 29/11/15.
 */
public class MainActivity extends AppCompatActivity
    implements
        NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final int NAVDRAWER_LAUNCH_DELAY = 150;
    public static final String PARAM_GEOID = "param_geoid";
    private static final String uriPrefix = "http://meteo.elol.fr/geoid/";
    private static final String linkPrefix = "android-app://fr.elol.meteo/http/meteo.elol.fr/geoid/";
    private static final int REQUEST_ADD_CITY = 3001;
    private static final int REQUEST_ADD_CITY_FROM_WIDGET = 3002;

    private static final int MY_PERMISSIONS_REQUEST = 1;

    private DrawerLayout drawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private CollapsingToolbarLayout collapsingToolbar;

    private NavigationView navigation;
    private Menu drawerMenu;
    private Menu optionsMenu;
    private List<View> menuCityActions;

    private TextView fixTitle;
    private TextView fixSubtitle;
    private TextView tempTB;
    private ImageView iconTB;
    private Boolean removableCity = false;
    private Boolean displayMenuTB = true;
    private ProgressBar mProgressBar;

    MyPagerAdapter mPagerAdapter;
    ViewPager mViewPager;

    MenuEntry[] cities;
    InfoCity lastReply;

    // geoid of latest localized position
    public MenuEntry mCurrentPositionCity = null;

    // User wants to see forecast at current location
    private Boolean m_bCurrentLocation = true;

    // GCM
    private static String SENDER_ID = "333579571645";
    GoogleCloudMessaging gcm;
    String regid;
    // xGCM

    // Wear
    private GoogleApiClient wearClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main2016);

        if (savedInstanceState == null && !Preferences.getHasNoted(this)) {

            int usage = Preferences.getUsage(this)+1;
            Preferences.setUsage(this, usage);

            if (usage == 10) {
                AlertDialog.Builder ad = new AlertDialog.Builder (this);
                ad.setTitle (R.string.avis_interesse);
                ad.setMessage (R.string.noter_application);
                ad.setPositiveButton(R.string.oui, new DialogInterface.OnClickListener () {
                    @Override
                    public void onClick(DialogInterface dlg, int arg) {
                        noteApplication();
                    }
                });
                ad.setNegativeButton(R.string.non, new DialogInterface.OnClickListener () {
                    @Override
                    public void onClick(DialogInterface dlg, int arg) {
                        Preferences.setHasNoted(MainActivity.this, true);
                    }
                });
                ad.setNeutralButton(R.string.plus_tard, new DialogInterface.OnClickListener () {
                    @Override
                    public void onClick(DialogInterface dlg, int arg) {
                        Preferences.setUsage(MainActivity.this, 0);
                    }
                });
                ad.setCancelable(false);
                ad.show();

            }
        }


        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Drawer
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.open_drawer, R.string.close_drawer) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                refreshDrawerForecast();

            }
        };
        drawer.setDrawerListener(mDrawerToggle);

        // Navigation
        navigation = (NavigationView) findViewById(R.id.navigation);
        navigation.setNavigationItemSelectedListener(this);
        drawerMenu = navigation.getMenu();

        collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);

        fixTitle = (TextView) findViewById(R.id.fix_title);
        fixSubtitle = (TextView) findViewById(R.id.fix_subtitle);

        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        if (m_bCurrentLocation) {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        ServicesOK = servicesConnected();

        if (ServicesOK) {
            testPermissions();

            // Wear
            wearClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle connectionHint) {
                            Log.d("Météo Wear", "onConnected: " + connectionHint);
                            // Now you can use the Data Layer API
                        }
                        @Override
                        public void onConnectionSuspended(int cause) {
                            Log.d("Météo Wear", "onConnectionSuspended: " + cause);
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.d("Météo Wear", "onConnectionFailed: " + result);
                        }
                    })
                            // Request access only to the Wearable API
                    .addApi(Wearable.API)
                    .build();
            // /Wear
        }



        getSupportActionBar().setTitle(null);

        prepareDisplay();

        toolbarConfig();

        refreshDrawerMenu();

        cities = (new Db(this)).getMenu();

        Bundle bargs = getIntent().getExtras();
        int paramPosition = -1;
        long geoid = 0;
        String newZip = "()";
        String newCity = "()";
        if (bargs != null) {
            geoid = bargs.getLong(PARAM_GEOID);// Open from widget

            if (geoid == 0 && getIntent().getData() != null) {
                Uri data = getIntent().getData();
                if (data.toString().startsWith(uriPrefix)) {
                    String strparam = data.toString().substring(uriPrefix.length());
                    String[] parts = strparam.split("/");
                    if (parts.length == 3) {
                        geoid = Long.parseLong(parts[0]);
                    }
                }
            }

            if (geoid != 0) {
                if (geoid == -1) {
                    // choose new city
                    startActivityForResult(new Intent(MainActivity.this, CitySelectActivity.class), REQUEST_ADD_CITY_FROM_WIDGET);
                } else if (cities != null) {
                    int i = 0;
                    for (MenuEntry city : cities) {
                        if (city.mGeoid == geoid) {
                            paramPosition = i;
                            break;
                        }
                        i++;
                    }
                }
                Log.d("Météo", "param geoid: " + geoid);
            }
        }
/*
        if (paramPosition == -1 && geoid > 0) {
            // add city, from widget or deeplink
            try {
                int pos = (new Db(this)).cityAddToMenu(
                        (int)geoid,
                        newCity,
                        newZip);
                onCitySelected(pos);
                sendRegistrationIdToBackend();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/


        onNavigationItemSelected(drawerMenu.getItem(paramPosition != -1 ? paramPosition : Preferences.getMenuSelected(this)));
    }

    private void startWithPermissions() {
        locationClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(AppIndex.APP_INDEX_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // GCM Registration
        gcm = GoogleCloudMessaging.getInstance(this);
        regid = Preferences.getRegistrationId(this);

        if (regid.isEmpty()) {
            registerInBackground();
        }

    }

    private void noteApplication() {
        Preferences.setHasNoted(MainActivity.this, true);
        final Intent MyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=fr.elol.meteo"));
        startActivity(MyIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ServicesOK) {
            if (locationClient != null) {
                Log.d("Meteo", "locationClient.connect onResume");
                locationClient.connect();
            }
            if (wearClient != null) {
                Log.d("Meteo Wear", "wearClient.connect onResume");
                wearClient.connect();
            }
        }
    }

    @Override
    protected void onPause() {
        if (ServicesOK) {
            if (locationClient != null) {
                Log.d("Meteo", "locationClient.disconnect onPause");
                if (locationClient.isConnecting() || locationClient.isConnected()) {
                    if (appUri != null) {
                        Action viewAction = Action.newAction(Action.TYPE_VIEW, appUriTitle, appUri);
                        AppIndex.AppIndexApi.end(locationClient, viewAction);
                        Log.d("appIndex", "End uri=" + appUri.toString());
                    }
                    locationClient.disconnect();
                }
            }
            if (wearClient != null) {
                Log.d("Meteo Wear", "wearClient.disconnect onPause");
                if (wearClient.isConnected()) {
                    wearClient.disconnect();
                }
            }
        }
        super.onPause();
    }

    public void toolbarConfig() {
        // Reset toolbar scroll
        CoordinatorLayout coordinator = (CoordinatorLayout) findViewById(R.id.content);
        AppBarLayout appbar = (AppBarLayout) findViewById(R.id.appBarLayout);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appbar.getLayoutParams();
        params.setBehavior(new AppBarLayout.Behavior() {
        });
        AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
        int[] consumed = new int[2];
        behavior.onNestedPreScroll(coordinator, appbar, null, 0, -1000, consumed);

        /* Workaround for bug in AppCompat v23.0.0: https://code.google.com/p/android/issues/detail?id=183166 */
        coordinator.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        appbar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            private Boolean displayTB = null;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                int max = appBarLayout.getTotalScrollRange();
                float imageScale = 1 + (float) verticalOffset / max;
                if (imageScale < 36f/100)
                    imageScale = 36f/100;
                imageView.setScaleX(imageScale);
                imageView.setScaleY(imageScale);

                float alpha = ((float) max + verticalOffset) / max;
                completeLayout.setAlpha(5f/4*alpha - 0.25f);

                float tbAlpha = 1f - alpha * 4f/3;
//                Log.d("Meteo", alpha+" "+tbAlpha);
                tempTB.setAlpha(tbAlpha);
                iconTB.setAlpha(tbAlpha);

                if (optionsMenu != null && optionsMenu.size() > 0) {
                    int menuAlpha = Math.max(0, (int)(255*(1-4*(1-alpha))));
                    optionsMenu.getItem(0).getIcon().setAlpha(menuAlpha);
                }


                tempNow.setTranslationX(verticalOffset);
                tempNowInt.setTranslationX(verticalOffset);
                tempNowExt.setTranslationX(verticalOffset);
                tempsDay.setTranslationX(verticalOffset);

                tempNow.setTranslationY(-verticalOffset / 4);
                tempNowInt.setTranslationY(-verticalOffset / 4);
                tempNowExt.setTranslationY(-verticalOffset / 4);
                tempsDay.setTranslationY(-verticalOffset / 4);

                int optionsMenuLimit = 3 * max / 4;
                Boolean newDisplayTB = (verticalOffset < optionsMenuLimit - max);

                displayMenuTB = !newDisplayTB;

                if (newDisplayTB != displayTB) {
                    displayTB = newDisplayTB;
                    tempTB.setVisibility(displayTB ? View.VISIBLE : View.GONE);
                    iconTB.setVisibility(displayTB ? View.VISIBLE : View.GONE);
                    invalidateOptionsMenu();
                }
            }
        });


    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        optionsMenu = menu;
        if (displayMenuTB && removableCity)
            getMenuInflater().inflate(R.menu.meteo, menu);
        else
            getMenuInflater().inflate(R.menu.global, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_del) {
            confirmAndDeleteMEnuEntry ();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ADD_CITY) {
            cities = new Db(this).getMenu();
            refreshDrawerMenu();
            Log.d("Meteo", "display city #"+resultCode);
            onNavigationItemSelected(drawerMenu.getItem(resultCode));
        } else if (requestCode == REQUEST_ADD_CITY_FROM_WIDGET) {
            setResult(RESULT_OK);
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        m_bCurrentLocation = false;

        switch (item.getItemId()) {
            case R.id.navig_at_position:
                drawer.closeDrawer(Gravity.LEFT);
                removableCity = false;
                invalidateOptionsMenu();
                item.setChecked(true);
                Preferences.setMenuSelected(this, cities == null ? 0 : cities.length);

                m_bCurrentLocation = true;
                mCurrentPositionCity = Preferences.getCurrentPositionCity(this);
                if (mCurrentPositionCity != null) {
                    displayCurrentLocationCity();
                } else {
                    if (mProgressBar != null)
                        mProgressBar.setVisibility(View.VISIBLE);
                }
                break;

            case R.id.navig_add_city:
                drawer.closeDrawer(Gravity.LEFT);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivityForResult(new Intent(MainActivity.this, CitySelectActivity.class), REQUEST_ADD_CITY);
                    }
                }, NAVDRAWER_LAUNCH_DELAY);
                break;

            case R.id.navig_map:
                drawer.closeDrawer(Gravity.LEFT);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(MainActivity.this, MapsActivity.class));
                    }
                }, NAVDRAWER_LAUNCH_DELAY);
                break;

            case R.id.navig_informations:
                drawer.closeDrawer(Gravity.LEFT);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(MainActivity.this, InfoTabbedActivity.class));
                    }
                }, NAVDRAWER_LAUNCH_DELAY);
                break;

            case R.id.navig_note:
                noteApplication();
                break;

            default: // city
                removableCity = true;
                invalidateOptionsMenu();
                drawer.closeDrawer(Gravity.LEFT);
                item.setChecked(true);
                if (item.getItemId() < Menu.FIRST+cities.length) {
                    int pos = item.getItemId() - Menu.FIRST;
                    Preferences.setMenuSelected(this, pos);

                    // Try to get info in db
                    if (getLocalInfo(true, cities[pos])) {
                        displayInfo();
                    } else {
                        if (!getDistantInfo(cities[pos])) {
                            if (getLocalInfo (false, cities[pos]))
                                displayInfo ();
                        }
                    }

                    WearListener.wearSetData(this, wearClient, cities[pos]);

                    // Send App Index link
                    if (appUri != null && locationClient != null
                            && (locationClient.isConnected() || locationClient.isConnecting())) {
                        Action viewAction = Action.newAction(Action.TYPE_VIEW, appUriTitle, appUri);
                        AppIndex.AppIndexApi.end(locationClient, viewAction);
                        Log.d("appIndex", "End uri=" + appUri.toString());
                    }
                    MenuEntry city = cities[pos];
                    String str = linkPrefix + city.mGeoid + "/" + city.mZip + "/" + city.mName;
                    appUri = Uri.parse(str);
                    appUriTitle = "Météo " + city.mName + " (" + city.mZip+")";
                    if (locationClient != null
                            && (locationClient.isConnected() || locationClient.isConnecting())) {
                        Action viewAction = Action.newAction(Action.TYPE_VIEW, appUriTitle, appUri);
                        AppIndex.AppIndexApi.start(locationClient, viewAction);
                        Log.d("appIndex", "Start uri=" + appUri.toString());
                    }
                    // End App Index link
                }
        }
        return false;
    }

    public void displayCurrentLocationCity () {
        // Try to get info in db
        if (getLocalInfo(true, mCurrentPositionCity)) {
            displayInfo();
        } else {
            if (!getDistantInfo(mCurrentPositionCity)) {
                if (getLocalInfo (false, mCurrentPositionCity))
                    displayInfo ();
            }
        }
    /*    FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, CityFragment.newInstance(mCurrentPositionCity, CityFragment.ARG_TYPE_CURRENT_LOCATION), TAG_FRAGMENT_CITY)
                .commit();*/
    }

    private void refreshDrawerMenu() {
        MenuEntry[] cities = (new Db(this)).getMenu(true);
        menuCityActions = new ArrayList<>();
        drawerMenu.clear();
        if (cities != null) {
            for (int i=0; i<cities.length; i++) {
                MenuItem item = drawerMenu.add(Menu.NONE, Menu.FIRST+i, Menu.NONE, cities[i].mShortName);
                item.setCheckable(true);
                item.setActionView(R.layout.item_menu_entry_city_2016);
                View v = item.getActionView();
                menuCityActions.add(v);
                ((TextView)v.findViewById(R.id.cp)).setText(cities[i].mZip);
            }
        }
        navigation.inflateMenu(R.menu.drawer);
        refreshDrawerForecast();
    }

    private void refreshDrawerForecast() {
        Log.d("Meteo", "update fc");
        Db db = new Db(this);
        MenuEntry[] cities = db.getMenu(true);
        if (cities != null) {
            for (int i = 0; i < cities.length; i++) {
                ForecastCity fc = db.getCurrentForecast(cities[i].mGeoid, 1);
                if (fc == null)
                    continue;
                View v = menuCityActions.get(i);
                ((TextView)v.findViewById(R.id.tempText)).setText(fc.mTempe+"°");

                Integer picto = fc.mPicto;
                Boolean isDay = db.getIsDay(cities[i].mGeoid);
                if (picto != null) {
                    String str = "icon"+picto+(isDay ? "" : "n")+"_36";
                    int res = getResources().getIdentifier(str, "drawable", getPackageName());
                    ((ImageView)v.findViewById(R.id.iconImg)).setImageResource(res);
                } else {
                    ((ImageView)v.findViewById(R.id.iconImg)).setImageResource(android.R.color.transparent);
                }

            }
        }
    }

    private void confirmAndDeleteMEnuEntry () {
        DialogInterface.OnClickListener dlgClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        doDeleteMEnuEntry();
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Vous allez supprimer les prévisions pour "+lastReply.mCity.mName+" ?")
                .setPositiveButton("Continuer", dlgClickListener)
                .setNegativeButton("Annuler", dlgClickListener)
                .show();

    }

    private void doDeleteMEnuEntry () {
        Db db = new Db (this);
        if (cities != null && Preferences.getMenuSelected(this) < cities.length) { // Can be null if on current location
            db.removeMenuEntry(cities[Preferences.getMenuSelected(this)].mGeoid);
//            sendRegistrationIdToBackend();
            refreshDrawerMenu();
            Preferences.setMenuSelected(this, 1);
            cities = db.getMenu();
            onNavigationItemSelected(drawerMenu.getItem(0));
        }
    }

    /**
     * Get forecast information from webservice
     * @return true if network connection is ok
     */
    private Boolean getDistantInfo (final MenuEntry me) {
        ProgressBar pbar = (ProgressBar) findViewById(R.id.progressBar);
        if (pbar != null) {
            pbar.setVisibility(View.VISIBLE);
        }
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            Integer geoid = me.mGeoid;
            String u = "https://meteo.android.elol.fr/ajax/fc.php?id="+geoid;
            GetJSONData task = new GetJSONData(this, 0, new GetJSONData.CallBackListener() {

                @Override
                public void callback(int range, String str) {
                    try {
                        City city = new City (me);
                        lastReply = new InfoCity (city);
                        JSONObject oall = new JSONObject(str);

                        String station = oall.getString("station");
                        Integer stationid = Integer.parseInt(oall.getString("stationid"));
                        if (!station.equals("")) {
                            city.setStation(station);
                        }
                        city.setStationId(stationid);

                        JSONArray a = (JSONArray) oall.get("forecast");
                        for (int i=0; i<a.length(); i++) {
                            JSONObject o = (JSONObject) a.get(i);
                            Integer duration = Integer.parseInt(o.getString("for_duration"));
                            switch (duration) {
                                case 1:
                                    lastReply.addForecast(
                                            Integer.parseInt(o.getString("for_fiability")),
                                            Integer.parseInt(o.getString("for_picto")),
                                            Integer.parseInt(o.getString("for_nebu")),
                                            o.getString("for_nebu_phrase"),
                                            Integer.parseInt(o.getString("for_precip")),
                                            o.getString("for_precip_phrase"),
                                            Integer.parseInt(o.getString("for_tempe")),
                                            Integer.parseInt(o.getString("for_tempe_res")),
                                            Integer.parseInt(o.getString("for_pression")),
                                            Integer.parseInt(o.getString("for_vent_moy")),
                                            Integer.parseInt(o.getString("for_vent_raf")),
                                            Integer.parseInt(o.getString("for_dir")),
                                            null,
                                            null,
                                            o.getString("for_start"),
                                            o.getString("for_end"),
                                            Integer.parseInt(o.getString("for_duration"))
                                    );
                                    break;

                                case 6:
                                    lastReply.addForecast(
                                            Integer.parseInt(o.getString("for_fiability")),
                                            Integer.parseInt(o.getString("for_picto")),
                                            Integer.parseInt(o.getString("for_nebu")),
                                            o.getString("for_nebu_phrase"),
                                            Integer.parseInt(o.getString("for_precip")),
                                            o.getString("for_precip_phrase"),
                                            Integer.parseInt(o.getString("for_tempe")),
                                            Integer.parseInt(o.getString("for_tempe_res")),
                                            Integer.parseInt(o.getString("for_pression")),
                                            Integer.parseInt(o.getString("for_vent_moy")),
                                            Integer.parseInt(o.getString("for_vent_raf")),
                                            Integer.parseInt(o.getString("for_dir")),
                                            Integer.parseInt(o.getString("for_tempe_min")),
                                            Integer.parseInt(o.getString("for_tempe_max")),
                                            o.getString("for_start"),
                                            o.getString("for_end"),
                                            Integer.parseInt(o.getString("for_duration"))
                                    );
                                    break;

                                case 24:
                                    lastReply.addForecast(
                                            Integer.parseInt(o.getString("for_fiability")),
                                            Integer.parseInt(o.getString("for_picto")),
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            Integer.parseInt(o.getString("for_tempe_min")),
                                            Integer.parseInt(o.getString("for_tempe_max")),
                                            o.getString("for_start"),
                                            o.getString("for_end"),
                                            Integer.parseInt(o.getString("for_duration"))
                                    );
                                    break;
                            }
                        }

                        a = (JSONArray) oall.get("eph");
                        for (int i=0; i<a.length(); i++) {
                            JSONObject o = (JSONObject) a.get(i);
                            String strMoon4 = o.getString("moon4");
                            Integer moon4;
                            try {
                                moon4 = Integer.parseInt(strMoon4);
                            } catch (Exception e) {
                                moon4 = null;
                            }
                            lastReply.addEphemeris(o.getString("d"),
                                    o.getString("sunrise"),
                                    o.getString("sunset"),
                                    o.getString("moonrise"),
                                    o.getString("moonset"),
                                    Integer.parseInt(o.getString("moonphase")),
                                    moon4
                            );
                        }
                        (new Db(MainActivity.this)).infocitySave(lastReply);
                        displayInfo();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            task.execute(u);
            return true;
//        } else {
            // display error
        } else {
            return false;
        }

    }

    private Boolean getLocalInfo (Boolean updated, MenuEntry city) {
        Integer geoid = city.mGeoid;
        InfoCity ic = (new Db(this)).infocityGet(geoid, updated);
        if (ic != null) {
            lastReply = ic;
            return true;
        }
        return false;
    }

    private Typeface tf;
    private RelativeLayout completeLayout;
    private ImageView imageView;
    private TextView tempNowExt;
    private TextView tempNowInt;
    private TextView tempNow;
    private TextView condition1;
    private TextView condition2;
    private TextView tempsDay;
    private TextView info;
    private TextView windIcon;
    private TextView beaufortIcon;
    private TextView wind;
    private TextView sunriseIcon;
    private TextView sunriseText;
    private TextView sunsetIcon;
    private TextView sunsetText;
    private TextView moonPhase;

    private void prepareDisplay() {
        tf = Typeface.createFromAsset(getAssets(), "fonts/weathericons.ttf");
        completeLayout = (RelativeLayout) findViewById(R.id.layout);
        imageView = (ImageView) findViewById(R.id.imageView);
        tempNowExt = (TextView) findViewById(R.id.tempNowExt);
        tempNowExt.setTypeface(tf);
        tempNowInt = (TextView) findViewById(R.id.tempNowInt);
        tempNowInt.setTypeface(tf);
        tempNow = (TextView) findViewById(R.id.tempNow);
        tempNow.setTypeface(tf);
        condition1 = (TextView) findViewById(R.id.conditionText1);
        condition2 = (TextView) findViewById(R.id.conditionText2);
        tempsDay = (TextView) findViewById(R.id.tempsDay);
        tempsDay.setTypeface(tf);
        info = (TextView) findViewById(R.id.infoText);
        info.setTypeface(tf);
        windIcon = (TextView) findViewById(R.id.windIcon);
        windIcon.setTypeface(tf);
        beaufortIcon = (TextView) findViewById(R.id.beaufortIcon);
        beaufortIcon.setTypeface(tf);
        wind = (TextView) findViewById(R.id.windText);
        wind.setTypeface(tf);
        sunriseIcon = (TextView) findViewById(R.id.sunriseIcon);
        sunriseIcon.setTypeface(tf);
        sunriseText = (TextView) findViewById(R.id.sunriseText);
        sunriseText.setTypeface(tf);
        sunsetIcon = (TextView) findViewById(R.id.sunsetIcon);
        sunsetIcon.setTypeface(tf);
        sunsetText = (TextView) findViewById(R.id.sunsetText);
        sunsetText.setTypeface(tf);
        moonPhase = (TextView) findViewById(R.id.moonPhase);
        moonPhase.setTypeface(tf);

        tempTB = (TextView) findViewById(R.id.tempTB);
        tempTB.setTypeface(tf);
        iconTB = (ImageView) findViewById(R.id.iconTB);

    }

    private void displayInfo() {

        if (lastReply == null)
            return;

        // Titre, sous-titre
        fixTitle.setText(lastReply.mCity.mName);
        if (lastReply.mCity.mStation != null) {
            fixSubtitle.setVisibility(View.VISIBLE);
            fixSubtitle.setText("Prévisions pour " + lastReply.mCity.mStation);
        } else {
            fixSubtitle.setVisibility(View.GONE);
            fixSubtitle.setText(null);
        }

        // Pager
        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), lastReply);
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);
        Log.d("Meteo", "set tab " + Preferences.getTab(this));
        final Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mViewPager.getChildCount() == 0)
                    mHandler.postDelayed(this, 50);
                else
                    mViewPager.setCurrentItem(Preferences.getTab(MainActivity.this), false);
            }
        }, 50);
        //        mViewPager.setCurrentItem(Preferences.getTab(this));
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                Preferences.setTab(MainActivity.this, position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        // Infos du moment (collapsed)
        ForecastCity currentFc1h = lastReply.getForecast(0, 1);
        ForecastCity currentFc6h = lastReply.getForecast(0, 6);
        EphemerisCity ephemeris = lastReply.getEphemeris(0);

        if (currentFc1h == null || currentFc6h == null)
            return;

        // Condition image

        String str = "icon" + currentFc1h.mPicto + (ephemeris.isDay(new Date()) ? "" : "n") + "_100";
        int res = getResources().getIdentifier(str, "drawable", getPackageName());
        imageView.setImageResource(res);
        str = "icon" + currentFc1h.mPicto + (ephemeris.isDay(new Date()) ? "" : "n") + "_36";
        res = getResources().getIdentifier(str, "drawable", getPackageName());
        iconTB.setImageResource(res);


        // Temp now exterior icon
        tempNowExt.setText("\uf053");

        // Temp now interior icon
        tempNowInt.setText("\uf054");
        if (currentFc1h.mTempe > 0) {
            tempNowInt.setTextColor(getResources().getColor(R.color.red));
        } else {
            tempNowInt.setTextColor(getResources().getColor(R.color.blue));
        }

        // Temperature now
        tempNow.setText(currentFc1h.mTempe + "\uf03c");
        tempTB.setText(currentFc1h.mTempe + "\uf042");

        // Condition text
        condition1.setText("Nébulosité : " + currentFc1h.mNebu + " %");
        condition2.setText("Cumul pluies : " + ((float) currentFc1h.mPrecip / 10) + " mm");

        // Temperatures day
        tempsDay.setText(currentFc6h.mTempeMax + "\uf042\n" + currentFc6h.mTempeMin + "\uf042");

        // Info text
        info.setText(currentFc1h.mTempeRes + "\uf03c ressenti" + "\nP.A. " + currentFc1h.mPression + " hPa");

        // Wind Icon
        windIcon.setText(WeatherIcon.getDirVent(currentFc1h.mDirVent));

        // Beaufort Icon
        beaufortIcon.setText(WeatherIcon.getBeaufort(currentFc1h.mVentMoyen));

        // Wind text
        wind.setText("moy. " + currentFc1h.mVentMoyen + " km/h" + "\nraf. " + currentFc1h.mVentRaf + " km/h");

        // Sun rise/set
        sunriseIcon.setText("\uf051");
        sunriseText.setText(ephemeris.mSunrise);
        sunsetIcon.setText("\uf052");
        sunsetText.setText(ephemeris.mSunset);


        // Moon phase
        moonPhase.setText(WeatherIcon.getMoonphase(ephemeris.mMoonphase));

        TextView moonriseIcon;
        TextView moonriseText;
        TextView moonsetIcon;
        TextView moonsetText;
        if (ephemeris.moonRiseFirst()) {
            moonriseIcon = (TextView) findViewById(R.id.moon1Icon);
            moonriseText = (TextView) findViewById(R.id.moon1Text);
            moonsetIcon = (TextView) findViewById(R.id.moon2Icon);
            moonsetText = (TextView) findViewById(R.id.moon2Text);
        } else {
            moonriseIcon = (TextView) findViewById(R.id.moon2Icon);
            moonriseText = (TextView) findViewById(R.id.moon2Text);
            moonsetIcon = (TextView) findViewById(R.id.moon1Icon);
            moonsetText = (TextView) findViewById(R.id.moon1Text);
        }
        if (moonriseIcon != null) {
            moonriseIcon.setTypeface(tf);
            moonriseIcon.setText("\uf058");
        }
        if (ephemeris.mMoonrise != null) {
            if (moonriseText != null) {
                moonriseText.setTypeface(tf);
                moonriseText.setText(ephemeris.mMoonrise);
            }
        } else {
            EphemerisCity eph2 = lastReply.getEphemeris(1);
            if (moonriseText != null) {
                moonriseText.setTypeface(tf);
                moonriseText.setText(eph2.mMoonrise + " (+1)");
            }
        }

        if (moonsetIcon != null) {
            moonsetIcon.setTypeface(tf);
            moonsetIcon.setText("\uf044");
        }
        if (ephemeris.mMoonset != null) {
            if (moonsetText != null) {
                moonsetText.setTypeface(tf);
                moonsetText.setText(ephemeris.mMoonset);
            }
        } else {
            EphemerisCity eph2 = lastReply.getEphemeris(1);
            if (moonsetText != null) {
                moonsetText.setTypeface(tf);
                moonsetText.setText(eph2.mMoonset + " (+1)");
            }
        }

        ProgressBar pbar = (ProgressBar) findViewById(R.id.progressBar);
        if (pbar != null) {
            pbar.setVisibility(View.GONE);
        }
    }

    public class MyPagerAdapter extends FragmentStatePagerAdapter {
        InfoCity lastReply;

        public MyPagerAdapter(FragmentManager fm, InfoCity lastReply) {
            super(fm);
            this.lastReply = lastReply;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            Log.d("Meteo", "get tab item "+position);
            return MyTabbedFragment.newInstance(position + 1, lastReply);
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.taball);
                case 1:
                    return getString(R.string.tab1h);
                case 2:
                    return getString(R.string.tab6h);
                case 3:
                    return getString(R.string.tab24h);
            }
            return null;
        }
    }

    public static class MyTabbedFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static MyTabbedFragment newInstance(int sectionNumber, InfoCity lastReply) {
            MyTabbedFragment fragment = new MyTabbedFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            args.putSerializable("lastReply", lastReply);
            fragment.setArguments(args);
            return fragment;
        }

        public MyTabbedFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            Bundle args = getArguments();
            int position = args.getInt(ARG_SECTION_NUMBER);
            View rootView = inflater.inflate(R.layout.activity_main_tab_2016, container, false);
            RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.listView);
            mRecyclerView.setHasFixedSize(false);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mRecyclerView.setItemAnimator(new DefaultItemAnimator());

            if (mRecyclerView != null) {
                switch (position) {
                    case 1:
                        mRecyclerView.setAdapter(new ForecastAllRecyclerAdapter(getActivity(), (InfoCity) getArguments().getSerializable("lastReply")));
                        break;
                    case 2:
                        mRecyclerView.setAdapter(new Forecast1hRecyclerAdapter(getActivity(), (InfoCity) getArguments().getSerializable("lastReply")));
                        break;
                    case 3:
                        mRecyclerView.setAdapter(new Forecast6hRecyclerAdapter(getActivity(), (InfoCity) getArguments().getSerializable("lastReply")));
                        break;
                    case 4:
                        mRecyclerView.setAdapter(new Forecast24hRecyclerAdapter(getActivity(), (InfoCity) getArguments().getSerializable("lastReply")));
                        break;
                }
            }
            return rootView;
        }

    }

    // Google Services

    /**
     * Localisation by Google Play Services
     */
    private Boolean ServicesOK = false;
    private GoogleApiClient locationClient;
    private Uri appUri = null;
    private String appUriTitle;

    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        // Return a Dialog to the DialogFragment.

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates",
                    "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason.
            // resultCode holds the error code.
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getSupportFragmentManager(),
                        "Location Updates");
            }
            return false;
        }
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Log.d("Meteo", "LocationClient connected");
        if (BuildConfig.HAS_GS) {
            Location loc = LocationServices.FusedLocationApi.getLastLocation(locationClient);
            if (loc == null) {
                mProgressBar.setVisibility(View.GONE);
            }

            LocationRequest req;
            req = LocationRequest.create();
            // Use high accuracy
            req.setPriority(
                    LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            // Set the update interval to 15 minutes
            req.setInterval(15 * 60 * 1000);
            // Set the fastest update interval to 15 minutes
            req.setFastestInterval(15 * 60 * 1000);
            LocationServices.FusedLocationApi.requestLocationUpdates(locationClient, req, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Display the connection status
        Log.d("Meteo", "LocationClient connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        Log.d("Meteo", "connection failed");
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        Log.d("Meteo", "Update Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());

        Location oldLocation = Preferences.getOldLocation(this);

        if (oldLocation != null) {
            Log.d("Météo", "Distance: "+oldLocation.distanceTo(location));
        }
        if (oldLocation == null || oldLocation.distanceTo(location) > 1000) {
            Preferences.setOldLocation(this, location);

            String u = "https://meteo.android.elol.fr/ajax/geoname_proche.php?lat=" + location.getLatitude() + "&lng=" + location.getLongitude();
            GetJSONData task = new GetJSONData(this, 0, new GetJSONData.CallBackListener() {

                @Override
                public void callback(int range, String str) {
                    try {
                        JSONArray a = new JSONArray(str);
                        JSONObject o = (JSONObject) a.get(0);
                        Integer geoid = o.getInt("geoid");
                        String name = o.getString("name");
                        if (mCurrentPositionCity == null || !mCurrentPositionCity.mName.equals(name)) {
                            mCurrentPositionCity = new MenuEntry(geoid, name, null, null, null, null);
                            Preferences.setCurrentPositionCity(MainActivity.this, mCurrentPositionCity);
                            if (m_bCurrentLocation) {
                                if (mProgressBar != null)
                                    mProgressBar.setVisibility(View.GONE);
                                displayCurrentLocationCity();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            task.execute(u);
        }
    }


    // GCM

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void,String,String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg;
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(MainActivity.this);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // For this demo: we don't need to send it because the device
                    // will send upstream messages to a server that echo back the
                    // message using the 'from' address in the message.

                    // Persist the regID - no need to register again.
                    Preferences.storeRegistrationId(MainActivity.this, regid);

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    sendRegistrationIdToBackend();

                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.d("Météo", "msg: "+msg);
//                mDisplay.append(msg + "\n");
            }
        }.execute(null, null, null);
    }

    public void sendRegistrationIdToBackend () {
        String key = Preferences.getRegistrationId(this);
        String url = "https://meteo.android.elol.fr/ajax/register.php?key="+key;
        MenuEntry[] entries = (new Db(this)).getMenu();
        if (entries != null) {
            for (MenuEntry entry : entries) {
                url += "&geo[]=" + entry.mGeoid;
            }
        }
        Log.d("Météo", "url: "+url);
        GetJSONData task = new GetJSONData(this, 0, null);
        task.execute(url);
    }

    // Permissions
    private void testPermissions() {
        Boolean locationPermissionCheck = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        Boolean accountsPermissionCheck = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED);

        if (locationPermissionCheck && accountsPermissionCheck) {
            Log.d("GEN", "Position & Accounts Permissions OK");
            startWithPermissions();
        } else {
            Log.d("GEN", "Position Permission NOT OK");
            ArrayList<String> neededPermissions = new ArrayList<>();
            if (!locationPermissionCheck)
                neededPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (!accountsPermissionCheck)
                neededPermissions.add(Manifest.permission.GET_ACCOUNTS);
            String[] simpleArray = new String[ neededPermissions.size() ];
            neededPermissions.toArray(simpleArray);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(simpleArray, MY_PERMISSIONS_REQUEST);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                Boolean locationGranted = false;
                Boolean accountsGranted = false;
                for(int i=0; i<permissions.length; i++) {
                    if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i])) {
                        locationGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    } else if (Manifest.permission.GET_ACCOUNTS.equals(permissions[i])) {
                        accountsGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    }
                }
                if (locationGranted && accountsGranted) {
                    startWithPermissions();
                } else {
                    Log.d("GEN", "Position Permission NOT Granted");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    // TODO
                }
                return;
            }

        }
    }

}
