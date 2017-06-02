package fr.elol.meteo.data;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Ephemeris for a city
 */
public class EphemerisCity implements Serializable {
    public Date mDate;
    public String mSunrise;
    public String mSunset;
    public String mMoonrise;
    public String mMoonset;
    public Integer mMoonphase;
    public Integer mMoon4;

    private Date mSunriseDate;
    private Date mSunsetDate;

    public EphemerisCity (String date, String sunrise, String sunset, String moonrise, String moonset,
                          Integer moonphase, Integer moon4) {
        SimpleDateFormat dfm = new SimpleDateFormat("yyyy-MM-dd");
        try {
            mDate = dfm.parse(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mSunrise = sunrise;
        mSunset = sunset;
        mMoonrise = (moonrise != null && moonrise.equals("null")) ? null : moonrise;
        mMoonset = (moonset != null && moonset.equals("null")) ? null : moonset;
        mMoonphase = moonphase;
        mMoon4 = moon4;

        SimpleDateFormat dfm2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            mSunriseDate = dfm2.parse(date + " " + mSunrise);
            mSunsetDate = dfm2.parse(date + " " + mSunset);
        } catch (Exception e) {
            mSunriseDate = null;
            mSunsetDate = null;
            e.printStackTrace();
        }
    }

    public Boolean moonRiseFirst () {
        if (mMoonset == null)
            return true;
        else if (mMoonrise == null)
            return false;
        else
            return Integer.parseInt(mMoonrise.substring(0, 2)) < Integer.parseInt(mMoonset.substring(0, 2));
    }

    public Boolean isDay (Date d) {
        if (mSunriseDate == null || mSunsetDate == null)
            return true;
        return mSunriseDate.before(d) && d.before(mSunsetDate);
    }
}
