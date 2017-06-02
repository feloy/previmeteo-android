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
public class ForecastAllRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_1H_HEAD = 0;
    private static final int TYPE_1H = 1;
    private static final int TYPE_6H_HEAD = 2;
    private static final int TYPE_6H = 3;
    private static final int TYPE_24H_HEAD = 4;
    private static final int TYPE_24H = 5;
    private static final int TYPE_DIVIDER = 6;

    private LayoutInflater mInflater;
    private Typeface tf;

    InfoCity mInfoCity;
    int mCount;
    Context mContext;

    String[] daysShort = { "", "Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam" };
    String[] daysLong = { "", "Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi" };
    String[] dayparts = { "Nuit", "Matin", "A.-midi", "Soirée"};

    ArrayList<PositionHolder> mPositionHolders;

    public ForecastAllRecyclerAdapter(Context context, InfoCity infoCity) {
        mInflater = LayoutInflater.from(context);
        mInfoCity = infoCity;
        mPositionHolders = new ArrayList<> ();
        mContext = context;

        tf = Typeface.createFromAsset(mContext.getAssets(), "fonts/weathericons.ttf");

        Calendar c = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        Boolean head24added = false;
        Boolean firstHead6added = false;

        mPositionHolders.add(new PositionHolder(TYPE_1H_HEAD, null));
        Integer firstFc1hIndex = mInfoCity.getForecastIndex(0, 1);
        for (int i=0; i<mInfoCity.mForecastList.size(); i++) {
            ForecastCity fc = mInfoCity.mForecastList.get(i);
            switch (fc.mDuration) {
                case 1:
                    if (i >= firstFc1hIndex)
                        mPositionHolders.add(new PositionHolder (TYPE_1H, fc));
                    break;
                case 6:
                    c.setTime(fc.mStart);
                    if (c.get(Calendar.MONTH) > now.get(Calendar.MONTH) ||
                            c.get(Calendar.DAY_OF_MONTH) >= now.get(Calendar.DAY_OF_MONTH)) {
                        if (c.get(Calendar.HOUR_OF_DAY) == 0) {
                            mPositionHolders.add(new PositionHolder(TYPE_DIVIDER, null));
                            mPositionHolders.add(new PositionHolder(TYPE_6H_HEAD, fc));
                            firstHead6added = true;
                        }
                        if (firstHead6added)
                            mPositionHolders.add(new PositionHolder (TYPE_6H, fc));
                    }
                    break;
                case 24:
                    if (!head24added) {
                        mPositionHolders.add(new PositionHolder(TYPE_DIVIDER, null));
                        mPositionHolders.add(new PositionHolder(TYPE_24H_HEAD, null));
                        head24added = true;
                    }
                    mPositionHolders.add(new PositionHolder (TYPE_24H, fc));
                    break;
            }
        }
        mCount = mPositionHolders.size();
    }

    @Override
    public int getItemCount() {
        return mCount;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_1H:
            case TYPE_6H: {
                View view = mInflater.inflate(R.layout.item_forecast, parent, false);
                return new ViewHolder(view);
            }
            case TYPE_24H: {
                View view = mInflater.inflate(R.layout.item_forecast_24h, parent, false);
                return new ViewHolder24h(view);
            }
            case TYPE_1H_HEAD:
            case TYPE_6H_HEAD:
            case TYPE_24H_HEAD: {
                View view = mInflater.inflate(R.layout.item_forecast_6h_head, parent, false);
                return new ViewHolderHead(view);
            }
            case TYPE_DIVIDER: {
                View view = mInflater.inflate(R.layout.item_forecast_divider, parent, false);
                return new ViewHolderDivider(view);
            }
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case TYPE_1H:
            case TYPE_6H: {
                ForecastCity fc = mPositionHolders.get(position).mForecast;
                Calendar c = Calendar.getInstance();
                Calendar now = Calendar.getInstance();
                c.setTime(fc.mStart);

                int viewType = mPositionHolders.get(position).mViewType;
                ViewHolder theHolder = (ViewHolder)holder;
                switch (viewType) {
                    case TYPE_1H: {
                        int dom = c.get(Calendar.DAY_OF_WEEK);
                        int nowDom = now.get(Calendar.DAY_OF_WEEK);
                        Boolean isDay = mInfoCity.getEphemeris(dom == nowDom ? 0 : 1).isDay(fc.mStart);
                        theHolder.timeText.setText(c.get(Calendar.HOUR_OF_DAY) + "h");

                        String str = "icon"+fc.mPicto+(isDay ? "" : "n")+"_36";
                        int res = mContext.getResources().getIdentifier(str, "drawable", mContext.getPackageName());
                        theHolder.pictoImg.setImageResource(res);

                        theHolder.tempText.setText(fc.mTempe+ WeatherIcon.WI_CELSIUS);
                        // + " (" + fc.mTempeRes+WeatherIcon.WI_DEGREES + ")"
                        theHolder.windText.setText (WeatherIcon.getDirVent(fc.mDirVent)+" "
                                + WeatherIcon.getBeaufort(fc.mVentMoyen));

                        String str1 = fc.mNebu > 0 ? WeatherIcon.WI_CLOUD+" "+fc.mNebu+" %" : "";
                        int precip = (int)Math.ceil((float)fc.mPrecip/10);
                        String str2 = precip > 0 ? WeatherIcon.WI_UMBRELLA + " " + precip +" mm" : "";
                        theHolder.phraseText.setText (str1 + "\n" + str2);

//                    view.setBackgroundColor(getResources().getColor (isDay ? R.color.bgDay : R.color.bgNight));
                    }
                    break;

                    case TYPE_6H: {
                        Integer h = c.get(Calendar.HOUR_OF_DAY);
                        Boolean isDay = (h == 6 || h == 12);

                        Calendar cEnd = Calendar.getInstance();
                        cEnd.setTime(fc.mEnd);

                        theHolder.timeText.setText(dayparts[c.get(Calendar.HOUR_OF_DAY)/6]);

                        String str = "icon"+fc.mPicto+(isDay ? "" : "n")+"_36";
                        int res = mContext.getResources().getIdentifier(str, "drawable", mContext.getPackageName());
                        theHolder.pictoImg.setImageResource(res);

                        theHolder.tempText.setText(fc.mTempe+WeatherIcon.WI_CELSIUS);
                        theHolder.windText.setText (WeatherIcon.getDirVent(fc.mDirVent)+" "
                                + WeatherIcon.getBeaufort(fc.mVentMoyen));

                        String str1 = fc.mNebu > 0 ? WeatherIcon.WI_CLOUD+" "+fc.mNebu+" %" : "";
                        int precip = (int)Math.ceil((float)fc.mPrecip/10);
                        String str2 = precip > 0 ? WeatherIcon.WI_UMBRELLA + " " + precip +" mm" : "";
                        theHolder.phraseText.setText (str1 + "\n" + str2);

//                    view.setBackgroundColor(getResources().getColor (isDay ? R.color.bgDay : R.color.bgNight));
                    }
                    break;
                }
            }
            break;
            case TYPE_24H: {
                ForecastCity fc = mPositionHolders.get(position).mForecast;
                Calendar c = Calendar.getInstance();
                c.setTime(fc.mStart);
                ViewHolder24h theHolder = (ViewHolder24h)holder;
                Integer dow = c.get(Calendar.DAY_OF_WEEK);
                Integer dom = c.get(Calendar.DAY_OF_MONTH);
                theHolder.timeText.setText(daysShort[dow] + " " + dom);

                String str = "icon"+fc.mPicto+"_36";
                int res = mContext.getResources().getIdentifier(str, "drawable", mContext.getPackageName());
                theHolder.pictoImg.setImageResource(res);

                theHolder.tempMinText.setText(fc.mTempeMin+WeatherIcon.WI_CELSIUS);
                theHolder.tempMaxText.setText(fc.mTempeMax+WeatherIcon.WI_CELSIUS);

                EphemerisCity eph = mInfoCity.getEphemeris(fc.mStart);
                theHolder.infoText.setText(WeatherIcon.getMoonphaseQuarters(eph.mMoon4));

//            view.setBackgroundColor(getResources().getColor(R.color.bgDay));
            }
            break;
            case TYPE_1H_HEAD:
            case TYPE_6H_HEAD:
            case TYPE_24H_HEAD: {
                ViewHolderHead theHolder = (ViewHolderHead)holder;

                if (mPositionHolders.get(position).mViewType == TYPE_24H_HEAD) {
                    theHolder.titleText.setText("Les 2 prochaines semaines");

                } else if (mPositionHolders.get(position).mViewType == TYPE_1H_HEAD) {
                    theHolder.titleText.setText("Les prochaines heures");
                } else {
                    ForecastCity fc = mPositionHolders.get(position).mForecast;
                    Calendar c = Calendar.getInstance();
                    c.setTime(fc.mStart);
                    theHolder.titleText.setText(daysLong[c.get(Calendar.DAY_OF_WEEK)] + " " + c.get(Calendar.DAY_OF_MONTH));

                    EphemerisCity eph = mInfoCity.getEphemeris(fc.mStart);
                    theHolder.infoText.setText(WeatherIcon.getMoonphaseQuarters(eph.mMoon4));
//                view.setBackgroundColor(getResources().getColor(R.color.bgNight));
                }
            }
            break;
            case TYPE_DIVIDER: {
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mPositionHolders.get(position).mViewType;
    }
/*
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PositionHolder positionHolder = mPositionHolders.get(position);
        switch (positionHolder.mViewType) {
            case TYPE_1H:
            case TYPE_6H:
                return getView1h(position, convertView, parent);
            case TYPE_24H:
                return getView24h(position, convertView, parent);
            case TYPE_1H_HEAD:
            case TYPE_6H_HEAD:
            case TYPE_24H_HEAD:
                return getViewHead(position, convertView, parent);
            case TYPE_DIVIDER:
                return getViewDivider (position, convertView, parent);
            default:
                return null; // should not happen
        }
    }

    private View getView1h(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder holder;

        if(convertView == null) {
            view = mInflater.inflate(R.layout.item_forecast, parent, false);
            holder = new ViewHolder();
            holder.timeText = (TextView) view.findViewById(R.id.timeText);
            holder.pictoImg = (ImageView) view.findViewById(R.id.pictoImg);
            holder.tempText = (TextView) view.findViewById(R.id.tempText);
            holder.tempText.setTypeface(tf);
            holder.windText = (TextView) view.findViewById(R.id.windText);
            holder.windText.setTypeface(tf);
            holder.phraseText = (TextView) view.findViewById(R.id.phraseText);
            holder.phraseText.setTypeface(tf);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }

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

            case TYPE_6H: {
                Integer h = c.get(Calendar.HOUR_OF_DAY);
                Boolean isDay = (h == 6 || h == 12);

                Calendar cEnd = Calendar.getInstance();
                cEnd.setTime(fc.mEnd);

                holder.timeText.setText(dayparts[c.get(Calendar.HOUR_OF_DAY)/6]);

                String str = "icon"+fc.mPicto+(isDay ? "" : "n")+"_36";
                int res = mContext.getResources().getIdentifier(str, "drawable", mContext.getPackageName());
                holder.pictoImg.setImageResource(res);

                holder.tempText.setText(fc.mTempe+WeatherIcon.WI_CELSIUS);
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
        return view;
    }

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

    private View getViewHead(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolderHead holderHead;

        if(convertView == null) {
            view = mInflater.inflate(R.layout.item_forecast_6h_head, parent, false);
            holderHead = new ViewHolderHead();
            holderHead.titleText = (TextView) view.findViewById(R.id.titleText);
            holderHead.infoText = (TextView) view.findViewById(R.id.infoText);
            holderHead.infoText.setTypeface(tf);
            view.setTag(holderHead);
        } else {
            view = convertView;
            holderHead = (ViewHolderHead) view.getTag();
        }

        if (mPositionHolders.get(position).mViewType == TYPE_24H_HEAD) {
            holderHead.titleText.setText("La semaine prochaine");

        } else if (mPositionHolders.get(position).mViewType == TYPE_1H_HEAD) {
            holderHead.titleText.setText("Les prochaines heures");
        } else {
            ForecastCity fc = mPositionHolders.get(position).mForecast;
            Calendar c = Calendar.getInstance();
            c.setTime(fc.mStart);
            holderHead.titleText.setText(daysLong[c.get(Calendar.DAY_OF_WEEK)] + " " + c.get(Calendar.DAY_OF_MONTH));

            EphemerisCity eph = mInfoCity.getEphemeris(fc.mStart);
            holderHead.infoText.setText(WeatherIcon.getMoonphaseQuarters(eph.mMoon4));
//                view.setBackgroundColor(getResources().getColor(R.color.bgNight));
        }
        return view;
    }

    private View getViewDivider(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.item_forecast_divider, parent, false);
        } else {
            view = convertView;
        }
        return view;
    }
*/

    private class ViewHolder extends RecyclerView.ViewHolder {
        public TextView timeText, tempText, windText, phraseText;
        public ImageView pictoImg;

        public ViewHolder (View view) {
            super(view);
            timeText = (TextView) view.findViewById(R.id.timeText);
            pictoImg = (ImageView) view.findViewById(R.id.pictoImg);
            tempText = (TextView) view.findViewById(R.id.tempText);
            tempText.setTypeface(tf);
            windText = (TextView) view.findViewById(R.id.windText);
            windText.setTypeface(tf);
            phraseText = (TextView) view.findViewById(R.id.phraseText);
            phraseText.setTypeface(tf);
        }
    }

    private class ViewHolderHead extends RecyclerView.ViewHolder {
        public TextView titleText, infoText;
        public ViewHolderHead(View view) {
            super(view);
            titleText = (TextView) view.findViewById(R.id.titleText);
            infoText = (TextView) view.findViewById(R.id.infoText);
            infoText.setTypeface(tf);
        }
    }

    private class ViewHolder24h extends RecyclerView.ViewHolder {
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

    public class ViewHolderDivider extends RecyclerView.ViewHolder {
        public ViewHolderDivider(View view) {
            super(view);
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