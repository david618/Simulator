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
 * Sends lines of a text file to a TCP Server 
 * Lines are sent at a specified rate.
 * While sending the send rates are adjusted to try to overcome hardware differences.
 * The maximum possible rate depends on hardware and network.
 * You can use Java options (-Xms2048m -Xmx2048m) to set the Heap Size available.
 * 
 * 30 Aug 2017: Updated to renable support to append time; modified code to adjust rate
 *     dynamically to more closely achieve the requested rate.
 *     Testing from 10,000 to 180,000
 *         BurstDelay = 0: Rate with 1% of requested rate
 *         BurstDelay = 100: Rate with 3% of requested rate
 *         Peak around 150,000/s on my computer i7 with 32GB RAM
 * 
 * Creator: David Jennings
 */
package com.esri.simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author david
 */
public class Tcp {

    private String server;
    private Integer port;

    private OutputStream os = null;

    public String getServer() {
        return server;
    }

    public Integer getPort() {
        return port;
    }

    public Tcp(String server, Integer port) {
        this.server = server;
        this.port = port;

        try {
            Socket skt = new Socket(this.server, port);
            this.os = skt.getOutputStream();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void shutdown() {
        try {
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * @param filename File with lines of data to be sent.
     * @param rate Rate in lines per second to send.
     * @param numToSend Number of lines to send. If more than number of lines in
     * file will resend from start.
     * @param burstDelay Number of milliseconds to burst at; set to 0 to send
     * one line at a time
     * @param appendTime If set to true system time is appended (assumes csv)
     */
    public void sendFile(String filename, Integer rate, Integer numToSend, Integer burstDelay, boolean appendTime) {
        try {
            
            // Read File
            FileReader fr = new FileReader(filename);
            BufferedReader br = new BufferedReader(fr);

            // Load Array with Lines from File
            ArrayList<String> lines = new ArrayList<>();

            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }

            br.close();
            fr.close();
            
            // Create Iterator from Array
            Iterator<String> linesIt = lines.iterator();
                        
            // Get the System Time as st (Start Time)            
            Long st = System.currentTimeMillis();

            // Count of Records Sent
            Integer cnt = 0;
            
            // Tweak used to adjust delays to try and get requested rate
            Long tweak = 0L;
            
            // Delay between each send in nano seconds            
            Double ns_delay = 1000000000.0 / (double) rate;

            long ns = ns_delay.longValue() - tweak;
            if (ns < 0) {
                ns = 0;  // can't be less than 0 
            }
            
            // *********** If burstDelay = 0 then send Constant Rate using nanosecond delay *********
            if (burstDelay == 0) {

                while (cnt < numToSend) {
                    
                    if (cnt % rate == 0 && cnt > 0) {
                        // Calculate rate and adjust as needed
                        Double curRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;                                                
                                                
                        System.out.println(cnt + "," + String.format("%.0f", curRate));                        
                    }
                    
                    if (cnt % 1000 == 0  && cnt > 0) {
                        // Calculate rate and adjust as needed
                        Double curRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;                                                
                        
                        // rate difference as percentage 
                        Double rateDiff = (rate - curRate)/rate;
                        
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

                    this.os.write(line.getBytes());
                    this.os.flush();

                    long etime = 0;
                    do {
                        // This approach uses a lot of CPU                    
                        etime = System.nanoTime();
                        // Adding the following sleep for a few microsecond reduces system load; however,
                        // it also negatively effects the throughput
                        //Thread.sleep(0,100);  
                    } while (stime + ns >= etime);

                }
            } else {
                // *********** SEND in bursts every msDelay ms  *********
                
                // Calculate number of events to send during each burst                 
                Integer numPerBurst = Math.round(rate / 1000 * burstDelay);

                if (numPerBurst < 1) {
                    // Send at least one per burst
                    numPerBurst = 1;
                }
                
                Integer delay = burstDelay;

                while (cnt < numToSend) {

                    // Adjust delay every burst
                    Double curRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;                                                                            
                    Double rateDiff = (rate - curRate)/rate;                            
                    tweak = (long) (rateDiff * rate);                                           
                    delay = delay - Math.round(tweak / 1000.0f);                                                                            
                    if (delay < 0) {
                        delay = 0;  // delay cannot be negative
                    } else {
                        Thread.sleep(delay);
                    }
                    
                    Integer i = 0;
                    while (i < numPerBurst) {
                        if (cnt % rate == 0  && cnt > 0) {
                            curRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;                                                                            
                            System.out.println(cnt + "," + String.format("%.0f", curRate));
                        }

                        cnt += 1;                        
                        
                        i += 1;
                        if (!linesIt.hasNext()) {
                            linesIt = lines.iterator();  // Reset Iterator
                        }

                        if (appendTime) {
                            line = linesIt.next() + "," + String.valueOf(System.currentTimeMillis()) + "\n";
                        } else {
                            line = linesIt.next() + "\n";
                        }

                        this.os.write(line.getBytes());
                        this.os.flush();
                        
                        // Break out as soon as numToSend is reached
                        if (cnt >= numToSend) break;
                        
                    }
                    
                }

            }

            Double sendRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;

            System.out.println(cnt + "," + String.format("%.0f", sendRate));

        } catch (Exception e) {
            // Could fail on very large files that would fill heap space 
            System.out.println(os.toString());
            e.printStackTrace();

        }
    }



    public static void main(String args[]) {

        // Example Command Line args: localhost 5565 faa-stream.csv 1000 10000
        int numargs = args.length;
        if (numargs != 5 && numargs != 6) {
            // append append time option was added to support end-to-end latency; I used it for Trinity testing
            System.err.println("Usage: Tcp <server> <port> <file> <rate> <numrecords> (burstDelay) (append-time)");
            System.err.println("server: The IP or hostname of server to send events to.");
            System.err.println("port: The TCP port to send events to.");
            System.err.println("filename: sends line by line from this file.");
            System.err.println("rate: Attempts to send at this rate.");
            System.err.println("numrecords: Sends this many lines; file is automatically recycled if needed.");
            System.err.println("burstDelay in ms; defaults to 0; messages are sent at constant rate");
            System.err.println("append-time defaults to false; Adds system time as extra parameter to each request. ");
        } else {
            // Initial the Tcp Class with the server and port
            if (numargs >=5 && numargs <=7) {
                
                Tcp t = new Tcp(args[0], Integer.parseInt(args[1]));

                switch (numargs) {
                    case 5: 
                        t.sendFile(args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]), 0, false);
                        break;
                    case 6:
                        int burstDelay = Integer.parseInt(args[5]);
                        if (burstDelay < 10 || burstDelay > 1000) {
                            System.err.println("Invalid burstDelay; valid values are 10 to 1000 ms");
                            break;
                        }
                        if (burstDelay > 200) {
                            System.out.println("WARNING: For larger values of burstDelay it can a while to achieve the requested rate.");
                        }
                        
                        t.sendFile(args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]),burstDelay , false);
                        break;
                    case 7:
                        t.sendFile(args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Boolean.parseBoolean(args[6]));
                        break;
                }
                
                t.shutdown();

            }                        
            
        }

    }
}
