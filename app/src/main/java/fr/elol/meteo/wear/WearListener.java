package fr.elol.meteo.wear;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
import java.util.Calendar;

import fr.elol.meteo.data.EphemerisCity;
import fr.elol.meteo.data.ForecastCity;
import fr.elol.meteo.data.InfoCity;
import fr.elol.meteo.data.MenuEntry;
import fr.elol.meteo.helpers.Db;
import fr.elol.meteo.helpers.Preferences;

/**
 * Created by philippe on 03/03/15.
 */
public class WearListener extends WearableListenerService {
    private GoogleApiClient mGoogleApiClient;
    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        Log.d("Meteo", "Message received");

        if ("need-update".equals(messageEvent.getPath())) {
            MenuEntry[] cities = (new Db(this)).getMenu();
            int selected = Preferences.getMenuSelected(this);
            wearSetData (this, mGoogleApiClient, cities[selected]);
        }
    }

    // Wear
    public static void wearSetData (Context context, GoogleApiClient wearClient, MenuEntry city) {
        if (wearClient == null)
            return;
        InfoCity ic = (new Db(context)).infocityGet ((int)city.mGeoid, false);
        if (ic == null)
            return;
        ForecastCity currentFc1h = ic.getForecast(0, 1);
        ForecastCity currentFc6h = ic.getForecast(0, 6);
        EphemerisCity ephemeris = ic.getEphemeris(0);
        EphemerisCity ephemeris1 = ic.getEphemeris(1);
        if (currentFc1h == null || currentFc6h == null || ephemeris == null)  {
            return;
        }

        ArrayList<Integer> pictos = new ArrayList<>();
        ArrayList<Integer> tempes = new ArrayList<>();
        int firstHour = 0;
        for (int i=0; i<12; i++) {
            ForecastCity fc = ic.getForecast(i, 1);
            if (fc != null) {
                pictos.add(fc.mPicto);
                tempes.add(fc.mTempe);
                if (i == 0) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(fc.mStart);
                    firstHour = c.get(Calendar.HOUR_OF_DAY);
                }
            }
        }
        PutDataMapRequest mapReq = PutDataMapRequest.create("/weather");
        mapReq.getDataMap().putString("fr.elol.meteo.weather.name", ic.mCity.mShortName);
        mapReq.getDataMap().putInt("fr.elol.meteo.weather.picto", currentFc1h.mPicto);
        mapReq.getDataMap().putInt("fr.elol.meteo.weather.tempe", currentFc1h.mTempe);
        mapReq.getDataMap().putInt("fr.elol.meteo.weather.tempeMin", currentFc6h.mTempeMin);
        mapReq.getDataMap().putInt("fr.elol.meteo.weather.tempeMax", currentFc6h.mTempeMax);
        mapReq.getDataMap().putInt("fr.elol.meteo.weather.nebu", currentFc1h.mNebu);
        mapReq.getDataMap().putInt("fr.elol.meteo.weather.precip", currentFc1h.mPrecip);
        mapReq.getDataMap().putIntegerArrayList("fr.elol.meteo.weather.pictos", pictos);
        mapReq.getDataMap().putIntegerArrayList("fr.elol.meteo.weather.tempes", tempes);
        mapReq.getDataMap().putInt("fr.elol.meteo.weather.firstHour", firstHour);


        mapReq.getDataMap().putString("fr.elol.meteo.weather.sunrise", ephemeris.mSunrise);
        mapReq.getDataMap().putString("fr.elol.meteo.weather.sunset", ephemeris.mSunset);
        mapReq.getDataMap().putString("fr.elol.meteo.weather.sunrise1", ephemeris1.mSunrise);
        mapReq.getDataMap().putInt("fr.elol.meteo.weather.moonphase", ephemeris.mMoonphase);

        PutDataRequest req = mapReq.asPutDataRequest();
        Wearable.DataApi.putDataItem(wearClient, req);
        Log.d("Meteo Wear", "Data set "+ic.mCity.mShortName);
    }
}
