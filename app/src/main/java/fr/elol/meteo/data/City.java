package fr.elol.meteo.data;

import java.io.Serializable;

/**
 * City information provided by webservice
 */
public class City implements Serializable {
    public Integer mGeoid;
    public String mName;
    public String mStation;
    public Integer mStationId;
    public Double mLat;
    public Double mLng;
    public Integer mElevation;
    public String mZip;
    public Integer mLevel;
    public String mShortName;

    public City (Integer geoid, String name, Double lat, Double lng, Integer elevation,
                 String zip, Integer level) {
        mGeoid = geoid;
        mName = name;
        if (mName.length() > MenuEntry.MAX_TITLE_LENGTH) {
            mShortName = mName.substring(0, MenuEntry.MAX_TITLE_LENGTH) + "\u2026";
        } else {
            mShortName = mName;
        }
        mStation = null;
        mLat = lat;
        mLng = lng;
        mElevation = elevation;
        mZip = zip;
        mLevel = level;
    }

    public City (MenuEntry me) {
        mGeoid = me.mGeoid;
        mName = me.mName;
        mZip = me.mZip;
    }

    public void setStation (String station) {
        mStation = station;
    }
    public void setStationId (Integer stationid) {
        mStationId = stationid;
    }
}
