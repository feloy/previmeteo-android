package fr.elol.meteo.data;

/**
 * Forecast information for a city, from Previmeteo
 */
public class ForecastMap {
    public City city;
    public Integer mPicto;
    public String mNebuPhrase;
    public String mPrecipPhrase;
    public Integer mPression;
    public Integer mTempe;
    public Integer mTempeRes;
    public Integer mVentMoyen;
    public Integer mDirVent;
    public Boolean mIsDay;
    public Integer mTempeMin;
    public Integer mTempeMax;

    public ForecastMap (Integer geoid, String name, String zip, Double lat, Double lng, Integer level,
                        Integer picto, String nebuPhrase, String precipPhrase, Integer pression, Integer tempe, Integer tempeRes,
                        Integer ventMoyen, Integer dirVent, Boolean isDay, Integer tempeMin, Integer tempeMax) {

        city = new City (geoid, name, lat, lng, null, zip, level);
        mPicto = picto;
        mNebuPhrase = nebuPhrase;
        mPrecipPhrase = precipPhrase;
        mPression = pression;
        mTempe = tempe;
        mTempeRes = tempeRes;
        mVentMoyen = ventMoyen;
        mDirVent = dirVent;
        mIsDay = isDay;
        mTempeMin = tempeMin;
        mTempeMax = tempeMax;
    }
}
