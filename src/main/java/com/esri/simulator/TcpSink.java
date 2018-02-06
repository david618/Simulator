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
/**
 * Listens on TCP Port for messages.
 * Counts Messages based on value of sampleEveryMessages adds a point to the linear regresssion
 * After collecting three samples it will output the rate.
 * After 10 second pause the count and regression are reset.
 *
 * Creator: David Jennings
 */
package com.esri.simulator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author david
 */
public class TcpSink {

    private class GetCounts extends TimerTask {

        ArrayList<TcpSinkServer1> tssList;

        long st = 0L;
        long currentCnt = 0L;
        long prevCnt = 0L;

        public GetCounts(ArrayList<TcpSinkServer1> tssList) {
            this.tssList = tssList;
            this.st = 0L;
            this.currentCnt = 0L;
            this.prevCnt = 0L;
        }

        @Override
        public void run() {                       
            currentCnt = 0L;
            for (TcpSinkServer1 tss : tssList) {
                currentCnt += tss.getCnt();
            }

            if (currentCnt > prevCnt) {
                System.out.println(System.currentTimeMillis() + "," + currentCnt);       
            } else {
                if (currentCnt > 0) {
                    System.out.println("Done");
                    long et = 0;
                    for (TcpSinkServer1 tss : tssList) {
                        if (tss.lastTime > et) et = tss.lastTime;
                    }
                    
                    
                    Double rate = ((double) currentCnt/(et - st) * 1000);
                    System.out.println("Average Rate: " + rate);
                    currentCnt = 0L;
                    prevCnt = 0L;
                }
                
            }                                   
            prevCnt = currentCnt;

        }

    }

    Integer port;
    Integer sampleEveryNSecs;
    Boolean displayMessages;

    private void listenForConnections() {
        try {
            ServerSocket ss = new ServerSocket(port);

            System.out.println("After starting this; create or restart the GeoEvent service.");
            System.out.println("Once connected you see a 'Listening' message");

            ArrayList<TcpSinkServer1> tssList = new ArrayList<>();

            // Setup Timer to Get Counts
            Timer timer = new Timer(true);
            timer.scheduleAtFixedRate(new GetCounts(tssList), 0, 5000);

            while (true) {
                Socket cs = ss.accept();
                TcpSinkServer1 ts = new TcpSinkServer1(cs, displayMessages);
                ts.start();
                tssList.add(ts);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TcpSink(Integer port, Integer sampleEveryNSecs, Boolean displayMessages) {
        this.port = port;
        this.sampleEveryNSecs = sampleEveryNSecs;
        this.displayMessages = displayMessages;
        listenForConnections();

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

        if (numargs < 1 || numargs > 3) {
            System.err.print("Usage: TcpSink <port-to-listen-on> (<sample-every-N-records/1000>) (<display-messages/false>)\n");
        } else {

            switch (numargs) {
                case 1:
                    new TcpSink(Integer.parseInt(args[0]), 1000, false);
                    break;
                case 2:
                    new TcpSink(Integer.parseInt(args[0]), Integer.parseInt(args[1]), false);
                    break;
                default:
                    new TcpSink(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Boolean.parseBoolean(args[2]));
                    break;
            }

        }

    }

}
