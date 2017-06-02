package fr.elol.meteo;

import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

/**
 * Created by philippe on 28/02/15.
 */
public class DataService extends WearableListenerService {

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("Meteo Wear", "onDataChanged");
        final List<DataEvent> events = FreezableUtils
                .freezeIterable(dataEvents);
        for (DataEvent event : events) {
            Uri uri = event.getDataItem().getUri();
            Log.d("Meteo Wear", "received from nodeId: "+uri.getHost());
            final String path = uri != null ? uri.getPath() : null;
            if ("/weather".equals(path)) {
                final DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                WeatherData wd = new WeatherData(
                        uri.getHost(),
                        map.getString(WeatherData.KEY_NAME),
                        map.getInt(WeatherData.KEY_PICTO),
                        map.getInt(WeatherData.KEY_TEMPE),
                        map.getInt(WeatherData.KEY_TEMPE_MIN),
                        map.getInt(WeatherData.KEY_TEMPE_MAX),
                        map.getInt(WeatherData.KEY_NEBU),
                        map.getInt(WeatherData.KEY_PRECIP),

                        map.getString(WeatherData.KEY_SUNRISE),
                        map.getString(WeatherData.KEY_SUNSET),
                        map.getString(WeatherData.KEY_SUNRISE1),
                        map.getInt(WeatherData.KEY_MOONPHASE),

                        map.getInt(WeatherData.KEY_FIRST_HOUR),
                        map.getIntegerArrayList(WeatherData.KEY_PICTOS),
                        map.getIntegerArrayList(WeatherData.KEY_TEMPES)
                        );
                wd.save(this);
            }
        }
    }
}