package fr.elol.meteo.data;

import java.io.Serializable;

/**
 * Contains information for displaying an entry in the navigation drawer
 */
public class MenuEntry implements Serializable {
    public static final int MAX_TITLE_LENGTH = 20;

    public Integer mGeoid;
    public String mName;
    public String mShortName;
    public String mZip;
    public Integer mPicto;
    public Integer mTemp;
    public Boolean mIsDay;

    public MenuEntry(Integer geoid, String name, String zip, Integer picto, Integer temp,
                     Boolean isDay) {
        mGeoid = geoid;
        mName = name;
        if (mName.length() > MAX_TITLE_LENGTH) {
            mShortName = mName.substring(0, MAX_TITLE_LENGTH) + "\u2026";
        } else {
            mShortName = mName;
        }
        mZip = zip;
        mPicto = picto;
        mTemp = temp;
        mIsDay = isDay;
    }
}