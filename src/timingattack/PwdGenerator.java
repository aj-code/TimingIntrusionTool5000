package timingattack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author adrian
 */
public class PwdGenerator {

    private static final String DEFAULT_CHARSET = "qwertyuiopasdfghjklzxcvbnm0123456789";

    public static List<String> getPlainText(int length) {
        return getPlainText(length, "");
    }
    
    public static List<String> getPlainText(int length, String prefix) {
        return getPlainText(DEFAULT_CHARSET, length, prefix);
    }

    public static List<String> getPlainText(String charset, int length, String prefix) {

        List<String> pwds = new ArrayList<String>();

        String padding = getPadding(length - prefix.length());

        for (char c : charset.toCharArray()) 
            pwds.add(prefix + c + padding);
        
        return pwds;
    }
    
    
    public static List<String> getPlainTextWithKnown(int pwdLength, String knownPassword) {
        return getPlainTextWithKnown(DEFAULT_CHARSET, pwdLength, knownPassword);
    }

    public static List<String> getPlainTextWithKnown(String charset, int pwdLength, String knownPassword) {
        
        
        //make last char wrong so we always take the same code path
        char lastChar = knownPassword.charAt(knownPassword.length()-1);
        char newLastChar = lastChar != 'a' ? 'a' : 'b';
        knownPassword = knownPassword.substring(0, knownPassword.length()-1) + newLastChar;
        System.out.println("Note: Last char of known password changed so we always follow fail login code path.");
        
        
        List<String> pwds = getPlainText(charset, pwdLength, "");

        //ditch generated pwd starting with same char
        Iterator<String> iter = pwds.iterator();
        while (iter.hasNext()) {
            if (iter.next().startsWith("" + knownPassword.charAt(0)))
                iter.remove();
        }

        pwds.add(knownPassword);

        return pwds;
    }

    public static List<String> getForLength(int minLength, int maxLength) {
        
        List<String> pwds = new ArrayList<String>();
        for (int i = minLength; i < maxLength; i++) {
            
            StringBuilder b = new StringBuilder();
            for (int k = 0; k < i; k++)
                b.append('m');
            
            pwds.add(b.toString());
        }
        
        return pwds;
        
    }
    
    
    private static String getPadding(int len) {

        StringBuilder p = new StringBuilder();
        while (p.length() < len - 1)
            p.append('a');

        return p.toString();
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