/**
 * Used by WebSocketSink
 *
 * Creator: David Jennings
 */
package com.esri.simulator;

import java.util.Timer;
import java.util.TimerTask;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.eclipse.jetty.websocket.WebSocket;

/**
 *
 * @author david
 */
//@WebSocket(maxBinaryMessageSize = 1024 * 1024)
public class WebSocketMessage implements WebSocket.OnTextMessage {

    boolean printmessages;
    int sampleEvery;

    public WebSocketMessage(int sampleEvery, boolean printmessages) {
        this.printmessages = printmessages;
        this.sampleEvery = sampleEvery;
        this.numSamples = 0;
        this.cnt = 0;
        this.timer = new Timer();
        this.regression = new SimpleRegression();

    }

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

        if (printmessages) {
            System.out.println(s);
        } else if (cnt % sampleEvery == 0) {
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

    @Override
    public void onOpen(Connection connection) {
        if (cnt > 0) {
            System.out.println("Websocket connected");
            numSamples = 0;
            cnt = 0;
            timer = new Timer();
            regression = new SimpleRegression();
            con = connection;
        } else {
            System.out.println("Listening");
        }
    }

    @Override
    public void onClose(int i, String s) {
        //System.out.println(System.currentTimeMillis());
        double rcvRate = 0.0;
        if (numSamples >= 3) {
            rcvRate = regression.getSlope() * 1000;
        }
        if (numSamples > 5) {
            double rateStdErr = regression.getSlopeStdErr();
            System.out.println("Number of Samples,Count,Rate,StdErr");
            System.out.format("%d, %d , %.2f, %.4f\n", numSamples, cnt, rcvRate, rateStdErr);
        } else if (numSamples >= 3) {
            System.out.println("Number of Samples,Count,Rate,StdErr");
            System.out.format("%d, %d , %.2f\n", numSamples, cnt, rcvRate);
        }
        //System.out.println("Websocket connection lost");
        //System.out.println("Number Received: " + this.cnt);

    }
}
