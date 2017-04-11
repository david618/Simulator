/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.esri.simulator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

/**
 *
 * @author david
 */
public class KafkaTopicMon {

    class CheckCount extends TimerTask {

        long cnt1 = 0;
        long cnt2 = -1;
        long stcnt = 0;
        int numSamples = 0;
        long t1 = 0L;
        long t2 = 0L;
        SimpleRegression regression;

        public CheckCount() {
            regression = new SimpleRegression();
            cnt1 = 0;
            cnt2 = -1;
            stcnt = 0;
            numSamples = 0;
            t1 = 0L;
            t2 = 0L;

        }

        @Override
        public void run() {
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                    .map(p -> new TopicPartition(topic, p.partition()))
                    .collect(Collectors.toList());
            consumer.assign(partitions);
            consumer.seekToEnd(Collections.emptySet());
            Map<TopicPartition, Long> endPartitions = partitions.stream()
                    .collect(Collectors.toMap(Function.identity(), consumer::position));
            consumer.seekToBeginning(Collections.emptySet());
            cnt1 = partitions.stream().mapToLong(p -> endPartitions.get(p) - consumer.position(p)).sum();

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
                long cnt = cnt2 - stcnt;
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

        }

    }

    Timer timer;
    String brokers;
    String topic;
    KafkaConsumer<String, String> consumer;

    public KafkaTopicMon(String brokers, String topic, long sampleRateMS) {

//        this.brokers = "k1:9092";
//        this.topic = "satellites";
        this.brokers = brokers;
        this.topic = topic;

        try {
            Properties props = new Properties();
            props.put("bootstrap.servers", this.brokers);
            // I should include another parameter for group.id this would allow differenct consumers of same topic
            props.put("group.id", "abc");
            props.put("enable.auto.commit", "true");
            props.put("auto.commit.interval.ms", 1000);
            props.put("auto.offset.reset", "earliest");
            props.put("session.timeout.ms", "30000");
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

            consumer = new KafkaConsumer<>(props);

            timer = new Timer();
            timer.schedule(new CheckCount(), 0, sampleRateMS);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {

        int numargs = args.length;
        if (numargs != 2 && numargs != 3) {
            System.err.print("Usage: KakfaTopicMon <brokers> <topic> (<sampleRateMS>)\n");
        } else if (numargs == 2) {
            new KafkaTopicMon(args[0], args[1], 5000);
        } else {
            new KafkaTopicMon(args[0], args[1], Integer.parseInt(args[2]));
        }

    }

}
