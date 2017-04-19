package com.esri.simulator;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
     * Assumes you have mesos dns installed and configured. So you should be
     * able to ping marathon.mesos and <hub-name>.marathon.mesos from the server
     * you run this on
     *
     */
    public String getBrokers(String kafkaName) {
        String brokers = "";
        try {

            // Since no port was specified assume this is a hub name
            String url = "http://marathon.mesos:8080/v2/apps/" + kafkaName;
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

            //System.out.println(result);
            JSONObject json = new JSONObject(result.toString());
            JSONObject app = json.getJSONObject("app");
            JSONArray tasks = app.getJSONArray("tasks");
            JSONObject task = tasks.getJSONObject(0);
            JSONArray ports = task.getJSONArray("ports");

            int k = 0;
            brokers = "";

            while (k < ports.length() && brokers.isEmpty()) {
                try {

                    Integer port = ports.getInt(k);
                    //System.out.println(port);

                    k++;

                    // Now get brokers from service
                    url = "http://" + kafkaName + ".marathon.mesos:" + String.valueOf(port) + "/v1/connection";

                    //System.out.println(url);
                    request = new HttpGet(url);

                    response = httpclient.execute(request);
                    rd = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent()));

                    result = new StringBuffer();
                    line = "";
                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }

                    //System.out.println(result);
                    json = new JSONObject(result.toString());

                    JSONArray addresses = json.getJSONArray("address");

                    for (int i = 0; i < addresses.length(); i++) {
                        if (i > 0) {
                            brokers += ",";
                        }
                        brokers += addresses.getString(i);
                    }

                    
                } catch (Exception e) {
                    brokers = "";
                }

            }

        } catch (Exception e) {

            brokers = "Could not find brokers.";
        }

        System.out.println("brokers: " + brokers);
        return brokers;
    }

    public String getElasticSearchTransportAddresses(String esFrameworkName)  {
        // Get the Transport Addresses for given Elasticsearch Framework Name (e.g. elasticsearch by default)
        String addresses = "";

        try {
            // Since no port was specified assume this is service name
            //String url = "http://leader.mesos/service/" + esFrameworkName + "/v1/tasks";
            String url = "http://marathon.mesos:8080/v2/apps/" + esFrameworkName;
            //System.out.println(url);

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

            //System.out.println(result);
            JSONObject json = new JSONObject(result.toString());
            JSONObject app = json.getJSONObject("app");
            JSONArray tasks = app.getJSONArray("tasks");
            JSONObject task = tasks.getJSONObject(0);
            JSONArray ports = task.getJSONArray("ports");

            int port = ports.getInt(0);
            String eip = task.getString("host");

            url = "http://" + eip + ":" + port + "/v1/tasks";

            request = new HttpGet(url);

            response = httpclient.execute(request);
            rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            result = new StringBuffer();
            line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            JSONArray jsonArray = new JSONArray(result.toString());

            int i = 0;
            while (i < jsonArray.length()) {
                JSONObject item = jsonArray.getJSONObject(i);
                String ta = item.getString("transport_address");
                //System.out.println(ta);
                if (i > 0) {
                    addresses += ",";
                }
                addresses += ta;
                i++;
            }

        } catch (Exception e) {
            addresses = "Could not find elasticsearch transports.";
        }

        System.out.println("elastic transports: " + addresses);
        return addresses;
    }

    public String getElasticSearchHttpAddresses(String esAppName)  {
        // Get the Http Addresses for given Elasticsearch Framework Name (e.g. elasticsearch by default)

        String addresses = "";

        try {
            // Since no port was specified assume this is a hub name
            //String url = "http://leader.mesos/service/" + esAppName + "/v1/tasks";
            String url = "http://marathon.mesos:8080/v2/apps/" + esAppName;
            //System.out.println(url);

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

            //System.out.println(result);
            JSONObject json = new JSONObject(result.toString());
            JSONObject app = json.getJSONObject("app");
            JSONArray tasks = app.getJSONArray("tasks");
            JSONObject task = tasks.getJSONObject(0);
            JSONArray ports = task.getJSONArray("ports");

            int port = ports.getInt(0);
            String eip = task.getString("host");

            url = "http://" + eip + ":" + port + "/v1/tasks";;

            request = new HttpGet(url);

            response = httpclient.execute(request);
            rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            result = new StringBuffer();
            line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            JSONArray jsonArray = new JSONArray(result.toString());

            int i = 0;
            while (i < jsonArray.length()) {
                JSONObject item = jsonArray.getJSONObject(i);
                String ta = item.getString("http_address");
                //System.out.println(ta);
                if (i > 0) {
                    addresses += ",";
                }
                addresses += ta;
                i++;
            }

        } catch (Exception e) {
            addresses = "Could not find elasticsearch web addresses.";
        }
        
        System.out.println("elastic web: " + addresses);
        return addresses;
    }

    public String getElasticSearchClusterName(String esAppName)  {
        // Get the Cluster Name for given Elasticsearch Framework Name (e.g. elasticsearch by default)

        String clusterName = "";

        try {
            // Since no port was specified assume this is a hub name
            //String url = "http://leader.mesos/service/" + esAppName + "/v1/cluster";
            String url = "http://marathon.mesos:8080/v2/apps/" + esAppName;
            //System.out.println(url);

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

            //System.out.println(result);
            JSONObject json = new JSONObject(result.toString());
            JSONObject app = json.getJSONObject("app");
            JSONArray tasks = app.getJSONArray("tasks");
            JSONObject task = tasks.getJSONObject(0);
            JSONArray ports = task.getJSONArray("ports");

            int port = ports.getInt(0);
            String eip = task.getString("host");

            url = "http://" + eip + ":" + port + "/v1/cluster";;

            request = new HttpGet(url);

            response = httpclient.execute(request);
            rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            result = new StringBuffer();
            line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            json = new JSONObject(result.toString());
            JSONObject config = json.getJSONObject("configuration");
            clusterName = config.getString("ElasticsearchClusterName");

        } catch (Exception e) {
            clusterName = "Could not find elasticsearch cluster name.";
        }

        System.out.println("elastic cluster name: " + clusterName);
        return clusterName;

    }



    public static void main(String[] args) {

        int numargs = args.length;

        if (numargs != 2) {
            System.err.print("Usage: MarathonInfo <kafka|elastic> <framework-name>\n");
        } else {

            MarathonInfo t = new MarathonInfo();
            
            String typ = args[0];
            
            if (typ.equalsIgnoreCase("kafka")) {
                t.getBrokers(args[1]);
            } else if (typ.equalsIgnoreCase("elastic")) {
                t.getElasticSearchClusterName(args[1]);
                t.getElasticSearchHttpAddresses(args[1]);
                t.getElasticSearchTransportAddresses(args[1]);                
            } else {
                System.err.println("First parameter should be <kafka|elastic>");
                System.err.print("Usage: MarathonInfo <kafka|elastic> <framework-name>\n");
            }                                  

        }

    }

}
