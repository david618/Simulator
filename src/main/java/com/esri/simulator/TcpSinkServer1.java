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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author david
 */
public class TcpSinkServer1 extends Thread {

    private Socket socket = null;
    boolean displayMessages;
    long cnt;
    long lastTime;
    long firstTime;

    public TcpSinkServer1(Socket socket, boolean displayMessages) {
        this.socket = socket;
        this.cnt = 0;
        this.lastTime = 0L;
        this.firstTime = 0L;
        this.displayMessages = displayMessages;
    }

    public long getCnt() {
        return cnt;
    }

    public long getLastTime() {
        return lastTime;
    }

    public long getFirstTime() {
        return firstTime;
    }
    
    
    
    public void terminate() {
        
        try {
            cnt = 0L;            
            this.lastTime = 0L;
            this.firstTime = 0L;
            socket.close();            
            this.interrupt();
            Thread.currentThread().interrupt();
        } catch (IOException ex) {
            Logger.getLogger(TcpSenderThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    

    @Override
    public void run() {
        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

            System.out.println("Listening");

            while (true) {
                if (in.ready()) {

                    String line = in.readLine();
                    lastTime = System.currentTimeMillis();

                    if (displayMessages) {
                        System.out.println(line);

                    } else {
                        cnt += 1;
                        if (cnt == 1) {
                            firstTime = System.currentTimeMillis();
                        }
                    }

                } 

            }

        } catch (IOException e) {
            //e.printStackTrace();
        }

    }

}
