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
 * Sends lines of a text file to a TCP Server 
 * Lines are sent at a specified rate.
 * While sending the send rates are adjusted to try to overcome hardware differences.
 * The maximum possible rate depends on hardware and network.
 * You can use Java options (-Xms2048m -Xmx2048m) to set the Heap Size available.
 * 
 * 30 Aug 2017: Updated to renable support to append time; modified code to adjust rate
 *     dynamically to more closely achieve the requested rate.
 *     Testing from 10,000 to 180,000
 *         BurstDelay = 0: Rate with 1% of requested rate
 *         BurstDelay = 100: Rate with 3% of requested rate
 *         Peak around 150,000/s on my computer i7 with 32GB RAM
 * 
 * 7 Sep 2017: Added Tcp: If a DNS name is provide a lookup is done and a socket is opened to each
 *      ip associated with the name.  tcp-kafka.marathon.mesos might have 4 ip; each ip gets a socket.
 *      With this change I could get rates with 4 instances up to around 500,000/s on Azure.
 *
 * 26 Jan 2018: Modified to used a linked block queue and threads.
 *      Combined parameters server port into one server:port.
 *      The server:port can be ip:port, dns-name:port, app[marathon-app-name], or app[marathon-app-name:portindex]
 *      Uses Marathon-Info to lookup ip and ports for marathon-app-name if needed.
 *      Uses DNS lookup and InetAddress to find ip's for a given name.
 *      Each thread is assigned a ip:port in a round robin fashion. 
 *      These changes increased max send rate from 140k/s to close to 600k/s
 *
 * 
 * Creator: David Jennings
 */
package com.esri.simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

/**
 *
 * @author david
 */
public class Tcp3 {

    private static final String IPADDRESS_PATTERN
            = "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5]).*";

    LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<>();

    /**
     *
     * @param appNamePattern
     * ip:port/app[marathon-app-name(:index)]/dns-name:port
     * @param filename File with lines of data to be sent.
     * @param rate Rate in lines per second to send.
     * @param numToSend Number of lines to send. If more than number of lines in
     * file will resend from start.
     * @param numThreads
     * @param appendTime If set to true system time is appended (assumes csv)
     */
    public void sendFile(String appNamePattern, String filename, Integer rate, Integer numToSend, Integer numThreads, boolean appendTime) {
        try {

            // See if the url contains app(name)
            String appPat = "app\\[(.*)\\]";

            // Test to see if the appName pattern matches app[some-app-name]
            Pattern appPattern = Pattern.compile(appPat);
            Matcher appMatcher = appPattern.matcher(appNamePattern);

            // Test to see if the appName looks like it contains a ip address num.num.num.num
            Pattern IPpattern = Pattern.compile(IPADDRESS_PATTERN);
            Matcher IPmatcher = IPpattern.matcher(appNamePattern);

            ArrayList<IPPort> ipPorts = new ArrayList<>();

            if (appMatcher.find()) {
                // appNamePatter has app pattern
                String appName = appMatcher.group(1);

                int portIndex = 0;
                String appNameParts[] = appName.split(":");
                if (appNameParts.length > 1) {
                    appName = appNameParts[0];
                    portIndex = Integer.parseInt(appNameParts[1]);
                }
                System.out.println("appName:" + appName);
                System.out.println("portIndex:" + portIndex);
                MarathonInfo mi = new MarathonInfo();

                ipPorts = mi.getIPPorts(appName, portIndex);
            } else if (IPmatcher.matches()) {
                // appNamePattern looks like an IP
                String ipPortParts[] = appNamePattern.split(":");
                if (ipPortParts.length != 2) {
                    throw new UnsupportedOperationException("You need to provide IP:port");
                }
                int port = Integer.parseInt(ipPortParts[1]);

                IPPort ipport = new IPPort(ipPortParts[0], port);
                ipPorts.add(ipport);

            } else {
                // Assume it's a DNS-name:port

                String namePortParts[] = appNamePattern.split(":");
                if (namePortParts.length != 2) {
                    throw new UnsupportedOperationException("You need to provide dns-name:port");
                }
                String dnsName = namePortParts[0];
                int port = Integer.parseInt(namePortParts[1]);

                Lookup lookup = new Lookup(dnsName, Type.A);
                lookup.run();
                //System.out.println(lookup.getErrorString());
                if (lookup.getAnswers() == null) {
                    InetAddress addr = InetAddress.getByName(dnsName);

                    IPPort ipport = new IPPort(addr.getHostAddress(), port);
                    ipPorts.add(ipport);
                } else {
                    for (Record ans : lookup.getAnswers()) {
                        String ip = ans.rdataToString();
                        IPPort ipport = new IPPort(ip, port);
                        ipPorts.add(ipport);
                        System.out.println(ipport);

                    }
                }

            }

            for (IPPort ipport : ipPorts) {
                if (ipport.getPort() == -1) {
                    ipPorts.remove(ipport);
                }
            }

            if (ipPorts.size() == 0) {
                throw new UnsupportedOperationException("Could not discover the any ip port combinations.");
            }

            // Read File
            FileReader fr = new FileReader(filename);
            BufferedReader br = new BufferedReader(fr);

            // Load Array with Lines from File
            ArrayList<String> lines = new ArrayList<>();

            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }

            br.close();
            fr.close();

            // Create the TcpSenderThreads
            TcpSenderThread[] threads = new TcpSenderThread[numThreads];

            for (int i = 0; i < numThreads; i++) {
                // Use modulo to get one of the ipport's 0
                IPPort ipPort = ipPorts.get((i + 1) % ipPorts.size());
                threads[i] = new TcpSenderThread(lbq, ipPort.getIp(), ipPort.getPort());
                threads[i].start();

            }            
            
//            // Start a Timer 
            Timer timer = new Timer(true);
            timer.scheduleAtFixedRate(new LoadEventIntoQueue(lbq, lines, numToSend, rate, threads), 0, 1000);            


        } catch (Exception e) {

            e.printStackTrace();

        }
    }

    public static void main(String args[]) {

        // Example Command Line args: localhost 5565 faa-stream.csv 1000 10000
        int numargs = args.length;
        if (numargs < 4 || numargs > 6) {
            // append append time option was added to support end-to-end latency; I used it for Trinity testing
            System.err.println("Usage: Tcp2 <server:port> <file> <rate> <numrecords> (numThreads=1) (append-time=false)");
            System.err.println("server:port: The IP or hostname of server to send events to. Could be ip:port, dns-name:port, or app[marathon-app-name(:portindex)]");
            System.err.println("filename: sends line by line from this file.");
            System.err.println("rate: Attempts to send at this rate.");
            System.err.println("numrecords: Sends this many lines; file is automatically recycled if needed.");
            System.err.println("numThread: Number of threads defaults to 1");
            System.err.println("append-time: Adds system time as extra parameter to each request. ");
        } else {
            // Initial the Tcp Class with the server and port

            String serverPort = args[0];
            String filename = args[1];
            Integer rate = Integer.parseInt(args[2]);
            Integer numrecords = Integer.parseInt(args[3]);
            Integer numThreads = 1;
            Boolean appendTime = false;

            switch (numargs) {
                case 5:
                    numThreads = Integer.parseInt(args[4]);
                    break;
                case 6:
                    numThreads = Integer.parseInt(args[4]);
                    appendTime = Boolean.parseBoolean(args[5]);
                    break;
            }

            Tcp3 t = new Tcp3();
            t.sendFile(serverPort, filename, rate, numrecords, numThreads, appendTime);

        }

    }
}
