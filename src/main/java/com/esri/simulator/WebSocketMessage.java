/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.esri.simulator;

import java.util.Timer;
import java.util.TimerTask;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.eclipse.jetty.websocket.WebSocket;
import org.json.JSONArray;

/**
 *
 * @author david
 */
//@WebSocket(maxBinaryMessageSize = 1024 * 1024)
public class WebSocketMessage implements WebSocket.OnTextMessage {

    class resetCounts extends TimerTask {

        @Override
        public void run() {
            double rcvRate = regression.getSlope() * 1000;
            if (numSamples > 5) {
                double rateStdErr = regression.getSlopeStdErr();
                System.out.format("%d , %.2f, %.4f\n", cnt, rcvRate, rateStdErr);
            } else if (numSamples >= 3) {
                System.out.format("%d , %.2f\n", cnt, rcvRate);
            } else {
                System.out.println("Not enough samples to calculate rate. ");
            }
            numSamples = 0;
            cnt = 0;
            timer = new Timer();
            regression = new SimpleRegression();
        }

    }

    Timer timer;
    int numSamples;
    SimpleRegression regression;
    Connection con;

    public Integer getCnt() {
        return cnt;
    }

    private Integer cnt = 0;
    //private Connection con;

    @Override
    public void onMessage(String s) {
        cnt++;
//        if (cnt % 1000 == 0) System.out.println(cnt);
//        timer.cancel();
//        timer.schedule(new resetCounts(), 0, 5000);

        if (cnt % 1000 == 0) {
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
        //System.out.println(cnt);
//        System.out.println(s);
//        JSONArray array = new JSONArray(s);
//        cnt += array.length();
//        System.out.println(cnt);

        //if (cnt % 1000 == 0) System.out.println(cnt);
        //System.out.println(data);
        //if (cnt >= numFeatures) this.con.close();
    }

    @Override
    public void onOpen(Connection connection) {
        //System.out.println("Websocket connected");
        numSamples = 0;
        cnt = 0;
        timer = new Timer();
        regression = new SimpleRegression();
        con = connection;
    }

    @Override
    public void onClose(int i, String s) {
        //System.out.println(System.currentTimeMillis());
        //System.out.println("Websocket connection lost");        
        double rcvRate = 0.0;
        if (numSamples >= 3) {
            rcvRate = regression.getSlope() * 1000;
        }
        if (numSamples > 5) {
            double rateStdErr = regression.getSlopeStdErr();
            System.out.format("%d , %.2f, %.4f\n", cnt, rcvRate, rateStdErr);
        } else if (numSamples >= 3) {
            System.out.format("%d , %.2f\n", cnt, rcvRate);
        }        
        //System.out.println("Number Received: " + this.cnt);

    }
}
