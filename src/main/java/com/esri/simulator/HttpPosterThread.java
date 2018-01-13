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
import java.io.UnsupportedEncodingException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

/**
 *
 * @author david
 */
public class HttpPosterThread extends Thread {

    LinkedBlockingQueue<String> lbq;
    private volatile boolean running = true;

    private final String url;
    // Tell the server I'm Firefox
    private final String USER_AGENT = "Mozilla/5.0";

    //private CloseableHttpClient httpClient;
    private final CloseableHttpClient httpClient;

    private final HttpPost httpPost;
    
    private long cntErr;
    private long cnt;

    public long getCntErr() {
        return cntErr;
    }

    public long getCnt() {
        return cnt;
    }
    

    
    HttpPosterThread(LinkedBlockingQueue<String> lbq, String url) throws Exception {
        this.lbq = lbq;
        this.url = url;

        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                builder.build());
        httpClient = HttpClients.custom().setSSLSocketFactory(
                sslsf).build();

        //httpClient = HttpClientBuilder.create().build();
        httpPost = new HttpPost(url);

        cntErr = 0;
    }

    public void terminate() {
        running = false;
    }

    @Override
    public void run() {
        try {
            while (running) {
                String line = lbq.take();
                if (line == null) {
                    break;
                }
                StringEntity postingString = new StringEntity(line);

                httpPost.setEntity(postingString);
                //httpPost.setHeader("Content-type","plain/text");
                httpPost.setHeader("Content-type", "application/json");

                HttpResponse resp = httpClient.execute(httpPost);
                //CloseableHttpResponse resp = httpClient.execute(httpPost);

                if (resp.getStatusLine().getStatusCode() != 200) {
                    cntErr += 1;
                }
                
                // Using EntityUtils.consume hurt my kafkaHttp; did not help other ingest
                //HttpEntity respEntity = resp.getEntity();
                //EntityUtils.consume(respEntity);
                httpPost.releaseConnection();

                cnt += 1;
                

            }

        } catch (InterruptedException ex) {
            Logger.getLogger(HttpPosterThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(HttpPosterThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HttpPosterThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
