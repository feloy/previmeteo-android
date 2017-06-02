package fr.elol.meteo.data;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Forecast information for a city
 */
public class ForecastCity implements Serializable {
    public Integer mFiability; //*
    public Integer mPicto;
    public Integer mNebu; //*
    public String mNebuPhrase;
    public Integer mPrecip; // In  mm/10 //*
    public String mPrecipPhrase;
    public Integer mTempe;
    public Integer mTempeRes;
    public Integer mPression;
    public Integer mVentMoyen;
    public Integer mVentRaf;//*
    public Integer mDirVent;
    public Integer mTempeMin;
    public Integer mTempeMax;
    public Date mStart;
    public Date mEnd;
    public Integer mDuration;

    public ForecastCity(Integer fiability, Integer picto, Integer nebu, String nebuPhrase,
                        Integer precip, String precipPhrase, Integer tempe,
                        Integer tempeRes, Integer pression, Integer ventMoyen, Integer ventRaf, Integer dirVent,
                        Integer tempeMin, Integer tempeMax,
                        String start, String end, Integer duration) {
        mFiability = fiability;
        mPicto = picto;
        mNebu = nebu;
        mNebuPhrase = nebuPhrase;
        mPrecip = precip;
        mPrecipPhrase = precipPhrase;
        mTempe = tempe;
        mTempeRes = tempeRes;
        mPression = pression;
        mVentMoyen = ventMoyen;
        mVentRaf = ventRaf;
        mDirVent = dirVent;
        mTempeMin = tempeMin;
        mTempeMax = tempeMax;
        SimpleDateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            mStart = dfm.parse(start);
            mEnd = dfm.parse(end);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mDuration = duration;
    }
}
