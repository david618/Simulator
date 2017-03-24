/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.esri.simulator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 *
 * @author david
 */
public class TcpSinkServer extends Thread {

    private Socket socket = null;
    private boolean calcLatency;

    public TcpSinkServer(Socket socket, boolean calcLatency) {
        this.socket = socket;

    }

    @Override
    public void run() {
        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

            String inputLine = "";

            // Read lines from Socket forever
            LocalDateTime st = LocalDateTime.now();
            LocalDateTime et = st;

            Integer cnt = 0;
            Long sumLatencies = 0L;

            while (true) {
                if (in.ready()) {

                    String line = in.readLine();

                    // Something to add 
                    if (this.calcLatency) {

                        try {
                            // Assumes csv and that input also has time in nanoSeconds from epoch
                            long tsent = Long.parseLong(line.substring(line.lastIndexOf(",") + 1));

                            long trcvd = System.currentTimeMillis();

                            sumLatencies += (trcvd - tsent);
                            // If trcvd appended then latency will be measured between Kafka write/read
                            //line += String.valueOf(trcvd);
                        } catch (Exception e) {
                            System.out.println("For Latency Calculations last field in CSV must be milliseconds from Epoch");
                            this.calcLatency = false;
                        }

                    }
                    cnt += 1;
                    if (cnt == 1) {
                        st = LocalDateTime.now();
                    }
                    et = LocalDateTime.now();

                } else {
                    Thread.sleep(100); // wait 100 ms
                    // if elapsed time since last read output and reset
                    LocalDateTime ct = LocalDateTime.now();
                    Duration cdelta = Duration.between(et, ct);
                    Double celapsedSeconds = (double) cdelta.getSeconds() + cdelta.getNano() / 1000000000.0;

                    if (cnt > 0 && celapsedSeconds > 5.0) {
                        // After 10 seconds
                        Double rcvRate = 0.0;
                        Double avgLatency = 0.0;

                        if (st != null) {

                            Duration delta = Duration.between(st, et);

                            Double elapsedSeconds = (double) delta.getSeconds() + delta.getNano() / 1000000000.0;

                            rcvRate = (double) cnt / elapsedSeconds;

                            avgLatency = (double) sumLatencies / (double) cnt;  //ms
                        }

                        long tm = System.currentTimeMillis();

                        if (this.calcLatency) {
                            //System.out.println(cnt + "," + rcvRate + "," + avgLatency);
                            System.out.format("%d , %.0f , %.3f\n", cnt, rcvRate, avgLatency);
                        } else {
                            //System.out.println(cnt + "," + rcvRate);
                            System.out.format("%d , %.0f\n", cnt, rcvRate);
                        }
                        // Reset
                        st = LocalDateTime.now();
                        et = st;

                        cnt = 0;
                        sumLatencies = 0L;

                    }

                }
                

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
