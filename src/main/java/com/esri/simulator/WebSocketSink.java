/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.esri.simulator;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

// https://github.com/jetty-project/embedded-jetty-websocket-examples


/**
 *
 * @author david
 */
public class WebSocketSink {
    
//    private void trustAll() {
//        TrustManager[] trustAllCerts = new TrustManager[]{
//                new X509TrustManager() {
//
//                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//                        return new X509Certificate[0];
//                    }
//
//
//                    public void checkClientTrusted(
//                            java.security.cert.X509Certificate[] certs, String authType) {
//                    }
//
//
//                    public void checkServerTrusted(
//                            java.security.cert.X509Certificate[] certs, String authType) {
//                    }
//                }
//        };
//        try {
//            SSLContext sc = SSLContext.getInstance("SSL");
//            sc.init(null, trustAllCerts, new java.security.SecureRandom());
//            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//
//        } catch (GeneralSecurityException e) {
//            System.out.println("Oops");
//        }
//    }
    
    public void connectWebsocket() {


        try {
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setTrustAll(true);
            sslContextFactory.start();

            //WebSocketClient wsc = new WebSocketClient(sslContextFactory);


            final WebSocketClientFactory factory = new WebSocketClientFactory();

            factory.start();

         


            WebSocketClient client = factory.newWebSocketClient();



            //String url = "wss://W12AGS104.JENNINGS.HOME:6143/arcgis/ws/services/FAA/StreamServer/subscribe";
            String url = "wss://W12.EXAMPLE.COM:6143/arcgis/ws/services/FAA-Stream/StreamServer/subscribe";
            //String url = "ws://W12.EXAMPLE.COM:6180/arcgis/ws/services/FAA-Stream/StreamServer/subscribe";
            //String url = "ws://localhost:8080/WhiteboardApp/whiteboardendpoint";
            //String url = "ws://localhost:8080/websats/satstream";
            

            URI uri = new URI(url);

            //WebSocketMessage msg = new WebSocketMessage();

            WebSocket.Connection websocketConnection = client.open(uri, new WebSocketMessage()).get(5, TimeUnit.SECONDS);

            //System.out.println(System.currentTimeMillis());
            websocketConnection.setMaxTextMessageSize(1000000);
            websocketConnection.setMaxIdleTime(5000);
            
            //websocketConnection.setMaxIdleTime(-1);
            while (true) {
                if (websocketConnection.isOpen()) {
                    Thread.sleep(1000);
                } else {
                    // Reopen
                    websocketConnection = client.open(uri, new WebSocketMessage()).get(5, TimeUnit.SECONDS);
                    websocketConnection.setMaxTextMessageSize(1000000);
                    websocketConnection.setMaxIdleTime(5000);
                }
            } 
            
            
            


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    

    
    
    public static void main(String[] args) {
        WebSocketSink a = new WebSocketSink();
        a.connectWebsocket();
        
        
        
    }
}
