package fr.elol.meteo.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import fr.elol.meteo.R;
import fr.elol.meteo.data.EphemerisCity;
import fr.elol.meteo.data.ForecastCity;
import fr.elol.meteo.helpers.Db;

/**
 * Created by philippe on 11/02/15.
 */
public class MeteoIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public MeteoIntentService() {
        super("GcmIntentService");
        Log.d("Météo", "MeteoIntentService constructor");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {

            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {

                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

                Log.i("Météo", "Received: " + extras.toString());

                String message = (String)extras.get("message");
                try {
                    JSONObject o = new JSONObject(message);
                    Integer stationId = Integer.parseInt(o.getString("id"));

                    if (o.has("forecast")) {
                        Integer duration = o.getInt("duration");
                        JSONArray fcs = o.getJSONArray("forecast");
                        ArrayList<ForecastCity> fcList = new ArrayList<>();
                        for (int i=0; i<fcs.length(); i++) {
                            JSONArray fc = fcs.getJSONArray(i);
                            switch (duration) {
                                case 1:
                                    fcList.add(new ForecastCity(
                                        Integer.parseInt(fc.getString(0)),
                                        Integer.parseInt(fc.getString(1)),
                                        Integer.parseInt(fc.getString(2)),
                                        null,
                                        Integer.parseInt(fc.getString(3)),
                                        null,
                                        Integer.parseInt(fc.getString(4)),
                                        Integer.parseInt(fc.getString(5)),
                                        Integer.parseInt(fc.getString(6)),
                                        Integer.parseInt(fc.getString(7)),
                                        Integer.parseInt(fc.getString(8)),
                                        Integer.parseInt(fc.getString(9)),
                                        null,
                                        null,
                                        fc.getString(12),
                                        fc.getString(13),
                                        duration
                                    ));
                                break;

                                case 6:
                                    fcList.add(new ForecastCity(
                                            Integer.parseInt(fc.getString(0)),
                                            Integer.parseInt(fc.getString(1)),
                                            Integer.parseInt(fc.getString(2)),
                                            null,
                                            Integer.parseInt(fc.getString(3)),
                                            null,
                                            Integer.parseInt(fc.getString(4)),
                                            Integer.parseInt(fc.getString(5)),
                                            Integer.parseInt(fc.getString(6)),
                                            Integer.parseInt(fc.getString(7)),
                                            Integer.parseInt(fc.getString(8)),
                                            Integer.parseInt(fc.getString(9)),
                                            Integer.parseInt(fc.getString(10)),
                                            Integer.parseInt(fc.getString(11)),
                                            fc.getString(12),
                                            fc.getString(13),
                                            duration
                                    ));
                                    break;

                                case 24:
                                    fcList.add(new ForecastCity(
                                            Integer.parseInt(fc.getString(0)),
                                            Integer.parseInt(fc.getString(1)),
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
                                            Integer.parseInt(fc.getString(10)),
                                            Integer.parseInt(fc.getString(11)),
                                            fc.getString(12),
                                            fc.getString(13),
                                            duration
                                    ));
                                    break;
                            }
                        }
                        (new Db(getApplicationContext())).forecastListSave(stationId, duration, fcList);

                    } else if (o.has("eph")) {
                        JSONArray ephs = o.getJSONArray("eph");
                        ArrayList<EphemerisCity> ephList = new ArrayList<>();
                        for (int i=0; i<ephs.length(); i++) {
                            JSONArray eph = ephs.getJSONArray(i);
                            String strMoon4 = eph.getString(6);
                            Integer moon4;
                            try {
                                moon4 = Integer.parseInt(strMoon4);
                            } catch (Exception e) {
                                moon4 = null;
                            }
                            ephList.add(new EphemerisCity(
                                    eph.getString(0),
                                    eph.getString(1),
                                    eph.getString(2),
                                    eph.getString(3),
                                    eph.getString(4),
                                    Integer.parseInt(eph.getString(5)),
                                    moon4
                            ));
                        }
                        (new Db(getApplicationContext())).ephemerisListSave(stationId, ephList);

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        MeteoBroadcastReceiver.completeWakefulIntent(intent);
    }
}
