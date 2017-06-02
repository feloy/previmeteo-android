package fr.elol.meteo.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import fr.elol.meteo.R;
import fr.elol.meteo.data.City;
import fr.elol.meteo.data.EphemerisCity;
import fr.elol.meteo.data.ForecastCity;
import fr.elol.meteo.data.InfoCity;
import fr.elol.meteo.design2016.MainActivity;
import fr.elol.meteo.helpers.Db;
import fr.elol.meteo.helpers.GetJSONData;
import fr.elol.meteo.helpers.Preferences;
import fr.elol.meteo.helpers.WeatherIcon;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link AppWidget1x1ConfigureActivity AppWidget1x1ConfigureActivity}
 */
public class AppWidget1x1 extends AppWidgetProvider {

    private static final String SYNC_CLICKED    = "majClick";
    private static final String OPEN_CLICKED    = "openClick";
    private static final String WIDGET_ID = "widgetId";

    static Paint mBackgroundPaint = null, mWatchPaint, mWatchPaint2, mWatchPaint3, mHourNowPaint;
    static Paint mHandPaintHr, mHandPaintMin, mHandPaintSec;
    static Paint mMinPaint, m5MinsPaint;
    static Paint mCityPaint;
    static Paint mTempesPaint;
    static Paint mHourPaint;
    static Paint mPictosPaint;
    static Paint mInfosPaint;

    static Bitmap[] mPictoD;
    static Bitmap[] mPictoN;

