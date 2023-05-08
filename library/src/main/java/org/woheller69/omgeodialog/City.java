package org.woheller69.omgeodialog;

public class City {

    private String cityName;
    private String countryCode;
    private float lon;
    private float lat;

    public City() {
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public void setLatitude(float latitude) {
        lat = latitude;
    }

    public float getLatitude() {
        return lat;
    }

    public float getLongitude() {
        return lon;
    }

    public void setLongitude(float lon) {
        this.lon = lon;
    }
}
