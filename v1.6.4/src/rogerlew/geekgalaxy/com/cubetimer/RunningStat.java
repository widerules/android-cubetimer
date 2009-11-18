/*
 * RunningStats.java
 * 
 *     This class implements a recursive filter for accurately
 *     and efficiently computing running mean, standard deviation,
 *     minimum, maximum, and count.
 *
 *     Ported and Adapted from:
 *         http://www.johndcook.com/standard_deviation.html
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

public class RunningStat {
    public int m_n;
    public double m_oldM, m_newM, m_oldS, m_newS, m_min, m_max;
    
    public RunningStat() {
        clear();
    }
    
    public RunningStat(int m_n_, double m_oldM_, double m_newM_, 
                                 double m_oldS_, double m_newS_, 
                                 double m_min_,  double m_max_) {
        m_n    = m_n_;
        m_oldM = m_oldM_;
        m_newM = m_newM_;
        m_oldS = m_oldS_;
        m_newS = m_newS_;
        m_min  = m_min_;
        m_max  = m_max_;
    }
    
    /*
     * Get Methods for Saving RunningStat
     * 
     */
    /*
    public int getM_n()       { return m_n;    }
    public double getM_oldM() { return m_oldM; }
    public double getM_newM() { return m_newM; }
    public double getM_oldS() { return m_oldS; }
    public double getM_newS() { return m_newS; }
    public double getM_min()  { return m_min;  }
    public double getM_max()  { return m_max;  }
    */
    public void clear() {
        m_n = 0;
        m_oldM = -1;
        m_newM = -1;
        m_oldS = -1;
        m_newS = -1;
        m_min = Double.MAX_VALUE;
        m_max = Double.MIN_VALUE;
    }

    public void push(double x) {
        m_n++;

        if (x < m_min) m_min = x;
        if (x > m_max) m_max = x;
        
        // See Knuth TAOCP vol 2, 3rd edition, page 232
        if (m_n == 1) {
            m_oldM = m_newM = x;
            m_oldS = 0.0;
        } else {
            m_newM = m_oldM + (x - m_oldM)/m_n;
            m_newS = m_oldS + (x - m_oldM)*(x - m_newM);

            // set up for next iteration
            m_oldM = m_newM; 
            m_oldS = m_newS;
        }
    }

    public int getCount()
        { return m_n; }
    
    public void setCount(int count) 
        { m_n=count; }
    
    public double mean()
        { return (m_n > 0) ? m_newM : 0.0; }

    public double variance()
        { return (m_n > 1) ? m_newS/(m_n - 1) : 0.0 ; }

    public double stdDeviation()
        { return Math.sqrt(variance()); }
    
    public double minimum()
        { return m_min; }
    
    public double maximum()
        { return m_max; }
}
