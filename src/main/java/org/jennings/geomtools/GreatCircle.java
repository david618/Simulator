/**
 * Great Circle
 *
 * Code is pretty rough; I got it working for creating circles returning JSONArray
 *
 * Still could use some refinement.
 *
 */
package org.jennings.geomtools;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author jenningd
 */
public class GreatCircle {

    private final double D2R = Math.PI / 180.0;
    private final double R2D = 180.0 / Math.PI;

    private final static DecimalFormat df8 = new DecimalFormat("###0.00000000");
    private final static DecimalFormat df5 = new DecimalFormat("###0.00000");
    private final static DecimalFormat df3 = new DecimalFormat("###0.000");

    final boolean debug = false;

    /**
     *
     * @param coord1 First Coordinate
     * @param coord2 Second Coordinate
     * @return Distance (km) and Bearing (-180 to 180 from North)
     */
    public DistanceBearing getDistanceBearing(GeographicCoordinate coord1, GeographicCoordinate coord2) {

        double lon1 = coord1.getLon();
        double lat1 = coord1.getLon();

        double lon2 = coord2.getLon();
        double lat2 = coord2.getLon();

        DistanceBearing distB = new DistanceBearing();

        double gcDist = 0.0;
        double bearing = 0.0;

        try {

            double lon1R = lon1 * D2R;
            double lat1R = lat1 * D2R;
            double lon2R = lon2 * D2R;
            double lat2R = lat2 * D2R;

            /*
            Functions are a little whacky around the north and south pole.
            The only valid bearing from north pole is -180.
            I wouldn't trust the program for points near the poles.
             */
            if (lat1 - 90.0 < 0.00001) {
                // very close to north pole distance R * theta            
                double l = 90.0 - lat2;
                gcDist = Earth.Radius * l * D2R / 1000.0;
                // let bearing in lon2
                bearing = lon2;
            } else if (lat1 + 90 < 0.00001) {
                // very close to south pole distance R * theta
                double l = lat2 + 90.0;
                gcDist = Earth.Radius * l * D2R / 1000.0;
                bearing = lon2;

            } else {

                // law of Cosines
                double lambda = Math.abs(lon2R - lon1R);

                double x1 = Math.cos(lat2R) * Math.sin(lambda);

                double x2 = Math.cos(lat1R) * Math.sin(lat2R)
                        - Math.sin(lat1R) * Math.cos(lat2R) * Math.cos(lambda);

                double x3 = Math.sin(lat1R) * Math.sin(lat2R)
                        + Math.cos(lat1R) * Math.cos(lat2R) * Math.cos(lambda);

                double x4 = Math.sqrt(x1 * x1 + x2 * x2);

                double sigma = Math.atan2(x4, x3);

                gcDist = sigma * Earth.Radius / 1000.0;

                double y1 = Math.sin(lon2R - lon1R) * Math.cos(lat2R);

                double y2 = Math.cos(lat1R) * Math.sin(lat2R)
                        - Math.sin(lat1R) * Math.cos(lat2R) * Math.cos(lon2R - lon1R);

                double y3 = Math.atan2(y1, y2);

                bearing = (y3 * R2D) % 360;

                // Conver to value from -180 to 180
                bearing = bearing > 180.0 ? bearing - 360.0 : bearing;

            }
        } catch (Exception e) {
            gcDist = -1;
            bearing = 0;
        }

        distB = new DistanceBearing(gcDist, bearing);

        return distB;
    }

    public GeographicCoordinate getNewCoordPair(GeographicCoordinate coord1, DistanceBearing distB) {

        double lat1 = coord1.getLat();
        double lon1 = coord1.getLon();
        double lat2 = 0.0;
        double lon2 = 0.0;

        boolean bln360 = false;

        try {

            // Allow for lon values 180 to 360 (adjust them to -180 to 0)
            double lonDD = lon1;
            if (lonDD > 180.0 && lonDD <= 360) {
                lonDD = lonDD - 360;
                lon1 = lonDD;
                bln360 = true;
            }

            double alpha;
            double l;
            double k;
            double gamma;
            double phi;
            double theta;
            double hdng2;

            double hdng = distB.getBearing();

            if (hdng < 0) {
                hdng = hdng + 360;
            }

            // Round the input            
            BigDecimal bd = new BigDecimal(hdng);
            bd = bd.setScale(6, BigDecimal.ROUND_HALF_UP);
            hdng = bd.doubleValue();

            double dist = distB.getDistance() * 1000;

            if (lat1 == 90 || lat1 == -90) {
                // hdng doesn't make a lot of since at the poles assume this is just the lon
                lon2 = hdng;
                alpha = dist / Earth.Radius;
                if (lat1 == 90) {
                    lat2 = 90 - alpha * R2D;
                } else {
                    lat2 = -90 + alpha * R2D;
                }

            } else if (hdng == 0 || hdng == 360) {
                // going due north within some rounded number
                alpha = dist / Earth.Radius;
                lat2 = lat1 + alpha * R2D;
                lon2 = lon1;
            } else if (hdng == 180) {
                // going due south witin some rounded number
                alpha = dist / Earth.Radius;
                lat2 = lat1 - alpha * R2D;
                lon2 = lon1;
            } else if (hdng == 90) {
                lat2 = lat1;
                l = 90 - lat1;
                alpha = dist / Earth.Radius / Math.sin(l * D2R);
                //phi = Math.asin(Math.sin(alpha)/ Math.sin(l*D2R));                 
                lon2 = lon1 + alpha * R2D;
            } else if (hdng == 270) {
                lat2 = lat1;
                l = 90 - lat1;
                alpha = dist / Earth.Radius / Math.sin(l * D2R);
                //phi = Math.asin(Math.sin(alpha)/ Math.sin(l*D2R));                       
                lon2 = lon1 - alpha * R2D;
            } else if (hdng > 0 && hdng < 180) {
                l = 90 - lat1;
                alpha = dist / Earth.Radius;
                k = Math.acos(Math.cos(alpha) * Math.cos(l * D2R)
                        + Math.sin(alpha) * Math.sin(l * D2R) * Math.cos(hdng * D2R));
                lat2 = 90 - k * R2D;
                double n = (Math.cos(alpha) - Math.cos(k) * Math.cos(l * D2R))
                        / (Math.sin(k) * Math.sin(l * D2R));
                if (n > 1) {
                    n = 1;
                }
                if (n < -1) {
                    n = -1;
                }
                phi = Math.acos(n);
                lon2 = lon1 + phi * R2D;
                theta = Math.sin(phi) * Math.sin(l * D2R) / Math.sin(alpha);
                hdng2 = 180 - theta * R2D;
            } else if (hdng > 180 && hdng < 360) {
                gamma = 360 - hdng;
                l = 90 - lat1;
                alpha = dist / Earth.Radius;
                k = Math.acos(Math.cos(alpha) * Math.cos(l * D2R)
                        + Math.sin(alpha) * Math.sin(l * D2R) * Math.cos(gamma * D2R));
                lat2 = 90 - k * R2D;
                double n = (Math.cos(alpha) - Math.cos(k) * Math.cos(l * D2R))
                        / (Math.sin(k) * Math.sin(l * D2R));
                if (n > 1) {
                    n = 1;
                }
                if (n < -1) {
                    n = -1;
                }
                phi = Math.acos(n);
                lon2 = lon1 - phi * R2D;
                theta = Math.sin(phi) * Math.sin(l * D2R) / Math.sin(alpha);
                hdng2 = 180 - theta * R2D;
            }

            int decimalPlaces = 12;
            bd = new BigDecimal(lat2);
            bd = bd.setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP);
            lat2 = bd.doubleValue();

            bd = new BigDecimal(lon2);
            bd = bd.setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP);
            lon2 = bd.doubleValue();

