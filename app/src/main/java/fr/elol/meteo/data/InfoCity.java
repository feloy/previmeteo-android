package fr.elol.meteo.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Structure containing all information about a city (forecasts, ephemeris)
 */
public class InfoCity implements Serializable {

    public City mCity;
    public ArrayList<ForecastCity> mForecastList;
    public ArrayList<EphemerisCity> mEphemerisList;

    public InfoCity (City city) {
        mCity = city;
        mForecastList = new ArrayList<> ();
        mEphemerisList = new ArrayList<> ();
    }

    public void addForecast(Integer fiability, Integer picto, Integer nebu, String nebuPhrase,
                            Integer precip, String precipPhrase, Integer tempe,
                       Integer tempeRes, Integer pression, Integer ventMoyen, Integer ventRaf, Integer dirVent,
                       Integer tempeMin, Integer tempeMax,
                       String start, String end, Integer duration) {
        mForecastList.add (new ForecastCity (fiability, picto, nebu, nebuPhrase, precip, precipPhrase, tempe,
                                   tempeRes, pression, ventMoyen, ventRaf, dirVent,
                                   tempeMin, tempeMax,
                                   start, end, duration));
    }

    public ForecastCity getForecast (Integer hoursAfter, Integer duration) {
        long tms = System.currentTimeMillis() + hoursAfter*60*60*1000;
        for (int i=0; i<mForecastList.size(); i++) {
            ForecastCity fc = mForecastList.get(i);
            if (fc.mDuration.equals(duration) &&
                fc.mStart.getTime() <= tms && tms < fc.mEnd.getTime()) {
                return fc;
            }
        }
        return null;
    }

    public Integer getForecastIndex (Integer hoursAfter, Integer duration) {
        long tms = System.currentTimeMillis() + hoursAfter*60*60*1000;
        for (int i=0; i<mForecastList.size(); i++) {
            ForecastCity fc = mForecastList.get(i);
            if (fc.mDuration.equals(duration) &&
                    fc.mStart.getTime() <= tms && tms < fc.mEnd.getTime()) {
                return i;
            }
        }
        return null;
    }

    public void addEphemeris (String date, String sunrise, String sunset,
                              String moonrise, String moonset, Integer moonphase, Integer moon4) {
        mEphemerisList.add (new EphemerisCity(date, sunrise, sunset, moonrise, moonset,
                                              moonphase, moon4));
    }

    public EphemerisCity getEphemeris (Integer daysAfter) {
        for (int i=0; i<mEphemerisList.size(); i++) {
            EphemerisCity eph = mEphemerisList.get(i);
            long tms = System.currentTimeMillis() + daysAfter*24*60*60*1000;
            if (eph.mDate.getTime () <= tms && tms < (eph.mDate.getTime() + 24*60*60*1000)) {
                return eph;
            }
        }
        return null;
    }

    public EphemerisCity getEphemeris (Date atdate) {
        Calendar cAtdate = Calendar.getInstance();
        Calendar cEph = Calendar.getInstance();
        cAtdate.setTime(atdate);
        int dom = cAtdate.get(Calendar.DAY_OF_MONTH);

        for (int i=0; i<mEphemerisList.size(); i++) {
            EphemerisCity eph = mEphemerisList.get(i);
            cEph.setTime(eph.mDate);
            if (cEph.get(Calendar.DAY_OF_MONTH) == dom) {
                return eph;
            }
        }
        return null;
    }
}
