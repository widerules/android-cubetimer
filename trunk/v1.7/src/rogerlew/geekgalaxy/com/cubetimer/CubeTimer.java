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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class CubeTimer extends Activity {
    // Mode Constants
    private static final int STOPPED = 0; 
    private static final int RUNNING = 1; 
    
    // Menu Ids
    private static final int PUZZLE_ID = Menu.FIRST+1;
    private static final int INSPECTION_ID = Menu.FIRST+2;
    private static final int HELP_ID = Menu.FIRST+3;
    private static final int CLEAR_ALL_ID = Menu.FIRST+4;
    private static final int BEEP_ID = Menu.FIRST+5;
    private static final int HELP_ACTIVTY_REQUEST_CODE=1337;
    
    // Timer Constant
    private static final long POLL_TIME = 34L;
    
    // Android Objects
    private View view;
    private TextView beepTextView;
    private TextView puzzleTextView;
    private TextView scrambleTextView;
    private TextView timeTextView;
    private TextView inspectionTextView;
    private TextView notesTextView;
    private TextView bestTextView;
    private TextView aveTextView;
    private TextView sdTextView;
    private WebView  mWebView;
    private SharedPreferences settings;
    private AlertDialog insDialog;
    private AlertDialog puzzleDialog;
    private AlertDialog saveDialog;
    private AlertDialog clrallDialog;
    private MediaPlayer mp;

    
    // State and time variables
    private int puzzle;
    private String[] puzzleStrings ={"2x2x2",
                                     "3x3x3",
                                     "4x4x4",
                                     "5x5x5",
                                     "6x6x6",
                                     "7x7x7",
                                     "Megaminx",
                                     "Pyraminx",
                                     "Square One",
                                     "UFO"};
    
    private static final int NUM_PUZZLES=10;
    private int mode=STOPPED;
    private long inspection_time;
    private long t0; 
    private long elapsed;   
    private int beep_mode=1;
    
 // Control Objects
    private Timer timer;
    private RunningStat[] rs = new RunningStat[NUM_PUZZLES];
    
    
    /*
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Initialize TextViews
        beepTextView     =   (TextView)findViewById(R.id.beep);
        puzzleTextView     = (TextView)findViewById(R.id.puzzle);
        scrambleTextView   = (TextView)findViewById(R.id.scramble);
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
     
        setBeepMode(getBeepMode());
        
        getPuzzleIndex();
        for (int pid=0; pid<NUM_PUZZLES; pid++) {
            loadRecords(pid);
            saveRecords(pid);
        }
        setRecords();
        setInspectionIndex(getInspectionIndex());

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
        
        // Initialize puzzle dialog (bound to menu key)
        AlertDialog.Builder puzzleBuilder = new AlertDialog.Builder(this);
        puzzleBuilder.setTitle("Select Puzzle");
        puzzleBuilder.setSingleChoiceItems(R.array.puzzles, 
                                           getPuzzleIndex(),
                                           new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                setPuzzleIndex(item);
                setScrambleTextView();
                loadRecords(puzzle);
                setRecords();
                saveRecords(puzzle);
                puzzleDialog.hide();
            }
        });
        
        puzzleDialog = puzzleBuilder.create();
        
        // Initialize save dialog
        AlertDialog.Builder saveBuilder = new AlertDialog.Builder(this);
        saveBuilder.setMessage("Would you like to save this time?");
        saveBuilder.setCancelable(false);
        saveBuilder.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                rs[puzzle].push((double)(elapsed-inspection_time)/1000);
                setRecords(); saveRecords(puzzle);
            }
        });
        
        saveBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        
        saveDialog = saveBuilder.create();

        // Initialize clear all dialog
        AlertDialog.Builder clrallBuilder = new AlertDialog.Builder(this);
        clrallBuilder.setMessage("Do you really want to erase ALL records?");
        clrallBuilder.setCancelable(false);
        clrallBuilder.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                for (int pid=0; pid<NUM_PUZZLES; pid++) {
                    rs[pid].clear();
                    saveRecords(pid);
                }              
                setRecords();
                Toast.makeText(getApplicationContext(),
                               "All records have been cleared",
                               Toast.LENGTH_SHORT).show();
            }
        });
        
        clrallBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        
        clrallDialog = clrallBuilder.create();
        
        // Initialize puzzle text
        puzzleTextView.setText(puzzleStrings[puzzle]);
        
        // Initialize WebView for Scrambler
        mWebView = (WebView) findViewById(R.id.webview);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);

        // Register a new JavaScript interface called HTMLOUT  
        mWebView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");  
        
        // Set scramble text
        setScrambleTextView();
        
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
                     setScrambleTextView();

                     // Record time only if time if past inspection epoch
                     double solvetime=(elapsed-inspection_time)/1000;
                     if (solvetime > 0) {
                         if (solvetime < 8.) {
                             saveDialog.show();
                         }
                         else if ((rs[puzzle].getCount()==0) || (rs[puzzle].stdDeviation()==0)) {
                             rs[puzzle].push((double)(elapsed-inspection_time)/1000);
                             setRecords(); saveRecords(puzzle);
                         }
                         else if ((solvetime < rs[puzzle].mean()-2*rs[puzzle].stdDeviation()) ||
                                  (solvetime > rs[puzzle].mean()+2*rs[puzzle].stdDeviation())) {
                             saveDialog.show();
                         } else {
                             rs[puzzle].push((double)(elapsed-inspection_time)/1000);
                             setRecords(); saveRecords(puzzle);
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
                    rs[puzzle].clear(); elapsed=0;
                    setRecords(); saveRecords(puzzle); 
                    Toast.makeText(getApplicationContext(),
                                   puzzleStrings[puzzle]+" records have been cleared",
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
            
                    if (elapsed-inspection_time>=-500L && !mp.isPlaying() && beep_mode==1)
                        { mp.seekTo(0); mp.start(); } 
                } else {
                    timeTextView.setText(formatClock(elapsed-inspection_time));
                    if (mp.isPlaying()) { mp.pause(); }
                }
            }
        }
    };

    /*
     * Message handler for updating scramble display
     */
    private Handler scrambleHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String s;
            if (msg.what==1) { // 3x3x3, 25 moves
                int [] breaks = {7,16};
                s = formatScramble(msg.obj.toString(), breaks);
            }
            else if (msg.what==2) { // 4x4x4, 40 moves
                int [] breaks = {9,19,29};
                s = formatScramble(msg.obj.toString(), breaks);
            }
            else if (msg.what==3) { // 5x5x5, 60 moves
                int [] breaks = {11,23,35,47};
                s = formatScramble(msg.obj.toString(), breaks);
            }
            else if (msg.what==4) { // 6x6x6, 80 moves  11 12 11 12 11 12 11
                int [] breaks = {10,22,33,45,56,68};
                s = formatScramble(msg.obj.toString(), breaks);
            }
            else if (msg.what==5) { // 7x7x7, 100 moves 
                int [] breaks = {12,24,37,49,62,74,87};
                s = formatScramble(msg.obj.toString(), breaks);                
            }
            else if (msg.what==7 || msg.what==9) { // Pyraminx or UFO
                int [] breaks = {msg.obj.toString().split(" ").length/2-1};
                s = formatScramble(msg.obj.toString(), breaks);   
            }
//            else if (msg.what==8) { // Square One
//                int step = msg.obj.toString().split(" ").length/4;
//                int [] breaks = {step+1, step+step*2+1, step+step*3+1};
//                s = formatScramble(msg.obj.toString(), breaks);   
//            } 
            else // 2x2x2 and Megaminx
                s = msg.obj.toString();
            
            scrambleTextView.setText(s);
        }
    };

    /*
     * Javascript interface for updating scramble display
     */
    class MyJavaScriptInterface {  
        MyJavaScriptInterface() {}

        /**
         * This is not called on the UI thread
         * 
         * Post a runnable to change scrambleTextView
         * in the UI thread
         */
        public void setScramble(final String s) {
            scrambleHandler.post(new Runnable() {
                public void run() {
                    scrambleHandler.sendMessage(
                        Message.obtain(scrambleHandler, puzzle, s));
                } 
            });
        }
    }
    
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
        menu.add(0, PUZZLE_ID,     0, "Set Puzzle");
        menu.add(0, INSPECTION_ID, 1, "Set Inspection Time");
        menu.add(0, CLEAR_ALL_ID,  2, "Clear All Records");
        menu.add(0, HELP_ID,       3, "Help");
        menu.add(0, BEEP_ID,       4, "Toggle Beep On/Off");

        return super.onCreateOptionsMenu(menu);
    }
    
    /*
     * when menu button option selected 
     */
    @Override 
    public boolean onOptionsItemSelected(MenuItem item) {
      return applyMenuChoice(item) || super.onOptionsItemSelected(item);
    }
    
    public boolean applyMenuChoice(MenuItem item) {
        switch (item.getItemId()) {
          case PUZZLE_ID:
              puzzleDialog.show();
              return true;
          case INSPECTION_ID:
              insDialog.show();
              return true;
          case CLEAR_ALL_ID:
              clrallDialog.show();
              return true;
          case BEEP_ID:
              setBeepMode((beep_mode+1)%2);
              if (beep_mode==1)
                  Toast.makeText(getApplicationContext(),
                                 "Beep has been turned ON",
                                 Toast.LENGTH_LONG).show();  
              else
                  Toast.makeText(getApplicationContext(),
                                 "Beep has been turned OFF",
                                 Toast.LENGTH_LONG).show();  
              return true;
          case HELP_ID:
              Intent intent = new Intent(this, Help.class);
              //start new activity
              super.startActivityForResult(intent, HELP_ACTIVTY_REQUEST_CODE); 
              return true;
          }

          return false;
      }
    
    /*
     * Function to convert long to strings
     */
    private String long2str(long x){
        return new Long(x).toString();
    }
    
    private void setScrambleTextView() {
        scrambleTextView.setText("loading...");
        
        if (puzzle==6) {
            scrambleTextView.setGravity(Gravity.LEFT);
            scrambleTextView.setTypeface(Typeface.MONOSPACE);
        } else {
            scrambleTextView.setGravity(Gravity.CENTER);
            scrambleTextView.setTypeface(Typeface.SERIF);
        }
        
        if (puzzle==0) { // 2x2x2
            scrambleTextView.setTextSize(24);
            mWebView.loadUrl("file:///android_asset/scramble_cube_222.htm?num=1");
        }
        else if (puzzle==1) { // 3x3x3
            scrambleTextView.setTextSize(24);
            mWebView.loadUrl("file:///android_asset/scramble_cube.htm?size=3&num=1&len=25");
        }
        else if (puzzle==2) { // 4x4x4
            scrambleTextView.setTextSize(24);
            mWebView.loadUrl("file:///android_asset/scramble_cube.htm?size=4&num=1&len=40");
        }
        else if (puzzle==3) { // 5x5x5
            scrambleTextView.setTextSize(20);
            mWebView.loadUrl("file:///android_asset/scramble_cube.htm?size=5&num=1&len=60");
        }
        else if (puzzle==4) { // 6x6x6
            scrambleTextView.setTextSize(15);
            mWebView.loadUrl("file:///android_asset/scramble_cube.htm?size=6&num=1&len=80");
        }
        else if (puzzle==5) { // 7x7x7
            scrambleTextView.setTextSize(14);
            mWebView.loadUrl("file:///android_asset/scramble_cube.htm?size=7&num=1&len=100");
        }
        else if (puzzle==6) { // Megaminx
            scrambleTextView.setTextSize(12);
            mWebView.loadUrl("file:///android_asset/scramble_megaminx2008.htm?num=1");
        } 
        else if (puzzle==7) { // Pyraminx
            scrambleTextView.setTextSize(24);
            mWebView.loadUrl("file:///android_asset/scramble_pyraminx2009.htm?num=1");
        } 
        else if (puzzle==8) { // Square One
            scrambleTextView.setTextSize(24);
            mWebView.loadUrl("file:///android_asset/scramble_square1.htm?num=1&len=40");
        } 
        else if (puzzle==9) { // UFO
            scrambleTextView.setTextSize(24);
            mWebView.loadUrl("file:///android_asset/scramble_ufo.htm?num=1");
        } 
    }

    /*
     * Method to read beep state from stored data
     */
    private int getBeepMode() {
        int fn_tmp = settings.getInt("beep_mode", -1);
        
        if (fn_tmp < 0) 
            { setBeepMode(1); return 1; }
        else 
            { setBeepMode(fn_tmp); }
        
        return fn_tmp;
    }
    
    /*
     * Method to write inspection time to stored data
     */
    private void setBeepMode(int x) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("beep_mode", x);
        editor.commit();
        beep_mode=x;

        if (beep_mode==1)
            beepTextView.setText("Beep: ON");
        else
            beepTextView.setText("Beep: OFF");
    }
    
    /*
     * Method to read inspection time from stored data
     */
    private int getInspectionIndex() {
        int fn_tmp = settings.getInt("inspection_index", -1);
        
        if (fn_tmp < 0) 
            { setInspectionIndex(1); return 1; }
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
     * Method to read puzzle index from stored data
     */
    private int getPuzzleIndex() {
        int fn_tmp = settings.getInt("puzzle_index", -1);
        
        if (fn_tmp < 0) 
            { setPuzzleIndex(1); return 0; }
        else 
            { setPuzzleIndex(fn_tmp); }
        
        return fn_tmp;
    }
    
    /*
     * Method to write puzzle index to stored data
     */
    private void setPuzzleIndex(int x) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("puzzle_index", x);
        editor.commit();
        puzzle=x;
        
        puzzleTextView.setText(puzzleStrings[puzzle]); 
//        setScrambleTextView();
    }
    
    /*
     * Method to write RunningStats instances to stored data
     */
    private void saveRecords(int pid) {
        String p=new Integer(pid).toString();
        
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(  p+"m_n",           rs[pid].m_n);
        editor.putFloat(p+"m_oldM", (float)rs[pid].m_oldM);
        editor.putFloat(p+"m_newM", (float)rs[pid].m_newM);
        editor.putFloat(p+"m_oldS", (float)rs[pid].m_oldS);
        editor.putFloat(p+"m_newS", (float)rs[pid].m_newS);
        editor.putFloat(p+"m_min",  (float)rs[pid].m_min);
        editor.putFloat(p+"m_max",  (float)rs[pid].m_max);
        editor.commit();
    }
    
    /*
     * Method to initialize RunningStats from stored data
     */    
    private void loadRecords(int pid) {
        String p=new Integer(pid).toString();
        
        int m_n = settings.getInt(p+"m_n", -1);
        double m_oldM = (double)settings.getFloat(p+"m_oldM", (float)-1);
        double m_newM = (double)settings.getFloat(p+"m_newM", (float)-1);
        double m_oldS = (double)settings.getFloat(p+"m_oldS", (float)-1);
        double m_newS = (double)settings.getFloat(p+"m_newS", (float)-1);
        double m_min  = (double)settings.getFloat(p+"m_min",  (float)-1);
        double m_max  = (double)settings.getFloat(p+"m_max",  (float)-1);
        
        if ((m_n < 0) || (m_oldM < 0) || (m_newM < 0) || 
                         (m_oldS < 0) || (m_newS < 0) ||
                         (m_min  < 0) || (m_max  < 0) ) 
            rs[pid] = new RunningStat(); 
        else 
            rs[pid] = new RunningStat(m_n, m_oldM, m_newM, 
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

    private String formatScramble(String s, int []breaks) {
        String new_s="";
        
        int i=0;
        for (String word : s.split(" ")) {
            new_s+=word+" ";
            for (int br : breaks)
                if (i==br)
                    new_s+="\n";
            i++;
        }
        return new_s;
        
    }
        
    /*
     * Method to set records fields
     */
    private void setRecords() {
        if (rs[puzzle].getCount()==0) {
            bestTextView.setText(   "best : --:--.--");
            aveTextView.setText("ave of 0 : --:--.--");
            sdTextView.setText(    "sddev : --:--.--");    
            
        } else {
            long best_aslong = Math.round( rs[puzzle].minimum()*1000);
            long mean_aslong = Math.round( rs[puzzle].mean()*1000);
            long sd_aslong   = Math.round( rs[puzzle].stdDeviation()*1000);
            String cnt_asstr = new Integer(rs[puzzle].getCount()).toString();
            
            bestTextView.setText("best : " + formatClock(best_aslong));
            aveTextView.setText("ave of " + cnt_asstr + " : " +
                                formatClock(mean_aslong));
            sdTextView.setText("sd dev : " + formatClock(sd_aslong));
        }
    }
}
