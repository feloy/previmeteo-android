package fr.elol.meteo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;

/**
 * Created by philippe on 28/02/15.
 */
public class WeatherData {
    public String mSenderNodeId;

    public String mCityShortname;
    public int mPicto;
    public int mTempe;
    public int mTempeMin;
    public int mTempeMax;
    public int mNebu;
    public int mPrecip;

    public String mSunrise;
    public String mSunset;
    public String mSunrise1;
    public int mMoonphase;

    public int mFirstHour;
    public ArrayList<Integer> mPictos;
    public ArrayList<Integer> mTempes;

    public static final String KEY_SENDER = "fr.elol.meteo.weather.sender";
    public static final String KEY_NAME = "fr.elol.meteo.weather.name";
    public static final String KEY_PICTO = "fr.elol.meteo.weather.picto";
    public static final String KEY_TEMPE = "fr.elol.meteo.weather.tempe";
    public static final String KEY_TEMPE_MIN = "fr.elol.meteo.weather.tempeMin";
    public static final String KEY_TEMPE_MAX = "fr.elol.meteo.weather.tempeMax";
    public static final String KEY_NEBU = "fr.elol.meteo.weather.nebu";
    public static final String KEY_PRECIP = "fr.elol.meteo.weather.precip";

    public static final String KEY_SUNRISE = "fr.elol.meteo.weather.sunrise";
    public static final String KEY_SUNSET = "fr.elol.meteo.weather.sunset";
    public static final String KEY_SUNRISE1 = "fr.elol.meteo.weather.sunrise1";
    public static final String KEY_MOONPHASE = "fr.elol.meteo.weather.moonphase";

    public static final String KEY_FIRST_HOUR = "fr.elol.meteo.weather.firstHour";
    public static final String KEY_PICTOS = "fr.elol.meteo.weather.pictos";
    public static final String KEY_TEMPES = "fr.elol.meteo.weather.tempes";

    public static final int MAX_TITLE_LENGTH = 14;

    public WeatherData (String senderNodeId, String cityShortname, int picto, int tempe, int tempeMin, int tempeMax,
                        int nebu, int precip,
                        String sunrise, String sunset, String sunrise1, int moonphase,
                        int firstHour, ArrayList<Integer> pictos, ArrayList<Integer> tempes) {

        mSenderNodeId = senderNodeId;

        mCityShortname = cityShortname;
        if (mCityShortname.length() > MAX_TITLE_LENGTH) {
            mCityShortname = mCityShortname.substring(0, MAX_TITLE_LENGTH) + "\u2026";
        }
        mPicto = picto;
        mTempe = tempe;
        mTempeMin = tempeMin;
        mTempeMax = tempeMax;
        mNebu = nebu;
        mPrecip = precip;

        mSunrise = sunrise;
        mSunset = sunset;
        mSunrise1 = sunrise1;
        mMoonphase = moonphase;

        mFirstHour = firstHour;
        mPictos = pictos;
        mTempes = tempes;
    }

    public void save (Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_SENDER, mSenderNodeId);
        editor.putString(KEY_NAME, mCityShortname);
        editor.putInt(KEY_PICTO, mPicto);
        editor.putInt(KEY_TEMPE, mTempe);
        editor.putInt(KEY_TEMPE_MIN, mTempeMin);
        editor.putInt(KEY_TEMPE_MAX, mTempeMax);
        editor.putInt(KEY_NEBU, mNebu);
        editor.putInt(KEY_PRECIP, mPrecip);

        editor.putString(KEY_SUNRISE, mSunrise);
        editor.putString(KEY_SUNSET, mSunset);
        editor.putString(KEY_SUNRISE1, mSunrise1);
        editor.putInt(KEY_MOONPHASE, mMoonphase);

        editor.putInt(KEY_FIRST_HOUR, mFirstHour);
        for (int i=0; i<12; i++) {
            editor.putInt(KEY_PICTOS + i, mPictos.get(i));
            editor.putInt(KEY_TEMPES + i, mTempes.get(i));
        }
        editor.apply();
    }

    public static WeatherData load (Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        ArrayList<Integer> pictos = new ArrayList<>();
        ArrayList<Integer> tempes = new ArrayList<>();
        for (int i=0; i<12; i++) {
            pictos.add(sp.getInt(KEY_PICTOS + i, 0));
            tempes.add(sp.getInt(KEY_TEMPES + i, 0));
        }
        return new WeatherData (
                sp.getString(KEY_SENDER, ""),
                sp.getString(KEY_NAME, ""),
                sp.getInt(KEY_PICTO, 0),
                sp.getInt(KEY_TEMPE, 0),
                sp.getInt(KEY_TEMPE_MIN, 0),
                sp.getInt(KEY_TEMPE_MAX, 0),
                sp.getInt(KEY_NEBU, 0),
                sp.getInt(KEY_PRECIP, 0),

                sp.getString(KEY_SUNRISE, ""),
                sp.getString(KEY_SUNSET, ""),
                sp.getString(KEY_SUNRISE1, ""),
                sp.getInt(KEY_MOONPHASE, 0),

                sp.getInt(KEY_FIRST_HOUR, 0),
                pictos, tempes);
    }
}
