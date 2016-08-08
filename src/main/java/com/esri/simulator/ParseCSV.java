/*
Used to test parseing lines from the text file.  
Used to figure out how to parse quoted strings in a csv file that contains commas.
 */
package com.esri.simulator;

import com.esri.core.geometry.OperatorExportToJson;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author david
 */
public class ParseCSV {


    void parseLine(String line) {
        try {
            
            // Use RegEx to handle quoted strings.  Tried many varitions from web; this one worked. 
            // Ref: http://stackoverflow.com/questions/16532981/parsing-comma-separated-values-containing-quoted-commas-and-newlines
            Pattern pattern = Pattern.compile("(([^\"][^,]*)|\"([^\"]*)\"),?");
            
            /*
            First Group is outer parenthesis 
            Second Group is inter parenthesis on left.  (([^\"][^,]*)) String starting with any character other than quote or comma  
            Third Group is inter parenthisis on right. \"([^\"]*)\") String starting with quote and ending with quote; parens exclude the quote
            
            Adding ?: makes the group non-capturing 
            Pattern pattern = Pattern.compile("(?:([^\"][^,]*)|\"([^\"]*)\"),?");
            
            This would change code so group(1) and group(2) instead of 2,3 below.
            
            Tested at http://www.regexr.com/
            */

            ArrayList<String> vals = new ArrayList<String>();
            
            
            
            Matcher matcher = pattern.matcher(line);
            
            int i = 0;
            vals = new ArrayList<String>();
            while (matcher.find()) {
                
                if (matcher.group(2) != null) {
                    //System.out.print(matcher.group(2) + "|");
                    vals.add(i, matcher.group(2));
                } else if (matcher.group(3) != null) {                    
                    //System.out.print(matcher.group(3) + "|");
                    vals.add(i, matcher.group(3));
                }
                i += 1;
            }         
            
            
            System.out.println(vals.get(2) + "," + vals.get(4) + "," + vals.get(5));

            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    void parseFile(String filename) {
        try {
            FileReader fr = new FileReader(filename);
            BufferedReader br = new BufferedReader(fr);        
            
            // Use RegEx to handle quoted strings.  Tried many varitions from web; this one worked. 
            // Ref: http://stackoverflow.com/questions/16532981/parsing-comma-separated-values-containing-quoted-commas-and-newlines
            Pattern pattern = Pattern.compile("(([^\"][^,]*)|\"([^\"]*)\"),?");
            
            ArrayList<String> vals = new ArrayList<String>();

            String line;            
                      
            while ((line = br.readLine()) != null) {
                
                vals = new ArrayList<String>();  
                int i = 0;
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    if (matcher.group(2) != null) {
                        //System.out.print(matcher.group(2) + "|");
                        vals.add(i, matcher.group(2));
                    } else if (matcher.group(3) != null) {
                        //System.out.print(matcher.group(3) + "|");
                        vals.add(i, matcher.group(3));
                    }
                    i += 1;
                }         
                String tn = vals.get(2);
                
                //06/22/2013 12:02:00 AM,
                
                DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss aa");
                Date dt = df.parse(vals.get(3));
                
                Calendar cal = Calendar.getInstance();
                cal.setTime(dt);
                Long tm = cal.getTimeInMillis();
                
                                       
                Double lon = Double.parseDouble(vals.get(4));
                Double lat = Double.parseDouble(vals.get(5));
                
                               
                Point pt = new Point(lon, lat);
                
                SpatialReference sr = SpatialReference.create(4326);
                
                String jsonString = OperatorExportToJson.local().execute(sr, pt);
               

                //System.out.println();
                System.out.println(tn + "," + lon + "," + lat + "," + tm);
                System.out.println(jsonString);
                
            }            
            
            br.close();
            fr.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    
    public static void main(String args[]) {
        ParseCSV t = new ParseCSV();
        t.parseFile("faa-stream.csv");
        
        //t.parseLine("FAA-Stream,13511116,DLH427,06/22/2013 12:02:00 AM,-55.1166666666667,45.1166666666667,82.660223052638,540,350,A343,DLH,KPHL,EDDF,JET,COMMERCIAL,ACTIVE,GA,\"-55.1166666666667,45.1166666666667,350.0\"");
    }
}
