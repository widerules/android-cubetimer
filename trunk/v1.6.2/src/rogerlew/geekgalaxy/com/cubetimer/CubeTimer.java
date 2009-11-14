/*
 * CubeTimer.java
 * 
 * This class implements an Android Activity which
 * generates random scrambles for a 3x3x3 Rubik's 
 * Cube and provides an inspection countdown and 
 * stopwatch. App will list and save best and 
 * average solve times.
 * 
 * Copyright (c) 2009, Roger Lew
 * All rights reserved.
 * 
 * Licensed under BSD.
 */
package rogerlew.geekgalaxy.com.cubetimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
	// Constants
	private static final int STOPPED = 0; 
    private static final int RUNNING = 1; 
    
    // Android Objects
    private View view;
	private TextView scramble;
	private TextView time;
	private TextView ins_time;
	private TextView notes;
	private TextView best;
	private TextView average;
	private TextView sd;
	private SharedPreferences settings;
	private AlertDialog insDialog;
	
	// Control Objects
	private Timer timer;
	private Scrambler scrambler;
	private RunningStat rs;
	
	// State and time variables
	private int mode=0; // 0=stopped, 1=running
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
        time     = (TextView)findViewById(R.id.time);
        ins_time = (TextView)findViewById(R.id.ins_time);
        best     = (TextView)findViewById(R.id.best);
        average  = (TextView)findViewById(R.id.average);
        sd       = (TextView)findViewById(R.id.sd);
        notes    = (TextView)findViewById(R.id.notes);

		// Keep screen on
        view = (View)findViewById(R.id.tblview);
        view.setKeepScreenOn(true);
        
        // Display instructions for small screens
        if (getWindowManager().getDefaultDisplay().getHeight() < 400){
        	notes.setText("");
        	Toast.makeText(getApplicationContext(),
  		  	         R.string.instructions,
  			         Toast.LENGTH_LONG).show();        	
        } 
        	
        // Initialized stored data
        settings = getPreferences(MODE_PRIVATE);
        loadRecords(); setRecords(); saveRecords();
        getInspectionIndex();

        // Initialize inspection dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Inspection Time");
		builder.setSingleChoiceItems(R.array.ins_times, 
				                     getInspectionIndex(), 
				                     new DialogInterface.OnClickListener() {
	    	public void onClick(DialogInterface dialog, int item) {
	    		setInspectionIndex(item);
	    		insDialog.hide();
	    	}
		});
		insDialog = builder.create();
		

        
        // Initialize Scrambler
        scrambler = new Scrambler();
        
        // Initialize scramble text
        scramble = (TextView)findViewById(R.id.scramble);
        scramble.setText((CharSequence)scrambler.genScramble());
        	
        // Initialize screen tap listener
        view.setOnClickListener(new View.OnClickListener() {  
        	public void onClick(View view) {
            	 switch (mode) {
            	 	case STOPPED:               	 
            	 		time.setText(long2str(inspection_time/1000));
            	 		mode=1; // toggle mode
            	 		t0=SystemClock.uptimeMillis();
            	 		break;
            	 		
            	 	case RUNNING:
            	 		scramble.setText(scrambler.genScramble());
            	 		if (elapsed-inspection_time > 0) {
            	 			rs.push((double)(elapsed-inspection_time)/1000);
            	 			setRecords(); saveRecords();
            	 		}
            	 		mode=0; // toggle mode
            	 		break;
            	 } 
            } 
        }); 
        
        // Initialize long pause on screen listener
        view.setOnLongClickListener(new View.OnLongClickListener() {
              public boolean onLongClick(View view) {
            	  rs.clear(); elapsed=0;
            	  saveRecords(); setRecords();
            	  Toast.makeText(getApplicationContext(),
    	 			  	         "All Records have been cleared",
    	 				         Toast.LENGTH_SHORT).show();
            	  return true;
            }
        });
        
        // Initialize timer object. Timer calls mHandler every 34 ms
        timer = new Timer();
   	 	timer.scheduleAtFixedRate(new TimerTask() {
   	 		public void run() {
                mHandler.sendMessage(
                    Message.obtain(
                        mHandler, mode, SystemClock.uptimeMillis()));
            }
        }, 0L, 34L);
    }

    /*
     * Message handler for updating time display
     */
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
		    elapsed = SystemClock.uptimeMillis() - t0;
		    
		    if (msg.what==RUNNING) {
		    	if (elapsed<inspection_time) {
		    		long current = Long.parseLong(time.getText().toString());
		    		
		    		if (elapsed > current+1000) {
		    			long rem = (inspection_time-elapsed+1000)/1000;
		    			time.setText(long2str(rem));
		    		}
		    	} else {
		    		time.setText(formatClock(elapsed-inspection_time));
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
	 * Function to convert longs to strings
	 */
	private String long2str(long x){
		return new Long(x).toString();
	}

	/*
	 * Method to read inspection time from stored data
	 */
    private int getInspectionIndex() {
    	int fn_tmp = settings.getInt("inspection_index", -1);
    	
    	if (fn_tmp < 0) {
    		setInspectionIndex(1);
    		return 0;
    	} else {
    		setInspectionIndex(fn_tmp);
    	}
    	
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
			
		ins_time.setText("Inspection Time: " + 
				         long2str(inspection_time/1000L) + "s"); 
    }

    /*
     * Formats elapsed time in the "MM:SS.HH" format
     * (HH is hundreths)
     */
    private String formatClock(long elapsed) {
    	if (elapsed<0)
    		return "00:00.00";
    	
    	elapsed/=10;
    	
    	String hundreths = Integer.toString((int)(elapsed % 100));
    	String seconds   = Integer.toString((int)(elapsed % 6000)/100);
    	String minutes   = Integer.toString((int)(elapsed / 6000));
    	
    	if (hundreths.length()<2) hundreths="0"+hundreths;
    	if (seconds.length()<2)   seconds="0"+seconds;
    	if (minutes.length()<2)   minutes="0"+minutes;
    	
    	return minutes+":"+seconds+"."+hundreths;
    }
    
    /*
     * Method to write RunningStats instances to stored data
     */
    private void saveRecords() {
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putInt("m_n",      rs.getM_n());
    	editor.putFloat("m_oldM", (float)rs.getM_oldM());
    	editor.putFloat("m_newM", (float)rs.getM_newM());
    	editor.putFloat("m_oldS", (float)rs.getM_oldS());
    	editor.putFloat("m_newS", (float)rs.getM_newS());
    	editor.putFloat("m_min",  (float)rs.getM_min());
    	editor.putFloat("m_max",  (float)rs.getM_max());
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
        
        if ((m_n<0) || (m_oldM<0) || (m_newM<0) || 
        		       (m_oldS<0) || (m_newS<0) ||
        		       (m_min<0)  || (m_max<0) ) {
        	rs = new RunningStat();	
        } else {
        	rs = new RunningStat(m_n, m_oldM, m_newM, 
        			                  m_oldS, m_newS, 
        			                  m_min,  m_max);
        }
    }
    
    /*
     * Method to set records fields
     */
    private void setRecords() {
    	if (rs.getCount()==0) {
    		best.setText("best : --:--.--");
    		average.setText("ave of 0 : --:--.--");
    		sd.setText("sddev : --:--.--");    		
    	} else {
    		best.setText("best : " + 
    				     formatClock(Math.round(rs.minimum()*1000)));
    		
    		average.setText("ave of " + 
    				        new Integer(rs.getCount()).toString() + " : " +
    				        formatClock(Math.round(rs.mean()*1000)));
    		
    		sd.setText("sd dev : " + 
    				   formatClock(Math.round(rs.standardDeviation()*1000)));
    	}
    }
}