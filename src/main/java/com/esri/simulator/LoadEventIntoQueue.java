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
package com.esri.simulator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author david
 */
public class LoadEventIntoQueue extends TimerTask {

    private final LinkedBlockingQueue<String> lbq;
    private final ArrayList<String> lines;
    private final Integer numToSend;
    private final Integer numPerSec;
    private final TcpSenderThread[] threads;

    private Integer runCnt;
    private int cnt;
    private Iterator<String> linesIt;
    Long st = System.currentTimeMillis();

    public LoadEventIntoQueue(LinkedBlockingQueue<String> lbq, ArrayList<String> lines, Integer numToSend, Integer numPerSec, TcpSenderThread[] threads) {
        this.lbq = lbq;
        this.lines = lines;
        this.numToSend = numToSend;
        this.numPerSec = numPerSec;
        this.threads = threads;

        this.cnt = 0;
        this.linesIt = lines.iterator();
        this.st = System.currentTimeMillis();
        this.runCnt = 0;
    }

    @Override
    public void run() {
        int i = 0;

        runCnt++;

        // Every 5 runs display the rate
        if (runCnt % 5 == 0) {

            int cnts = 0;
            int cntErr = 0;

            // Get Counts from Threads
            for (TcpSenderThread thread : threads) {
                cnts += thread.getCnt();
                cntErr += thread.getCntErr();
            }

            Double curRate = (double) cnts / (System.currentTimeMillis() - st) * 1000;

            System.out.println(cnts + "," + cntErr + "," + String.format("%.0f", curRate));
        }

        while (i < numPerSec) {

            i++;
            

            if (cnt >= numToSend) {
                // End Send
                this.cancel();

                int cnts = 0;
                int cntErr = 0;
                int prevCnts = 0;

                while (true) {

                    // Calculate rate and output every 5000ms 
                    // Get Counts from Threads
                    for (TcpSenderThread thread : threads) {
                        cnts += thread.getCnt();
                        cntErr += thread.getCntErr();
                    }

                    Double curRate = (double) cnts / (System.currentTimeMillis() - st) * 1000;

                    System.out.println(cnts + "," + cntErr + "," + String.format("%.0f", curRate));

                    // End if the lbq is empty
                    if (lbq.isEmpty()) {
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
                    
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(LoadEventIntoQueue.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

                // Terminate Threads
                for (TcpSenderThread thread : threads) {
                    thread.terminate();
                }

                cnts = 0;
                cntErr = 0;

                for (TcpSenderThread thread : threads) {
                    cnts += thread.getCnt();
                    cntErr += thread.getCntErr();
                }

                Double sendRate = (double) cnts / (System.currentTimeMillis() - st) * 1000;

                System.out.println(cnts + "," + cntErr + "," + String.format("%.0f", sendRate));

                System.exit(0);
            }

            if (!linesIt.hasNext()) {
                linesIt = lines.iterator();  // Reset Iterator
            }

            String line = linesIt.next() + "\n";

            try {
                lbq.put(line);
            } catch (InterruptedException ex) {
                Logger.getLogger(LoadEventIntoQueue.class.getName()).log(Level.SEVERE, null, ex);
            }

            cnt += 1;

        }

        runCnt += 1;

    }
}
