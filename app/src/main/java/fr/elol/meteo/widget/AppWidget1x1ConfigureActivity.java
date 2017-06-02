package fr.elol.meteo.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import fr.elol.meteo.R;
import fr.elol.meteo.data.MenuEntry;
import fr.elol.meteo.design2016.MainActivity;
import fr.elol.meteo.helpers.Db;
import fr.elol.meteo.helpers.Preferences;

/**
 * The configuration screen for the {@link AppWidget1x1 AppWidget1x1} AppWidget.
 */
public class AppWidget1x1ConfigureActivity extends Activity {

    private ListView mListView;
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private static final String PREFS_NAME = "fr.elol.meteo.widget.AppWidget1x1";
    private static final String PREF_PREFIX_KEY = "appwidget_";
    private static final String PREF_THEME_PREFIX_KEY = "appwidgetTheme_";

    public static final int RESULT_SELECT_NEW_CITY = 1001;

    public AppWidget1x1ConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.app_widget1x1_configure);
        mListView = (ListView)findViewById(R.id.listView);
        mListView.setOnItemClickListener(mOnClickListener);

        RadioGroup radioGroup = (RadioGroup)findViewById(R.id.radioGroup);
        switch (Preferences.getPreferredWidgetTheme(this)) {
            case Preferences.WIDGET_THEME_LIGHT:
                radioGroup.check(R.id.radioButton1);
                break;
            case Preferences.WIDGET_THEME_DARK:
                radioGroup.check(R.id.radioButton2);
                break;
            case Preferences.WIDGET_THEME_TRANSPARENT_LIGHT:
                radioGroup.check(R.id.radioButton3);
                break;
            case Preferences.WIDGET_THEME_TRANSPARENT_DARK:
                radioGroup.check(R.id.radioButton4);
                break;
        }

        MenuEntry[] cities = (new Db(this)).getMenu();
        if (cities != null) {
            mListView.setAdapter(new MenuEntryArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    cities));
        } else {
            Intent intent2 = new Intent(this, MainActivity.class);
            Bundle b = new Bundle ();
            b.putLong(MainActivity.PARAM_GEOID, -1);
            intent2.putExtras(b);
            startActivityForResult(intent2, RESULT_SELECT_NEW_CITY);
        }

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            //return;
        }
    }

    AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            final Context context = AppWidget1x1ConfigureActivity.this;

            RadioGroup radioGroup = (RadioGroup)findViewById(R.id.radioGroup);
            int theme = Preferences.WIDGET_THEME_LIGHT;
            switch (radioGroup.getCheckedRadioButtonId()) {
                case R.id.radioButton1:
                    theme = Preferences.WIDGET_THEME_LIGHT;
                    break;
                case R.id.radioButton2:
                    theme = Preferences.WIDGET_THEME_DARK;
                    break;
                case R.id.radioButton3:
                    theme = Preferences.WIDGET_THEME_TRANSPARENT_LIGHT;
                    break;
                case R.id.radioButton4:
                    theme = Preferences.WIDGET_THEME_TRANSPARENT_DARK;
                    break;
            }
            Preferences.setPreferredWidgetTheme(AppWidget1x1ConfigureActivity.this, theme);
            setTheme (context, mAppWidgetId, theme);
            saveTitlePref(context, mAppWidgetId, id);

            // It is the responsibility of the configuration activity to update the app widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            AppWidget1x1.updateAppWidget(context, appWidgetManager, mAppWidgetId);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_SELECT_NEW_CITY) {
            MenuEntry[] cities = (new Db(this)).getMenu();
            if (cities != null) {
                MenuEntryArrayAdapter adapter = new MenuEntryArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        cities);
                mListView.setAdapter(adapter);
//                adapter.notifyDataSetChanged();
            }
        }
    }

    // Write the prefix to the SharedPreferences object for this widget
    static void saveTitlePref(Context context, int appWidgetId, long val) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putLong(PREF_PREFIX_KEY + appWidgetId, val);
        prefs.commit();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static long loadTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getLong(PREF_PREFIX_KEY + appWidgetId, 0);
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.commit();
    }

    static int getTheme(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getInt(PREF_THEME_PREFIX_KEY + appWidgetId, Preferences.WIDGET_THEME_LIGHT);

    }
    static void setTheme(Context context, int appWidgetId, int val) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_THEME_PREFIX_KEY + appWidgetId, val);
        prefs.commit();
    }


    private class MenuEntryArrayAdapter extends ArrayAdapter<MenuEntry> {

        public MenuEntryArrayAdapter(Context context, int textViewResourceId, MenuEntry[] objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).mGeoid;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = LayoutInflater.from(this.getContext())
                        .inflate(R.layout.item_city, parent, false);

                viewHolder = new ViewHolder();
                viewHolder.mName = (TextView) convertView.findViewById(R.id.cityName);
                viewHolder.mZip = (TextView) convertView.findViewById(R.id.cityZip);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            MenuEntry item = getItem(position);
            if (item != null) {
                // My layout has only one TextView
                // do whatever you want with your string and long
                viewHolder.mName.setText(item.mName);
                viewHolder.mZip.setText(item.mZip);
            }

            return convertView;
        }

        private class ViewHolder {
            private TextView mName, mZip;
        }
    }
}



