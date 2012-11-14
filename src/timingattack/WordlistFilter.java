package timingattack;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;


/**
 *
 * @author adrian
 */
public class WordlistFilter {
    
    private static final int MAX_LIST_SIZE = 300;

    private final File inFile;
    private final File outFile;
    private final String hashType;

    public WordlistFilter(String hashType, String inFile, String outFile) throws IOException {

        this.inFile = new File(inFile);
        this.outFile = new File(outFile);
        this.hashType = hashType;

        if (!this.inFile.exists()) {
            throw new IOException("Wordlist file doesn't exist: " + inFile);
        }

    }

    public void start(String hashPrefix) throws IOException, NoSuchAlgorithmException, InterruptedException {

        ArrayBlockingQueue<List<String>> in = new ArrayBlockingQueue<List<String>>(100);
        ArrayBlockingQueue<List<String>> out = new ArrayBlockingQueue<List<String>>(100);

        int threadNum = Runtime.getRuntime().availableProcessors();
        
        //setup filter threads
        FilterThread[] threads = new FilterThread[threadNum];
        for (int i = 0; i < threadNum; i++) {
            threads[i] = new FilterThread(hashPrefix, hashType, in, out);
            threads[i].start();
        }
        
        //setup output thread
        new OutputThread(out, outFile).start();
        
        //read input
        List<String> pwds = new ArrayList<String>(MAX_LIST_SIZE);
        BufferedReader reader = null;
        
        try {
            
            reader = new BufferedReader(new FileReader(inFile));

            String word = null;
            while ((word = reader.readLine()) != null) {
                
                if (pwds.size() >= MAX_LIST_SIZE) {
                    in.put(pwds);
                    pwds = new ArrayList<String>(MAX_LIST_SIZE);
                }

                pwds.add(word);

            }
            
            //final stuff
            if (!pwds.isEmpty())
                in.put(pwds);
            
        } finally {
            if (reader != null)
                reader.close();
        }
        
        
        //send kill signals
        for (int i = 0; i < threadNum; i++) //filter threads
            in.put(new ArrayList<String>());
        out.put(new ArrayList<String>()); //output thread
        
        
    }
    
    
    public static void main(String[] args) throws Exception {
        
        String inList = "/home/adrian/Archive/PasswordLists/rockyou_ordered_by_likelyhood.txt";
        String outList = "/home/adrian/Desktop/filtered_wordlist.txt";
        
        WordlistFilter f = new WordlistFilter("SHA-1", inList, outList);
        f.start("0");
        
    }

    public String getOutputPath() {
        return outFile.getAbsolutePath();
    }

    private class FilterThread extends Thread {

        private final ArrayBlockingQueue<List<String>> in;
        private final ArrayBlockingQueue<List<String>> out;
        private final Hasher hasher;
        private final String prefix;

        public FilterThread(String prefix, String hashType, ArrayBlockingQueue<List<String>> in, ArrayBlockingQueue<List<String>> out) throws NoSuchAlgorithmException {
            this.in = in;
            this.out = out;
            this.hasher = new Hasher(hashType);
            this.prefix = prefix;
            
            this.setName("FilterThread " + this.getName());
        }

        @Override
        public void run() {
            try {

                while (true) {
                    List<String> pwds = in.take();

                    if (pwds.isEmpty()) //end condition
                        return;

                    List<String> filteredPwds = new ArrayList<String>(100);
                    for (String pwd : pwds) {

                        if (hasher.hash(pwd).startsWith(prefix)) {
                            filteredPwds.add(pwd);
                        }

                    }

                    if (!filteredPwds.isEmpty()) {
                        out.offer(filteredPwds);
                    }
                }

            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

        }
    }

    private class OutputThread extends Thread {

        private final ArrayBlockingQueue<List<String>> pwdQueue;
        private final File outFile;

        public OutputThread(ArrayBlockingQueue<List<String>> pwdQueue, File outFile) {
            this.pwdQueue = pwdQueue;
            this.outFile = outFile;
            
            this.setName("OutputThread");
        }

        @Override
        public void run() {
            
            PrintWriter writer = null;
            try  {

                writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream(outFile)));
                
                while (true) {
                    List<String> pwds = pwdQueue.take();

                    if (pwds.isEmpty()) //end condition
                        return;

                    for (String pwd : pwds) {
                        writer.println(pwd);
                    }
                }

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                if (writer != null)
                    writer.close();
            }
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