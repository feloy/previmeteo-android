package fr.elol.meteo.design2016;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import fr.elol.meteo.R;
import fr.elol.meteo.data.City;
import fr.elol.meteo.helpers.Db;
import fr.elol.meteo.helpers.GetJSONData;

/**
 * Created by philippe on 01/12/15.
 */
public class CitySelectActivity extends AppCompatActivity {

    private ListView mDrawerListView;
    private ProgressBar mProgressBar;
    private TextView mCounterText;
    private EditText mEdit;

    private int iQuery; // Query number, permits to ignore older requests

    private final int TRIGGER_SEARCH = 1;
    private final long SEARCH_TRIGGER_DELAY_IN_MS = 1000;

    private ArrayList<City> mEntries;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == TRIGGER_SEARCH) {
                triggerSearch();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.city_select);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar2);
        mProgressBar.setVisibility(View.INVISIBLE);
        iQuery = 0;

        mCounterText = (TextView) findViewById(R.id.counterText);

        mDrawerListView = (ListView) findViewById(R.id.listView);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mEntries != null) {
                    City newCity = mEntries.get (i);
                    if (newCity != null) {
                        try {
                            int pos = new Db(CitySelectActivity.this).cityAddToMenu(
                                    newCity.mGeoid,
                                    newCity.mName,
                                    newCity.mZip);
                            setResult(pos);
                            finish();
//                            mListener.onCitySelected(pos);
//                            mListener.sendRegistrationIdToBackend();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        mEdit = (EditText) findViewById(R.id.editText);
        if (mEdit != null) {

            mEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
                @Override
                public void afterTextChanged(Editable editable) {
                    handler.removeMessages(TRIGGER_SEARCH);
                    handler.sendEmptyMessageDelayed(TRIGGER_SEARCH, SEARCH_TRIGGER_DELAY_IN_MS);
                }
            });
        }
    }

    public void triggerSearch () {
        String s = mEdit.getText().toString();
        int zip = -1;
        try {
            zip = Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }
        if ((s.length() == 5 && zip != -1)
                || (zip == -1 && s.length() > 1)) {

            // Get list of cities with this zip code
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.VISIBLE);
            }
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                String q = "";
                try {
                    q = URLEncoder.encode(s, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String u = "https://meteo.android.elol.fr/ajax/search.php?q=" + q;
                GetJSONData task = new GetJSONData(this, ++iQuery, new GetJSONData.CallBackListener() {

                    @Override
                    public void callback(int range, String str) {
                        try {
                            Integer nb;
                            try {
                                nb = Integer.parseInt(str);
                            } catch (Exception e) {
                                nb = -1;
                            }
                            if (nb > 0) {
                                mCounterText.setVisibility(View.VISIBLE);
                                mCounterText.setText(nb+" résultats, veuillez préciser");
                                mDrawerListView.setAdapter(new CityAdapter(CitySelectActivity.this, new ArrayList<City>()));
                            } else {
                                mCounterText.setVisibility(View.GONE);
                                JSONArray a = new JSONArray(str);
                                if (range < iQuery) {
                                    Log.d("Meteo", "Ignore because of range " + range + " / " + iQuery);
                                    return;
                                }
                                mEntries = new ArrayList<>();
                                for (int i = 0; i < a.length(); i++) {
                                    try {
                                        JSONObject o = (JSONObject) a.get(i);
                                        mEntries.add(new City(o.getInt("geoid"),
                                                o.getString("name"),
                                                null, null, null,
                                                o.getString("zip"),
                                                null));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                mDrawerListView.setAdapter(new CityAdapter(CitySelectActivity.this, mEntries));
                            }
                            if (mProgressBar != null) {
                                mProgressBar.setVisibility(View.INVISIBLE);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                task.execute(u);
            } else {
                Toast.makeText(this, R.string.erreur_connexion_reseau,
                        Toast.LENGTH_SHORT)
                        .show();
                if (mProgressBar != null) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private class CityAdapter extends ArrayAdapter<City> {
        public CityAdapter(Context context, ArrayList<City> cities) {
            super(context, 0, cities);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;

            if (convertView == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.item_city, parent, false);
                holder = new ViewHolder();
                holder.cityName = (TextView) view.findViewById(R.id.cityName);
                holder.cityZip = (TextView) view.findViewById(R.id.cityZip);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            }

            City city = getItem(position);
            holder.cityName.setText(city.mName);
            holder.cityZip.setText(city.mZip);
            return view;
        }
    }

    private class ViewHolder {
        public TextView cityName, cityZip;
    }
}
