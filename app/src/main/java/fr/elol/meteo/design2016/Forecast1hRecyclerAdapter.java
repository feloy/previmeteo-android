package fr.elol.meteo.design2016;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

import fr.elol.meteo.R;
import fr.elol.meteo.data.ForecastCity;
import fr.elol.meteo.data.InfoCity;
import fr.elol.meteo.helpers.WeatherIcon;

/**
 * Created by philippe on 23/05/15.
 */
public class Forecast1hRecyclerAdapter extends  RecyclerView.Adapter<Forecast1hRecyclerAdapter.ViewHolder>  {

    private static final int TYPE_1H = 1;

    private Typeface tf;
    private Context mContext;
    InfoCity mInfoCity;
    int mCount;
    ArrayList<PositionHolder> mPositionHolders;

    public Forecast1hRecyclerAdapter(Context context, InfoCity infoCity) {
        mInfoCity = infoCity;
        mPositionHolders = new ArrayList<> ();
        mContext = context;

        tf = Typeface.createFromAsset(mContext.getAssets(), "fonts/weathericons.ttf");

        Integer firstFc1hIndex = mInfoCity.getForecastIndex(0, 1);
        for (int i=0; i<mInfoCity.mForecastList.size(); i++) {
            ForecastCity fc = mInfoCity.mForecastList.get(i);
            switch (fc.mDuration) {
                case 1:
                    if (i >= firstFc1hIndex)
                        mPositionHolders.add(new PositionHolder (TYPE_1H, fc));
                    break;
            }
        }
        mCount = mPositionHolders.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forecast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ForecastCity fc = mPositionHolders.get(position).mForecast;
        Calendar c = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        c.setTime(fc.mStart);

        int viewType = mPositionHolders.get(position).mViewType;
        switch (viewType) {
            case TYPE_1H: {
                int dom = c.get(Calendar.DAY_OF_WEEK);
                int nowDom = now.get(Calendar.DAY_OF_WEEK);
                Boolean isDay = mInfoCity.getEphemeris(dom == nowDom ? 0 : 1).isDay(fc.mStart);
                holder.timeText.setText(c.get(Calendar.HOUR_OF_DAY) + "h");

                String str = "icon"+fc.mPicto+(isDay ? "" : "n")+"_36";
                int res = mContext.getResources().getIdentifier(str, "drawable", mContext.getPackageName());
                holder.pictoImg.setImageResource(res);

                holder.tempText.setText(fc.mTempe+ WeatherIcon.WI_CELSIUS);
                // + " (" + fc.mTempeRes+WeatherIcon.WI_DEGREES + ")"
                holder.windText.setText (WeatherIcon.getDirVent(fc.mDirVent)+" "
                        + WeatherIcon.getBeaufort(fc.mVentMoyen));

                String str1 = fc.mNebu > 0 ? WeatherIcon.WI_CLOUD+" "+fc.mNebu+" %" : "";
                int precip = (int)Math.ceil((float)fc.mPrecip/10);
                String str2 = precip > 0 ? WeatherIcon.WI_UMBRELLA + " " + precip +" mm" : "";
                holder.phraseText.setText (str1 + "\n" + str2);

//                    view.setBackgroundColor(getResources().getColor (isDay ? R.color.bgDay : R.color.bgNight));
            }
            break;
        }
    }

    @Override
    public int getItemCount() {
        return mCount;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView timeText, tempText, windText, phraseText;
        public ImageView pictoImg;

        public ViewHolder(View view) {
            super(view);
            this.timeText = (TextView) view.findViewById(R.id.timeText);
            this.pictoImg = (ImageView) view.findViewById(R.id.pictoImg);
            this.tempText = (TextView) view.findViewById(R.id.tempText);
            this.tempText.setTypeface(tf);
            this.windText = (TextView) view.findViewById(R.id.windText);
            this.windText.setTypeface(tf);
            this.phraseText = (TextView) view.findViewById(R.id.phraseText);
            this.phraseText.setTypeface(tf);
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
