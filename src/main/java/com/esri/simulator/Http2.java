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

 /*
 * Sends lines of a text file to a HTTP Server using HTTP Post
 * Lines are sent at a specified rate.
 * 
 * Creator: David Jennings
 */
package com.esri.simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

/**
 *
 * @author david
 */
public class Http2 {

    private String url;

    // Tell the server I'm Firefox
    private final String USER_AGENT = "Mozilla/5.0";

    //private CloseableHttpClient httpClient;
    private CloseableHttpClient httpClient;
    private HttpPost httpPost;

    public Http2(String url) throws Exception {

        
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 20
        cm.setDefaultMaxPerRoute(20);
        
        // Increase max connections for localhost:80 to 50
        HttpHost localhost = new HttpHost("locahost", 80);
        cm.setMaxPerRoute(new HttpRoute(localhost), 50);        
        
        this.url = url;

        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                builder.build());
        httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setSSLSocketFactory(sslsf).build();

        //httpClient = HttpClientBuilder.create().build();

        httpPost = new HttpPost(url);

    }

    private void postLine(String line) throws Exception {

        StringEntity postingString = new StringEntity(line);

        httpPost.setEntity(postingString);
        //httpPost.setHeader("Content-type","plain/text");
        httpPost.setHeader("Content-type", "application/json");

        HttpResponse resp = httpClient.execute(httpPost);
        //CloseableHttpResponse resp = httpClient.execute(httpPost);

        //resp.getStatusLine().getStatusCode()
        
        // Using EntityUtils.consume hurt my kafkaHttp; did not help other ingest
        //HttpEntity respEntity = resp.getEntity();
        //EntityUtils.consume(respEntity);
        httpPost.releaseConnection();
    }

    /**
     *
     * @param filename File with lines of data to be sent.
     * @param rate Rate in lines per second to send.
     * @param numToSend Number of lines to send. If more than number of lines in
     * file will resend from start.
     * @param burstDelay Number of milliseconds to burst at; set to 0 to send
     * one line at a time
     * @param tweak Used to tweak the actual send rate adjusting for hardware.
     * (optional)
     */
    public void sendFile(String filename, Integer rate, Integer numToSend, Integer burstDelay, boolean appendTime) {
        try {
            FileReader fr = new FileReader(filename);
            BufferedReader br = new BufferedReader(fr);

            // Read the file into an array
            ArrayList<String> lines = new ArrayList<String>();

            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }

            br.close();
            fr.close();

            Iterator<String> linesIt = lines.iterator();

            // Get the System Time as st (Start Time)            
            Long st = System.currentTimeMillis();

            Integer cnt = 0;
            
            // Tweak used to adjust delays to try and get requested rate
            Long tweak = 0L;            

            /*
                For rates < 100/s burst is better
                For rates > 100/s continous is better            
             */
            // *********** SEND Constant Rate using nanosecond delay *********
            if (burstDelay == 0) {
                // Delay between each send in nano seconds            
                Double ns_delay = 1000000000.0 / (double) rate;

                // By adding some to the delay you can fine tune to achieve the desired output
                ns_delay = ns_delay - (tweak * 100);

                long ns = ns_delay.longValue();
                if (ns < 0) {
                    ns = 0;  // can't be less than 0 
                }

                while (cnt < numToSend) {
                    
                   if (cnt % rate == 0 && cnt > 0) {
                        // Calculate rate and adjust as needed
                        Double curRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;

                        System.out.println(cnt + "," + String.format("%.0f", curRate));
                    }

                    if (cnt % 1000 == 0 && cnt > 0) {
                        // Calculate rate and adjust as needed
                        Double curRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;

                        // rate difference as percentage 
                        Double rateDiff = (rate - curRate) / rate;

                        // Add or subracts up to 100ns 
                        tweak = (long) (rateDiff * rate);

                        // By adding some to the delay you can fine tune to achieve the desired output
                        ns = ns - tweak;
                        if (ns < 0) {
                            ns = 0;  // can't be less than 0 
                        }

                    }                    
                    
                    cnt += 1;

                    if (!linesIt.hasNext()) {
                        linesIt = lines.iterator();  // Reset Iterator
                    }
                    
                    final long stime = System.nanoTime();

                    if (appendTime) {
                        // assuming CSV

                        line = linesIt.next() + "," + String.valueOf(System.currentTimeMillis()) + "\n";
                    } else {
                        line = linesIt.next() + "\n";
                    }

                    postLine(line);
                    //System.out.println(line);

                    long etime = 0;
                    do {
                        // This approach uses a lot of CPU                    
                        etime = System.nanoTime();
                        // Adding the following sleep for a few microsecond reduces the load
                        // However, it also effects the through put
                        //Thread.sleep(0,100);  
                    } while (stime + ns >= etime);

                }
            } else {
                // *********** SEND in bursts every msDelay ms  *********

                Integer msDelay = burstDelay;
                Integer numPerBurst = Math.round(rate / 1000 * msDelay);

                if (numPerBurst < 1) {
                    numPerBurst = 1;
                }

                Integer delay = burstDelay;
                
                while (cnt < numToSend) {
                    
                   // Adjust delay every burst
                    Double curRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;
                    Double rateDiff = (rate - curRate) / rate;
                    tweak = (long) (rateDiff * rate);
                    delay = delay - Math.round(tweak / 1000.0f);
                    if (delay < 0) {
                        delay = 0;  // delay cannot be negative
                    } else {
                        Thread.sleep(delay);
                    }                    
                    
                    

                    Integer i = 0;
                    while (i < numPerBurst) {
                        if (cnt % rate == 0 && cnt > 0) {
                            curRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;
                            System.out.println(cnt + "," + String.format("%.0f", curRate));
                        }

                        cnt += 1;                        
                        
                        i += 1;
                        if (!linesIt.hasNext()) {
                            linesIt = lines.iterator();  // Reset Iterator
                        }

                        if (appendTime) {
                            line = linesIt.next() + "," + String.valueOf(System.currentTimeMillis()) + "\n";
                        } else {
                            line = linesIt.next() + "\n";
                        }

                        postLine(line);
                        
                        // Break out as soon as numToSend is reached
                        if (cnt >= numToSend) {
                            break;
                        }                        

                    }

                }

            }
            Double sendRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;

            System.out.println(cnt + "," + String.format("%.0f", sendRate));
            

        } catch (Exception e) {
            // Could fail on very large files that would fill heap space 
//            System.out.println(con.toString());
            e.printStackTrace();

        }
    }



    public static void main(String args[]) throws Exception {

        // Example Command Line args: localhost:8001 simFile_1000_10s.dat 1000 10000
        int numargs = args.length;
        if (numargs != 4 && numargs != 5) {
            // append append time option was added to support end-to-end latency; I used it for Trinity testing but it's a little confusing
            //System.err.print("Usage: Http <url> <file> <rate> <numrecords> (<append-time-csv>)\n");
            System.err.print("Usage: Http2 <url> <file> <rate> <numrecords> (<numthreads>)\n");
            System.err.print("url: Where to post messages to.\n");
            System.err.print("file: Post lines from this file.\n");
            System.err.print("rate: Number to post per second.\n");
            System.err.print("threads: Number of threads to use.\n");
        } else {
            String url = args[0];
            String file = args[1];
            Integer rate = Integer.parseInt(args[2]);
            Integer numrecords = Integer.parseInt(args[3]);
            Integer numThreads = Integer.parseInt(args[4]);
            Boolean appendTime = false;
            if (numargs == 6) {
                appendTime = Boolean.parseBoolean(args[5]);
            }
            Http2 t = new Http2(url);
            t.sendFile(file, rate, numrecords, 0, appendTime);

        }

    }
}
