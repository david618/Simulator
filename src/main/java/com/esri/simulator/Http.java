/*
 * (C) Copyright 2017 David Jennings
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     David Jennings
 */

/*
 * Sends lines of a text file to a HTTP Server using HTTP Post
 * Lines are sent at a specified rate.
 * 
 * Creator: David Jennings
 */
package com.esri.simulator;


import java.io.BufferedReader;
import java.io.FileReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author david
 */
public class Http {
    private String url;

    // Tell the server I'm Firefox
    private final String USER_AGENT = "Mozilla/5.0";

    private CloseableHttpClient httpClient;
    private HttpPost httpPost;


    public Http(String url) throws Exception {


        this.url = url;

        httpClient = HttpClientBuilder.create().build();

        httpPost = new HttpPost(url);

    }


    private void postLine(String line) throws Exception {

        StringEntity postingString = new StringEntity(line);

        httpPost.setEntity(postingString);
        //httpPost.setHeader("Content-type","plain/text");
        httpPost.setHeader("Content-type","application/json");

        CloseableHttpResponse resp = httpClient.execute(httpPost);
        
        HttpEntity respEntity = resp.getEntity();
        EntityUtils.consume(respEntity);

        //httpPost.releaseConnection();
    }

    
    /**
     * 
     * @param filename File with lines of data to be sent.
     * @param rate Rate in lines per second to send.
     * @param numToSend Number of lines to send. If more than number of lines in file will resend from start.
     * @param burstDelay Number of milliseconds to burst at; set to 0 to send one line at a time 
     * @param tweak Used to tweak the actual send rate adjusting for hardware.  (optional)
     */
    public void sendFile(String filename, Integer rate, Integer numToSend, Integer burstDelay, boolean appendTime, Integer tweak) {
        try {
            FileReader fr = new FileReader(filename);
            BufferedReader br = new BufferedReader(fr);
            
            // Read the file into an array
            ArrayList<String> lines = new ArrayList<String>();
            
            
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);                
            }
            
            br.close();
            fr.close();
            
            Iterator<String> linesIt = lines.iterator();
            
            // Get the System Time
            LocalDateTime st = LocalDateTime.now();

            Integer cnt = 0;
            
            /*
                For rates < 100/s burst is better
                For rates > 100/s continous is better            
            */
            
            
            // *********** SEND Constant Rate using nanosecond delay *********
            if (burstDelay == 0) {
                // Delay between each send in nano seconds            
                Double ns_delay = 1000000000.0 / (double) rate;
                
                // By adding some to the delay you can fine tune to achieve the desired output
                ns_delay = ns_delay - (tweak * 100);
                
                long ns = ns_delay.longValue();
                if (ns < 0) ns = 0;  // can't be less than 0 




                while (cnt < numToSend) {
                    cnt += 1;
                    LocalDateTime ct = LocalDateTime.now();

                    if (!linesIt.hasNext()) linesIt = lines.iterator();  // Reset Iterator

                    final long stime = System.nanoTime();

                    if (appendTime) {
                        // assuming CSV

                        line = linesIt.next() + "," + String.valueOf(System.currentTimeMillis()) + "\n";
                    } else {
                        line = linesIt.next() + "\n";
                    }



                    postLine(line);
                    //System.out.println(line);

                    
                    long etime = 0;
                    do {
                        // This approach uses a lot of CPU                    
                        etime = System.nanoTime();
                        // Adding the following sleep for a few microsecond reduces the load
                        // However, it also effects the through put
                        //Thread.sleep(0,100);  
                    } while (stime + ns >= etime);                

                }
            } else {
                // *********** SEND in bursts every msDelay ms  *********

                Integer msDelay = burstDelay;
                Integer numPerBurst = Math.round(rate / 1000 * msDelay); 

                if (numPerBurst < 1) numPerBurst = 1;

                while (cnt < numToSend) {
                    cnt += numPerBurst;

                    Integer i = 0;
                    while (i < numPerBurst) {
                        i += 1;
                        if (!linesIt.hasNext()) linesIt = lines.iterator();  // Reset Iterator


                        if (appendTime) {
                            line = linesIt.next() + "," + String.valueOf(System.currentTimeMillis()) + "\n";
                        } else {
                            line = linesIt.next() + "\n";
                        }


                        postLine(line);

                    }
                    // If you remove some part of the time which is used for sending
                    
                    Integer delay = msDelay - tweak;
                    if (delay < 0) delay = 0;
                    
                    Thread.sleep(delay);
                }
            
            }            
            Double sendRate = 0.0;

            if (st != null ) {
                LocalDateTime et = LocalDateTime.now();

                Duration delta = Duration.between(st, et);

                Double elapsedSeconds = (double) delta.getSeconds() + delta.getNano() / 1000000000.0;

                sendRate = (double) cnt / elapsedSeconds;                
            }                

            System.out.println(cnt + "," + sendRate);            
            
        } catch (Exception e) {
            // Could fail on very large files that would fill heap space 
//            System.out.println(con.toString());
            e.printStackTrace();
            
        }
    }
    
    /**
     * Forwards to sendFile with tweak default value of 0.
     * @param filename
     * @param rate
     * @param numToSend
     * @param burstDelay 
     */
    public void sendFile(String filename, Integer rate, Integer numToSend, Integer burstDelay, boolean appendTime) {
        // tweak at 0
        sendFile(filename, rate, numToSend, burstDelay, appendTime, 0);
    }
    
    
    public static void main(String args[]) throws Exception {
       
        // Example Command Line args: localhost:8001 simFile_1000_10s.dat 1000 10000

        int numargs = args.length;
        if (numargs != 4 && numargs != 5 ) {
            // append append time option was added to support end-to-end latency; I used it for Trinity testing but it's a little confusing
            //System.err.print("Usage: Http <url> <file> <rate> <numrecords> (<append-time-csv>)\n");
            System.err.print("Usage: Http <url> <file> <rate> <numrecords> \n");
        } else {
            String url = args[0];
            String file = args[1];
            Integer rate = Integer.parseInt(args[2]);
            Integer numrecords = Integer.parseInt(args[3]);
            Boolean appendTime = false;
            if (numargs == 5) {
                appendTime = Boolean.parseBoolean(args[4]);
            }
            Http t = new Http(url);
            t.sendFile(file, rate, numrecords, 0, appendTime);


        }

        
    }
}
