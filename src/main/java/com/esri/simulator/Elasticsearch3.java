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
 * Test Class
 *
 * Try using HTTP Post /bulk api
 * vip: data.elastic.l4lb.thisdcos.directory:9200 http
 * vip: data.elastic.l4lb.thisdcos.directory:9300 transport
 *
 * Having issues with DCOS Elastic framework and Transport Client
 * The Transport Client works fine with Elasticsearch 5 installed outside of DCOS.
 *
 * David Jennings
 * 
 * 13 Nov 2017
 * NOTE: Based on testing using sparktest; I suspect if I hyper-threaded this like I did for tcp I could get faster rates.
 * In a Round-robin fashion send requests to each of the elasticsearch nodes.  
 * 
 */
package com.esri.simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

/**
 *
 * @author david
 */
public class Elasticsearch3 {

    private final String USER_AGENT = "Mozilla/5.0";
    private HttpClient httpClient;
    private HttpPost httpPost;

    private String strURL;  // http://data.sats-sat03.l4lb.thisdcos.directory:9200/index/type
    private Integer esbulk;

    public Elasticsearch3(String strURL, Integer esbulk) {

        try {

            if (strURL.endsWith("/")) {
                this.strURL = strURL + "_bulk";
            } else {
                this.strURL = strURL + "/_bulk";
            }
            
            

            this.esbulk = esbulk;

            if (esbulk < 0) {
                esbulk = 0;
            }

            httpClient = HttpClientBuilder.create().build();

            httpPost = new HttpPost(this.strURL);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void postLine(String data) throws Exception {

        StringEntity postingString = new StringEntity(data);

        //System.out.println(data);
        httpPost.setEntity(postingString);
        //httpPost.setHeader("Content-type","plain/text");
        httpPost.setHeader("Content-type", "application/json");

        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String resp = httpClient.execute(httpPost, responseHandler);

        //JSONObject jsonResp = new JSONObject(resp);
        //System.out.println(jsonResp);
        httpPost.releaseConnection();
    }

    public void sendFile(String filename, Integer rate, Integer numToSend) {

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

            // Get the System Time
            LocalDateTime st = LocalDateTime.now();

            Integer cnt = 0;

            /*
                For rates < 100/s burst is better
                For rates > 100/s continous is better            
             */
            // Estimate time for to send all events requested
            double estTime = numToSend / rate;

            // Time for each block
            double timePerGroupMS = estTime * this.esbulk / numToSend * 1000.0;
            //System.out.println(timePerGroupMS);

            long msDelay = Math.round(timePerGroupMS);
            //System.out.println(msDelay);                                

            while (cnt < numToSend) {

                Integer i = 0;
                line = "";
                long stime = System.currentTimeMillis();

                while (i < this.esbulk && cnt < numToSend) {
                    i += 1;
                    cnt += 1;
                    if (!linesIt.hasNext()) {
                        linesIt = lines.iterator();  // Reset Iterator
                    }
                    line += "{\"index\": {}}\n";
                    line += linesIt.next() + "\n";
                }

                postLine(line);
                long etime = System.currentTimeMillis() - stime;

                long delay = msDelay - etime;
                if (delay > 0) {                
                    Thread.sleep(delay);
                }
            }

            Double sendRate = 0.0;

            if (st != null) {
                LocalDateTime et = LocalDateTime.now();

                Duration delta = Duration.between(st, et);

                Double elapsedSeconds = (double) delta.getSeconds() + delta.getNano() / 1000000000.0;

                sendRate = (double) cnt / elapsedSeconds;
            }

            System.out.println(cnt + "," + sendRate);

        } catch (Exception e) {
            // Could fail on very large files that would fill heap space 
//            System.out.println(con.toString());
            e.printStackTrace();

        }

    }

    public static void main(String[] args) {

        // Command line example: a3:9300 simulator simfile simFile_1000_10s.json 100 1000 20
        int numargs = args.length;
        if (numargs != 4 && numargs != 5) {
            System.err.print("Usage: Elasticsearch3 <elastic-url-to-type-index> <file> <rate> <numrecords> (<elastic-bulk-num>)\n");
        } else {

            String url = args[0];
            String filename = args[1];
            Integer rate = Integer.parseInt(args[2]);
            Integer numRecords = Integer.parseInt(args[3]);

            Integer elasticBulk = 1000;
            if (numargs == 5) {
                elasticBulk = Integer.parseInt(args[4]);
            }

            if (elasticBulk < 100 ) {
                System.out.println("Smallest supported elastic-bulk-num is 100");
            } else {
                Elasticsearch3 t = new Elasticsearch3(url, elasticBulk);
                t.sendFile(filename, rate, numRecords);
                
            }

        }

    }

}
