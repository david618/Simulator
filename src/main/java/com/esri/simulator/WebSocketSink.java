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
 * Listens on on Web Socket for messages. 
 * Counts Messages based on value of sampleEveryMessages adds a point to the linear regresssion
 * After collecting three samples it will output the rate.
 * After 10 second pause the count and regression are reset.
 * 
 * Creator: David Jennings
 */
package com.esri.simulator;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

// Embedded Example
// https://github.com/jetty-project/embedded-jetty-websocket-examples

/**
 *
 * @author david
 */
public class WebSocketSink {
    
    final int MAX_MESSAGE_SIZE = 1000000;
    
    public void connectWebsocket(String url, int sampleEveryN, boolean showMessages) {
        
        try {
            int timeout = 9000; // 9 seconds
            
            System.out.println("NOTE: For GeoEvent Stream Service append /subscribe to the Web Socket URL");            
            System.out.println("Starting: If you see rapid connection lost messages. Ctrl-C and check you're URL");
            
            
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setTrustAll(true);
            sslContextFactory.start();

            final WebSocketClientFactory factory = new WebSocketClientFactory();

            factory.start();

            WebSocketClient client = factory.newWebSocketClient();

            URI uri = new URI(url);

            //WebSocketMessage msg = new WebSocketMessage();

            //WebSocket.Connection websocketConnection = client.open(uri, new WebSocketMessage(sampleEveryN, showMessages)).get(5, TimeUnit.SECONDS);
            WebSocket.Connection websocketConnection = client.open(uri, new WebSocketMessage(sampleEveryN, showMessages),timeout, TimeUnit.SECONDS);

            //System.out.println(System.currentTimeMillis());
            websocketConnection.setMaxTextMessageSize(MAX_MESSAGE_SIZE);
            //System.out.println(timeout);
            websocketConnection.setMaxIdleTime(timeout);
            
            
            
            
            //websocketConnection.setMaxIdleTime(-1);
            while (true) {
                if (websocketConnection.isOpen()) {                    
                    // Wait a second
                    Thread.sleep(1000);
                } else {
                    // Reopen
                    
                    websocketConnection = client.open(uri, new WebSocketMessage(sampleEveryN, showMessages)).get(5, TimeUnit.SECONDS);
                    websocketConnection.setMaxTextMessageSize(MAX_MESSAGE_SIZE);
                    websocketConnection.setMaxIdleTime(timeout);
                }
            } 

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    

    
    
    public static void main(String[] args) {
        
        



            //String url = "wss://W12AGS104.JENNINGS.HOME:6143/arcgis/ws/services/FAA/StreamServer/subscribe";
            //String url = "wss://W12.EXAMPLE.COM:6143/arcgis/ws/services/FAA-Stream/StreamServer/subscribe";
            //String url = "ws://W12.EXAMPLE.COM:6180/arcgis/ws/services/FAA-Stream/StreamServer/subscribe";
            //String url = "ws://localhost:8080/WhiteboardApp/whiteboardendpoint";
            //String url = "ws://localhost:8080/websats/satstream";
//            String url = "ws://localhost:8084/websats/SatStream/subscribe";
//            String url = "ws://esri105.westus.cloudapp.azure.com/websats/SatStream/subscribe";
            
        
        int numargs = args.length;
        
        if (numargs < 1 || numargs > 3 ) {
            System.err.print("Usage: WebSocketSink <ws-url> (<sample-every-N-records/1000>) (<display-messages/false>)\n");            
            System.err.print("NOTE: For GeoEvent Stream Service append /subscribe to the Web Socket URL\n");            
        } else {
            WebSocketSink a = new WebSocketSink();
            
            switch (numargs) {
                case 1:
                    a.connectWebsocket(args[0], 1000, false);                
                    break;
                case 2:
                    a.connectWebsocket(args[0], Integer.parseInt(args[1]), false);                
                    break;
                default:                    
                    a.connectWebsocket(args[0], Integer.parseInt(args[1]), Boolean.parseBoolean(args[2]) );                
                    break;
            }
            
            
        }        
        
        
        
    }
}
