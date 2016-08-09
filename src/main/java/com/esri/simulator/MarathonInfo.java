/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.esri.simulator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author david
 */
public class MarathonInfo {

    
    /**
     * 
     * @param kafkaName
     * @return comma separated list of brokers
     * @throws Exception 
     * 
     * Uses the Marathon rest api to get the brokers for the specified KafkaName
     * 
     * Assumes you have mesos dns installed and configured.
     * So you should be able to ping marathon.mesos and <hub-name>.marathon.mesos from the server you run this on
     * 
     */
    public String getBrokers(String kafkaName) throws Exception {
        String brokers = "";

        // Since no port was specified assume this is a hub name
        String url = "http://marathon.mesos/marathon/v2/apps/" + kafkaName;
        System.out.println(url);
        
        // Support for https
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                builder.build());
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(
                sslsf).build();
       
        HttpGet request = new HttpGet(url);

        HttpResponse response = httpclient.execute(request);
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        
        System.out.println(result);

        JSONObject json = new JSONObject(result.toString());
        JSONObject app = json.getJSONObject("app");
        JSONArray tasks = app.getJSONArray("tasks");
        JSONObject task = tasks.getJSONObject(0);
        JSONArray ports = task.getJSONArray("ports");
        Integer port = ports.getInt(0);
        System.out.println(port);

        // Now get brokers from service
        url = "http://" + kafkaName + ".marathon.mesos:" + String.valueOf(port) + "/v1/connection";

        System.out.println(url);
        
        request = new HttpGet(url);

        response = httpclient.execute(request);
        rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        result = new StringBuffer();
        line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        
        System.out.println(result);
        json = new JSONObject(result.toString());

        JSONArray addresses = json.getJSONArray("address");

        brokers = "";
        for (int i = 0; i < addresses.length(); i++) {
            if (i > 0) {
                brokers += ",";
            }
            brokers += addresses.getString(i);
        }

        System.out.println(brokers);

        return brokers;
    }

}
