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
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

/**
 *
 * @author david
 */
public class Http2 {

    private static final String IPADDRESS_PATTERN
            = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

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
            ArrayList<String> lines = new ArrayList<>();

            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }

            br.close();
            fr.close();

            Iterator<String> linesIt = lines.iterator();

            // See if the url contains app(name)
            String appPat = "app\\[(.*)\\]";

            Pattern pattern = Pattern.compile(appPat);
            Matcher matcher = pattern.matcher(url);

            URL aURL = new URL(url);
            String server = aURL.getHost();

            Pattern IPpattern = Pattern.compile(IPADDRESS_PATTERN);
            Matcher IPmatcher = IPpattern.matcher(server);

            String appStr = null;
            ArrayList<IPPort> ipPorts = null;
            if (matcher.find()) {
                appStr = matcher.group();
                MarathonInfo mi = new MarathonInfo();
                ipPorts = mi.getIPPorts(matcher.group(1), 0);
            } else if (IPmatcher.matches()) {
                // This is an IP appStr and ipPorts leave null
                // Send url as is
            } else {
                Lookup lookup = new Lookup(server, Type.A);
                lookup.run();
                System.out.println(lookup.getErrorString());
                
                int port = aURL.getPort();
                
                if (port == -1) {
                    appStr = server;
                } else {
                    appStr = server + ":" + Integer.toString(port);
                }
               
                ipPorts = new ArrayList<>();
                for (Record ans : lookup.getAnswers()) {
                    String ip = ans.rdataToString();                    
                    IPPort ipport = new IPPort(ip, port);     
                    ipPorts.add(ipport);
                    System.out.println(ipport);

                }

            }

            // Get the System Time as st (Start Time)            
            Long st = System.currentTimeMillis();

            Integer cnt = 0;

            // Create the HttpPosterThread
            HttpPosterThread[] threads = new HttpPosterThread[numThreads];

            for (int i = 0; i < threads.length; i++) {
                if (appStr == null) {
                    threads[i] = new HttpPosterThread(lbq, url);
                } else {
                    
                    if (ipPorts.size() == 0) {
                        throw new Exception("No instances found!");
                    }
                    
                    IPPort ipPort = ipPorts.get((i + 1) % ipPorts.size());

                    String urlIP = url.replace(appStr, ipPort.toString());
                    //System.out.println(urlIP);
                    threads[i] = new HttpPosterThread(lbq, urlIP);

                }

                threads[i].start();
            }

            Long timeLastDisplayedRate = System.currentTimeMillis();
            Long timeStartedBatch = System.currentTimeMillis();

            while (cnt < numToSend) {

                if (System.currentTimeMillis() - timeLastDisplayedRate > 5000) {
                    // Calculate rate and output every 5000ms 
                    timeLastDisplayedRate = System.currentTimeMillis();

                    int cnts = 0;
                    int cntErr = 0;

                    // Get Counts from Threads
                    for (HttpPosterThread thread : threads) {
                        cnts += thread.getCnt();
                        cntErr += thread.getCntErr();
                    }

                    Double curRate = (double) cnts / (System.currentTimeMillis() - st) * 1000;

                    System.out.println(cnts + "," + cntErr + "," + String.format("%.0f", curRate));

                }

                if (!linesIt.hasNext()) {
                    linesIt = lines.iterator();  // Reset Iterator
                }

                if (appendTime) {
                    // assuming CSV

                    line = linesIt.next() + "," + String.valueOf(System.currentTimeMillis()) + "\n";
                } else {
                    line = linesIt.next() + "\n";
                }

                lbq.put(line);

                cnt += 1;

                if (cnt % rate == 0) {
                    // Wait until one second before queuing next rate events into lbq
                    // Send rate events wait until 1 second is up
                    long timeToWait = 1000 - (System.currentTimeMillis() - timeStartedBatch);
                    if (timeToWait > 0) {
                        Thread.sleep(timeToWait);
                    }
                    timeStartedBatch = System.currentTimeMillis();
                }

            }

            int cnts = 0;
            int cntErr = 0;
            int prevCnts = 0;

            while (true) {
                if (System.currentTimeMillis() - timeLastDisplayedRate > 5000) {
                    // Calculate rate and output every 5000ms 
                    timeLastDisplayedRate = System.currentTimeMillis();

                    // Get Counts from Threads
                    for (HttpPosterThread thread : threads) {
                        cnts += thread.getCnt();
                        cntErr += thread.getCntErr();
                    }

                    Double curRate = (double) cnts / (System.currentTimeMillis() - st) * 1000;

                    System.out.println(cnts + "," + cntErr + "," + String.format("%.0f", curRate));

                    // End if the lbq is empty
                    if (lbq.size() == 0) {
                        System.out.println("Queue Empty");
                        break;
                    }

                    // End if the cnts from threads match what was sent
                    if (cnts >= numToSend) {
                        System.out.println("Count Sent >= Number Requested");
                        break;
                    }

                    // End if cnts is changing 
                    if (cnts == prevCnts) {
                        System.out.println("Counts are not changing.");
                        break;
                    }

                    cnts = 0;
                    cntErr = 0;
                    prevCnts = cnts;

                }
            }

            // Terminate Threads
            for (HttpPosterThread thread : threads) {
                thread.terminate();
            }

            cnts = 0;
            cntErr = 0;

            for (HttpPosterThread thread : threads) {
                cnts += thread.getCnt();
                cntErr += thread.getCntErr();
            }

            Double sendRate = (double) cnts / (System.currentTimeMillis() - st) * 1000;

            System.out.println(cnts + "," + cntErr + "," + String.format("%.0f", sendRate));

            System.exit(0);

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
            // url: http://server:ip/path 
            // Consider including url: app(http-kafka)/path with this look up ip:port using mesos
            // e.g.  curl http://master.mesos:8080/v2/apps/http-kafka | jq '[.app.tasks[] | {ip: .ipAddresses[].ipAddress, port: .ports[0]}]'
            // http://app(http-kafka)/path would be replaced with http://ip:port/path if more than one then round-robin assign to each thread
            System.err.print("Usage: Http2 <url> <file> <rate> <numrecords> (<numthreads=1>) \n");
        } else {
            String url = args[0];
            String file = args[1];
            Integer rate = Integer.parseInt(args[2]);
            Integer numrecords = Integer.parseInt(args[3]);

            Integer numthreads = 1;
            if (numargs > 4) {
                numthreads = Integer.parseInt(args[4]);
            }

            Boolean appendTime = false;
            if (numargs == 6) {
                appendTime = Boolean.parseBoolean(args[5]);
            }

            Http2 t = new Http2();
            t.sendFile(url, file, rate, numrecords, numthreads, appendTime);

        }

    }
}
