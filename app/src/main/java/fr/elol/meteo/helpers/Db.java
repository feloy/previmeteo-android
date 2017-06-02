package fr.elol.meteo.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import fr.elol.meteo.data.City;
import fr.elol.meteo.data.EphemerisCity;
import fr.elol.meteo.data.ForecastCity;
import fr.elol.meteo.data.InfoCity;
import fr.elol.meteo.data.MenuEntry;

/**
 * Datadabse helper
 */
public class Db extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "meteo";
    private static final int DATABASE_VERSION = 11;

    // Tables names
    private static final String TABLE_MENU = "menu"; // List of cities in menu
    private static final String TABLE_CITY = "city";
    private static final String TABLE_FORECAST = "forecast";
    private static final String TABLE_EPHEMERIS = "ephemeris";

    // Columns names
    private static final String
        GEO_ID = "geo_id",
        GEO_NAME = "geo_name",
        GEO_STATION = "geo_station",
        GEO_STATION_ID = "geo_station_id",
        GEO_LAT = "geo_lat",
        GEO_LNG = "geo_lng",
        GEO_ELEVATION = "geo_elevation",
        GEO_ZIP = "geo_zip",
        GEO_LAST_UPDATE = "geo_last_update";

    private static final String
        FOR_ID = "for_id",
        FOR_START = "for_start",
        FOR_END = "for_end",
        FOR_DURATION = "for_duration",
        FOR_FIABILITY = "for_fiability",
        FOR_PICTO = "for_picto",
        FOR_NEBU_PC = "for_nebu_pc",
        FOR_NEBU = "for_nebu",
        FOR_PRECIP_MMM = "for_precip_mmm",
        FOR_PRECIP = "for_precip",
        FOR_TEMPE = "for_tempe",
        FOR_TEMPE_RES = "for_tempe_res",
        FOR_PRESSION = "for_pression",
        FOR_VENT_MOYEN = "for_vent_moyen",
        FOR_VENT_RAF = "for_vent_raf",
        FOR_DIR_VENT = "for_dir_vent",
        FOR_TEMPE_MIN = "for_tempe_min",
        FOR_TEMPE_MAX = "for_tempe_max";

    private static final String
        EPH_ID = "eph_id",
        EPH_DATE = "eph_date",
        EPH_SUNRISE = "eph_sunrise",
        EPH_SUNSET = "eph_sunset",
        EPH_MOONRISE = "eph_moonrise",
        EPH_MOONSET = "eph_moonset",
        EPH_MOONPHASE = "eph_moonphase",
        EPH_MOON4 = "eph_moon4";

    private static final String CREATE_TABLE_MENU =
            "CREATE TABLE " + TABLE_MENU + "("
            + GEO_ID + " INTEGER PRIMARY KEY, "
            + GEO_NAME + " TEXT, "
            + GEO_ZIP + " TEXT"
            + ")";

    private static final String CREATE_TABLE_CITY =
            "CREATE TABLE " + TABLE_CITY + "("
                    + GEO_ID + " INTEGER PRIMARY KEY, "
                    + GEO_NAME + " TEXT, "
                    + GEO_STATION + " TEXT, "
                    + GEO_STATION_ID + " INTEGER, "
                    + GEO_LAT + " INTEGER, " // x 100.000
                    + GEO_LNG + " INTEGER, " // x 100.000
                    + GEO_ELEVATION + " INTEGER, "
                    + GEO_ZIP + " TEXT,"
                    + GEO_LAST_UPDATE + " INTEGER"
                    + ")";

    private static final String CREATE_TABLE_FORECAST =
            "CREATE TABLE " + TABLE_FORECAST + "("
            + FOR_ID + " INTEGER PRIMARY KEY, "
            + GEO_ID + " INTEGER, "
            + FOR_START + " INTEGER, " // timestamp in seconds
            + FOR_END + " INTEGER, " // timestamp in seconds
            + FOR_DURATION + " INTEGER, " // in hours
            + FOR_FIABILITY + " INTEGER, "
            + FOR_PICTO + " INTEGER, "
            + FOR_NEBU_PC + " INTEGER, "
            + FOR_NEBU + " TEXT, "
            + FOR_PRECIP_MMM + " INTEGER, "
            + FOR_PRECIP + " TEXT, "
            + FOR_TEMPE + " INTEGER, "
            + FOR_TEMPE_RES + " INTEGER, "
            + FOR_PRESSION + " INTEGER, "
            + FOR_VENT_MOYEN + " INTEGER, "
            + FOR_VENT_RAF + " INTEGER, "
            + FOR_DIR_VENT + " INTEGER, "
            + FOR_TEMPE_MIN + " INTEGER, "
            + FOR_TEMPE_MAX + " INTEGER"
            + ")";

    private static final String CREATE_TABLE_EPHEMERIS =
            "CREATE TABLE " + TABLE_EPHEMERIS + "("
            + EPH_ID + " INTEGER PRIMARY KEY, "
            + GEO_ID + " INTEGER, "
            + EPH_DATE + " INTEGER, "
            + EPH_SUNRISE + " TEXT, "
            + EPH_SUNSET + " TEXT, "
            + EPH_MOONRISE + " TEXT, "
            + EPH_MOONSET + " TEXT, "
            + EPH_MOONPHASE + " INTEGER, "
            + EPH_MOON4 + " INTEGER"
            + ")";

    public Db (Context ctx) {
        super (ctx, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MENU);
        db.execSQL(CREATE_TABLE_CITY);
        db.execSQL(CREATE_TABLE_FORECAST);
        db.execSQL(CREATE_TABLE_EPHEMERIS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MENU);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CITY);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FORECAST);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EPHEMERIS);
            onCreate(db);
        }
    }

    /*
     * Add a city and returns its range, in alphabetic order
     */
    public int cityAddToMenu(int id, String name, String zip) {
        int ret = 0;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values;

        Cursor cursor = db.query(TABLE_MENU,
                new String[] { GEO_ID }, GEO_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);

        Boolean found = (cursor != null && cursor.getCount() > 0);

        if (!found) {
            values = new ContentValues();
            values.put(GEO_ID, id);
            values.put(GEO_NAME, name);
            values.put(GEO_ZIP, zip);
            db.insert(TABLE_MENU, null, values);
        }

        MenuEntry[] all = getMenu ();
        for (int i=0; i < all.length; i++) {
            if (all[i].mGeoid == id) {
                ret = i;
            }
        }
        db.close ();
        return ret;
    }

    public MenuEntry[] getMenu() {
        return getMenu(false);
    }

    public MenuEntry[] getMenu(Boolean withForecast) {
        MenuEntry[] ret = null;
        String q = "SELECT * FROM " + TABLE_MENU + " ORDER BY " + GEO_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(q, null);
        if (c.moveToFirst()) {
            int i=0;
            ret = new MenuEntry[c.getCount()];
            do {
                ret[i] = new MenuEntry(c.getInt(c.getColumnIndex(GEO_ID)),
                                  c.getString(c.getColumnIndex(GEO_NAME)),
                                  c.getString(c.getColumnIndex(GEO_ZIP)),
                                  null, null, null);
                if (withForecast) {
                    ForecastCity fc = getCurrentForecast(ret[i].mGeoid, 1);
                    if (fc != null) {
                        ret[i].mPicto = fc.mPicto;
                        ret[i].mTemp = fc.mTempe;
                        ret[i].mIsDay = getIsDay(ret[i].mGeoid);
                    }
                }
                i++;
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return ret;
    }

    public void infocitySave (InfoCity ic) {
        // TODO in another thread
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values;
        values = new ContentValues();
        values.put(GEO_ID, ic.mCity.mGeoid);
        values.put(GEO_NAME, ic.mCity.mName);
        values.put(GEO_STATION, ic.mCity.mStation != null ? ic.mCity.mStation : "");
        values.put(GEO_STATION_ID, ic.mCity.mStationId);
        if (ic.mCity.mLat != null)
            values.put(GEO_LAT, ic.mCity.mLat * 100000.0);
        if (ic.mCity.mLng != null)
            values.put(GEO_LNG, ic.mCity.mLng * 100000.0);
        if (ic.mCity.mElevation != null)
            values.put(GEO_ELEVATION, ic.mCity.mElevation);
        if (ic.mCity.mZip != null)
            values.put(GEO_ZIP, ic.mCity.mZip);
        values.put(GEO_LAST_UPDATE, (new Date()).getTime() / 1000);
        db.insertWithOnConflict(TABLE_CITY, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        // delete all previous forecasts for this city
        db.delete(TABLE_FORECAST, GEO_ID + " = ?", new String[] { ""+ ic.mCity.mGeoid });

        // Save forecasts
        for (int i=0; i<ic.mForecastList.size(); i++) {
            ForecastCity fc = ic.mForecastList.get(i);
            values = new ContentValues();
            values.put(GEO_ID, ic.mCity.mGeoid);
            values.put(FOR_START, fc.mStart.getTime() / 1000);
            values.put(FOR_END, fc.mEnd.getTime() / 1000);
            values.put(FOR_DURATION, fc.mDuration);
            values.put(FOR_FIABILITY, fc.mFiability);
            values.put(FOR_PICTO, fc.mPicto);
            values.put(FOR_NEBU_PC, fc.mNebu);
            values.put(FOR_NEBU, fc.mNebuPhrase);
            values.put(FOR_PRECIP_MMM, fc.mPrecip);
            values.put(FOR_PRECIP, fc.mPrecipPhrase);
            values.put(FOR_TEMPE, fc.mTempe);
            values.put(FOR_TEMPE_RES, fc.mTempeRes);
            values.put(FOR_PRESSION, fc.mPression);
            values.put(FOR_VENT_MOYEN, fc.mVentMoyen);
            values.put(FOR_VENT_RAF, fc.mVentRaf);
            values.put(FOR_DIR_VENT, fc.mDirVent);
            values.put(FOR_TEMPE_MIN, fc.mTempeMin);
            values.put(FOR_TEMPE_MAX, fc.mTempeMax);
            db.insert(TABLE_FORECAST, null, values);
        }

        // delete previous ephemeris for this city
        db.delete(TABLE_EPHEMERIS, GEO_ID + " = ?", new String[] { ""+ic.mCity.mGeoid });

        // save ephemeris
        for (int i=0; i<ic.mEphemerisList.size(); i++) {
            EphemerisCity eph = ic.mEphemerisList.get(i);
            values = new ContentValues();
            values.put(GEO_ID, ic.mCity.mGeoid);
            values.put(EPH_DATE, eph.mDate.getTime() / 1000);
            values.put(EPH_SUNRISE, eph.mSunrise);
            values.put(EPH_SUNSET, eph.mSunset);
            values.put(EPH_MOONRISE, eph.mMoonrise);
            values.put(EPH_MOONSET, eph.mMoonset);
            values.put(EPH_MOONPHASE, eph.mMoonphase);
            values.put(EPH_MOON4, eph.mMoon4);
            db.insert(TABLE_EPHEMERIS, null, values);
        }

        db.close ();
    }

    /**
     * Save forecasts received from GCM
     * @param stationId
     * @param duration
     * @param forecastList
     */
    public void forecastListSave (Integer stationId, Integer duration, ArrayList<ForecastCity> forecastList) {
        String q = "SELECT * FROM " + TABLE_CITY + " WHERE " + GEO_STATION_ID + " = ?";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery(q, new String[]{String.valueOf(stationId)});
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            do {
                Integer geoId = c.getInt(c.getColumnIndex(GEO_ID));

                // delete all previous forecasts for this city and duration
                db.delete(TABLE_FORECAST, GEO_ID + " = ? AND " + FOR_DURATION + " = ?", new String[] { ""+ geoId, ""+duration });

                for (ForecastCity fc: forecastList) {
                    ContentValues values = new ContentValues();
                    values.put(GEO_ID, geoId);
                    values.put(FOR_START, fc.mStart.getTime() / 1000);
                    values.put(FOR_END, fc.mEnd.getTime() / 1000);
                    values.put(FOR_DURATION, fc.mDuration);
                    values.put(FOR_FIABILITY, fc.mFiability);
                    values.put(FOR_PICTO, fc.mPicto);
                    values.put(FOR_NEBU_PC, fc.mNebu);
                    values.put(FOR_NEBU, fc.mNebuPhrase);
                    values.put(FOR_PRECIP_MMM, fc.mPrecip);
                    values.put(FOR_PRECIP, fc.mPrecipPhrase);
                    values.put(FOR_TEMPE, fc.mTempe);
                    values.put(FOR_TEMPE_RES, fc.mTempeRes);
                    values.put(FOR_PRESSION, fc.mPression);
                    values.put(FOR_VENT_MOYEN, fc.mVentMoyen);
                    values.put(FOR_VENT_RAF, fc.mVentRaf);
                    values.put(FOR_DIR_VENT, fc.mDirVent);
                    values.put(FOR_TEMPE_MIN, fc.mTempeMin);
                    values.put(FOR_TEMPE_MAX, fc.mTempeMax);
                    db.insert(TABLE_FORECAST, null, values);
                }
                ContentValues vals = new ContentValues();
                vals.put(GEO_LAST_UPDATE, (new Date()).getTime() / 1000);
                db.update(TABLE_CITY, vals, GEO_ID+" = ?", new String[] { ""+geoId});

            } while (c.moveToNext());
            c.close();
        }

        db.close();
    }

    /**
     * Save ephemeris received from GCM
     * @param stationId
     * @param ephemerisList
     */
    public void ephemerisListSave (Integer stationId, ArrayList<EphemerisCity> ephemerisList) {
        String q = "SELECT * FROM " + TABLE_CITY + " WHERE " + GEO_STATION_ID + " = ?";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery(q, new String[]{String.valueOf(stationId)});
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            do {
                Integer geoId = c.getInt(c.getColumnIndex(GEO_ID));

                // delete previous ephemeris for this city
                db.delete(TABLE_EPHEMERIS, GEO_ID + " = ?", new String[] { ""+geoId });

                for (EphemerisCity eph: ephemerisList) {
                    ContentValues values = new ContentValues();
                    values.put(GEO_ID, geoId);
                    values.put(EPH_DATE, eph.mDate.getTime() / 1000);
                    values.put(EPH_SUNRISE, eph.mSunrise);
                    values.put(EPH_SUNSET, eph.mSunset);
                    values.put(EPH_MOONRISE, eph.mMoonrise);
                    values.put(EPH_MOONSET, eph.mMoonset);
                    values.put(EPH_MOONPHASE, eph.mMoonphase);
                    values.put(EPH_MOON4, eph.mMoon4);
                    db.insert(TABLE_EPHEMERIS, null, values);
                }

                ContentValues vals = new ContentValues();
                vals.put(GEO_LAST_UPDATE, (new Date()).getTime() / 1000);
                db.update(TABLE_CITY, vals, GEO_ID+" = ?", new String[] { ""+geoId});

            } while (c.moveToNext());
            c.close();
        }

        db.close();
    }

    /**
     * Updates are at between 3h/5h and 14h/16h every day
     * @param d the date of latest data in db
     * @return true if d is outdated
     */
    private Boolean isOutdated (Date d) {
        Calendar dCal = Calendar.getInstance();
        dCal.setTime(d);

        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        int hod = now.get(Calendar.HOUR_OF_DAY);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        if (hod < 5) {
            // yesterday 14h
            now.set(Calendar.HOUR_OF_DAY, 14);
            now.setTimeInMillis(now.getTimeInMillis() - 1000l * 24 * 60 * 60);
        } else if (hod < 16) {
            // today 3h
            now.set(Calendar.HOUR_OF_DAY, 3);
        } else {
            // today 16h
            now.set(Calendar.HOUR_OF_DAY, 14);
        }

        SimpleDateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date debug = new Date (now.getTimeInMillis());

        Log.d("Météo", "Last update available at "+dfm.format(debug));
        return dCal.before(now);
    }

    public InfoCity infocityGet (Integer geoid, Boolean updated) {
        InfoCity ic = null;
        City city;
        String q = "SELECT * FROM " + TABLE_CITY + " WHERE " + GEO_ID + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(q, new String[] { String.valueOf(geoid)});
        if (c != null) {
            if (!c.moveToFirst()) {
                c.close();
                db.close();
                return null;
            }
            if (updated) {
                Date cityLastUpdate = new Date(1000l * c.getInt(c.getColumnIndex(GEO_LAST_UPDATE)));
                if (isOutdated(cityLastUpdate)) {
                    c.close();
                    db.close();
                    return null;
                }
            }
            city = new City(geoid,
                    c.getString(c.getColumnIndex(GEO_NAME)),
                    c.getDouble(c.getColumnIndex(GEO_LAT)),
                    c.getDouble(c.getColumnIndex(GEO_LNG)),
                    c.getInt(c.getColumnIndex(GEO_ELEVATION)),
                    c.getString(c.getColumnIndex(GEO_ZIP)),
                    null);
            String station = c.getString(c.getColumnIndex(GEO_STATION));
            if (station != null && !station.equals("")) {
                city.setStation(station);
            }
            Integer stationid = c.getInt(c.getColumnIndex(GEO_STATION_ID));
            city.setStationId(stationid);
            c.close();
            ic = new InfoCity(city);

            // Forecast
            q = "SELECT * FROM " + TABLE_FORECAST + " WHERE " + GEO_ID + " = ?";
            c = db.rawQuery(q, new String[]{String.valueOf(geoid)});
            if (c != null) {
                c.moveToFirst();
                do {
                    SimpleDateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    ic.addForecast(
                            c.getInt(c.getColumnIndex(FOR_FIABILITY)),
                            c.getInt(c.getColumnIndex(FOR_PICTO)),
                            c.getInt(c.getColumnIndex(FOR_NEBU_PC)),
                            c.getString(c.getColumnIndex(FOR_NEBU)),
                            c.getInt(c.getColumnIndex(FOR_PRECIP_MMM)),
                            c.getString(c.getColumnIndex(FOR_PRECIP)),
                            c.getInt(c.getColumnIndex(FOR_TEMPE)),
                            c.getInt(c.getColumnIndex(FOR_TEMPE_RES)),
                            c.getInt(c.getColumnIndex(FOR_PRESSION)),
                            c.getInt(c.getColumnIndex(FOR_VENT_MOYEN)),
                            c.getInt(c.getColumnIndex(FOR_VENT_RAF)),
                            c.getInt(c.getColumnIndex(FOR_DIR_VENT)),
                            c.getInt(c.getColumnIndex(FOR_TEMPE_MIN)),
                            c.getInt(c.getColumnIndex(FOR_TEMPE_MAX)),
                            dfm.format(new Date(1000l * c.getInt(c.getColumnIndex(FOR_START)))),
                            dfm.format(new Date(1000l * c.getInt(c.getColumnIndex(FOR_END)))),
                            c.getInt(c.getColumnIndex(FOR_DURATION))
                    );
                } while (c.moveToNext());
                c.close();
            }

            // Ephemeris
            q = "SELECT * FROM " + TABLE_EPHEMERIS + " WHERE " + GEO_ID + " = ?";
            c = db.rawQuery(q, new String[]{String.valueOf(geoid)});
            if (c != null) {
                c.moveToFirst();
                do {
                    try {
                        SimpleDateFormat dfm = new SimpleDateFormat("yyyy-MM-dd");
                        ic.addEphemeris(
                                dfm.format(new Date(1000l * c.getInt(c.getColumnIndex(EPH_DATE)))),
                                c.getString(c.getColumnIndex(EPH_SUNRISE)),
                                c.getString(c.getColumnIndex(EPH_SUNSET)),
                                c.getString(c.getColumnIndex(EPH_MOONRISE)),
                                c.getString(c.getColumnIndex(EPH_MOONSET)),
                                c.getInt(c.getColumnIndex(EPH_MOONPHASE)),
                                c.isNull(c.getColumnIndex(EPH_MOON4)) ? null : c.getInt(c.getColumnIndex(EPH_MOON4))
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (c.moveToNext());
                c.close();
            }
        }
        db.close();
        return ic;
    }

    public ForecastCity getCurrentForecast (Integer geoid, Integer duration) {
        ForecastCity ret = null;
        String q = "SELECT * FROM " + TABLE_FORECAST + " WHERE "
                + GEO_ID + " = ? "
                + " AND " + FOR_START + " < ?"
                + " AND " + FOR_DURATION + " = ? "
                + " ORDER BY " + FOR_START + " DESC"
                + " LIMIT 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(q, new String[]{
                String.valueOf(geoid),
                String.valueOf((new Date()).getTime() / 1000),
                String.valueOf (duration)
        });
        if (c != null && c.getCount() == 1) {
            c.moveToFirst();
            SimpleDateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            ret = new ForecastCity (
                    c.getInt(c.getColumnIndex(FOR_FIABILITY)),
                    c.getInt(c.getColumnIndex(FOR_PICTO)),
                    c.getInt(c.getColumnIndex(FOR_NEBU_PC)),
                    c.getString(c.getColumnIndex(FOR_NEBU)),
                    c.getInt(c.getColumnIndex(FOR_PRECIP_MMM)),
                    c.getString(c.getColumnIndex(FOR_PRECIP)),
                    c.getInt(c.getColumnIndex(FOR_TEMPE)),
                    c.getInt(c.getColumnIndex(FOR_TEMPE_RES)),
                    c.getInt(c.getColumnIndex(FOR_PRESSION)),
                    c.getInt(c.getColumnIndex(FOR_VENT_MOYEN)),
                    c.getInt(c.getColumnIndex(FOR_VENT_RAF)),
                    c.getInt(c.getColumnIndex(FOR_DIR_VENT)),
                    c.getInt(c.getColumnIndex(FOR_TEMPE_MIN)),
                    c.getInt(c.getColumnIndex(FOR_TEMPE_MAX)),
                    dfm.format(new Date(1000l * c.getInt(c.getColumnIndex(FOR_START)))),
                    dfm.format(new Date(1000l * c.getInt(c.getColumnIndex(FOR_END)))),
                    c.getInt(c.getColumnIndex(FOR_DURATION))
            );
        }
        if (c != null)
            c.close();
        db.close();
        return ret;
    }

    public Boolean getIsDay (Integer geoid) {
        Boolean ret = true;
        String q = "SELECT * FROM " + TABLE_EPHEMERIS + " WHERE "
                + GEO_ID + " = ?"
                + " AND " + EPH_DATE + " < ?"
                + " ORDER BY " + EPH_DATE + " DESC"
                + " LIMIT 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(q, new String[]{
                String.valueOf(geoid),
                String.valueOf((new Date()).getTime() / 1000)
        });
        if (c != null && c.getCount() == 1) {
            c.moveToFirst();
            SimpleDateFormat dfm = new SimpleDateFormat("yyyy-MM-dd");
            EphemerisCity eph = new EphemerisCity(
                            dfm.format(new Date(1000l * c.getInt(c.getColumnIndex(EPH_DATE)))),
                            c.getString(c.getColumnIndex(EPH_SUNRISE)),
                            c.getString(c.getColumnIndex(EPH_SUNSET)),
                            c.getString(c.getColumnIndex(EPH_MOONRISE)),
                            c.getString(c.getColumnIndex(EPH_MOONSET)),
                            c.getInt(c.getColumnIndex(EPH_MOONPHASE)),
                            c.isNull(c.getColumnIndex(EPH_MOON4)) ? null : c.getInt(c.getColumnIndex(EPH_MOON4))
                    );
            ret = eph.isDay(new Date ());
        }
        if (c != null)
            c.close();
        db.close ();
        return ret;
    }

    /**
     * Remove a city from the navigation dawer menu
     */
    public void removeMenuEntry (Integer geoid) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MENU, GEO_ID + " = ?", new String[] { String.valueOf(geoid) });
        db.close ();
    }
}
