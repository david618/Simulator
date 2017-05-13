/*
Send data to Postgresql; In development.

 */
package com.esri.simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import org.jennings.geomtools.GeographicCoordinate;
import org.jennings.geomtools.GreatCircle;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author david
 */
public class Postgresql2 {

    final static int INT = 0;
    final static int LNG = 1;
    final static int DBL = 2;
    final static int STR = 3;

    final static int MAXSTRLEN = 100;

    private void printCreate(String tablename, String fileJsonLines, String geomFieldName, String oidFieldName) {

        try {

            /**
             * CREATE TABLE ecenter ( oid integer, clat double precision, clon
             * double precision, num integer );
             *
             * SELECT AddGeometryColumn('', 'ecenter','geom',4326,'POINT',2);
             *
             */
            FileReader fr = new FileReader(fileJsonLines);

            BufferedReader br = new BufferedReader(fr);

            String line = br.readLine();

            JSONObject json = null;

            String sql = "";

            if (line != null) {

                sql = "CREATE TABLE " + tablename + " (" + oidFieldName + " serial4,";

                // Create the Schema
                json = new JSONObject(line);

                Set<String> ks = json.keySet();

                for (String k : ks) {
                    //System.out.println(k);

                    Object val = json.get(k);

                    if (val instanceof Integer) {
                        sql += k + " integer,";
                    } else if (val instanceof Long) {
                        sql += k + " bigint,";;
                    } else if (val instanceof Double) {
                        sql += k + " double precision,";;
                    } else if (val instanceof String) {
                        sql += k + " varchar(" + MAXSTRLEN + "),";;
                    }

                }

                sql = sql.substring(0, sql.length() - 1) + ");";

                System.out.println(sql);

                sql = "SELECT AddGeometryColumn('','" + tablename + "','" + geomFieldName + "',4326,'POINT',2);";
                System.out.println(sql);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void run(String tablename, String fileJsonLines, String geomFieldName, String oidFieldName, String serverDB, String username, String password, String lonFieldName, String latFieldName) {
        try {

            // Create DB Connection
            Connection c = null;
            Statement stmt = null;
            c = DriverManager
                    .getConnection("jdbc:postgresql://" + serverDB,
                            username, password);
            c.setAutoCommit(false);

            stmt = c.createStatement();

            FileReader fr = new FileReader(fileJsonLines);

            BufferedReader br = new BufferedReader(fr);

            String line = br.readLine();

            String sqlPrefix = "";
            JSONObject json = null;

            HashMap<String, Integer> jsonMap = new HashMap<>();

            if (line != null) {

                sqlPrefix = "INSERT INTO " + tablename + " (" + oidFieldName + ",";

                // Create the Schema
                json = new JSONObject(line);

                Set<String> ks = json.keySet();

                for (String k : ks) {
                    //System.out.println(k);

                    Object val = json.get(k);

                    if (val instanceof Integer) {
                        jsonMap.put(k, INT);
                    } else if (val instanceof Long) {
                        jsonMap.put(k, LNG);
                    } else if (val instanceof Double) {
                        jsonMap.put(k, DBL);
                    } else if (val instanceof String) {
                        jsonMap.put(k, STR);
                    }
                    //System.out.println();
                    sqlPrefix += k + ",";

                }

                //oid,a,b,clat,clon,rot,num,geom
                //sqlPrefix = sqlPrefix.substring(0,sqlPrefix.length() - 1) + ") VALUES (";
                sqlPrefix += geomFieldName + ") VALUES (DEFAULT,";

            }

            int num = 0;

            while (line != null) {
                //System.out.println(line);
                // Create sql line
                String sql = "";

                sql = sqlPrefix;

                for (String key : jsonMap.keySet()) {
                    switch (jsonMap.get(key)) {
                        case INT:
                            sql += json.getInt(key) + ",";
                            break;
                        case LNG:
                            sql += json.getLong(key) + ",";
                            break;
                        case DBL:
                            sql += json.getDouble(key) + ",";
                            break;
                        case STR:
                            sql += "'" + json.getString(key).replace("'", "''") + "',";
                            break;
                        default:
                            break;
                    }

                }

                //ST_GeomFromText('POINT(-71.060316 48.432044)', 4326)
                //sql = sql.substring(0,sql.length() - 1) + ");";
                sql += "ST_GeomFromText('POINT(" + json.getDouble(lonFieldName) + " " + json.getDouble(latFieldName) + ")', 4326)" + ");";
                System.out.println(sql);
                
                stmt = c.createStatement();
                stmt.executeUpdate(sql);

                num += 1;

                if (num % 1000 == 0) {
                    c.commit();
                }



                line = br.readLine();

                if (line != null) {
                    json = new JSONObject(line);
                }

//                break;
            }

            c.commit();

            c.close();

            br.close();
            fr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        Postgresql2 t = new Postgresql2();

        String tableName = "planes3";
        String filename = "flights.json";
        String geomFieldName = "geom";
        String oidFieldName = "oid";

        String serverConn = "pg1:5432/db1";
        String username = "user1";
        String password = "user1";
        String lonFieldName = "lon";
        String latFieldName = "lat";

        //t.printCreate(tableName, filename, geomFieldName, oidFieldName);
//        t.run(tableName, filename, geomFieldName, oidFieldName, serverConn, username, password, lonFieldName, latFieldName);

        int numargs = args.length;

        if (numargs != 4 && numargs != 9) {
            System.err.println("Usage Print Create Table: Postgresql2 <tableName> <fileName> <geomFieldName> <oidFieldName>");
            System.err.println("Usage Load Data: Postgresql2 <tableName> <fileName> <geomFieldName> <oidFieldName> <serverConn> <username> <password> <lonFieldName> <latFieldName>");
        } else if (numargs == 4) {
            tableName = args[0];
            filename = args[1];
            geomFieldName = args[2];
            oidFieldName = args[3];
            t.printCreate(tableName, filename, geomFieldName, oidFieldName);
        } else {
            tableName = args[0];
            filename = args[1];
            geomFieldName = args[2];
            oidFieldName = args[4];
            serverConn = args[5];
            username = args[6];
            password = args[7];
            lonFieldName = args[8];
            latFieldName = args[9];
            
            t.run(tableName, filename, geomFieldName, oidFieldName, serverConn, username, password, lonFieldName, latFieldName);
        }

    }

}
