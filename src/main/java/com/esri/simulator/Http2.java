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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;


/**
 *
 * @author david
 */
public class Http2 {



    LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<>();


    /**
     *
     * @param url
     * @param filename File with lines of data to be sent.
     * @param rate Rate in lines per second to send.
     * @param numToSend Number of lines to send. If more than number of lines in
     * file will resend from start.
     * @param numThreads
     * @param appendTime
     */
    public void sendFile(String url, String filename, Integer rate, Integer numToSend, Integer numThreads, boolean appendTime) {
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

            // Get the System Time as st (Start Time)            
            Long st = System.currentTimeMillis();

            Integer cnt = 0;
            Integer cntErr = 0;

            // Tweak used to adjust delays to try and get requested rate
            Long tweak = 0L;

            /*
                For rates < 100/s burst is better
                For rates > 100/s continous is better            
             */
            // *********** SEND Constant Rate using nanosecond delay *********
            // Delay between each send in nano seconds            
            Double ns_delay = 1000000000.0 / (double) rate;

            // By adding some to the delay you can fine tune to achieve the desired output
            ns_delay = ns_delay - (tweak * 100);

            long ns = ns_delay.longValue();
            if (ns < 0) {
                ns = 0;  // can't be less than 0 
            }

            // Create the HttpPosterThread
            HttpPosterThread[] threads = new HttpPosterThread[numThreads];

            for (int i = 0; i < threads.length; i++) {
                threads[i] = new HttpPosterThread(lbq, url);
                threads[i].start();
            }
            
            
            
            while (cnt < numToSend) {

                if (cnt % rate == 0 && cnt > 0) {
                    // Calculate rate and adjust as needed
                    Double curRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;

                    if (cntErr > 0) {
                        System.out.println(cnt + "," + String.format("%.0f", curRate) + "," + cntErr + " not 200");
                    } else {
                        System.out.println(cnt + "," + String.format("%.0f", curRate));
                    }

                }

                if (cnt % 1000 == 0 && cnt > 0) {
                    // Calculate rate and adjust as needed
                    Double curRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;

                    // rate difference as percentage 
                    Double rateDiff = (rate - curRate) / rate;

                    // Add or subracts up to 100ns 
                    tweak = (long) (rateDiff * rate);

                    // By adding some to the delay you can fine tune to achieve the desired output
                    ns = ns - tweak;
                    if (ns < 0) {
                        ns = 0;  // can't be less than 0 
                    }

                }

                cnt += 1;

                if (!linesIt.hasNext()) {
                    linesIt = lines.iterator();  // Reset Iterator
                }

                final long stime = System.nanoTime();

                if (appendTime) {
                    // assuming CSV

                    line = linesIt.next() + "," + String.valueOf(System.currentTimeMillis()) + "\n";
                } else {
                    line = linesIt.next() + "\n";
                }

                lbq.put(line);
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
            
            // Close Threads
            
            while (lbq.size() > 0) {
                Thread.sleep(1000);
            }            
            
            
            for (int i = 0; i < threads.length; i++) {
                threads[i].terminate();
            }            

            Double sendRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;

            if (cntErr > 0) {
                System.out.println(cnt + "," + String.format("%.0f", sendRate) + "," + cntErr + " not 200");
            } else {
                System.out.println(cnt + "," + String.format("%.0f", sendRate));
            }

        } catch (Exception e) {
            // Could fail on very large files that would fill heap space 
//            System.out.println(con.toString());
            e.printStackTrace();

        }
    }

    public static void main(String args[]) throws Exception {

        // Example Command Line args: localhost:8001 simFile_1000_10s.dat 1000 10000
        int numargs = args.length;
        if (numargs < 4 || numargs > 6) {
            // append append time option was added to support end-to-end latency; I used it for Trinity testing but it's a little confusing
            //System.err.print("Usage: Http <url> <file> <rate> <numrecords> (<append-time-csv>)\n");
            System.err.print("Usage: Http <url> <file> <rate> <numrecords> (<numthreads=1>) \n");
        } else {
            String url = args[0];
            String file = args[1];
            Integer rate = Integer.parseInt(args[2]);
            Integer numrecords = Integer.parseInt(args[3]);

            Integer numthreads = 1;
            if (numargs > 4) {
                numthreads = Integer.parseInt(args[3]);
            }

            Boolean appendTime = false;
            if (numargs == 6) {
                appendTime = Boolean.parseBoolean(args[4]);
            }
            
            Http2 t = new Http2();
            t.sendFile(url, file, rate, numrecords, numthreads, appendTime);

        }

    }
}
