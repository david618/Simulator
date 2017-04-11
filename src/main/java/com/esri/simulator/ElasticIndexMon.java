/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.esri.simulator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Timer;
import java.util.TimerTask;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

/**
 *
 * @author david
 */
public class ElasticIndexMon {

    class CheckCount extends TimerTask {

        int cnt1;
        int cnt2;
        int stcnt;
        int numSamples;
        long t1;
        long t2;
        SimpleRegression regression;

        public CheckCount() {
            cnt1 = 0;
            cnt2 = -1;
            stcnt = 0;
            numSamples = 0;
            t1 = 0L;
            t2 = 0L;
            regression = new SimpleRegression();
        }

        @Override
        public void run() {
            try {

                // index/type
                String url = "http://" + esServer + "/" + index + "/_count";
                SSLContext sslContext = SSLContext.getInstance("SSL");

                CredentialsProvider provider = new BasicCredentialsProvider();
                UsernamePasswordCredentials credentials
                        = new UsernamePasswordCredentials(user, userpw);
                provider.setCredentials(AuthScope.ANY, credentials);

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
                        .setDefaultCredentialsProvider(provider)
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build();

                HttpGet request = new HttpGet(url);
                CloseableHttpResponse response = httpclient.execute(request);
                BufferedReader rd = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent()));

                Header contentType = response.getEntity().getContentType();
                String ct = contentType.getValue().split(";")[0];

                int responseCode = response.getStatusLine().getStatusCode();

                String line;
                StringBuffer result = new StringBuffer();
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }

                JSONObject json = new JSONObject(result.toString());

                //System.out.println(json.getInt("count"));
                cnt1 = json.getInt("count");

                t1 = System.currentTimeMillis();

                if (cnt2 == -1) {
                    cnt2 = cnt1;
                    stcnt = cnt1;

                } else if (cnt1 > cnt2) {
                    // Increase number of samples
                    numSamples += 1;
                    if (numSamples > 2) {
                        double rcvRate = regression.getSlope() * 1000;
                        System.out.println(numSamples + "," + t1 + "," + cnt1 + "," + rcvRate);
                    } else {
                        System.out.println(numSamples + "," + t1 + "," + cnt1);
                    }

                    // Add to Linear Regression
                    regression.addData(t1, cnt1);

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

            } catch (Exception e) {

            }

        }

    }

    Timer timer;
    String esServer;
    String index;
    String user;
    String userpw;

    public ElasticIndexMon(String esServer, String index, String user, String userpw, long sampleRateMS) {

//        esServer = "ags:9220";
//        index = "FAA-Stream/FAA-Stream";
//        user = "els_ynrqqnh";
//        userpw = "8jychjwcgn";
        this.esServer = esServer;
        this.index = index;
        this.user = user;
        this.userpw = userpw;
        timer = new Timer();
        timer.schedule(new CheckCount(), 0, sampleRateMS);
    }

    public static void main(String[] args) {
        int numargs = args.length;

        //FeatureLayerMon t = new FeatureLayerMon("https://ec2-52-14-149-22.us-east-2.compute.amazonaws.com:6443/arcgis/rest/services/Hosted/FAA-Stream/FeatureServer/0");
        //FeatureLayerMon t = new FeatureLayerMon("https://portal.example.com/arcgis/rest/services/Hosted/FAA-Stream/FeatureServer/0");
        if (numargs != 2 && numargs != 4 && numargs != 5) {
            System.err.print("Usage: ElasticIndexMon <ElasticsearchServerPort> <Index/Type> (<username> <password> <sampleRateMS>) \n");
        } else if (numargs == 2) {
            ElasticIndexMon t = new ElasticIndexMon(args[0], args[1], "", "", 5000);
        } else if (numargs == 4) {
            ElasticIndexMon t = new ElasticIndexMon(args[0], args[1], args[2], args[3], 5000);
        } else {
            ElasticIndexMon t = new ElasticIndexMon(args[0], args[1], args[2], args[3], Integer.parseInt(args[4]));
        }

    }
}
