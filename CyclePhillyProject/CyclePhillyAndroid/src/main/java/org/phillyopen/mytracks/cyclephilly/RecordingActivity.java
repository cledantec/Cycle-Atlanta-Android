/**	 Cycle Altanta, Copyright 2012 Georgia Institute of Technology
 *                                    Atlanta, GA. USA
 *
 *   @author Christopher Le Dantec <ledantec@gatech.edu>
 *   @author Anhong Guo <guoanhong15@gmail.com>
 *
 *   Updated/Modified for Atlanta's app deployment. Based on the
 *   CycleTracks codebase for SFCTA.
 *
 *   CycleTracks, Copyright 2009,2010 San Francisco County Transportation Authority
 *                                    San Francisco, CA, USA
 *
 * 	 @author Billy Charlton <billy.charlton@sfcta.org>
 *
 *   This file is part of CycleTracks.
 *
 *   CycleTracks is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   CycleTracks is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with CycleTracks.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.phillyopen.mytracks.cyclephilly;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;

import org.phillyopen.mytracks.cyclephilly.R;

public class RecordingActivity extends FragmentActivity implements ConnectionCallbacks, OnConnectionFailedListener {
	
	public static final int DETECTION_INTERVAL_MILLISECONDS = 10000; // 10s
	// Store the PendingIntent used to send activity recognition events back to the app
	private PendingIntent mActivityRecognitionPendingIntent;
    // Store the current activity recognition client
    private ActivityRecognitionClient mActivityRecognitionClient;
    private LocalBroadcastManager mBroadcastManager;
    private IntentFilter mIntentFilter;
    private Context mContext;
    // Flag that indicates if a request is underway.
    private boolean mInProgress;
    public enum REQUEST_TYPE {START, STOP}
    private REQUEST_TYPE mRequestType;
			
	Intent fi;
	TripData trip;
	boolean isRecording = false;
	Button pauseButton;
	Button finishButton;
	Timer timer;
	float curDistance;

    TextView txtStat;
    TextView txtDistance;
    TextView txtDuration;
    TextView txtCurSpeed;
    TextView txtMaxSpeed;
    TextView txtAvgSpeed;

    final SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");
     // This code is returned in Activity.onActivityResult
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    // Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();
    final Runnable mUpdateTimer = new Runnable() {
        public void run() {
            updateTimer();
        }
    };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.recording);

        txtStat =     (TextView) findViewById(R.id.TextRecordStats);
        txtDistance = (TextView) findViewById(R.id.TextDistance);
        txtDuration = (TextView) findViewById(R.id.TextDuration);
        txtCurSpeed = (TextView) findViewById(R.id.TextSpeed);
        txtMaxSpeed = (TextView) findViewById(R.id.TextMaxSpeed);
        txtAvgSpeed = (TextView) findViewById(R.id.TextAvgSpeed);

		pauseButton = (Button) findViewById(R.id.ButtonPause);
		finishButton = (Button) findViewById(R.id.ButtonFinished);

        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        mContext = getApplicationContext();
        mInProgress = false;
        mActivityRecognitionClient = new ActivityRecognitionClient(mContext, this, this);
        //////////
        mBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        mIntentFilter = new IntentFilter(ACTIVITY_SERVICE);
        mIntentFilter.addCategory(NOTIFICATION_SERVICE);
        //////////
        Intent intent = new Intent(mContext, ActivityRecognitionIntentService.class);
        mActivityRecognitionPendingIntent =
                PendingIntent.getService(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

		// Query the RecordingService to figure out what to do.
		Intent rService = new Intent(this, RecordingService.class);
		startService(rService);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) { stopUpdates(); }
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;

				switch (rs.getState()) {
					case RecordingService.STATE_IDLE:
						trip = TripData.createTrip(RecordingActivity.this);
						rs.startRecording(trip);
						startUpdates();
						isRecording = true;
						RecordingActivity.this.pauseButton.setEnabled(true);
						RecordingActivity.this.setTitle("Cycle Philly - Recording...");
						break;
					case RecordingService.STATE_RECORDING:
						long id = rs.getCurrentTrip();
						trip = TripData.fetchTrip(RecordingActivity.this, id);
						isRecording = true;
						startUpdates();
						RecordingActivity.this.pauseButton.setEnabled(true);
						RecordingActivity.this.setTitle("Cycle Philly - Recording...");
						break;
					case RecordingService.STATE_PAUSED:
						long tid = rs.getCurrentTrip();
						isRecording = false;
						trip = TripData.fetchTrip(RecordingActivity.this, tid);
						RecordingActivity.this.pauseButton.setEnabled(true);
						RecordingActivity.this.pauseButton.setText("Resume");
						RecordingActivity.this.setTitle("Cycle Philly - Paused...");
						break;
					case RecordingService.STATE_FULL:
						// Should never get here, right?
						break;
				}
				rs.setListener(RecordingActivity.this);
				unbindService(this);
			}
		};
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
		
		BroadcastReceiver updateListReceiver = new BroadcastReceiver() {
		   @Override
		   public void onReceive(Context context, Intent intent) {
		     // When an Intent is received from the update listener IntentService,
		     
			   // TODO:
			 Log.d("broadcast received", "Detected " + intent.getDataString());
		   }
		};

		// Pause button
		pauseButton.setEnabled(false);
		pauseButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				isRecording = !isRecording;
				if (isRecording) {
					pauseButton.setText("Pause");
					RecordingActivity.this.setTitle("Cycle Philly - Recording...");
					// Don't include pause time in trip duration
					if (trip.pauseStartedAt > 0) {
	                    trip.totalPauseTime += (System.currentTimeMillis() - trip.pauseStartedAt);
	                    trip.pauseStartedAt = 0;
					}
					Toast.makeText(getBaseContext(),"GPS restarted. It may take a moment to resync.", Toast.LENGTH_LONG).show();
				} else {
					pauseButton.setText("Resume");
					RecordingActivity.this.setTitle("Cycle Philly - Paused...");
					trip.pauseStartedAt = System.currentTimeMillis();
					Toast.makeText(getBaseContext(),"Recording paused; GPS now offline", Toast.LENGTH_LONG).show();
				}
				RecordingActivity.this.setListener();
			}
		});

		// Finish button
		finishButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				stopUpdates(); // done checking current activity
				
				// If we have points, go to the save-trip activity
				if (trip.numpoints > 0) {
				    // Handle pause time gracefully
                    if (trip.pauseStartedAt> 0) {
                        trip.totalPauseTime += (System.currentTimeMillis() - trip.pauseStartedAt);
                    }
                    if (trip.totalPauseTime > 0) {
                        trip.endTime = System.currentTimeMillis() - trip.totalPauseTime;
                    }
					// Save trip so far (points and extent, but no purpose or notes)
					fi = new Intent(RecordingActivity.this, SaveTrip.class);
					trip.updateTrip("","","","");
				}
				// Otherwise, cancel and go back to main screen
				else {
					Toast.makeText(getBaseContext(),"No GPS data acquired; nothing to submit.", Toast.LENGTH_SHORT).show();

					cancelRecording();

			    	// Go back to main screen
					fi = new Intent(RecordingActivity.this, MainInput.class);
					fi.putExtra("keep", true);
				}

				// Either way, activate next task, and then kill this task
				startActivity(fi);
				RecordingActivity.this.finish();
			}
		});
	}

	public void updateStatus(int points, float distance, float spdCurrent, float spdMax) {
	    this.curDistance = distance;

	    //TODO: check task status before doing this?
        if (points>0) {
            txtStat.setText(""+points+" data points received...");
        } else {
            txtStat.setText("Waiting for GPS fix...");
        }
        txtCurSpeed.setText(String.format("%1.1f mph", spdCurrent));
        txtMaxSpeed.setText(String.format("Max Speed: %1.1f mph", spdMax));

    	float miles = 0.0006212f * distance;
    	txtDistance.setText(String.format("%1.1f miles", miles));
	}

	void setListener() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				if (RecordingActivity.this.isRecording) {
					rs.resumeRecording();
				} else {
					rs.pauseRecording();
				}
				unbindService(this);
			}
		};
		// This should block until the onServiceConnected (above) completes, but doesn't
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}

	void cancelRecording() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) { }
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				rs.cancelRecording();
				stopUpdates();
				unbindService(this);
			}
		};
		// This should block until the onServiceConnected (above) completes.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}

	// onResume is called whenever this activity comes to foreground.
	// Use a timer to update the trip duration.
    @Override
    public void onResume() {
        super.onResume();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(mUpdateTimer);
            }
        }, 0, 1000);  // every second
    }

    void updateTimer() {
        if (trip != null && isRecording) {
            double dd = System.currentTimeMillis()
                        - trip.startTime
                        - trip.totalPauseTime;

            txtDuration.setText(sdf.format(dd));

            double avgSpeed = 3600.0 * 0.6212 * this.curDistance / dd;
            txtAvgSpeed.setText(String.format("%1.1f mph", avgSpeed));
        }
    }

    // Don't do pointless UI updates if the activity isn't being shown.
    @Override
    public void onPause() {
        super.onPause();
        if (timer != null) timer.cancel();
    }
    
    public void startUpdates() {
    	// If a request is not already underway
    	
    	mRequestType = REQUEST_TYPE.START;
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            return;
        }
        
        if (!mInProgress) {
            mInProgress = true; // Indicate that a request is in progress
            mActivityRecognitionClient.connect(); // Request a connection to Location Services
        } else {
            /*
             * A request is already underway. You can handle
             * this situation by disconnecting the client,
             * re-setting the flag, and then re-trying the
             * request.
             */
        	// TODO:
        }
    }
    
    public void stopUpdates() {
        // Set the request type to STOP
        mRequestType = REQUEST_TYPE.STOP;
        /*
         * Test for Google Play services after setting the request type.
         * If Google Play services isn't present, the request can be
         * restarted.
         */
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            return;
        }
        // If a request is not already underway
        if (!mInProgress) {
            // Indicate that a request is in progress
            mInProgress = true;
            // Request a connection to Location Services
            mActivityRecognitionClient.connect();
        //
        } else {
            /*
             * A request is already underway. You can handle
             * this situation by disconnecting the client,
             * re-setting the flag, and then re-trying the
             * request.
             */
        }
    }

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		// Turn off the request flag
        mInProgress = false;
        /*
         * If the error has a resolution, start a Google Play services
         * activity to resolve it.
         */
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        // If no resolution is available, display an error dialog
        } else {
            // Get the error code
            int errorCode = connectionResult.getErrorCode();
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    errorCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getSupportFragmentManager(), "Activity Recognition");
            }
        }
	}

	@Override
	public void onConnected(Bundle dataBundle) {
        
        switch (mRequestType) {
        	case STOP :
        		mActivityRecognitionClient.removeActivityUpdates(
                mActivityRecognitionPendingIntent);
        		break;
			case START:
				mActivityRecognitionClient.requestActivityUpdates(
		                DETECTION_INTERVAL_MILLISECONDS,
		                mActivityRecognitionPendingIntent);
				
				mInProgress = false;
		        mActivityRecognitionClient.disconnect();
				break;
			default:
				break;
        }
	}
    
	@Override
	public void onDisconnected() {
		// Turn off the request flag
        mInProgress = false;
        mActivityRecognitionClient.removeActivityUpdates(
                mActivityRecognitionPendingIntent);
        // Delete the client
        mActivityRecognitionClient = null;
	}
}
