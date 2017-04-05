/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.esri.simulator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;


/**
 *
 * @author david
 */
public class FeatureLayerMon {

    public FeatureLayerMon(String featureLayerURL, int sampleRateSec) {

        try {

            String strURL = featureLayerURL + "/query?where=1%3D1&returnCountOnly=true&f=json";

            URL url = new URL(strURL);

            SSLContext sslContext = SSLContext.getInstance("SSL");

            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    System.out.println("getAcceptedIssuers =============");
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs,
                        String authType) {
                    System.out.println("checkClientTrusted =============");
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs,
                        String authType) {
                    System.out.println("checkServerTrusted =============");
                }
            }}, new SecureRandom());


            CloseableHttpClient httpclient = HttpClients
                    .custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();

            int cnt1 = 0;
            int cnt2 = -1;
            int stcnt = 0;
            int numSamples = 0;
            long t1 = 0L;
            long t2 = 0L;

            SimpleRegression regression = new SimpleRegression();

            while (true) {
                try {

                    HttpGet request = new HttpGet(strURL);
                    CloseableHttpResponse response = httpclient.execute(request);
                    BufferedReader rd = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent()));

                    StringBuffer result = new StringBuffer();
                    String line = "";
                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }

                    //System.out.println(result);
                    JSONObject json = new JSONObject(result.toString());
                    request.abort();
                    response.close();

                    cnt1 = json.getInt("count");
                    t1 = System.currentTimeMillis();

                    if (cnt2 == -1) {
                        cnt2 = cnt1;
                        stcnt = cnt1;

                    } else if (cnt1 > cnt2) {
                        // Increase number of samples
                        numSamples += 1;
                        // Add to Linear Regression
                        regression.addData(t1, cnt1);
                        System.out.println(numSamples + "," + t1 + "," + cnt1);

                    } else if (cnt1 == cnt2 && numSamples > 0) {
                        numSamples -= 1;
                        // Remove the last sample
                        regression.removeData(t2, cnt2);
                        System.out.println("Removing: " + t2 + "," + cnt2);
                        // Output Results
                        int cnt = cnt2 - stcnt;
                        double rcvRate = regression.getSlope() * 1000;  // converting from ms to seconds

                        if (numSamples > 5) {
                            double rateStdErr = regression.getSlopeStdErr();
                            System.out.format("%d , %.2f, %.4f\n", cnt, rcvRate, rateStdErr);
                        } else if (numSamples >= 3) {
                            System.out.format("%d , %.2f\n", cnt, rcvRate);
                        } else {
                            System.out.println("Not enough samples to calculate rate. ");
                        }

                        // Reset 
                        cnt1 = -1;
                        cnt2 = -1;
                        stcnt = 0;
                        numSamples = 0;
                        t1 = 0L;
                        t2 = 0L;
                        regression = new SimpleRegression();

                    }

                    cnt2 = cnt1;
                    t2 = t1;

                    // Pause 5 seconds 
                    Thread.sleep(sampleRateSec * 1000);

                } catch (SSLHandshakeException e) {
                    System.out.println("Can't connect to the URL: " + featureLayerURL);
                    e.printStackTrace();
                    break;

                } catch (Exception a) {

                    System.out.println(a.getMessage());
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {

        // Command Line to watch counts on speicifed ArcGIS Server Feature Layer  
        /*
        NOTE: For latency calculations ensure all servers including the server running simulation
        are using time chrnonized.

        Run this command simulatneously on machines to compare time
        $ date +%s

        NTP command force update now:  $ sudo ntpdate -s time.nist.gov
        CHRONYD check status: $ chronyc tracking

         */
        int numargs = args.length;

        //FeatureLayerMon t = new FeatureLayerMon("https://ec2-52-14-149-22.us-east-2.compute.amazonaws.com:6443/arcgis/rest/services/Hosted/FAA-Stream/FeatureServer/0");
        //FeatureLayerMon t = new FeatureLayerMon("https://portal.example.com/arcgis/rest/services/Hosted/FAA-Stream/FeatureServer/0");
        if (numargs != 1 && numargs != 2) {
            System.err.print("Usage: FeatureLayerMon <Feature-Layer> (<sampleRateMS>) \n");
        } else if (numargs == 1) {
            FeatureLayerMon t = new FeatureLayerMon(args[0], 5);
        } else {
            FeatureLayerMon t = new FeatureLayerMon(args[0], Integer.parseInt(args[1]));
        }

    }
}
