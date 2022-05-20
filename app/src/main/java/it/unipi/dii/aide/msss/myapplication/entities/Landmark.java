package it.unipi.dii.aide.msss.myapplication.entities;


public class Landmark  {
    double latitude;
    double longitude;
    int score;
    int bound;

    public Landmark(double latitude, double longitude, int score, int treshold) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.score = score;
        this.bound = treshold;
    }

    public Landmark() {
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getBound() {
        return bound;
    }

    public void setBound(int treshold) {
        this.bound = treshold;
    }

}
