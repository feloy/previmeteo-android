package fr.elol.meteo.design2016;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

import fr.elol.meteo.R;
import fr.elol.meteo.data.EphemerisCity;
import fr.elol.meteo.data.ForecastCity;
import fr.elol.meteo.data.InfoCity;
import fr.elol.meteo.helpers.WeatherIcon;

/**
 * Created by philippe on 23/05/15.
 */
public class Forecast24hRecyclerAdapter extends RecyclerView.Adapter<Forecast24hRecyclerAdapter.ViewHolder24h>   {

    private static final int TYPE_24H = 0;

    private Typeface tf;

    InfoCity mInfoCity;
    int mCount;
    Context mContext;

    String[] daysShort = { "", "Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam" };
    String[] daysLong = { "", "Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi" };
    String[] dayparts = { "Nuit", "Matin", "A.-midi", "Soirée"};

    ArrayList<PositionHolder> mPositionHolders;

    public Forecast24hRecyclerAdapter(Context context, InfoCity infoCity) {
        mInfoCity = infoCity;
        mPositionHolders = new ArrayList<> ();
        mContext = context;

        tf = Typeface.createFromAsset(mContext.getAssets(), "fonts/weathericons.ttf");

        for (int i=0; i<mInfoCity.mForecastList.size(); i++) {
            ForecastCity fc = mInfoCity.mForecastList.get(i);
            switch (fc.mDuration) {
                case 24:
                    mPositionHolders.add(new PositionHolder (TYPE_24H, fc));
                    break;
            }
        }
        mCount = mPositionHolders.size();
    }

    @Override
    public ViewHolder24h onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forecast_24h, parent, false);
        return new ViewHolder24h(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder24h holder, int position) {
        ForecastCity fc = mPositionHolders.get(position).mForecast;
        Calendar c = Calendar.getInstance();
        c.setTime(fc.mStart);

        Integer dow = c.get(Calendar.DAY_OF_WEEK);
        Integer dom = c.get(Calendar.DAY_OF_MONTH);
        holder.timeText.setText(daysShort[dow] + " " + dom);

        String str = "icon"+fc.mPicto+"_36";
        int res = mContext.getResources().getIdentifier(str, "drawable", mContext.getPackageName());
        holder.pictoImg.setImageResource(res);

        holder.tempMinText.setText(fc.mTempeMin+WeatherIcon.WI_CELSIUS);
        holder.tempMaxText.setText(fc.mTempeMax+WeatherIcon.WI_CELSIUS);

        EphemerisCity eph = mInfoCity.getEphemeris(fc.mStart);
        holder.infoText.setText(WeatherIcon.getMoonphaseQuarters(eph.mMoon4));

//            view.setBackgroundColor(getResources().getColor(R.color.bgDay));
    }

    @Override
    public int getItemCount() {
        return mCount;
    }

/*
    private View getView24h(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder24h holder;

        if(convertView == null) {
            view = mInflater.inflate(R.layout.item_forecast_24h, parent, false);
            holder = new ViewHolder24h();
            holder.timeText = (TextView) view.findViewById(R.id.timeText);
            holder.pictoImg = (ImageView) view.findViewById(R.id.pictoImg);
            holder.tempMinText = (TextView) view.findViewById(R.id.tempMinText);
            holder.tempMinText.setTypeface(tf);
            holder.tempMaxText = (TextView) view.findViewById(R.id.tempMaxText);
            holder.tempMaxText.setTypeface(tf);
            holder.infoText = (TextView) view.findViewById(R.id.infoText);
            holder.infoText.setTypeface(tf);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder24h) view.getTag();
        }

        ForecastCity fc = mPositionHolders.get(position).mForecast;
        Calendar c = Calendar.getInstance();
        c.setTime(fc.mStart);

        Integer dow = c.get(Calendar.DAY_OF_WEEK);
        Integer dom = c.get(Calendar.DAY_OF_MONTH);
        holder.timeText.setText(daysShort[dow] + " " + dom);

        String str = "icon"+fc.mPicto+"_36";
        int res = mContext.getResources().getIdentifier(str, "drawable", mContext.getPackageName());
        holder.pictoImg.setImageResource(res);

        holder.tempMinText.setText(fc.mTempeMin+WeatherIcon.WI_CELSIUS);
        holder.tempMaxText.setText(fc.mTempeMax+WeatherIcon.WI_CELSIUS);

        EphemerisCity eph = mInfoCity.getEphemeris(fc.mStart);
        holder.infoText.setText(WeatherIcon.getMoonphaseQuarters(eph.mMoon4));

//            view.setBackgroundColor(getResources().getColor(R.color.bgDay));
        return view;
    }
*/

    public class ViewHolder24h extends RecyclerView.ViewHolder {
        public TextView timeText, tempMinText, tempMaxText, infoText;
        public ImageView pictoImg;

        public ViewHolder24h(View view) {
            super(view);
            timeText = (TextView) view.findViewById(R.id.timeText);
            pictoImg = (ImageView) view.findViewById(R.id.pictoImg);
            tempMinText = (TextView) view.findViewById(R.id.tempMinText);
            tempMinText.setTypeface(tf);
            tempMaxText = (TextView) view.findViewById(R.id.tempMaxText);
            tempMaxText.setTypeface(tf);
            infoText = (TextView) view.findViewById(R.id.infoText);
            infoText.setTypeface(tf);
        }
    }

    private class PositionHolder {
        public int mViewType;
        public ForecastCity mForecast;
        public PositionHolder(int viewType, ForecastCity forecast) {
            mViewType = viewType;
            mForecast = forecast;
        }
    }
}
