/** 
 * Sends features from a file directly to Elasticsearch.
 * Each line of input file should be json to be send.
 * The tool sends at a specified rate
 * Supports an optional parameter to bulk load; this sends the specified number of items in a group)
 * 
 * Creator: David Jennings
 * 
 */

package com.esri.simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

/**
 *
 * @author david
 */
public class Elasticsearch {

    Client client;

    public Elasticsearch(String transports, String clusterName) {
        try {

//            Settings settings = Settings.settingsBuilder().put("cluster.name", clusterName).build();
//            TransportClient tc = TransportClient.builder().settings(settings).build();
            
            // These are for Elasticsearch 5
            Settings settings = Settings.builder().put("cluster.name", clusterName).build();
            TransportClient tc = new PreBuiltTransportClient(settings);            
            

            String hosts[] = transports.split(",");
            for (String host : hosts) {
                String parts[] = host.split(":");
                InetAddress addr = InetAddress.getByName(parts[0]);
                Integer port = Integer.parseInt(parts[1]);

                tc.addTransportAddress(new InetSocketTransportAddress(addr, port));

                System.out.println(addr.toString() + "," + port.toString());

            }

            this.client = (Client) tc;

            System.out.println(this.client);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * @param idx
     * @param typ
     * @param filename
     * @param rate
     * @param numToSend
     */
    public void sendFile(String idx, String typ, String filename, Integer rate, Integer numToSend, Integer elasticBulk) {
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

            // Delay between each send in nano seconds            
            Double ns_delay = 1000000000.0 / (double) rate;

            long ns = ns_delay.longValue();
            if (ns < 0) {
                ns = 0;  // can't be less than 0 
            }

            BulkRequestBuilder bulkRequest = client.prepareBulk();

            while (cnt < numToSend) {
                cnt += 1;
                LocalDateTime ct = LocalDateTime.now();

                if (!linesIt.hasNext()) {
                    linesIt = lines.iterator();  // Reset Iterator
                }
                line = linesIt.next() + "\n";

                final long stime = System.nanoTime();

                UUID uuid = UUID.randomUUID();
//                    producer.send(new ProducerRecord<String,String>(this.topic, uuid.toString(),line));
                //IndexResponse resp = client.prepareIndex(idx,typ).setSource(line).get();
                bulkRequest.add(client.prepareIndex(idx, typ).setSource(line));

                if (cnt % elasticBulk == 0) {
                    BulkResponse bulkResponse = bulkRequest.get();
                    bulkRequest = client.prepareBulk();
                }

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

            if (bulkRequest.numberOfActions() > 0) {
                BulkResponse bulkResponse = bulkRequest.get();
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

            e.printStackTrace();

        }
    }

    public static void main(String args[]) {

        // Command line example: a3:9300 simulator simfile simFile_1000_10s.json 100 1000 20
        int numargs = args.length;
        if (numargs != 7 && numargs != 8) {
            System.err.print("Usage: Elasticsearch <elastic-search-transports> <cluster-name> <index> <type> <file> <rate> <numrecords> (<elastic-bulk-num>)\n");
        } else {

            String transports = args[0];
            String clusterName = args[1];
            String idx = args[2];
            String typ = args[3];
            String filename = args[4];
            Integer rate = Integer.parseInt(args[5]);
            Integer numRecords = Integer.parseInt(args[6]);            

            Integer elasticBulk = 1000;
            if (numargs == 8) {
                elasticBulk = Integer.parseInt(args[7]);
            }

//            String transports = "e2:9300";
//            String clusterName = "elasticsearch";
//            String idx = "sink";
//            String typ = "test1";
//            String filename = "simFile_1000_10s.json";
//            Integer rate = 1000;
//            Integer numRecords = 10000;
//            Integer elasticBulk = 1000;
            

            Elasticsearch t = new Elasticsearch(transports, clusterName);
            t.sendFile(idx, typ, filename, rate, numRecords, elasticBulk);

        }
    }
}
