package timingattack.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import timingattack.Statistics;

/**
 *
 * @author aj
 */
public class PlainTextTestController extends AttackController {
    
    private final String knownPassword;
    
    public PlainTextTestController(String knownPassword, List<String> pwdList, HashMap<String, List<Long>> measurements, AtomicInteger requestCount, double confidenceRequirements) {
        super(pwdList, measurements, requestCount, confidenceRequirements);
        this.knownPassword = knownPassword; //can be null
    }

    @Override
    public void roundComplete() throws Exception {
        super.roundComplete();
        
        if (roundCount > 500000) {
            System.out.format("After %d requests no clear winner was found.\n", requestCount.get());
            System.out.println("This server is probably NOT vulnerable.");
            
            isDone = true;
        }
    }
    
    

    
    @Override
    public void handleWinner(Statistics winnerStats) throws Exception {
        
        if (knownPassword != null && !winnerStats.getPwd().startsWith(knownPassword.substring(0, knownPassword.length()-1))) {
            System.out.println("WARNING: Known password was not the slowest, this is an unexpected result. Are you sure your known password is correct?");
            System.out.println("WARNING: Slowest password was: "+ winnerStats.getPwd());
            System.out.println("This server is probably NOT vulnerable.");
        } else {
            System.out.format("\n%s is significantly slower than others after %d requests.\n", winnerStats.getPwd(), requestCount.get());
            System.out.println("This server is probably VULNERABLE.");
        }

        
        isDone = true;
        
    }

    @Override
    public void printStats(List<Statistics> stats) {
        
        System.out.println(Statistics.getCsvHeader(false) + " isTarget");
        for (Statistics s : stats) {
            String csvLine = s.toCsvString();
            if (s.getPwd().startsWith(knownPassword.substring(0, knownPassword.length()-1))) 
                csvLine += " <--";
            
            System.out.println(csvLine);
        }
        
    }

    @Override
    protected Statistics getStats(String pwd, List<Long> times) {
        return new Statistics(pwd, times);
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