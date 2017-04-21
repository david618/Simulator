/*
 * DistanceBearing.java
 *
 */

package org.jennings.geomtools;

/**
 *
 * @author djennings
 */
public class DistanceBearing {
    
    /** Creates a new instance of DistanceBearing */
    public DistanceBearing() {
    }

    public DistanceBearing(double distance, double bearing) {
        
        if (distance * 1000 > Earth.Radius * Math.PI) {
            throw new NumberFormatException("distance must be less than half radius of Earth");
        }
        
        if (bearing > 360.0) {
            throw new NumberFormatException("bearing must be less than 180.0");
        }

        if (bearing < -180.0) {
            throw new NumberFormatException("bearing must be greater than -180.0");
        }
        
        this.distance = distance;
        this.bearing = bearing;
        this.bearingReverse = bearing > 0 ? bearing - 180.0 : bearing + 180;
    }

    
    
    /**
     * Holds value of property distance.
     */
    private double distance;

    /**
     * Getter for property distance.
     * @return Value of property distance.
     */
    public double getDistance() {
        return this.distance;
    }

    /**
     * Setter for property distance.
     * @param distance New value of property distance.
     */
    public void setDistance(double distance) {
        this.distance = distance;
    }

    /**
     * Holds value of property bearing.
     */
    private double bearing;

    /**
     * Getter for property bearing.
     * @return Value of property bearing.
     */
    public double getBearing() {
        return this.bearing;
    }

    /**
     * Setter for property bearing.
     * @param bearing New value of property bearing.
     */
    public void setBearing(double bearing) {
        this.bearing = bearing;
    }

    /**
     * Holds value of property bearing2to1.
     */
    private double bearingReverse;

    /**
     * Getter for property bearing2to1.
     * @return Value of property bearing2to1.
     */
    public double bearingReverse() {
        return this.bearingReverse;
    }


    
}