    // Called at updatePeriodMillis interval
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            AppWidget1x1ConfigureActivity.deleteTitlePref(context, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created

    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    public static Boolean downloadInfo (final Context context, final int geoid,
                                        final AppWidgetManager appWidgetManager,
                                        final int appWidgetId, final int layout) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String u = "https://meteo.android.elol.fr/ajax/fc.php?id="+geoid;
            Log.d("Météo", "url=" + u);
            GetJSONData task = new GetJSONData(context, 0, new GetJSONData.CallBackListener() {
                @Override
                public void callback(int range, String str) {
                    Log.d("Météo", "Res="+str);
                    try {
                        JSONObject oall = new JSONObject(str);
                        String name = oall.getString("name");
                        City city = new City(geoid, name, null, null, null, null, null);
                        InfoCity ic = new InfoCity (city);
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
                                    ic.addForecast(
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
                                    ic.addForecast(
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
                                    ic.addForecast(
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
                            ic.addEphemeris(o.getString("d"),
                                    o.getString("sunrise"),
                                    o.getString("sunset"),
                                    o.getString("moonrise"),
                                    o.getString("moonset"),
                                    Integer.parseInt(o.getString("moonphase")),
                                    moon4
                            );
                        }
                        (new Db(context)).infocitySave(ic);
                        updateAppWidget(context, appWidgetManager, appWidgetId, layout, false);
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

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        int layout = Preferences.getWidgetLayoutCanvas(context, appWidgetId) ?
                R.layout.app_widget_canvas_large : getLayout(context, appWidgetId);
        updateAppWidget(context, appWidgetManager, appWidgetId, layout, true);
    }


    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
        int appWidgetId, int layout, Boolean recursive) {

        Boolean inCanvas = layout == R.layout.app_widget_canvas_large;

        Log.d("Météo", "updateAppWidget");
        long geoId = AppWidget1x1ConfigureActivity.loadTitlePref(context, appWidgetId);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);

        if (geoId == 0) {
            views.setTextViewText(R.id.maj, "Erreur de mise à jour");
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        if (!inCanvas) {
            views.setOnClickPendingIntent(R.id.maj, getPendingSelfIntent(context, appWidgetId, SYNC_CLICKED));
        }
        views.setOnClickPendingIntent(R.id.linearlayout, getPendingSelfIntent(context, appWidgetId, OPEN_CLICKED));

        InfoCity ic = getLocalInfo(context, geoId, true);
        if (ic == null) {
            if (recursive)
                downloadInfo(context, (int) geoId, appWidgetManager, appWidgetId, layout);
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        ForecastCity currentFc1h = ic.getForecast(0, 1);
        ForecastCity currentFc6h = ic.getForecast(0, 6);
        EphemerisCity ephemeris = ic.getEphemeris(0);

        if (currentFc1h == null || currentFc6h == null || ephemeris == null)  {
            views.setTextViewText(R.id.maj, "Erreur de mise à jour");
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        if (inCanvas && Build.VERSION.SDK_INT >= 16) {

            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int hmax = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
//            int hmin = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
//            int wmax = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            int wmin = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(metrics);
            int width = (int)(wmin * metrics.density);
            int height = (int)(hmax * metrics.density);
            if (width > 0 && height > 0) {
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                onDrawRound(context, views, layout, canvas, new Rect(0, 0, width, height), ic);
                views.setImageViewBitmap(R.id.imageView, bitmap);
            }

        } else {
            views.setTextViewText(R.id.city, ic.mCity.mShortName);
            views.setTextViewText(R.id.temp1, currentFc1h.mTempe + "°C");
            views.setTextViewText(R.id.temp2, currentFc6h.mTempeMin + "° | " + currentFc6h.mTempeMax + "°");
            views.setTextViewText(R.id.nebu, currentFc1h.mNebu + " %");
            views.setTextViewText(R.id.precip, currentFc1h.mPrecip / 10.0 + " mm");
            String str = "icon" + currentFc1h.mPicto + (ephemeris.isDay(new Date()) ? "" : "n") + "_100";
            int res = context.getResources().getIdentifier(str, "drawable", context.getPackageName());
            views.setImageViewResource(R.id.picto, res);

            SimpleDateFormat formater = new SimpleDateFormat("'Mise à jour 'HH'h'mm");
            Date now = new Date();
            views.setTextViewText(R.id.maj, formater.format(now));
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        Log.d("Météo", "OptionsChanged");
        if (Build.VERSION.SDK_INT >= 16) {
            super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
            int hmax = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
//            int hmin = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
//            int wmax = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            int wmin = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            float ratio = (float)wmin/hmax;
            int minSize = Math.min(hmax, wmin);

            Log.d("Météo", "OptionsChanged: "+wmin+"x"+hmax+" => "+ratio);
//            Log.d("Météo", "OptionsChanged: "+wmax+"x"+hmin);
            RemoteViews views;
            int newLayout;
            if (0.7 < ratio && ratio < 1.3) {
                newLayout = R.layout.app_widget_canvas_large;
                Preferences.setWidgetLayoutCanvas(context, appWidgetId, true);
            } else {
                newLayout = getLayout(context, appWidgetId);
                Preferences.setWidgetLayoutCanvas(context, appWidgetId, false);
            }
            updateAppWidget(context, appWidgetManager, appWidgetId, newLayout, true);
        }
    }

    private static InfoCity getLocalInfo (Context context, long geoId, Boolean updated) {
        return (new Db(context)).infocityGet((int) geoId, updated);
    }

    protected static PendingIntent getPendingSelfIntent(Context context, int appWidgetId, String action) {
        Intent intent = new Intent(context, AppWidget1x1.class);
        intent.setAction(action);
        intent.putExtra(WIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, appWidgetId, intent, 0);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Météo", "Intent action: " + intent.getAction() + " / " + intent.getIntExtra(WIDGET_ID, 0));
        switch (intent.getAction()) {
            case SYNC_CLICKED:
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                updateAppWidget(context, appWidgetManager, intent.getIntExtra(WIDGET_ID, 0));
                break;

            case OPEN_CLICKED:
                long geoId = AppWidget1x1ConfigureActivity.loadTitlePref(context,
                        intent.getIntExtra(WIDGET_ID, 0));
                Intent intent2 = new Intent(context, MainActivity.class);
                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Bundle b = new Bundle ();
                b.putLong(MainActivity.PARAM_GEOID, geoId);
                intent2.putExtras(b);
                context.startActivity(intent2);
                break;
        }
        super.onReceive(context, intent);
    }

    private static int getLayout (Context context, int appWidgetId) {
        switch (AppWidget1x1ConfigureActivity.getTheme(context, appWidgetId)) {
            case Preferences.WIDGET_THEME_LIGHT:
                return R.layout.app_widget1x1_light;
            case Preferences.WIDGET_THEME_DARK:
                return R.layout.app_widget1x1_dark;
            case Preferences.WIDGET_THEME_TRANSPARENT_LIGHT:
                return R.layout.app_widget1x1_tr_light;
            case Preferences.WIDGET_THEME_TRANSPARENT_DARK:
                return R.layout.app_widget1x1_tr_dark;
            default:
                return R.layout.app_widget1x1_light;
        }
    }

    private static void createPaints(Context context) {
        if (mBackgroundPaint != null)
            return;

        Resources resources = context.getResources();

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(resources.getColor(R.color.analog_background));

        mWatchPaint = new Paint();
        mWatchPaint.setColor(resources.getColor(R.color.analog_watch_bg));
        mWatchPaint.setAntiAlias(true);
        mWatchPaint.setStyle(Paint.Style.STROKE);
        mWatchPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_round_stroke));

        mWatchPaint2 = new Paint();
        mWatchPaint2.setColor(resources.getColor(R.color.analog_background));
        mWatchPaint2.setAntiAlias(true);

        mWatchPaint3 = new Paint();
        mWatchPaint3.setColor(resources.getColor(R.color.analog_watch_bg));
        mWatchPaint3.setAntiAlias(true);

        mHourNowPaint = new Paint();
        mHourNowPaint.setColor(resources.getColor(R.color.hour_now));
        mHourNowPaint.setStrokeWidth(resources.getDimension(R.dimen.hour_now_stroke));
        mHourNowPaint.setAntiAlias(true);
        mHourNowPaint.setStrokeCap(Paint.Cap.ROUND);

        mHandPaintHr = new Paint();
        mHandPaintHr.setColor(resources.getColor(R.color.analog_hand_hour));
        mHandPaintHr.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_hour_stroke));
        mHandPaintHr.setAntiAlias(true);
        mHandPaintHr.setStrokeCap(Paint.Cap.ROUND);

        mHandPaintMin = new Paint();
        mHandPaintMin.setColor(resources.getColor(R.color.analog_hand_minute));
        mHandPaintMin.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_minute_stroke));
        mHandPaintMin.setAntiAlias(true);
        mHandPaintMin.setStrokeCap(Paint.Cap.ROUND);

        mMinPaint = new Paint();
        mMinPaint.setColor(resources.getColor(R.color.min_paint));
        mMinPaint.setStrokeWidth(resources.getDimension(R.dimen.min_stroke));
        mMinPaint.setAntiAlias(true);
        mMinPaint.setStrokeCap(Paint.Cap.ROUND);

        m5MinsPaint = new Paint();
        m5MinsPaint.setColor(resources.getColor(R.color.mins5_paint));
        m5MinsPaint.setStrokeWidth(resources.getDimension(R.dimen.mins5_stroke));
        m5MinsPaint.setAntiAlias(true);
        m5MinsPaint.setStrokeCap(Paint.Cap.ROUND);

        mCityPaint = new Paint ();
        mCityPaint.setColor(resources.getColor(R.color.city));
        mCityPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.cityTS));
        mCityPaint.setTextAlign(Paint.Align.CENTER);
        mCityPaint.setAntiAlias(true);

        mInfosPaint = new Paint ();
        mInfosPaint.setColor(resources.getColor(R.color.infos));
        mInfosPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.infosTS));
        mInfosPaint.setTextAlign(Paint.Align.CENTER);
        mInfosPaint.setAntiAlias(true);
        mInfosPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/weathericons.ttf"));

        mTempesPaint = new Paint ();
        mTempesPaint.setColor(resources.getColor(R.color.tempes));
        mTempesPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.tempesTS));
        mTempesPaint.setTextAlign(Paint.Align.CENTER);
        mTempesPaint.setAntiAlias(true);
        mTempesPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/weathericons.ttf"));

        mHourPaint = new Paint ();
        mHourPaint.setColor(resources.getColor(R.color.hour));
        mHourPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.hourTS));
        mHourPaint.setTextAlign(Paint.Align.CENTER);
        mHourPaint.setAntiAlias(true);
        mHourPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/weathericons.ttf"));

        mPictosPaint = new Paint();

        mPictoD = new Bitmap[13];
        mPictoN = new Bitmap[13];
        for (int i=0; i<13; i++) {
            String strD = "icon"+i+"_24";
            String strN = "icon"+i+"n_24";
            int res = context.getResources().getIdentifier(strD, "drawable", context.getPackageName());
            mPictoD[i] = BitmapFactory.decodeResource(context.getResources(), res);
            res = context.getResources().getIdentifier(strN, "drawable", context.getPackageName());
            mPictoN[i] = BitmapFactory.decodeResource(context.getResources(), res);
        }
    }

    private static void onDrawRound (Context context, RemoteViews views, int layout,
                                     Canvas canvas, Rect bounds, InfoCity ic) {
        createPaints(context);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        ForecastCity currentFc1h = ic.getForecast(0, 1);
        EphemerisCity ephemeris = ic.getEphemeris(0);
        EphemerisCity ephemeris1 = ic.getEphemeris(1);

        Resources resources = context.getResources();
        int tempes_diff_px = resources.getDimensionPixelSize(R.dimen.tempes_diff);
        int infos_diff_px = resources.getDimensionPixelSize(R.dimen.infos_diff);
        int hour_diff_px = resources.getDimensionPixelSize(R.dimen.hour_diff);
        int radius_diff_px = resources.getDimensionPixelSize(R.dimen.radius_diff);
        int quadrant_diff_px = resources.getDimensionPixelSize(R.dimen.quadrant_diff);
        int analog_hand_hour_len_px = resources.getDimensionPixelSize(R.dimen.analog_hand_hour_len);
        int analog_hand_minutes_len_px = resources.getDimensionPixelSize(R.dimen.analog_hand_minute_len);
        int mins_len_px = resources.getDimensionPixelSize(R.dimen.mins_len);
        int mins5_len_px = resources.getDimensionPixelSize(R.dimen.mins5_len);

//        WeatherData wd = WeatherData.load(WeatherWatchFace.this);
        Time mTime = new Time();
        mTime.setToNow();

        int width = bounds.width();
        int height = bounds.height();

        int radius = Math.min(width, height) / 2;

        // Find the center. Ignore the window insets so that, on round watches with a
        // "chin", the watch face is centered on the entire screen, not just the usable
        // portion.
        float centerX = width / 2f;
        float centerY = height / 2f;


        canvas.drawCircle(centerX, centerY, radius - radius_diff_px, mWatchPaint);
        canvas.drawCircle(centerX, centerY, radius - radius_diff_px, mWatchPaint2);
        canvas.drawCircle(centerX, centerY, radius - quadrant_diff_px, mWatchPaint3);

        // Draw quadrant
        for (int i = 0; i < 60; i++) {
            Boolean is5mins = (i % 5 == 0);
            int len = is5mins ? mins5_len_px : mins_len_px;
            float angle = i / 30f * (float) Math.PI;
            float x1 = (float) Math.sin(angle) * radius;
            float y1 = (float) -Math.cos(angle) * radius;
            float x2 = (float) Math.sin(angle) * (radius - len);
            float y2 = (float) -Math.cos(angle) * (radius - len);
            canvas.drawLine(centerX + x1, centerY + y1, centerX + x2, centerY + y2,
                    is5mins ? m5MinsPaint : mMinPaint);
        }

        int minutes = mTime.minute;
        float minRot = minutes / 30f * (float) Math.PI;
        float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

        float minLength = radius - 20;
        float hrLength = radius - 40;

        // Hour / minutes hands
        float minX = (float) Math.sin(minRot) * analog_hand_minutes_len_px;
        float minY = (float) -Math.cos(minRot) * analog_hand_minutes_len_px;
//        canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mHandPaintMin);

        float hrX = (float) Math.sin(hrRot) * analog_hand_hour_len_px;
        float hrY = (float) -Math.cos(hrRot) * analog_hand_hour_len_px;
//        canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHandPaintHr);

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



        if (ic.mCity.mShortName.length() > 0) {
            canvas.drawText(ic.mCity.mShortName, centerX, centerY + 4*metrics.density, mCityPaint);

            // Nebu
            int yNebu = - radius / 4 + 4*(int)metrics.density;
            int yPrecip = radius / 4 + 4*(int)metrics.density;

            if (currentFc1h.mNebu > 0) {
                canvas.drawText(WeatherIcon.WI_CLOUD + " " + currentFc1h.mNebu + " %",
                        centerX,
                        centerY + yNebu, mInfosPaint);
            }

            // Precip
            if (currentFc1h.mPrecip > 0) {
                int precip = (int) Math.ceil((float) currentFc1h.mPrecip / 10);
                canvas.drawText(WeatherIcon.WI_UMBRELLA + " " + precip + " mm",
                        centerX,
                        centerY + yPrecip, mInfosPaint);
            }

            // daylight
            Log.d("Météo Wear", ephemeris.mSunrise + " " + ephemeris.mSunset);
            Date dSunrise, dSunset, dSunrise1;
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            Calendar cRise = Calendar.getInstance();
            Calendar cSet = Calendar.getInstance();
            Calendar cRise1 = Calendar.getInstance();
            try {
                dSunrise = sdf.parse(ephemeris.mSunrise);
                dSunset = sdf.parse(ephemeris.mSunset);
                dSunrise1 = sdf.parse(ephemeris1.mSunrise);
            } catch (Exception e) {
                dSunrise = new Date();
                dSunset = new Date();
                dSunrise1 = new Date();
            }
            cRise.setTime(dSunrise);
            cSet.setTime(dSunset);
            cRise1.setTime(dSunrise1);
            int riseHour = cRise.get(Calendar.HOUR_OF_DAY);
            int setHour = cSet.get(Calendar.HOUR_OF_DAY);
            int rise1Hour = cRise1.get(Calendar.HOUR_OF_DAY);

            int descent = (int)mTempesPaint.descent();

            // 12 hours tempes / pictos
            Boolean started = false;
            Boolean complete = false;
            for (int i = 0; i < 12; i++) {

                int hour = firstHour + i;
                Boolean tomorrow = (hour > 23);
                hour = hour % 24;

                if (!started && hour >= mTime.hour) {
                    started = true;
                    if (i == 0) {
                        complete = true;
                    }
                }
                if (!started)
                    continue;

                Boolean isDay;
                if (tomorrow) {
                    isDay = hour > rise1Hour;
                } else if (i == 0 && (hour == riseHour || hour == setHour)) {
                    if (hour == riseHour) {
                        isDay = mTime.minute >= cRise.get(Calendar.MINUTE);
                    } else { // hour == setHour
                        isDay = mTime.minute < cSet.get(Calendar.MINUTE);
                    }
                } else {
                    isDay = hour > riseHour && hour <= setHour;
                }
                if (i == 0) {
                    Log.d("Météo Wear", riseHour + " " + hour + " " + setHour);
                }
                float angle = ((firstHour + i) % 12) / 6f * (float) Math.PI + 1 / 12f * (float) Math.PI;

                if ((firstHour+i) % 3 == 0) {
                    float hrAngle = ((firstHour + i) % 12) / 6f * (float) Math.PI;
                    float hourX = centerX + (float) Math.sin(hrAngle) * (radius - hour_diff_px);
                    float hourY = centerY + descent + (float) -Math.cos(hrAngle) * (radius - hour_diff_px);
                    int dispHour = (firstHour + i) % 24;
                    canvas.drawText("" + dispHour, hourX, hourY, mHourPaint);
                }

                float x = centerX + (float) Math.sin(angle) * (radius - tempes_diff_px);
                float y = centerY + descent + (float) -Math.cos(angle) * (radius - tempes_diff_px);
                mTempesPaint.setAlpha(255 - 10 * i);
                if (tempes.size() > i)
                    canvas.drawText(tempes.get(i) + WeatherIcon.WI_CELSIUS, x, y, mTempesPaint);

                float xPicto = centerX + (float) Math.sin(angle) * (radius - infos_diff_px);
                float yPicto = centerY + (float) -Math.cos(angle) * (radius - infos_diff_px);

                if (pictos.size() > i) {
                    final Bitmap picto;
                    if (isDay) {
                        picto = mPictoD[pictos.get(i)];
                    } else {
                        picto = mPictoN[pictos.get(i)];
                    }
                    canvas.drawBitmap(picto, xPicto - picto.getWidth() / 2, yPicto - picto.getHeight() / 2, mPictosPaint);
                }
            }

            if (complete) {
                // Draw Separator
                float hrRot1 = (mTime.hour / 6f) * (float) Math.PI;
                float x1 = (float) Math.sin(hrRot1) * radius;
                float y1 = (float) -Math.cos(hrRot1) * radius;
                float x2 = (float) Math.sin(hrRot1) * (radius - quadrant_diff_px);
                float y2 = (float) -Math.cos(hrRot1) * (radius - quadrant_diff_px);
                canvas.drawLine(centerX + x1, centerY + y1, centerX + x2, centerY + y2,
                        mHourNowPaint);
            }
        }
        Log.d("Meteo widget", "Drawn");
    }

}


