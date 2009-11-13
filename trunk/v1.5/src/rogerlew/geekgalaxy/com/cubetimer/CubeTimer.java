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
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
//import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import java.util.Timer;
import java.util.TimerTask;

public class CubeTimer extends Activity {
	private static final int STOPPED = 0; 
    private static final int RUNNING = 1; 
    
    private View view;
	private Scrambler scrambler;
	private TextView scramble;
	private TextView time;
	private TextView best;
	private TextView average;
	private Button button;
	private Spinner spinner;
	private SharedPreferences settings;
	
	private Timer timer;
	
	private int mode=0; // 0=stopped, 1=running
	
	private long inspection_time=0;
	private long t0; 
	private long elapsed;
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// Ignore orientation and keyboardHidden changes
		super.onConfigurationChanged(newConfig);
	}
	
    /*
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Initialized stored data
        settings = getPreferences(MODE_PRIVATE);
        getCount();        // if "count" isn't set, make 0L
        getAverage();	   // if "average" isn't set, make 0L
        getBest();		   // if "best" isn't set, make Long.MAX_VALUE
        getSpinnerIndex(); // if "spinner_index" isn't set, make 0

        // Keep screen on
        view = (View)findViewById(R.id.tblview);
        view.setKeepScreenOn(true);
        
        // Initialize Scrambler
        scrambler = new Scrambler();
        
        // Initialize scramble text
        scramble = (TextView)findViewById(R.id.scramble);
        scramble.setText((CharSequence)scrambler.genScramble());
        
        // Initialize time
        time = (TextView)findViewById(R.id.time);
        best = (TextView)findViewById(R.id.best);
        average = (TextView)findViewById(R.id.average);
        	
        // Initialize button
        button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View view) {
            	 switch (mode) {
            	 	case STOPPED:
            	 		String spinner_selection = (String)spinner.getSelectedItem();
            	 		if (spinner_selection.contains("15")) {
            	 			inspection_time=15*1000;
            	 			setSpinnerIndex(4);
            	 		}
            	 		else if (spinner_selection.contains("10")) {
            	 			inspection_time=10*1000;
            	 			setSpinnerIndex(3);
            	 		}
            	 		else if (spinner_selection.contains("5")) {
            	 			inspection_time=5*1000;
            	 			setSpinnerIndex(2);
            	 		}
            	 		else if (spinner_selection.contains("3")) {
            	 			inspection_time=3*1000;
            	 			setSpinnerIndex(1);
            	 		}
            	 		else {
            	 			inspection_time=1000;
            	 			setSpinnerIndex(0);
            	 		}
                	 
            	 		time.setText(new Long(inspection_time/1000).toString());
            	 		button.setText("Stop");
            	 		
            	 		mode=1; // toggle mode
            	 		t0=SystemClock.uptimeMillis();
            	 		break;
            	 		
            	 	case RUNNING:
            	 		scramble.setText((CharSequence)scrambler.genScramble());
            	 		button.setText("Start");
            	 		
            	 		setBestText();
            	 		setAverageText();
                    	 		
            	 		mode=0; // toggle mode
            	 		break;
            	 } 
            } 
        }); 
        
        // Set method for clearing records
        button.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
            	setCount(-1L);
            	setAverage(-1L);
            	setBest(-1L);

            	elapsed=0;
    	 		best.setText("--:--:-- (best)");
    	 		average.setText("--:--:-- (average)");
    	 		
    	 		
            	return true;
            }
        });
        
        // Initialize spinner
        spinner = (Spinner)findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                							     this, R.array.ins_times, 
                							 	 android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(getSpinnerIndex());
        
        // Initialize Records
        if (getBest() < 0) {
        	best.setText(formatClock(getBest()) + " (best)");
        } else {
        	best.setText("--:--:-- (best)");
        }
        
        if (getAverage() < -1) {
        	average.setText(formatClock(getAverage())
        					+ " (average of " +
        					new Long(getCount()).toString() + ")");
        } else {
        	average.setText("--:--:-- (average)");
        }
        
        
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
		    		
		    		if (elapsed > current+1000)
		    			time.setText(new Long((inspection_time-elapsed+1000)/1000).toString());
		    	} else {
		    		time.setText(formatClock(elapsed-inspection_time));
		    	}
		    }
		}
	};

    /*
     * Methods for getting and setting stored data
     */
    private long getBest() {
    	long fn_tmp = settings.getLong("best", -1L);
    	if (fn_tmp < 0L) {
    		setBest(Long.MAX_VALUE);
    		return  Long.MAX_VALUE;
    	}
    	return fn_tmp;
    }
    
    private void setBest(long x) {
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putLong("best", x);
        editor.commit();
    }
    
    private long getAverage() {
    	long fn_tmp = settings.getLong("average", -1L);
    	if (fn_tmp < 0L) {
    		setAverage(0L);
    		return 0L;
    	}
    	return fn_tmp;
    }
    
    private void setAverage(long x) {
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putLong("average", x);
        editor.commit();
    }
    
    private long getCount() {
    	long fn_tmp = settings.getLong("count", -1L);
    	if (fn_tmp < 0L) {
    		setCount(0L);
    		return 0L;
    	}
    	return fn_tmp;
    }
    
    private void setCount(long x) {
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putLong("count", x);
        editor.commit();
    }
    
    private int getSpinnerIndex() {
    	int fn_tmp = settings.getInt("spinner_index", -1);
    	if (fn_tmp < 0L) {
    		setSpinnerIndex(1);
    		return 0;
    	}
    	return fn_tmp;
    }
    
    private void setSpinnerIndex(int x) {
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putInt("spinner_index", x);
        editor.commit();
    }

    /*
     * Sets best TextView
     */
    private void setBestText() {
    	if (elapsed-inspection_time > 0L) {
    		if (elapsed-inspection_time < getBest())
    			setBest(elapsed-inspection_time);
    	
    		best.setText(formatClock(getBest()) + " (best)");
    	} else {
    		best.setText("--:--:-- (best)");
    	}
    }

    /*
     * Sets average TextView
     */
    private void setAverageText() {
    	if (elapsed-inspection_time > 0L) {
    		setAverage((getAverage()*getCount()+(elapsed-inspection_time))/(getCount()+1L));
    		setCount(getCount()+1L);
    	
    		average.setText(formatClock(getAverage())
    				        + " (average of " +
    						new Long(getCount()).toString() + ")");
    	} else {
        	average.setText("--:--:-- (average)");
        }
    }
    
    /*
     * Formats elapsed time in the "MM:SS:HH" format
     */
    private String formatClock(long elapsed) {
    	if (elapsed<0)
    		return "00:00:00";
    	
    	elapsed/=10;
    	
    	String hundreths = Integer.toString((int)(elapsed % 100));
    	String seconds   = Integer.toString((int)(elapsed % 6000)/100);
    	String minutes   = Integer.toString((int)(elapsed / 6000));
    	
    	if (hundreths.length()<2) hundreths="0"+hundreths;
    	if (seconds.length()<2)   seconds="0"+seconds;
    	if (minutes.length()<2)   minutes="0"+minutes;
    	
    	return minutes+":"+seconds+":"+hundreths;
    }
}