            if (lat2 > 90) {
                lat2 = 180 - lat2;
                lon2 = (lon2 + 180) % 360;
            }

            if (lon2 > 180) {
                lon2 = lon2 - 360;
            }

            if (lat2 < -90) {
                lat2 = -180 - lat2;
                lon2 = (lon2 - 180) % 360;
            }
            if (lon2 < -180) {
                lon2 = lon2 + 360;
            }

            // adjust the lon back to 360 scale if input was like that
            if (bln360) {
                if (lon2 < 0) {
                    lon2 = lon2 + 360;
                }
            }

        } catch (Exception e) {
            lon2 = -1000;
            lat2 = -1000;
        }

        if (lon2 < -180.0) {
            System.out.println(lon2);
            System.out.println(distB.getBearing());

        }

        GeographicCoordinate nc = new GeographicCoordinate(lon2, lat2);

        return nc;
    }

    public double findCrossing(double x1, double y1, double x2, double y2, double x) {
        double m = (y2 - y1) / (x2 - x1);

        return m * (x - x1) + y1;
    }

//    /**
//     * Generates coordinates for a circle around a Geographic Center
//     *
//     * @param center
//     * @param radiusKM
//     * @param numPoints
//     * @return
//     */
//    public GeographicCoordinate[] createCircle(GeographicCoordinate center, Double radiusKM, Integer numPoints, boolean isClockwise) {
//
//        GeographicCoordinate[] coords = new GeographicCoordinate[numPoints];
//
//        try {
//            if (numPoints == null) {
//                // if null default to 20 points
//                numPoints = 20;
//            }
//
//            if (radiusKM == null) {
//                // default to 50 meters or 0.050 km
//                radiusKM = 0.050;
//            }
//
//            double d = 360.0 / (numPoints - 1);
//            int i = 0;
//            GeographicCoordinate nc1 = new GeographicCoordinate();
//            while (i < numPoints - 1) {
//                double theta = 0.0;
//                if (isClockwise) {
//                    theta = i * d - 180.0;
//                } else {
//                    theta = 180.0 - i * d;
//                }
//
//                DistanceBearing distb = new DistanceBearing(radiusKM, theta);
//
//                GeographicCoordinate nc = getNewCoordPair(center, distb);
//
//                coords[i] = nc;
//
//                i++;
//                if (i == 1) {
//                    nc1 = nc;
//                }
//            }
//
//            // last point same as first
//            coords[i] = nc1;
//
//        } catch (Exception e) {
//            coords = null;
//        }
//
//        return coords;
//    }
    /**
     * Return geometry object rings for Esri or coordinates for GeoJSON
     *
     * @param clon
     * @param clat
     * @param a
     * @param b
     * @param rotationDeg
     * @param radius
     * @param isClockwise
     * @param numPoints
     * @param isEsri
     * @return
     */
    public JSONArray createEllipse3(double clon, double clat, Double a, Double b, Double rotationDeg, Integer numPoints, boolean isClockwise) {

        if (numPoints == null) {
            numPoints = 20;
        }

        if (a == null) {
            // default to 500 meters or 0.50 km
            a = 0.50;
        }

        if (b == null) {
            // default to 50 meters or 0.050 km
            b = 0.050;
        }

        if (rotationDeg == null) {
            rotationDeg = 0.0;
        }

        if (a < b) {
            // Switch a and b
            // Semimajor axis should be the larger of the two
            double t = a;
            a = b;
            b = t;
        }
        
        double c = Math.sqrt(a*a-b*b);
                
        // If this was a circle then even distribuation would be d
        double d = 360.0 / (numPoints - 1);

        // The number of points returns will depend on eccentricity
        ArrayList<GeographicCoordinate> coords = new ArrayList<>();

        int i = 0;
        double v = 360.0;
        GeographicCoordinate coord1 = new GeographicCoordinate(clon, clat);

        GeographicCoordinate lastcoord = coord1;

        while (v > 0.0) {

            double r = v * D2R;
            double sv = Math.sin(r);
            double cv = Math.cos(r);
            double radiusKM = a * b / Math.sqrt(a * a * sv * sv + b * b * cv * cv);
            DistanceBearing distb = new DistanceBearing(radiusKM, v);

            if (rotationDeg != 0.0) {
                double bearing = (distb.getBearing() + rotationDeg) % 360.0;
                distb.setBearing(bearing);
            }

            GeographicCoordinate nc = getNewCoordPair(coord1, distb);

            // Only add the nc if it is different from previous
            double lon = nc.getLon();
            double lat = nc.getLat();

            if (Math.abs(lon - lastcoord.getLon()) < 0.00001 && Math.abs(lat - lastcoord.getLat()) < 0.00001) {
                // skip this point too close to previous
            } else {
                coords.add(nc);
                lastcoord = nc;
            }

            // Scale d based on distance
//            double scale = ((a / b - b / a) / (b - a)) * (distb.getDistance() - a) + b / a;
//            if (scale > 1.0) {
//                scale = 1.0;
//            }
//            double scale = 1.0;
//            if (distb.getDistance() > c) {
//                scale = 1/10;
//            }
            
//            if (debug) {
//                System.out.println(scale + "," + distb.getDistance() + "," + distb.getBearing() + "," + v + "," + nc.getLon() + "," + nc.getLat());
//            }
            // Densifying the points around 0/360 and 180; just 10 degrees out really improved the polygons
            if (v > 350 || v < 10 || (v > 170 && v < 190)) {
                v = v - d*b/a;
            } else {
                v = v - d;
            }
            

        }

        // Added code to prevent adding a last point that was too close to the end point.
        GeographicCoordinate firstPoint = coords.get(0);
        GeographicCoordinate lastPoint = coords.get(coords.size() - 1);
        
        if (Math.abs(firstPoint.getLon() - lastPoint.getLon()) < 0.00001 && Math.abs(firstPoint.getLat() - lastPoint.getLat()) < 0.00001) {
            // Replace last point with first
            coords.set(coords.size() - 1, firstPoint);
        } else {
            // Append firstpoint 
            coords.add(firstPoint);

        }

        //coords.add(coords.get(0)); // Last point same as first

//        for (GeographicCoordinate tt: coords) {
//            System.out.println(tt.getLon() + "," + tt.getLat());
//        }
        GeographicCoordinate nc1 = new GeographicCoordinate();
        GeographicCoordinate nc2 = new GeographicCoordinate();

        JSONArray[] exteriorRing = new JSONArray[4];
        exteriorRing[0] = new JSONArray();
        exteriorRing[1] = new JSONArray();

        int ringNum = 0;
        int numRings = 1;

        nc1 = coords.get(0);
        i = 0;
        double lon1 = nc1.getLon();
        double lat1 = nc1.getLat();
        if (debug) {
            System.out.println(i + ":" + lon1 + "," + lat1);
        }
        JSONArray coord = new JSONArray("[" + lon1 + ", " + lat1 + "]");
        exteriorRing[ringNum].put(coord);
        i++;

        boolean crossedDTEast = false;
        boolean crossedDTWest = false;
        boolean crossedZeroEast = false;
        boolean crossedZeroWest = false;

        while (i < coords.size()) {

            nc2 = coords.get(i);
            // Compare coordinates
            double lon2 = nc2.getLon();
            double lat2 = nc2.getLat();

            if (lon2 >= 0.0 && lon1 <= 0.0) {

                if (Math.abs(lon2 - lon1) > 180.0) {
                    // Either gone from -179.xx to 179.xx (Crossing DT heading east)
                    if (debug) {
                        System.out.println("Crossing DT heading west");
                    }
                    crossedDTWest = true;

                    double x1 = (lon1 < 0) ? lon1 + 360.0 : lon1;
                    double x2 = (lon2 < 0) ? lon2 + 360.0 : lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 180.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    // Add point to current ring
                    coord = new JSONArray("[-180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                    if (crossedZeroWest) {
                        // South Pole in Footprint
                        coord = new JSONArray("[-180.0, -90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0, -90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedZeroWest) {
                        // South Pole in Footprint
                        coord = new JSONArray("[0.0, -90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[180, -90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Add point to other ring
                    coord = new JSONArray("[180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                } else {
                    // or gone from -1.xx to 1.xx  (Crossing 0 heading east)
                    if (debug) {
                        System.out.println("Crossing 0 heading east");
                    }

                    double x1 = lon1;
                    double x2 = lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 0.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    // Add point to current ring
                    if (lon2 > 0.0) {
                        coord = new JSONArray("[0.0, " + crossing + "]");
                        exteriorRing[ringNum].put(coord);
                    }

                    if (crossedDTEast) {
                        // North Pole is in footprint add 180,90 and 0, 90 
                        coord = new JSONArray("[0.0,90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[-180.0,90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedDTEast) {
                        // North pole is in foot print add point 0,90, -180,90  
                        coord = new JSONArray("[180.0,90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0,90.0]");
                        exteriorRing[ringNum].put(coord);

                    }

                    // Add point to other ring
                    if (lon2 > 0.0) {
                        coord = new JSONArray("[0.0, " + crossing + "]");
                        exteriorRing[ringNum].put(coord);
                    }

                    crossedZeroEast = true;
                }

            } else if (lon2 < 0.0 && lon1 > 0.0) {

                if (Math.abs(lon2 - lon1) > 180.0) {
                    // Either gone from 179.xx to -179.xx (Crossing DT heading west)                    
                    if (debug) {
                        System.out.println("Crossing DT heading east");
                    }
                    crossedDTEast = true;

                    double x1 = (lon1 < 0) ? lon1 + 360.0 : lon1;
                    double x2 = (lon2 < 0) ? lon2 + 360.0 : lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 180.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    coord = new JSONArray("[180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                    if (crossedZeroEast) {
                        // North Pole in Footprint
                        coord = new JSONArray("[180.0, 90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0, 90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedZeroEast) {
                        // North Pole in Footprint
                        coord = new JSONArray("[0.0, 90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[-180, 90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    coord = new JSONArray("[-180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                } else {
                    if (debug) {
                        System.out.println("Crossing 0 heading west");
                    }
                    crossedZeroWest = true;

                    // or gone from 1.xx to -1.xx (Crossing 0 heading west 
                    double x1 = lon1;
                    double x2 = lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 0.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    // Add point to current ring
                    coord = new JSONArray("[0.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                    if (crossedDTWest) {
                        // South Pole is in footprint 
                        coord = new JSONArray("[0.0,-90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[180.0,-90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedDTWest) {
                        // North pole is in foot print add point 0,90, -180,90  
                        coord = new JSONArray("[-180.0,-90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0,-90.0]");
                        exteriorRing[ringNum].put(coord);

                    }

                    // Add point to other ring
                    coord = new JSONArray("[0.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);
                }
            }

            if (debug) {
                System.out.println(i + ":" + lon2 + "," + lat2);
            }

            if (i < coords.size() - 1) {
                // Inject last coord
                coord = new JSONArray("[" + lon2 + ", " + lat2 + "]");
                exteriorRing[ringNum].put(coord);
            }

            nc1 = nc2;
            lon1 = nc1.getLon();
            lat1 = nc1.getLat();
            i++;

        }

        JSONArray polys = new JSONArray();
        JSONArray poly = new JSONArray();

        double plon = clon;
        double plat = clat;

        for (i = 0; i < 2; i++) {
            poly = new JSONArray();
            if (exteriorRing[i].length() > 0) {
                // Add last point same as first
                exteriorRing[i].put(exteriorRing[i].get(0));

                // This code checks for points that are too close in final exterior Rings
//                int k = 0;
//                while (k < exteriorRing[i].length()) {
//                    JSONArray pts = (JSONArray) exteriorRing[i].get(k);
//                    double lon = pts.getDouble(0);
//                    double lat = pts.getDouble(1);
//                    if (Math.abs(lon - plon) < 0.00001 && Math.abs(lat - plat) < 0.00001) {
//                        System.out.println(plon + "," + plat + "," + lon + "," + lat);
//                    }
//                    plon = lon;
//                    plat = lat;
//
//                    k++;
//                }

                if (isClockwise) {
                    // Reverse order 
                    JSONArray reversedPoly = new JSONArray();
                    int j = exteriorRing[i].length() - 1;
                    while (j >= 0) {
                        reversedPoly.put(exteriorRing[i].get(j));
                        j--;
                    }
                    poly.put(reversedPoly);
                } else {
                    poly.put(exteriorRing[i]);
                }
                polys.put(poly);
            }
        }

        if (debug) {
            System.out.println("HERE1");
            i = 0;
            while (i < exteriorRing[0].length()) {
                System.out.println(exteriorRing[0].get(i));
                i++;
            }

            System.out.println("HERE2");
            i = 0;
            while (i < exteriorRing[1].length()) {
                System.out.println(exteriorRing[1].get(i));
                i++;
            }

        }
        //System.out.println(poly);
        // Create the Geom
        return polys;

    }

    static double computeDpt(double a, double b, double theta) {
        double dp = 0.0;

        double dpt_sin = Math.pow(a * Math.sin(theta), 2.0);
        double dpt_cos = Math.pow(b * Math.cos(theta), 2.0);
        dp = Math.sqrt(dpt_sin + dpt_cos);

        return dp;
    }

    /**
     * Return geometry object rings for Esri or coordinates for GeoJSON
     *
     * @param clon
     * @param clat
     * @param a
     * @param b
     * @param rotationDeg
     * @param radius
     * @param isClockwise
     * @param numPoints
     * @param isEsri
     * @return
     */
    public JSONArray createEllipse2(double clon, double clat, Double a, Double b, Double rotationDeg, Integer numPoints, boolean isClockwise) {

        if (numPoints == null) {
            numPoints = 20;
        }

        if (a == null) {
            // default to 50 meters or 0.050 km
            a = 0.050;
        }

        if (b == null) {
            // default to 50 meters or 0.050 km
            a = 0.050;
        }

        if (rotationDeg == null) {
            rotationDeg = 0.0;
        }

        double theta = 0.0;
        double twoPi = Math.PI * 2.0;
        double deltaTheta = 0.0001;
        double numIntegrals = Math.round(twoPi / deltaTheta);
        double circ = 0.0;
        double dpt = 0.0;

        /* integrate over the elipse to get the circumference */
        for (int i = 0; i < numIntegrals; i++) {
            theta += i * deltaTheta;
            dpt = computeDpt(a, b, theta);
            circ += dpt;
        }
        System.out.println("number of interals = " + numIntegrals);
        System.out.println("circumference = " + circ * deltaTheta);

        int n = numPoints - 1; // Number of point
        int nextPoint = 0;
        double run = 0.0;
        theta = 0.0;

        GeographicCoordinate coord1 = new GeographicCoordinate(clon, clat);
        GeographicCoordinate coords[] = new GeographicCoordinate[numPoints];

        int cnt = 0;
        for (int i = 0; i < numIntegrals; i++) {
            theta += deltaTheta;
            double subIntegral = n * run / circ;
            if ((int) subIntegral >= nextPoint) {

                double sv = Math.sin(theta);
                double cv = Math.cos(theta);
                double radiusKM = a * b / Math.sqrt(a * a * sv * sv + b * b * cv * cv);
                DistanceBearing distb = new DistanceBearing(radiusKM, theta * R2D);
                GeographicCoordinate nc = getNewCoordPair(coord1, distb);

                double x = nc.getLon();
                double y = nc.getLat();
                coords[cnt] = nc;

                System.out.println(cnt + "," + "x=" + Math.round(x) + ", y=" + Math.round(y) + "," + theta);
                nextPoint++;
                cnt += 1;
            }
            run += computeDpt(a, b, theta);
        }

        coords[numPoints - 1] = coords[0]; // Last point same as first

        // Reverse the array
        for (int i = 0; i < coords.length / 2; i++) {
            GeographicCoordinate temp = coords[i];
            coords[i] = coords[coords.length - i - 1];
            coords[coords.length - i - 1] = temp;
        }

        GeographicCoordinate nc1 = new GeographicCoordinate();
        GeographicCoordinate nc2 = new GeographicCoordinate();
        GeographicCoordinate lastcoord = new GeographicCoordinate();

        JSONArray[] exteriorRing = new JSONArray[4];
        exteriorRing[0] = new JSONArray();
        exteriorRing[1] = new JSONArray();

        int ringNum = 0;
        int numRings = 1;

        nc1 = coords[0];
        int i = 0;
        double lon1 = nc1.getLon();
        double lat1 = nc1.getLat();
        if (debug) {
            System.out.println(i + ":" + lon1 + "," + lat1);
        }
        JSONArray coord = new JSONArray("[" + lon1 + ", " + lat1 + "]");
        exteriorRing[ringNum].put(coord);
        i++;

        boolean crossedDTEast = false;
        boolean crossedDTWest = false;
        boolean crossedZeroEast = false;
        boolean crossedZeroWest = false;

        while (i < numPoints) {
            nc2 = coords[i];
            // Compare coordinates
            double lon2 = nc2.getLon();
            double lat2 = nc2.getLat();

            if (lon2 >= 0.0 && lon1 <= 0.0) {

                if (Math.abs(lon2 - lon1) > 180.0) {
                    // Either gone from -179.xx to 179.xx (Crossing DT heading east)
                    if (debug) {
                        System.out.println("Crossing DT heading west");
                    }
                    crossedDTWest = true;

                    double x1 = (lon1 < 0) ? lon1 + 360.0 : lon1;
                    double x2 = (lon2 < 0) ? lon2 + 360.0 : lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 180.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    // Add point to current ring
                    coord = new JSONArray("[-180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                    if (crossedZeroWest) {
                        // South Pole in Footprint
                        coord = new JSONArray("[-180.0, -90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0, -90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedZeroWest) {
                        // South Pole in Footprint
                        coord = new JSONArray("[0.0, -90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[180, -90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Add point to other ring
                    coord = new JSONArray("[180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                } else {
                    // or gone from -1.xx to 1.xx  (Crossing 0 heading east)
                    if (debug) {
                        System.out.println("Crossing 0 heading east");
                    }

                    double x1 = lon1;
                    double x2 = lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 0.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    // Add point to current ring
                    if (lon2 > 0.0) {
                        coord = new JSONArray("[0.0, " + crossing + "]");
                        exteriorRing[ringNum].put(coord);
                    }

                    if (crossedDTEast) {
                        // North Pole is in footprint add 180,90 and 0, 90 
                        coord = new JSONArray("[0.0,90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[-180.0,90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedDTEast) {
                        // North pole is in foot print add point 0,90, -180,90  
                        coord = new JSONArray("[180.0,90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0,90.0]");
                        exteriorRing[ringNum].put(coord);

                    }

                    // Add point to other ring
                    if (lon2 > 0.0) {
                        coord = new JSONArray("[0.0, " + crossing + "]");
                        exteriorRing[ringNum].put(coord);
                    }

                    crossedZeroEast = true;
                }

            } else if (lon2 < 0.0 && lon1 > 0.0) {

                if (Math.abs(lon2 - lon1) > 180.0) {
                    // Either gone from 179.xx to -179.xx (Crossing DT heading west)                    
                    if (debug) {
                        System.out.println("Crossing DT heading east");
                    }
                    crossedDTEast = true;

                    double x1 = (lon1 < 0) ? lon1 + 360.0 : lon1;
                    double x2 = (lon2 < 0) ? lon2 + 360.0 : lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 180.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    coord = new JSONArray("[180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                    if (crossedZeroEast) {
                        // North Pole in Footprint
                        coord = new JSONArray("[180.0, 90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0, 90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedZeroEast) {
                        // North Pole in Footprint
                        coord = new JSONArray("[0.0, 90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[-180, 90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    coord = new JSONArray("[-180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                } else {
                    if (debug) {
                        System.out.println("Crossing 0 heading west");
                    }
                    crossedZeroWest = true;

                    // or gone from 1.xx to -1.xx (Crossing 0 heading west 
                    double x1 = lon1;
                    double x2 = lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 0.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    // Add point to current ring
                    coord = new JSONArray("[0.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                    if (crossedDTWest) {
                        // South Pole is in footprint 
                        coord = new JSONArray("[0.0,-90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[180.0,-90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedDTWest) {
                        // North pole is in foot print add point 0,90, -180,90  
                        coord = new JSONArray("[-180.0,-90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0,-90.0]");
                        exteriorRing[ringNum].put(coord);

                    }

                    // Add point to other ring
                    coord = new JSONArray("[0.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);
                }
            }

            if (debug) {
                System.out.println(i + ":" + lon2 + "," + lat2);
            }

            if (i < numPoints - 1) {
                // Don't inject last point here
                coord = new JSONArray("[" + lon2 + ", " + lat2 + "]");
                exteriorRing[ringNum].put(coord);
            }

            nc1 = nc2;
            lon1 = nc1.getLon();
            lat1 = nc1.getLat();
            i++;

        }

        JSONArray polys = new JSONArray();
        JSONArray poly = new JSONArray();

        for (i = 0; i < 2; i++) {
            poly = new JSONArray();
            if (exteriorRing[i].length() > 0) {
                exteriorRing[i].put(exteriorRing[i].get(0));

                if (isClockwise) {
                    // Reverse order 
                    JSONArray reversedPoly = new JSONArray();
                    int j = exteriorRing[i].length() - 1;
                    while (j >= 0) {
                        reversedPoly.put(exteriorRing[i].get(j));
                        j--;
                    }
                    poly.put(reversedPoly);
                } else {
                    poly.put(exteriorRing[i]);
                }
                polys.put(poly);
            }
        }

        if (debug) {
            System.out.println("HERE1");
            i = 0;
            while (i < exteriorRing[0].length()) {
                System.out.println(exteriorRing[0].get(i));
                i++;
            }

            System.out.println("HERE2");
            i = 0;
            while (i < exteriorRing[1].length()) {
                System.out.println(exteriorRing[1].get(i));
                i++;
            }

        }
        //System.out.println(poly);
        // Create the Geom
        return polys;

    }

    /**
     * Return geometry object rings for Esri or coordinates for GeoJSON
     *
     * @param clon
     * @param clat
     * @param a
     * @param b
     * @param rotationDeg
     * @param radius
     * @param isClockwise
     * @param numPoints
     * @param isEsri
     * @return
     */
    public JSONArray createEllipse(double clon, double clat, Double a, Double b, Double rotationDeg, Integer numPoints, boolean isClockwise) {

        if (numPoints == null) {
            numPoints = 20;
        }

        if (a == null) {
            // default to 50 meters or 0.050 km
            a = 0.050;
        }

        if (b == null) {
            // default to 50 meters or 0.050 km
            a = 0.050;
        }

        if (rotationDeg == null) {
            rotationDeg = 0.0;
        }

        double d = 360.0 / (numPoints - 1);

        GeographicCoordinate coords[] = new GeographicCoordinate[numPoints];

        int i = 0;
        while (i < numPoints - 1) {
            double v = 0.0;
            if (false) {  // default to counterclockwise
                v = i * d - 180.0;  // clockwise (Esri wants this)
            } else {
                v = 180.0 - i * d;  // counterclockwise (GeoJSON wants this)
            }

            GeographicCoordinate coord1 = new GeographicCoordinate(clon, clat);
            double r = v * D2R;
            double sv = Math.sin(r);
            double cv = Math.cos(r);
            double radiusKM = a * b / Math.sqrt(a * a * sv * sv + b * b * cv * cv);
            DistanceBearing distb = new DistanceBearing(radiusKM, v);

            if (rotationDeg != 0.0) {
                double bearing = (distb.getBearing() + rotationDeg) % 360.0;
                distb.setBearing(bearing);
            }

            GeographicCoordinate nc = getNewCoordPair(coord1, distb);

            coords[i] = nc;
            i++;

        }

        coords[numPoints - 1] = coords[0]; // Last point same as first

        GeographicCoordinate nc1 = new GeographicCoordinate();
        GeographicCoordinate nc2 = new GeographicCoordinate();
        GeographicCoordinate lastcoord = new GeographicCoordinate();

        JSONArray[] exteriorRing = new JSONArray[4];
        exteriorRing[0] = new JSONArray();
        exteriorRing[1] = new JSONArray();

        int ringNum = 0;
        int numRings = 1;

        nc1 = coords[0];
        i = 0;
        double lon1 = nc1.getLon();
        double lat1 = nc1.getLat();
        if (debug) {
            System.out.println(i + ":" + lon1 + "," + lat1);
        }
        JSONArray coord = new JSONArray("[" + lon1 + ", " + lat1 + "]");
        exteriorRing[ringNum].put(coord);
        i++;

        boolean crossedDTEast = false;
        boolean crossedDTWest = false;
        boolean crossedZeroEast = false;
        boolean crossedZeroWest = false;

        while (i < numPoints) {
            nc2 = coords[i];
            // Compare coordinates
            double lon2 = nc2.getLon();
            double lat2 = nc2.getLat();

            if (lon2 >= 0.0 && lon1 <= 0.0) {

                if (Math.abs(lon2 - lon1) > 180.0) {
                    // Either gone from -179.xx to 179.xx (Crossing DT heading east)
                    if (debug) {
                        System.out.println("Crossing DT heading west");
                    }
                    crossedDTWest = true;

                    double x1 = (lon1 < 0) ? lon1 + 360.0 : lon1;
                    double x2 = (lon2 < 0) ? lon2 + 360.0 : lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 180.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    // Add point to current ring
                    coord = new JSONArray("[-180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                    if (crossedZeroWest) {
                        // South Pole in Footprint
                        coord = new JSONArray("[-180.0, -90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0, -90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedZeroWest) {
                        // South Pole in Footprint
                        coord = new JSONArray("[0.0, -90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[180, -90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Add point to other ring
                    coord = new JSONArray("[180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                } else {
                    // or gone from -1.xx to 1.xx  (Crossing 0 heading east)
                    if (debug) {
                        System.out.println("Crossing 0 heading east");
                    }

                    double x1 = lon1;
                    double x2 = lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 0.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    // Add point to current ring
                    if (lon2 > 0.0) {
                        coord = new JSONArray("[0.0, " + crossing + "]");
                        exteriorRing[ringNum].put(coord);
                    }

                    if (crossedDTEast) {
                        // North Pole is in footprint add 180,90 and 0, 90 
                        coord = new JSONArray("[0.0,90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[-180.0,90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedDTEast) {
                        // North pole is in foot print add point 0,90, -180,90  
                        coord = new JSONArray("[180.0,90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0,90.0]");
                        exteriorRing[ringNum].put(coord);

                    }

                    // Add point to other ring
                    if (lon2 > 0.0) {
                        coord = new JSONArray("[0.0, " + crossing + "]");
                        exteriorRing[ringNum].put(coord);
                    }

                    crossedZeroEast = true;
                }

            } else if (lon2 < 0.0 && lon1 > 0.0) {

                if (Math.abs(lon2 - lon1) > 180.0) {
                    // Either gone from 179.xx to -179.xx (Crossing DT heading west)                    
                    if (debug) {
                        System.out.println("Crossing DT heading east");
                    }
                    crossedDTEast = true;

                    double x1 = (lon1 < 0) ? lon1 + 360.0 : lon1;
                    double x2 = (lon2 < 0) ? lon2 + 360.0 : lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 180.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    coord = new JSONArray("[180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                    if (crossedZeroEast) {
                        // North Pole in Footprint
                        coord = new JSONArray("[180.0, 90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0, 90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedZeroEast) {
                        // North Pole in Footprint
                        coord = new JSONArray("[0.0, 90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[-180, 90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    coord = new JSONArray("[-180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                } else {
                    if (debug) {
                        System.out.println("Crossing 0 heading west");
                    }
                    crossedZeroWest = true;

                    // or gone from 1.xx to -1.xx (Crossing 0 heading west 
                    double x1 = lon1;
                    double x2 = lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 0.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    // Add point to current ring
                    coord = new JSONArray("[0.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                    if (crossedDTWest) {
                        // South Pole is in footprint 
                        coord = new JSONArray("[0.0,-90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[180.0,-90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedDTWest) {
                        // North pole is in foot print add point 0,90, -180,90  
                        coord = new JSONArray("[-180.0,-90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0,-90.0]");
                        exteriorRing[ringNum].put(coord);

                    }

                    // Add point to other ring
                    coord = new JSONArray("[0.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);
                }
            }

            if (debug) {
                System.out.println(i + ":" + lon2 + "," + lat2);
            }

            if (i < numPoints - 1) {
                // Don't inject last point here
                coord = new JSONArray("[" + lon2 + ", " + lat2 + "]");
                exteriorRing[ringNum].put(coord);
            }

            nc1 = nc2;
            lon1 = nc1.getLon();
            lat1 = nc1.getLat();
            i++;

        }

        JSONArray polys = new JSONArray();
        JSONArray poly = new JSONArray();

        for (i = 0; i < 2; i++) {
            poly = new JSONArray();
            if (exteriorRing[i].length() > 0) {
                exteriorRing[i].put(exteriorRing[i].get(0));

                if (isClockwise) {
                    // Reverse order 
                    JSONArray reversedPoly = new JSONArray();
                    int j = exteriorRing[i].length() - 1;
                    while (j >= 0) {
                        reversedPoly.put(exteriorRing[i].get(j));
                        j--;
                    }
                    poly.put(reversedPoly);
                } else {
                    poly.put(exteriorRing[i]);
                }
                polys.put(poly);
            }
        }

        if (debug) {
            System.out.println("HERE1");
            i = 0;
            while (i < exteriorRing[0].length()) {
                System.out.println(exteriorRing[0].get(i));
                i++;
            }

            System.out.println("HERE2");
            i = 0;
            while (i < exteriorRing[1].length()) {
                System.out.println(exteriorRing[1].get(i));
                i++;
            }

        }
        //System.out.println(poly);
        // Create the Geom
        return polys;

    }

    /**
     * Return geometry object rings for Esri or coordinates for GeoJSON
     *
     * @param clon
     * @param clat
     * @param radius
     * @param numPoints
     * @param isEsri
     * @return
     */
    public JSONArray createCircle(double clon, double clat, Double radiusKM, Integer numPoints, boolean isClockwise) {

        if (numPoints == null) {
            numPoints = 20;
        }

        if (radiusKM == null) {
            // default to 50 meters or 0.050 km
            radiusKM = 0.050;
        }

        double d = 360.0 / (numPoints - 1);

        GeographicCoordinate coords[] = new GeographicCoordinate[numPoints];

        int i = 0;
        while (i < numPoints - 1) {
            double v = 0.0;
            if (false) {  // default to counterclockwise
                v = i * d - 180.0;  // clockwise (Esri wants this)
            } else {
                v = 180.0 - i * d;  // counterclockwise (GeoJSON wants this)
            }

            GeographicCoordinate coord1 = new GeographicCoordinate(clon, clat);
            DistanceBearing distb = new DistanceBearing(radiusKM, v);

            GeographicCoordinate nc = getNewCoordPair(coord1, distb);

            coords[i] = nc;
            i++;

        }

        coords[numPoints - 1] = coords[0]; // Last point same as first

        GeographicCoordinate nc1 = new GeographicCoordinate();
        GeographicCoordinate nc2 = new GeographicCoordinate();
        GeographicCoordinate lastcoord = new GeographicCoordinate();

        JSONArray[] exteriorRing = new JSONArray[4];
        exteriorRing[0] = new JSONArray();
        exteriorRing[1] = new JSONArray();

        int ringNum = 0;
        int numRings = 1;

        nc1 = coords[0];
        i = 0;
        double lon1 = nc1.getLon();
        double lat1 = nc1.getLat();
        if (debug) {
            System.out.println(i + ":" + lon1 + "," + lat1);
        }
        JSONArray coord = new JSONArray("[" + lon1 + ", " + lat1 + "]");
        exteriorRing[ringNum].put(coord);
        i++;

        boolean crossedDTEast = false;
        boolean crossedDTWest = false;
        boolean crossedZeroEast = false;
        boolean crossedZeroWest = false;

        while (i < numPoints) {
            nc2 = coords[i];
            // Compare coordinates
            double lon2 = nc2.getLon();
            double lat2 = nc2.getLat();

            if (lon2 >= 0.0 && lon1 <= 0.0) {

                if (Math.abs(lon2 - lon1) > 180.0) {
                    // Either gone from -179.xx to 179.xx (Crossing DT heading east)
                    if (debug) {
                        System.out.println("Crossing DT heading west");
                    }
                    crossedDTWest = true;

                    double x1 = (lon1 < 0) ? lon1 + 360.0 : lon1;
                    double x2 = (lon2 < 0) ? lon2 + 360.0 : lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 180.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    // Add point to current ring
                    coord = new JSONArray("[-180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                    if (crossedZeroWest) {
                        // South Pole in Footprint
                        coord = new JSONArray("[-180.0, -90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0, -90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedZeroWest) {
                        // South Pole in Footprint
                        coord = new JSONArray("[0.0, -90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[180, -90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Add point to other ring
                    coord = new JSONArray("[180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                } else {
                    // or gone from -1.xx to 1.xx  (Crossing 0 heading east)
                    if (debug) {
                        System.out.println("Crossing 0 heading east");
                    }

                    double x1 = lon1;
                    double x2 = lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 0.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    // Add point to current ring
                    if (lon2 > 0.0) {
                        coord = new JSONArray("[0.0, " + crossing + "]");
                        exteriorRing[ringNum].put(coord);
                    }

                    if (crossedDTEast) {
                        // North Pole is in footprint add 180,90 and 0, 90 
                        coord = new JSONArray("[0.0,90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[-180.0,90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedDTEast) {
                        // North pole is in foot print add point 0,90, -180,90  
                        coord = new JSONArray("[180.0,90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0,90.0]");
                        exteriorRing[ringNum].put(coord);

                    }

                    // Add point to other ring
                    if (lon2 > 0.0) {
                        coord = new JSONArray("[0.0, " + crossing + "]");
                        exteriorRing[ringNum].put(coord);
                    }

                    crossedZeroEast = true;
                }

            } else if (lon2 < 0.0 && lon1 > 0.0) {

                if (Math.abs(lon2 - lon1) > 180.0) {
                    // Either gone from 179.xx to -179.xx (Crossing DT heading west)                    
                    if (debug) {
                        System.out.println("Crossing DT heading east");
                    }
                    crossedDTEast = true;

                    double x1 = (lon1 < 0) ? lon1 + 360.0 : lon1;
                    double x2 = (lon2 < 0) ? lon2 + 360.0 : lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 180.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    coord = new JSONArray("[180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                    if (crossedZeroEast) {
                        // North Pole in Footprint
                        coord = new JSONArray("[180.0, 90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0, 90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedZeroEast) {
                        // North Pole in Footprint
                        coord = new JSONArray("[0.0, 90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[-180, 90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    coord = new JSONArray("[-180.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                } else {
                    if (debug) {
                        System.out.println("Crossing 0 heading west");
                    }
                    crossedZeroWest = true;

                    // or gone from 1.xx to -1.xx (Crossing 0 heading west 
                    double x1 = lon1;
                    double x2 = lon2;
                    double crossing = findCrossing(x1, lat1, x2, lat2, 0.0);
                    if (debug) {
                        System.out.println(crossing);
                    }

                    // Add point to current ring
                    coord = new JSONArray("[0.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);

                    if (crossedDTWest) {
                        // South Pole is in footprint 
                        coord = new JSONArray("[0.0,-90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[180.0,-90.0]");
                        exteriorRing[ringNum].put(coord);
                    }

                    // Swith to other ring
                    if (ringNum == 0) {
                        ringNum = 1;
                    } else if (ringNum == 1) {
                        ringNum = 0;
                    }

                    if (crossedDTWest) {
                        // North pole is in foot print add point 0,90, -180,90  
                        coord = new JSONArray("[-180.0,-90.0]");
                        exteriorRing[ringNum].put(coord);
                        coord = new JSONArray("[0.0,-90.0]");
                        exteriorRing[ringNum].put(coord);

                    }

                    // Add point to other ring
                    coord = new JSONArray("[0.0, " + crossing + "]");
                    exteriorRing[ringNum].put(coord);
                }
            }

            if (debug) {
                System.out.println(i + ":" + lon2 + "," + lat2);
            }

            if (i < numPoints - 1) {
                // Don't inject last point here
                coord = new JSONArray("[" + lon2 + ", " + lat2 + "]");
                exteriorRing[ringNum].put(coord);
            }

            nc1 = nc2;
            lon1 = nc1.getLon();
            lat1 = nc1.getLat();
            i++;

        }

        JSONArray polys = new JSONArray();
        JSONArray poly = new JSONArray();

        for (i = 0; i < 2; i++) {
            poly = new JSONArray();
            if (exteriorRing[i].length() > 0) {
                exteriorRing[i].put(exteriorRing[i].get(0));

                if (isClockwise) {
                    // Reverse order 
                    JSONArray reversedPoly = new JSONArray();
                    int j = exteriorRing[i].length() - 1;
                    while (j >= 0) {
                        reversedPoly.put(exteriorRing[i].get(j));
                        j--;
                    }
                    poly.put(reversedPoly);
                } else {
                    poly.put(exteriorRing[i]);
                }
                polys.put(poly);
            }
        }

        if (debug) {
            System.out.println("HERE1");
            i = 0;
            while (i < exteriorRing[0].length()) {
                System.out.println(exteriorRing[0].get(i));
                i++;
            }

            System.out.println("HERE2");
            i = 0;
            while (i < exteriorRing[1].length()) {
                System.out.println(exteriorRing[1].get(i));
                i++;
            }

        }
        //System.out.println(poly);
        // Create the Geom
        return polys;

    }

    public JSONObject generateProperties(int i, double lon, double lat, double size) {
        JSONObject properties = new JSONObject();

        properties.put("fid", i);
        properties.put("longitude", lon);
        properties.put("latitude", lat);
        properties.put("size", size);
        properties.put("rndfield1", generateRandomWords(8));
        properties.put("rndfield2", generateRandomWords(8));
        properties.put("rndfield3", generateRandomWords(8));
        properties.put("rndfield4", generateRandomWords(8));

        return properties;
    }

    public String generateRandomWords(int numchars) {
        Random random = new Random();
        char[] word = new char[numchars];
        for (int j = 0; j < word.length; j++) {
            word[j] = (char) ('a' + random.nextInt(26));
        }
        return new String(word);
    }

    public void testCircles() {
        GreatCircle gc = new GreatCircle();

        GeographicCoordinate center = new GeographicCoordinate(0, 0);

//        // Create a circle; returns points in clockwise direction start from bearing of 0 (North)
//        GeographicCoordinate[] coords = gc.createCircle(center, 120.0, 39, true);
//
//        System.out.println(coords.length);
//
//        // Show the points
//        int i = 0;
//        for (GeographicCoordinate coord : coords) {
//            System.out.println(i + ":" + coord);
//            i++;
//        }
//
//        System.out.println();
//        i = coords.length;
//        // Show the points in reverse order
//        while (i > 0) {
//            i--;
//            System.out.println(i + ":" + coords[i]);
//        }
        // Create GeoJSON 
        JSONObject featureCollection = new JSONObject();
        featureCollection.put("type", "FeatureCollection");

        JSONArray features = new JSONArray();

        JSONObject feature = new JSONObject();
        feature.put("type", "Feature");

        // Add properties (At least one is required)
        JSONObject properties = new JSONObject();
        properties.put("fid", 1);
        feature.put("properties", properties);

        // Get the coordinates
        JSONArray coords = gc.createCircle(-99.0, 33.0, 100.0, 100, false);

        JSONObject geom = new JSONObject();
        geom.put("type", "Polygon");
        geom.put("coordinates", coords);

        feature.put("geometry", geom);

        features.put(feature);
        featureCollection.put("features", features);

        //System.out.println(featureCollection.toString());
    }

    public void createEsriTest() {
        GreatCircle gc = new GreatCircle();

        double lon = -99.0;
        double lat = 33.0;
        double size = 300.0;
        int numPoints = 100;

        JSONArray features = new JSONArray();

        JSONObject feature = new JSONObject();

        // Add properties (At least one is required)
        JSONObject properties = gc.generateProperties(1, lon, lat, size);
        feature.put("feature", properties);

        // Get the coordinates
        JSONArray coords = gc.createCircle(lon, lat, size, numPoints, true);

        JSONObject geom = new JSONObject();
        geom.put("coordinates", coords);

        feature.put("geometry", geom);

        features.put(feature);

        JSONObject json = new JSONObject();
        json.put("features", features);

        //System.out.println(json.toString());
    }

    public void createGeojsonEllipseTest(double lon, double lat, double a, double b, double rot, int numPoints) {
        GreatCircle gc = new GreatCircle();

        JSONObject featureCollection = new JSONObject();
        featureCollection.put("type", "FeatureCollection");

        JSONArray features = new JSONArray();

        JSONObject feature = new JSONObject();
        feature.put("type", "Feature");

        // Add properties (At least one is required)
        JSONObject properties = gc.generateProperties(1, lon, lat, a);
        feature.put("properties", properties);

        // Get the coordinates
        JSONArray coords = gc.createEllipse3(lon, lat, a, b, rot, numPoints, false);

        JSONObject geom = new JSONObject();
        geom.put("type", "MultiPolygon");
        geom.put("coordinates", coords);

        feature.put("geometry", geom);

        features.put(feature);
        featureCollection.put("features", features);

        System.out.println(featureCollection.toString());
    }

    public void createGeojsonTest(double lon, double lat, double size, int numPoints) {
        GreatCircle gc = new GreatCircle();

        JSONObject featureCollection = new JSONObject();
        featureCollection.put("type", "FeatureCollection");

        JSONArray features = new JSONArray();

        JSONObject feature = new JSONObject();
        feature.put("type", "Feature");

        // Add properties (At least one is required)
        JSONObject properties = gc.generateProperties(1, lon, lat, size);
        feature.put("properties", properties);

        // Get the coordinates
        JSONArray coords = gc.createCircle(lon, lat, size, numPoints, false);

        JSONObject geom = new JSONObject();
        geom.put("type", "MultiPolygon");
        geom.put("coordinates", coords);

        feature.put("geometry", geom);

        features.put(feature);
        featureCollection.put("features", features);

        System.out.println(featureCollection.toString());

        //gc.testCircles();
    }

    public static void main(String[] args) {

        GreatCircle gc = new GreatCircle();

      //  gc.createGeojsonEllipseTest(-69.52,9.05,7,0.007, 0, 100);
        
        JSONArray json = gc.createCircle(-1, 30, 200.0, 20, true);
        int i = 0;
        //System.out.println(++i);
        Random rnd = new Random();
        while (i < 1000) {

            double a = rnd.nextDouble() * 1 + 0.01;  // a from 0.01 to 1.1km
            double b = rnd.nextDouble() * 1 + 0.01;  // b from 0.01 to 1.1km
            double r = rnd.nextDouble() * 360; // Rotation 0 to 360             

            double minLon = -180;
            double maxLon = 180;
            double lon = minLon + (maxLon - minLon) * rnd.nextDouble();

            double minLat = -80;
            double maxLat = 80;

            double lat = minLat + (maxLat - minLat) * rnd.nextDouble();

            gc.createGeojsonEllipseTest(lon, lat, a, b, r, 200);
            i++;
        }

//        JSONObject featureCollection = new JSONObject();
//        featureCollection.put("type", "FeatureCollection");
//
//        JSONArray features = new JSONArray();
//
//        JSONObject feature = new JSONObject();
//        feature.put("type", "Feature");
//
//        // Add properties (At least one is required)
//        JSONObject properties = new JSONObject();
//        properties.put("name", "test");
//        feature.put("properties", properties);
//               
//
//        JSONArray polys = new JSONArray();
//        JSONArray poly = new JSONArray();
//        JSONArray ring = new JSONArray();        
//        JSONArray coord = new JSONArray();
//        
//        double lllon = -180.0;
//        double lllat = 89.0;
//        
//        coord = new JSONArray("[" + (lllon) + "," + (lllat) + "]");
//        ring.put(coord);
//        coord = new JSONArray("[" + (lllon + 180.0)  + "," + (lllat) + "]");
//        ring.put(coord);
//        coord = new JSONArray("[" + (lllon + 180.0)  + "," + (lllat + 1.0) + "]");
//        ring.put(coord);
//        coord = new JSONArray("[" + (lllon)  + "," + (lllat + 1.0) + "]");;
//        ring.put(coord);
//        ring.put(ring.get(0));        
//        poly.put(ring);
//        polys.put(poly);
//
//
//        poly = new JSONArray();
//        ring = new JSONArray();                
//        
//        lllon = 0.0;
//        lllat = 89.0;
//        
//        coord = new JSONArray("[" + (lllon) + "," + (lllat) + "]");
//        ring.put(coord);
//        coord = new JSONArray("[" + (lllon + 180.0)  + "," + (lllat) + "]");
//        ring.put(coord);
//        coord = new JSONArray("[" + (lllon + 180.0)  + "," + (lllat + 1.0) + "]");
//        ring.put(coord);
//        coord = new JSONArray("[" + (lllon)  + "," + (lllat + 1.0) + "]");;
//        ring.put(coord);
//        ring.put(ring.get(0));        
//        poly.put(ring);
//        polys.put(poly);
//        
//        
//        JSONObject geom = new JSONObject();
//        geom.put("type", "MultiPolygon");
//        geom.put("coordinates", polys);
//
//        feature.put("geometry", geom);
//
//        features.put(feature);
//        featureCollection.put("features", features);
//
//        System.out.println(featureCollection.toString());        
    }

}
