/*
 * CubeTimer.java
 * 
 *     This class implements an Android Activity which
 *     generates random scrambles for a 3x3x3 Rubik's 
 *     Cube and provides an inspection countdown and 
 *     stopwatch. App will list and save best and 
 *     average solve times.
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class CubeTimer extends Activity {
    // Mode Constants
    private static final int STOPPED = 0; 
    private static final int RUNNING = 1; 
    
    // Timer Constant
    private static final long POLL_TIME = 34L;
    
    // Android Objects
    private View view;
    private TextView scrambleTextView;
    private TextView timeTextView;
    private TextView inspectionTextView;
    private TextView notesTextView;
    private TextView bestTextView;
    private TextView aveTextView;
    private TextView sdTextView;
    private SharedPreferences settings;
    private AlertDialog insDialog;
    private AlertDialog saveDialog;
    private MediaPlayer mp;

    // Control Objects
    private Timer timer;
    private Scrambler scrambler;
    private RunningStat rs;
    
    // State and time variables
    private int mode=STOPPED;
    private long inspection_time;
    private long t0; 
    private long elapsed;   
        
    /*
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Initialize TextViews
        timeTextView       = (TextView)findViewById(R.id.time);
        inspectionTextView = (TextView)findViewById(R.id.ins_time);
        bestTextView       = (TextView)findViewById(R.id.best);
        aveTextView        = (TextView)findViewById(R.id.average);
        sdTextView         = (TextView)findViewById(R.id.sd);
        notesTextView      = (TextView)findViewById(R.id.notes);

        // Keep screen on
        view = (View)findViewById(R.id.tblview);
        view.setKeepScreenOn(true);
        
        // Display instructions for small screens
        if (getWindowManager().getDefaultDisplay().getHeight() < 400){
            notesTextView.setText("");
            Toast.makeText(getApplicationContext(),
                     R.string.instructions,
                     Toast.LENGTH_LONG).show();         
        } 
        
        // Initialize media player
        mp = MediaPlayer.create(getBaseContext(), R.raw.longbeep);
        mp.start(); // start
        mp.pause(); // and immediately pause
        
        // Bind ringer volume controls to app
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        // Initialized stored data
        settings = getPreferences(MODE_PRIVATE);
        loadRecords(); setRecords(); saveRecords();
        getInspectionIndex();

        // Initialize inspection dialog (bound to menu key)
        AlertDialog.Builder insBuilder = new AlertDialog.Builder(this);
        insBuilder.setTitle("Inspection Time");
        insBuilder.setSingleChoiceItems(R.array.ins_times, 
                                        getInspectionIndex(), 
                                        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                setInspectionIndex(item);
                insDialog.hide();
            }
        });
        
        insDialog = insBuilder.create();
        
        // Initialize save dialog
        AlertDialog.Builder saveBuilder = new AlertDialog.Builder(this);
        saveBuilder.setMessage("Would you like to save this time?");
        saveBuilder.setCancelable(false);
        saveBuilder.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                rs.push((double)(elapsed-inspection_time)/1000);
                setRecords(); saveRecords();
            }
        });
        
        saveBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        
        saveDialog = saveBuilder.create();

        
        // Initialize Scrambler
        scrambler = new Scrambler();
        
        // Initialize scramble text
        scrambleTextView = (TextView)findViewById(R.id.scramble);
        scrambleTextView.setText((CharSequence)scrambler.getScramble());
            
        // Initialize screen tap listener
        view.setOnClickListener(new View.OnClickListener() {  
            public void onClick(View view) {
                 switch (mode) {
                 case STOPPED: // Currently stopped, start timer                   
                     timeTextView.setText(long2str(inspection_time/1000));
                     
                     mode=RUNNING; // toggle mode
                     t0=SystemClock.uptimeMillis();
                     break;
                        
                 case RUNNING: // Currently running, stop timer
                     scrambleTextView.setText(scrambler.getScramble());
                     
                     // Record time only if time if past inspection epoch
                     double solvetime=(elapsed-inspection_time)/1000;
                     if (solvetime > 0) {
                         if (solvetime < 8.) {
                             saveDialog.show();
                         }
                         else if ((rs.getCount()==0) || (rs.stdDeviation()==0)) {
                             rs.push((double)(elapsed-inspection_time)/1000);
                             setRecords(); saveRecords();
                         }
                         else if ((solvetime < rs.mean()-2*rs.stdDeviation()) ||
                                  (solvetime > rs.mean()+2*rs.stdDeviation())) {
                             saveDialog.show();
                         } else {
                             rs.push((double)(elapsed-inspection_time)/1000);
                             setRecords(); saveRecords();
                         }
                         
                     }
                     
                     mode=STOPPED; // toggle mode
                     break;
                 } 
            } 
        }); 
        
        // Initialize long pause on screen listener
        view.setOnLongClickListener(
            new View.OnLongClickListener() {
                public boolean onLongClick(View view) {
                    rs.clear(); elapsed=0;
                    saveRecords(); setRecords();
                    Toast.makeText(getApplicationContext(),
                                   "All Records have been cleared",
                                   Toast.LENGTH_SHORT).show();
                    return true;
              }
          });
        
        // Initialize timer object
        // Timer calls mHandler every POLL_TIME
        timer = new Timer();
        timer.scheduleAtFixedRate(
            new TimerTask() {
                public void run() {
                    mHandler.sendMessage(
                        Message.obtain(mHandler, mode, 
                                       SystemClock.uptimeMillis()));
                } 
            }, 0L, POLL_TIME);
        
    } // this ends OnCreate()

    /*
     * Message handler for updating time display
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what==RUNNING) {
                elapsed = SystemClock.uptimeMillis() - t0;
                if (elapsed<inspection_time) {
                    long rem = (inspection_time-elapsed+1000)/1000;
                    timeTextView.setText(long2str(rem));
            
                    if (elapsed-inspection_time>=-500L && !mp.isPlaying())
                        { mp.seekTo(0); mp.start(); } 
                } else {
                    timeTextView.setText(formatClock(elapsed-inspection_time));
                    if (mp.isPlaying()) { mp.pause(); }
                }
            }
        }
    };

    /*
     * Methods to override keyboard open
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    /*
     *  Methods to override options menu 
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        insDialog.show();
        return super.onPrepareOptionsMenu(menu);
    }

    /*
     * Function to convert long to strings
     */
    private String long2str(long x){
        return new Long(x).toString();
    }
    
    /*
     * Method to read inspection time from stored data
     */
    private int getInspectionIndex() {
        int fn_tmp = settings.getInt("inspection_index", -1);
        
        if (fn_tmp < 0) 
            { setInspectionIndex(1); return 0; }
        else 
            { setInspectionIndex(fn_tmp); }
        
        return fn_tmp;
    }
    
    /*
     * Method to write inspection time to stored data
     */
    private void setInspectionIndex(int x) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("inspection_index", x);
        editor.commit();
        
        switch(x) {
        case 0: inspection_time=1000L;  break;
        case 1: inspection_time=3000L;  break;
        case 2: inspection_time=5000L;  break;
        case 3: inspection_time=10000L; break;
        case 4: inspection_time=15000L; break;
        }
            
        inspectionTextView.setText("Inspection Time: " + 
                         long2str(inspection_time/1000L) + "s"); 
    }

    /*
     * Method to write RunningStats instances to stored data
     */
    private void saveRecords() {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(  "m_n",           rs.m_n);
        editor.putFloat("m_oldM", (float)rs.m_oldM);
        editor.putFloat("m_newM", (float)rs.m_newM);
        editor.putFloat("m_oldS", (float)rs.m_oldS);
        editor.putFloat("m_newS", (float)rs.m_newS);
        editor.putFloat("m_min",  (float)rs.m_min);
        editor.putFloat("m_max",  (float)rs.m_max);
        editor.commit();
    }
    
    /*
     * Method to initialize RunningStats from stored data
     */    
    private void loadRecords() {
        int m_n = settings.getInt("m_n", -1);
        double m_oldM = (double)settings.getFloat("m_oldM", (float)-1);
        double m_newM = (double)settings.getFloat("m_newM", (float)-1);
        double m_oldS = (double)settings.getFloat("m_oldS", (float)-1);
        double m_newS = (double)settings.getFloat("m_newS", (float)-1);
        double m_min  = (double)settings.getFloat("m_min",  (float)-1);
        double m_max  = (double)settings.getFloat("m_max",  (float)-1);
        
        if ((m_n < 0) || (m_oldM < 0) || (m_newM < 0) || 
                         (m_oldS < 0) || (m_newS < 0) ||
                         (m_min  < 0) || (m_max  < 0) ) 
            rs = new RunningStat(); 
        else 
            rs = new RunningStat(m_n, m_oldM, m_newM, 
                                      m_oldS, m_newS, 
                                      m_min,  m_max);
    }
    
    /*
     * Formats elapsed time in the "MM:SS.HH" format
     * (HH is hundreths)
     */
    private String formatClock(long elapsed) {
        if (elapsed<0)
            return "00:00.00";
        
        elapsed/=10; // convert from milliseconds to centiseconds
        
        String hundreths = Integer.toString((int)(elapsed%100));
        String seconds   = Integer.toString((int)(elapsed%6000)/100);
        String minutes   = Integer.toString((int)(elapsed/6000));
        
        if (hundreths.length() < 2)  hundreths = "0"+hundreths; 
        if (seconds.length()   < 2)  seconds   = "0"+seconds;   
        if (minutes.length()   < 2)  minutes   = "0"+minutes;   
        
        return minutes+":"+seconds+"."+hundreths;
    }
    
    /*
     * Method to set records fields
     */
    private void setRecords() {
        if (rs.getCount()==0) {
            bestTextView.setText(   "best : --:--.--");
            aveTextView.setText("ave of 0 : --:--.--");
            sdTextView.setText(    "sddev : --:--.--");    
            
        } else {
            long best_aslong = Math.round( rs.minimum()*1000);
            long mean_aslong = Math.round( rs.mean()*1000);
            long sd_aslong   = Math.round( rs.stdDeviation()*1000);
            String cnt_asstr = new Integer(rs.getCount()).toString();
            
            bestTextView.setText("best : " + formatClock(best_aslong));
            aveTextView.setText("ave of " + cnt_asstr + " : " +
                                formatClock(mean_aslong));
            sdTextView.setText("sd dev : " + formatClock(sd_aslong));
        }
    }
}
