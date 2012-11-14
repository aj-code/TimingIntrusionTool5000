package timingattack;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 *
 * @author aj
 */
public class Hasher {

    private static final int EXPECTED_ENTRY_COUNT = 16; //hex encoding
    private final MessageDigest md;
    private final String hashType;

    public Hasher(String hashType) throws NoSuchAlgorithmException {
        this.hashType = hashType;
        this.md = MessageDigest.getInstance(hashType);
    }

    public String getHashType() {
        return hashType;
    }

    
    public List<String> getCharsCollisionsWithKnown(String knownPwd, int prefixLength) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        List<String> pwds = getCharsCollisions();
        
        String knownHashPrefix = hash(knownPwd).substring(0,prefixLength);
        
        //remove hash with first char match
        Iterator<String> iter = pwds.iterator();
        while (iter.hasNext()) {
            String pwd = iter.next();
            if (hash(pwd).startsWith(knownHashPrefix.charAt(0)+""))
                iter.remove();
        }
        
        //get known hash prefix collision
        Random rand = new Random(0);
        int nextPwd = 0;
        String collidingPwd = null;
        while (true) {
            
            collidingPwd = String.format("%08d", nextPwd);
             
            String hexHash = hash(collidingPwd);
            if (hexHash.startsWith(knownHashPrefix)) 
                 break;   
            
            nextPwd = rand.nextInt(99999999);
            
        }
        
        //pair list down to 7 entries
        while (pwds.size() > 7)
            pwds.remove(0);
        
        pwds.add(collidingPwd);
        
        return pwds;
    }
    
    public List<String> getCharsCollisions() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return getCharsCollisions("");
    }

    public List<String> getCharsCollisions(String knownPrefix) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        
        HashMap<String, String> hashes = new HashMap<String, String>();

        Random rand = new Random(0);
        int nextPwd = 0;
        while (hashes.size() < EXPECTED_ENTRY_COUNT) {

            String password = String.format("%08d", nextPwd);
            String hexHash = hash(password);
            
            if (hexHash.startsWith(knownPrefix)) {
                String newHashPrefix = hexHash.substring(0, knownPrefix.length()+1);
                hashes.put(newHashPrefix, password);
            }

            nextPwd = rand.nextInt(99999999);
        }
        
        return new ArrayList<String>(hashes.values());

    }

    public String hash(String pwd) {

        try {

            md.reset();
            byte[] digest = md.digest(pwd.getBytes("UTF-8"));

            return toHex(digest);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private String toHex(byte[] bytes) { 
        //cheers maybewecouldstealavan (http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java)
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
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