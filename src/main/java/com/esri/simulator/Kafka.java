/*

 * Sends events from a tab delimited directly to Kafka

*/
package com.esri.simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 *
 * @author david
 */
public class Kafka {

    private Producer<String, String> producer;
    private String topic;
    
    public Kafka(String brokers, String topic) {
    
        try {
            Properties props = new Properties();
            props.put("bootstrap.servers",brokers);
            props.put("client.id", Kafka.class.getName());
            props.put("acks", "1");
            props.put("retries", 0);
            props.put("batch.size", 16384);
            props.put("linger.ms", 1);
            props.put("buffer.memory", 8192000);
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            /* Addin Simple Partioner didn't help */
            //props.put("partitioner.class", SimplePartitioner.class.getCanonicalName());
            
            this.producer = new KafkaProducer<>(props);
            this.topic = topic;
            
        } catch (Exception e) {
            e.printStackTrace();
        }        
    }
    
    /**
     * 
     * @param filename File with lines of data to be sent.
     * @param rate Rate in lines per second to send.
     * @param numToSend Number of lines to send. If more than number of lines in file will resend from start.
     * @param burstDelay Number of milliseconds to burst at; set to 0 to send one line at a time 
     * @param tweak Used to tweak the actual send rate adjusting for hardware.  (optional)
     */
    public void sendFile(String filename, Integer rate, Integer numToSend, Integer burstDelay, Integer tweak) {
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
            
            
            // *********** SEND Constant Rate using nanosecond delay *********
            if (burstDelay == 0) {
                // Delay between each send in nano seconds            
                Double ns_delay = 1000000000.0 / (double) rate;
                
                // By adding some to the delay you can fine tune to achieve the desired output
                ns_delay = ns_delay - (tweak * 100);
                
                long ns = ns_delay.longValue();
                if (ns < 0) ns = 0;  // can't be less than 0 




                while (cnt < numToSend) {
                    cnt += 1;
                    LocalDateTime ct = LocalDateTime.now();

                    if (!linesIt.hasNext()) linesIt = lines.iterator();  // Reset Iterator

                    line = linesIt.next() + "\n";

                    final long stime = System.nanoTime();
                    
                    UUID uuid = UUID.randomUUID();
                    producer.send(new ProducerRecord<String,String>(this.topic, uuid.toString(),line));

                    
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

                if (numPerBurst < 1) numPerBurst = 1;

                while (cnt < numToSend) {
                    cnt += numPerBurst;

                    Integer i = 0;
                    while (i < numPerBurst) {
                        i += 1;
                        if (!linesIt.hasNext()) linesIt = lines.iterator();  // Reset Iterator

                        line = linesIt.next() + "\n";

                        UUID uuid = UUID.randomUUID();
                        producer.send(new ProducerRecord<String,String>(this.topic, uuid.toString(),line));

                    }
                    // If you remove some part of the time which is used for sending
                    
                    Integer delay = msDelay - tweak;
                    if (delay < 0) delay = 0;
                    
                    Thread.sleep(delay);
                }
            
            }            
            Double sendRate = 0.0;

            if (st != null ) {
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
    
    /**
     * Forwards to sendFile with tweak default value of 0.
     * @param filename
     * @param rate
     * @param numToSend
     * @param burstDelay 
     */
    public void sendFile(String filename, Integer rate, Integer numToSend, Integer burstDelay) {
        sendFile(filename, rate, numToSend, burstDelay, 0);
    }
        
    
    
    public static void main(String args[]) {
        
        // Command Line d1.trinity.dev:9092 simFile simFile_1000_10s.dat 1000 10000
        
        if (args.length != 5) {
            System.err.print("Usage: Kafka <broker-list> <topic> <file> <rate> <numrecords>\n");
        } else {
            Kafka t = new Kafka(args[0], args[1]);
            t.sendFile(args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]), 0);

        }
                
       
    }
}
