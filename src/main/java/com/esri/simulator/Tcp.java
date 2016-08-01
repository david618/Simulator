/*
 * Test
 */
package com.esri.simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.OutputStream;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
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
     * @param numToSend Number of lines to send. If more than number of lines in file will resend from start.
     * @param burstDelay Number of milliseconds to burst at; set to 0 to send one line at a time 
     * @param tweak Used to tweak the actual send rate adjusting for hardware.  (optional)
     */
    public void sendFile(String filename, Integer rate, Integer numToSend, Integer burstDelay, Integer tweak) {
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

                    line = linesIt.next() + "\n";

                    final long stime = System.nanoTime();
                    
                    this.os.write(line.getBytes());
                    this.os.flush();

                    
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

                        line = linesIt.next() + "\n";

                        this.os.write(line.getBytes());
                        this.os.flush();                
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
            System.out.println(os.toString());
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
    public void sendFile(String filename, Integer rate, Integer numToSend, Integer burstDelay) {
        sendFile(filename, rate, numToSend, burstDelay, 0);
    }
    
    
    public static void main(String args[]) {
       
        if (args.length != 5) {
            System.err.print("Usage: Simulator <server> <port> <file> <rate> <numrecords>\n");
        } else {
            Tcp t = new Tcp(args[0], Integer.parseInt(args[1]));
            t.sendFile(args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]), 0);
            t.shutdown();
        }
        
//        for (int i=0; i< 1; i++) {
//            Tcp tcp = new Tcp("d1.trinity.dev", 5565);
//            tcp.sendFile("faa-stream.csv", 100000, 200000, 0);
//            tcp.shutdown();
//            
//        }
        
        
        
    }
}
