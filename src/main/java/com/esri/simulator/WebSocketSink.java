/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
    
    public void connectWebsocket(String url, int timeout, int sampleEveryN, boolean showMessages) {


        
        try {
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setTrustAll(true);
            sslContextFactory.start();

            final WebSocketClientFactory factory = new WebSocketClientFactory();

            factory.start();

            WebSocketClient client = factory.newWebSocketClient();

            URI uri = new URI(url);

            //WebSocketMessage msg = new WebSocketMessage();

            WebSocket.Connection websocketConnection = client.open(uri, new WebSocketMessage(sampleEveryN, showMessages)).get(5, TimeUnit.SECONDS);

            //System.out.println(System.currentTimeMillis());
            websocketConnection.setMaxTextMessageSize(MAX_MESSAGE_SIZE);
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
        
        if (numargs < 1 || numargs > 4 ) {
            System.err.print("Usage: WebSocketSink <ws-url> (<timeout-ms>) (<sample-every-N-records>) (<display-messages>)\n");
            System.err.print("Defaults: timeout-ms:10000, sample-every-N-records:1000, display-messages:false\n");
        } else {
            WebSocketSink a = new WebSocketSink();
            
            switch (numargs) {
                case 1:
                    a.connectWebsocket(args[0], 10000, 1000, false);                
                    break;
                case 2:
                    a.connectWebsocket(args[0], Integer.parseInt(args[1]), 1000, false);                
                    break;
                case 3:
                    a.connectWebsocket(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), false);                
                    break;
                default:
                    a.connectWebsocket(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Boolean.parseBoolean(args[3]));                
                    break;
            }
            
            
        }        
        
        
        
    }
}
