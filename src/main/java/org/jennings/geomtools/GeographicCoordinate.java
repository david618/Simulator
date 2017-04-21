/*
 * GeographicCoordinate.java
 *
 */

package org.jennings.geomtools;

/**
 *
 * @author djennings
 */

public class GeographicCoordinate {
    
    /** Creates a new instance of NewCoordinate */
    public GeographicCoordinate() {
        this.lon = 0.0;
        this.lat = 0.0;
    }

    public GeographicCoordinate(double lon, double lat) {
        
        if (lon > 180.0) {
            throw new NumberFormatException("lon must be less than 180.0");
        }
        
        if (lon < -180.0) {
            throw new NumberFormatException("lon must be greater than -180.0");
        }
        
        if (lat > 90.0) {
            throw new NumberFormatException("lat must be less than 90.0");
        }

        if (lat < -90.0) {
            throw new NumberFormatException("lat must be greater than -90.0");
        }
        
        this.lon = lon;
        this.lat = lat;
    }

    

    /**
     * Holds value of property lon.
     */
    private double lon;

    /**
     * Getter for property lon.
     * @return Value of property lon.
     */
    public double getLon() {
        return this.lon;
    }

    /**
     * Setter for property lon.
     * @param lon New value of property lon.
     */
    public void setLon(double lon) {
        this.lon = lon;
    }

    /**
     * Holds value of property lat.
     */
    private double lat;

    /**
     * Getter for property lat.
     * @return Value of property lat.
     */
    public double getLat() {
        return this.lat;
    }

    /**
     * Setter for property lat.
     * @param lat New value of property lat.
     */
    public void setLat(double lat) {
        this.lat = lat;
    }
    
    public String toString() { 
        return "[" + this.lon + "," + this.lat + "]";
    }
    
}
