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

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.entity.StringEntity;

/**
 *
 * @author david
 */
public class TcpSenderThread extends Thread {
    
    LinkedBlockingQueue<String> lbq;
    private volatile boolean running = true;

    private final String ip;
    private final int port;
    
    private OutputStream os;


    private long cntErr;
    private long cnt;

    public long getCntErr() {
        return cntErr;
    }

    public long getCnt() {
        return cnt;
    }

    public TcpSenderThread(LinkedBlockingQueue<String> lbq, String ip, int port) {
        this.lbq = lbq;
        this.ip = ip;
        this.port = port;
        try {
            Socket skt = new Socket(this.ip, this.port);
            this.os = skt.getOutputStream();
        } catch (IOException e) {
            System.out.println("Failed to created socket to: " +  this.ip + ":" + this.port);
        }
        
        
    }
    
    
    public void terminate() {
        running = false;
        try {
            os.close();
        } catch (IOException ex) {
            Logger.getLogger(TcpSenderThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            while (running) {
                String line = lbq.take();
                if (line == null) {
                    break;
                }
                // Send the String

                os.write(line.getBytes());
                os.flush();

                cnt += 1;

            }

        } catch (InterruptedException ex) {
            Logger.getLogger(HttpPosterThread.class.getName()).log(Level.SEVERE, null, ex);
            cntErr += 1;
        } catch (IOException ex) {
            Logger.getLogger(HttpPosterThread.class.getName()).log(Level.SEVERE, null, ex);
            cntErr += 1;
        }
    }    
    
}
