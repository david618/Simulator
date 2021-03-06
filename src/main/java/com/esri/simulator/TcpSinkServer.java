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
 * Used by TcpSink to listen for Messages.
 *
 * Creator: David Jennings
 */
package com.esri.simulator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 *
 * @author david
 */
public class TcpSinkServer extends Thread {

    private Socket socket = null;
    int sampleEveryNMessages;
    boolean displayMessages;

    public TcpSinkServer(Socket socket, int sampleEveryNMessages, boolean displayMessages) {
        this.socket = socket;
        this.sampleEveryNMessages = sampleEveryNMessages;
        this.displayMessages = displayMessages;
    }

    @Override
    public void run() {
        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

            SimpleRegression regression = new SimpleRegression();

            Integer cnt = 0;
            Integer numSamples = 0;
            Integer msSinceLastMessage = 0;

            System.out.println("Listening");

            while (true) {
                if (in.ready()) {

                    String line = in.readLine();

                    if (displayMessages) {
                        System.out.println(line);

                    } else {
                        cnt += 1;
                        if (cnt == 1) {
                            System.out.println("Count,SystemTimeMS,Count,(rate)");
                        }
                        msSinceLastMessage = 0;

                        if (cnt % sampleEveryNMessages == 0) {
                            long t = System.currentTimeMillis();
                            regression.addData(t, cnt);
                            numSamples += 1;
                            if (numSamples > 2) {
                                double rcvRate = regression.getSlope() * 1000;
                                System.out.println(numSamples + "," + t + "," + cnt + "," + rcvRate);
                            } else {
                                System.out.println(numSamples + "," + t + "," + cnt);
                            }
                        }
                    }

                } else {
                    Thread.sleep(100); // wait 100 ms
                    msSinceLastMessage += 100;

                    if (msSinceLastMessage >= 10000) {
                        // Reset after 10 seconds of no messages
                        if (cnt > 0) {
                            System.out.println("Listening");
                            cnt = 0;
                            numSamples = 0;
                            msSinceLastMessage = 0;
                            regression = new SimpleRegression();
                        }

                    }

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
