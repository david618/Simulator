/*
Listen on a port and start timer when first message is received
Count messages
After a pause of 10 seconds (no message)
Output Count, Rate 
 */
package com.esri.simulator;

import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author david
 */
public class TcpSink {

    public TcpSink(Integer port, boolean calcLatency) {
        try {
            ServerSocket ss = new ServerSocket(port);
            
            while (true) {
                Socket cs = ss.accept();
                TcpSinkServer ts = new TcpSinkServer(cs, calcLatency);
                ts.start();
                System.out.println("HERE");
            }
            
        } catch (Exception e) {
            
        }

        
        
    }

    public static void main(String[] args) {

        // Command Line Arg is port you want to listen on. 5565         
        /*
        NOTE: For latency calculations ensure all servers including the server running simulation
        are using time chrnonized.

        Run this command simulatneously on machines to compare time
        $ date +%s

        NTP command force update now:  $ sudo ntpdate -s time.nist.gov
        CHRONYD check status: $ chronyc tracking

         */
        int numargs = args.length;

//        new TcpSink(5565, false);
        if (numargs != 1 && numargs != 2) {
            System.err.print("Usage: TcpSink <port-to-listen-on> (<boolean-calc-latency>)\n");
        } else {

            if (numargs == 1) {
                new TcpSink(Integer.parseInt(args[0]), false);
            } else {
                new TcpSink(Integer.parseInt(args[0]), Boolean.parseBoolean(args[1]));
            }            
            
            

        }

    }

}
