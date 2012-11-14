package timingattack;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author aj
 */
public class Statistics implements Comparable<Statistics> {

    private String pwd, pwdHash;
    private int numMeasurements;
    private long tenthCentile;
    
    public Statistics(String pwd, List<Long> times) {
        this(pwd, null, times);
    }
    
    public Statistics(String pwd, String pwdHash, List<Long> times) {

        this.pwd = pwd;
        this.pwdHash = pwdHash;
        this.numMeasurements = times.size();
          
        Collections.sort(times);
                
        tenthCentile = times.get((int) Math.round(times.size() * 0.10));

    }
    
    @Override
    public int compareTo(Statistics o) {
        return Long.valueOf(tenthCentile).compareTo(o.getTenthCentile());
    }


    public String toCsvString() {
        return toCsvString(2);
    }
    
    public static String getCsvHeader(boolean includePwdHash) {
        
        if (includePwdHash) 
            return String.format("%13s, %11s, %8s, %11s,", "Password", "Hash Prefix", "Count", "10thCentile");
        else
            return String.format("%13s, %7s, %11s,", "Password", "Count", "10thCentile");
    }
    
    public String toCsvString(int hashPrefixLength) {
        
        if (pwdHash != null) {
            return String.format("%13s, %11s, %8d, %11d,", 
                                pwd, pwdHash.substring(0, hashPrefixLength+1), numMeasurements, tenthCentile);
        } else {
            return String.format("%13s, %7d, %11d,", 
                                pwd, numMeasurements, tenthCentile);            
        }
        
    }

    /*-----------GETTERS-----------*/
    public String getPwd() {
        return pwd;
    }

    public String getPwdHash() {
        return pwdHash;
    }

    public int getNumMeasurements() {
        return numMeasurements;
    }

    public long getTenthCentile() {
        return tenthCentile;
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