package timingattack.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import timingattack.Statistics;

/**
 *
 * @author aj
 */
public class PlainTextLengthController extends PlainTextTestController {

    public PlainTextLengthController(String knownPassword, List<String> pwdList, HashMap<String, List<Long>> measurements, AtomicInteger requestCount, double confidenceRequirements) {
        super(knownPassword, pwdList, measurements, requestCount, confidenceRequirements);
    }


    @Override
    public void handleWinner(Statistics winnerStats) throws Exception {
        
        System.out.format("\n%s is significantly slower than others after %d requests.\n", winnerStats.getPwd(), requestCount.get());
        System.out.format("The password length is probably %d chars.\n", winnerStats.getPwd().length());

        isDone = true;
        
    }
    
    @Override
    public void printStats(List<Statistics> stats) {
        
        System.out.println(Statistics.getCsvHeader(false));
        for (Statistics s : stats)       
            System.out.println(s.toCsvString());
        
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