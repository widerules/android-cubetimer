/*
 * Scrambler.java
 * 
 *     This class provides a 3 x 3 Rubik's cube scrambler.
 * 
 *     Example usage:
 *         Scrambler new_scramble = new Scrambler(25);
 *         System.out.println(new_scramble.genScramble());
 * 
 * 
 * 
 * Copyright (C) 2009 Roger Lew.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package rogerlew.geekgalaxy.com.cubetimer;
import java.util.*;

public class Scrambler {
    private int length;
    private Random dice = new Random();
    
    public Scrambler()        { this(25);   }
    public Scrambler(int len) { length=len; }
    
    public String getScramble() {
        Stack<Integer> stack = new Stack<Integer>();
        return buildScramble(length, stack);
    }
    
    private String buildScramble(int len, Stack<Integer> stack) {
        if (len==0) 
            return formatStack(stack);
        
        stack.push(randturn());
        
        if (stack.size()>1) {
            int last1=(Integer)stack.pop();
            int last2=(Integer)stack.pop();
            if (last1+last2==0) { 
                // turn undoes previous turn
                return buildScramble(len+1, stack);
            }
            else if (last1==last2) {
                // makes turn a 180 rotation
                stack.push(Math.abs(last2)*10);
                return buildScramble(len, stack);
            }           
            else if (last2>9) { 
                if (last2/10 == last1) {
                    // turn partially undoes 180
                    stack.push(Math.abs(last2)/-10);
                    return buildScramble(len, stack);
                }
                else if (last2/-10 == last1) {
                    // turn partially undoes 180
                    stack.push(Math.abs(last2)/10);
                    return buildScramble(len, stack);
                }       
            }
            stack.push(last2);
            stack.push(last1);
        }
        return buildScramble(len-1, stack); 
    }
    
    private int randturn() {
        return (dice.nextDouble()<.5) ? dice.nextInt(6)+1 : -1*(dice.nextInt(6)+1);
    }
    
    private String formatStack(Stack<Integer> stack) {
        String str="";
        
        int i=0;
        while (stack.size()!=0) {
            int turn=(Integer)stack.pop();
            
            if      (turn == -6) str+="R";
            else if (turn == -5) str+="L";
            else if (turn == -4) str+="F";
            else if (turn == -3) str+="B";
            else if (turn == -2) str+="D";
            else if (turn == -1) str+="U";
            else if (turn ==  6) str+="Ri";
            else if (turn ==  5) str+="Li";
            else if (turn ==  4) str+="Fi";
            else if (turn ==  3) str+="Bi";
            else if (turn ==  2) str+="Di";
            else if (turn ==  1) str+="Ui";
            else if (turn == 60) str+="R2";
            else if (turn == 50) str+="L2";
            else if (turn == 40) str+="F2";
            else if (turn == 30) str+="B2";
            else if (turn == 20) str+="D2";
            else if (turn == 10) str+="U2";
            
            if ((i==7) || (i==16) ) str+="\n";
            else if (i!=25)         str+=" ";
            
            i++;
        }
        return str;
    }
}
