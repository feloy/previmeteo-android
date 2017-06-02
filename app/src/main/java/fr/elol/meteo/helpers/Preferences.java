package fr.elol.meteo.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import fr.elol.meteo.R;
import fr.elol.meteo.data.MenuEntry;

/**
 * Helper for preferences
 */
public final class Preferences {
    // Last selected menu entry
    private static final String PREF_USER_CHOICE = "pref_choice";

    // Last displayed location
    private static final String PREF_OLD_LOCATION_LAT = "pref_old_location_lat";
    private static final String PREF_OLD_LOCATION_LNG = "pref_old_location_lng";
    private static final String PREF_CURRENT_POSITION_CITY_GEOID = "pref_current_pos_city_geoid";
    private static final String PREF_CURRENT_POSITION_CITY_NAME = "pref_current_pos_city_name";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PREF_TAB = "pref_tab";
    private static final String PREF_USAGE = "pref_usage";
    private static final String HAS_NOTED = "pref_has_noted";

    private static final String PREF_WIDGET_THEME = "pref_widget_theme";
    public static final int WIDGET_THEME_LIGHT = 1;
    public static final int WIDGET_THEME_DARK = 2;
    public static final int WIDGET_THEME_TRANSPARENT_LIGHT= 3;
    public static final int WIDGET_THEME_TRANSPARENT_DARK = 4;
    private static final String PREF_WIDGET_LAYOUT = "pref_widget_layout";

    public static int getMenuSelected (Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getInt(PREF_USER_CHOICE, 0); // Menu position 0
    }

    public static void setMenuSelected (Context ctx, int choice) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PREF_USER_CHOICE, choice);
        editor.apply();
    }

    public static Location getOldLocation (Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        float lat = sp.getFloat(PREF_OLD_LOCATION_LAT, 0);
        float lng = sp.getFloat(PREF_OLD_LOCATION_LNG, 0);
        if (lat == 0 && lng == 0) {
            return null;
        } else {
            Location l = new Location ("");
            l.setLatitude(lat);
            l.setLongitude(lng);
            return l;
        }
    }

    public static void setOldLocation (Context ctx, Location l) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(PREF_OLD_LOCATION_LAT, (float)l.getLatitude());
        editor.putFloat(PREF_OLD_LOCATION_LNG, (float) l.getLongitude());
        editor.apply();
    }

    public static MenuEntry getCurrentPositionCity (Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        int geoid = sp.getInt(PREF_CURRENT_POSITION_CITY_GEOID, 0);
        String name = sp.getString(PREF_CURRENT_POSITION_CITY_NAME, "");
        if (geoid == 0)
            return null;
        else
            return new MenuEntry (geoid, name, null, null, null, null);
    }

    public static void setCurrentPositionCity(Context ctx, MenuEntry e) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PREF_CURRENT_POSITION_CITY_GEOID, e.mGeoid);
        editor.putString(PREF_CURRENT_POSITION_CITY_NAME, e.mName);
        editor.apply();
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    public static void storeRegistrationId(Context context, String regId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i("Météo", "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    public static String getRegistrationId(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String registrationId = sp.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i("Météo", "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = sp.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i("Météo", "App version changed.");
            return "";
        }
        return registrationId;
    }

    public static int getPreferredWidgetTheme (Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(PREF_WIDGET_THEME, WIDGET_THEME_LIGHT);
    }

    public static void setPreferredWidgetTheme (Context context, int pref) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PREF_WIDGET_THEME, pref);
        editor.apply();
    }

    public static Boolean getWidgetLayoutCanvas (Context context, int wid) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_WIDGET_LAYOUT+wid, false);
    }

    public static void setWidgetLayoutCanvas (Context context, int wid, Boolean pref) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(PREF_WIDGET_LAYOUT+wid, pref);
        editor.apply();
    }


    public static int getTab(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(PREF_TAB, 0);
    }

    public static void setTab(Context context, int pref) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PREF_TAB, pref);
        editor.apply();
    }


    public static int getUsage(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(PREF_USAGE, 0);
    }

    public static void setUsage(Context context, int pref) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PREF_USAGE, pref);
        editor.apply();
    }

    public static Boolean getHasNoted(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(HAS_NOTED, false);
    }

    public static void setHasNoted(Context context, Boolean val) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(HAS_NOTED, val);
        editor.apply();
    }

}
