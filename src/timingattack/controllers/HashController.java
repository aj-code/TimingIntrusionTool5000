package timingattack.controllers;


import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import timingattack.Hasher;
import timingattack.Statistics;
import timingattack.WordlistFilter;

/**
 *
 * @author aj
 */
public class HashController extends AttackController {

    private final int charsToGuess;
    private final Hasher hasher;
    private final WordlistFilter wordlistFilter;
    private String hashPrefix = "";

    public HashController(int charsToGuess, String hashType, WordlistFilter wordlistFilter, List<String> pwdList, HashMap<String, List<Long>> measurements, AtomicInteger requestCount, double confidenceRequirements) throws NoSuchAlgorithmException {
        super(pwdList, measurements, requestCount, confidenceRequirements);
        
        this.charsToGuess = charsToGuess;
        this.hasher = new Hasher(hashType);
        this.wordlistFilter = wordlistFilter;
        
    }
    
    @Override
    public void handleWinner(Statistics winnerStats) throws Exception {


        if (hashPrefix.length() == charsToGuess - 1) { //-1 cause we haven't worked out the new prefix yet

            hashPrefix = winnerStats.getPwdHash().substring(0, charsToGuess);

            System.out.println("Timing attack done. The guessed hash prefix is: " + hashPrefix);
            
            if (wordlistFilter != null) {
                System.out.println("Creating filtered wordlist...");
                wordlistFilter.start(hashPrefix);
                System.out.println("Done! Wordlist at: " + wordlistFilter.getOutputPath());
            }
            
            isDone = true;

        } else {

            hashPrefix += winnerStats.getPwdHash().substring(hashPrefix.length(), hashPrefix.length()+1);

            waitForRequestsToFinish();

            synchronized (measurements) {
                measurements.clear();
            }
            
            pwdList.clear();
            roundCount = 1;

            System.out.format("hash prefix guessed so far is %s\n", hashPrefix);
            System.out.println("Calulating hash collisions for next character.");
            pwdList.addAll(hasher.getCharsCollisions(hashPrefix));
            System.out.println("Done. Starting timing attack on next character.");
            
            Thread.sleep(5000); //pause to allow user to read output

        }
    }


    @Override
    public void printStats(List<Statistics> stats) {
        
        int hashPrefixLength = hashPrefix.length() < 2 ? 2 : hashPrefix.length();

        System.out.println(Statistics.getCsvHeader(true));
        for (Statistics s : stats) 
            System.out.println(s.toCsvString(hashPrefixLength));
    }

    @Override
    protected Statistics getStats(String pwd, List<Long> times) {
        return new Statistics(pwd, hasher.hash(pwd), times);
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