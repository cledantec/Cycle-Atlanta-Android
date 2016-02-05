package edu.gatech.ppl.cycleatlanta;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import edu.gatech.ppl.cycleatlanta.region.ObaRegionsTask;
import edu.gatech.ppl.cycleatlanta.region.elements.ObaRegion;
import edu.gatech.ppl.cycleatlanta.region.utils.LocationHelper;
import edu.gatech.ppl.cycleatlanta.region.utils.LocationUtils;
import edu.gatech.ppl.cycleatlanta.region.utils.MapHelpV2;
import edu.gatech.ppl.cycleatlanta.region.utils.PreferenceUtils;
import edu.gatech.ppl.cycleatlanta.region.utils.RegionUtils;
import edu.gatech.ppl.cycleatlanta.region.utils.UIUtils;

public class FragmentMainInput extends Fragment implements
        OnMyLocationButtonClickListener, LocationHelper.Listener,
        ObaRegionsTask.Callback {

    public static final String ARG_SECTION_NUMBER = "section_number";

    private static final String TAG = "FragmentMainInput";

    Intent fi;
    TripData trip;
    NoteData note;
    boolean isRecording = false;
    Timer timer;
    float curDistance;

    TextView txtDuration;
    TextView txtDistance;
    TextView txtCurSpeed;

    LocationHelper mLocationHelper;

    int zoomFlag = 1;

    Location currentLocation = new Location("");

    final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    // Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();
    final Runnable mUpdateTimer = new Runnable() {
        public void run() {
            updateTimer();
        }
    };

    private static final long REGION_UPDATE_THRESHOLD = 1000 * 60 * 60 * 24 * 7;

    private static final String CHECK_REGION_VER = "checkRegionVer";

    GoogleMap mMap;
    UiSettings mUiSettings;
    protected GoogleApiClient mGoogleApiClient;

    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000) // 5 seconds
            .setFastestInterval(16) // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    public FragmentMainInput() {
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make sure GoogleApiClient is connected, if available
        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        // Tear down GoogleApiClient
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupGooglePlayServices();

        mLocationHelper = new LocationHelper(getActivity());
        mLocationHelper.registerListener(this);

        checkRegionStatus();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View rootView = inflater.inflate(R.layout.activity_main_input,
                container, false);
        setUpMapIfNeeded();

        Intent rService = new Intent(getActivity(), RecordingService.class);
        ServiceConnection sc = new ServiceConnection() {
            public void onServiceDisconnected(ComponentName name) {
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                IRecordService rs = (IRecordService) service;
                int state = rs.getState();
                if (state > RecordingService.STATE_IDLE) {
                    if (state == RecordingService.STATE_FULL) {
                        startActivity(new Intent(getActivity(),
                                TripPurposeActivity.class));
                    }

                    getActivity().finish();
                }
                getActivity().unbindService(this); // race? this says
                // we no longer care
            }
        };
        // This needs to block until the onServiceConnected (above) completes.
        // Thus, we can check the recording status before continuing on.
        getActivity().bindService(rService, sc, Context.BIND_AUTO_CREATE);

        // Log.d("Jason", "Start2");

        // And set up the record button
        Button startButton = (Button) rootView.findViewById(R.id.buttonStart);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isRecording == false) {
                    // Before we go to record, check GPS status
                    final LocationManager manager = (LocationManager) getActivity()
                            .getSystemService(Context.LOCATION_SERVICE);
                    if (!manager
                            .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        buildAlertMessageNoGps();
                    } else {
                        // startActivity(i);
                        // call function in Recording Activity
                        // Toast.makeText(getApplicationContext(),
                        // "Start Clicked",Toast.LENGTH_LONG).show();
                        startRecording();
                        // MainInputActivity.this.finish();
                    }
                } else if (isRecording == true) {
                    // pop up: save, discard, cancel
                    buildAlertMessageSaveClicked();
                }
            }
        });

        Button noteThisButton = (Button) rootView
                .findViewById(R.id.buttonNoteThis);
        noteThisButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final LocationManager manager = (LocationManager) getActivity()
                        .getSystemService(Context.LOCATION_SERVICE);
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    buildAlertMessageNoGps();
                } else {
                    fi = new Intent(getActivity(), NoteTypeActivity.class);
                    // update note entity
                    note = NoteData.createNote(getActivity());

                    fi.putExtra("noteid", note.noteid);

                    Log.v("Jason", "Note ID in MainInput: " + note.noteid);

                    if (isRecording == true) {
                        fi.putExtra("isRecording", 1);
                    } else {
                        fi.putExtra("isRecording", 0);
                    }

                    note.updateNoteStatus(NoteData.STATUS_INCOMPLETE);

                    double currentTime = System.currentTimeMillis();

                    if (currentLocation != null) {
                        note.addPointNow(currentLocation, currentTime);

                        // Log.v("Jason", "Note ID: "+note);

                        startActivity(fi);
                        getActivity().overridePendingTransition(
                                R.anim.slide_in_right, R.anim.slide_out_left);
                        // getActivity().finish();
                    } else {
                        Toast.makeText(getActivity(),
                                "No GPS data acquired; nothing to submit.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // copy from Recording Activity
        txtDuration = (TextView) rootView
                .findViewById(R.id.textViewElapsedTime);
        txtDistance = (TextView) rootView.findViewById(R.id.textViewDistance);
        txtCurSpeed = (TextView) rootView.findViewById(R.id.textViewSpeed);

        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return rootView;
    }

    private void checkRegionStatus() {
        //First check for custom API URL set by user via Preferences, since if that is set we don't need region info from the REST API
        if (!TextUtils.isEmpty(Application.get().getCustomApiUrl())) {
            return;
        }

        // Check if region is hard-coded for this build flavor
        if (BuildConfig.USE_FIXED_REGION) {
            ObaRegion r = RegionUtils.getRegionFromBuildFlavor();
            // Set the hard-coded region
            RegionUtils.saveToProvider(getActivity(), Collections.singletonList(r));
            Application.get().setCurrentRegion(r);
            // Disable any region auto-selection in preferences
            PreferenceUtils
                    .saveBoolean(getString(R.string.preference_key_auto_select_region), false);
            return;
        }

        boolean forceReload = false;
        boolean showProgressDialog = true;

        //If we don't have region info selected, or if enough time has passed since last region info update,
        //force contacting the server again
        if (Application.get().getCurrentRegion() == null ||
                new Date().getTime() - Application.get().getLastRegionUpdateDate()
                        > REGION_UPDATE_THRESHOLD) {
            forceReload = true;
            Log.d(TAG,
                    "Region info has expired (or does not exist), forcing a reload from the server...");
        }

        if (Application.get().getCurrentRegion() != null) {
            //We already have region info locally, so just check current region status quietly in the background
            showProgressDialog = false;
        }

        try {
            PackageInfo appInfo = getActivity().getPackageManager().getPackageInfo(
                    getActivity().getPackageName(), PackageManager.GET_META_DATA);
            SharedPreferences settings = Application.getPrefs();
            final int oldVer = settings.getInt(CHECK_REGION_VER, 0);
            final int newVer = appInfo.versionCode;

            if (oldVer < newVer) {
                forceReload = true;
            }
            PreferenceUtils.saveInt(CHECK_REGION_VER, appInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            // Do nothing
        }

        //Check region status, possibly forcing a reload from server and checking proximity to current region
        ObaRegionsTask task = new ObaRegionsTask(getActivity(), this, forceReload,
                showProgressDialog);
        task.execute();
    }

    private void setupGooglePlayServices() {
        // Init Google Play Services as early as possible in the Fragment lifecycle to give it time
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity())
                == ConnectionResult.SUCCESS) {
            mGoogleApiClient = LocationUtils.getGoogleApiClientWithCallbacks(getActivity());
            mGoogleApiClient.connect();
        }
    }

    public void updateStatus(int points, float distance, float spdCurrent,
                             float spdMax) {
        this.curDistance = distance;

        txtCurSpeed.setText(String.format("%1.1f mph", spdCurrent));

        float miles = 0.0006212f * distance;
        txtDistance.setText(String.format("%1.1f miles", miles));
    }

    void cancelRecording() {
        final Button startButton = (Button) getActivity().findViewById(
                R.id.buttonStart);
        startButton.setText("Start");
        // startButton.setBackgroundColor(0x4d7d36);
        Intent rService = new Intent(getActivity(), RecordingService.class);
        ServiceConnection sc = new ServiceConnection() {
            public void onServiceDisconnected(ComponentName name) {
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                IRecordService rs = (IRecordService) service;
                rs.cancelRecording();
                getActivity().unbindService(this);
            }
        };
        // This should block until the onServiceConnected (above) completes.
        getActivity().bindService(rService, sc, Context.BIND_AUTO_CREATE);

        isRecording = false;

        txtDuration = (TextView) getActivity().findViewById(
                R.id.textViewElapsedTime);
        txtDuration.setText("00:00:00");
        txtDistance = (TextView) getActivity().findViewById(
                R.id.textViewDistance);
        txtDistance.setText("0.0 miles");

        txtCurSpeed = (TextView) getActivity().findViewById(R.id.textViewSpeed);
        txtCurSpeed.setText("0.0 mph");
    }

    void startRecording() {
        // Query the RecordingService to figure out what to do.
        final Button startButton = (Button) getActivity().findViewById(
                R.id.buttonStart);
        Intent rService = new Intent(getActivity(), RecordingService.class);
        getActivity().startService(rService);
        ServiceConnection sc = new ServiceConnection() {
            public void onServiceDisconnected(ComponentName name) {
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                IRecordService rs = (IRecordService) service;

                switch (rs.getState()) {
                    case RecordingService.STATE_IDLE:
                        trip = TripData.createTrip(getActivity());
                        rs.startRecording(trip);
                        isRecording = true;
                        startButton.setText("Save");
                        // startButton.setBackgroundColor(0xFF0000);
                        // MainInputActivity.this.pauseButton.setEnabled(true);
                        // MainInputActivity.this
                        // .setTitle("Cycle Atlanta - Recording...");
                        break;
                    case RecordingService.STATE_RECORDING:
                        long id = rs.getCurrentTrip();
                        trip = TripData.fetchTrip(getActivity(), id);
                        isRecording = true;
                        startButton.setText("Save");
                        // startButton.setBackgroundColor(0xFF0000);
                        // MainInputActivity.this.pauseButton.setEnabled(true);
                        // MainInputActivity.this
                        // .setTitle("Cycle Atlanta - Recording...");
                        break;
                    // case RecordingService.STATE_PAUSED:
                    // long tid = rs.getCurrentTrip();
                    // isRecording = false;
                    // trip = TripData.fetchTrip(MainInputActivity.this, tid);
                    // // MainInputActivity.this.pauseButton.setEnabled(true);
                    // // MainInputActivity.this.pauseButton.setText("Resume");
                    // // MainInputActivity.this
                    // // .setTitle("Cycle Atlanta - Paused...");
                    // break;
                    case RecordingService.STATE_FULL:
                        // Should never get here, right?
                        break;
                }
                rs.setListener((FragmentMainInput) getActivity()
                        .getSupportFragmentManager().findFragmentByTag(
                                "android:switcher:" + R.id.pager + ":0"));
                getActivity().unbindService(this);
            }
        };
        getActivity().bindService(rService, sc, Context.BIND_AUTO_CREATE);

        isRecording = true;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity());
        builder.setMessage(
                "Your phone's GPS is disabled. Cycle Atlanta needs GPS to determine your location.\n\nGo to System Settings now to enable GPS?")
                .setCancelable(false)
                .setPositiveButton("GPS Settings...",
                        new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog,
                                                final int id) {
                                final ComponentName toLaunch = new ComponentName(
                                        "com.android.settings",
                                        "com.android.settings.SecuritySettings");
                                final Intent intent = new Intent(
                                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                                intent.setComponent(toLaunch);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivityForResult(intent, 0);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog,
                                                final int id) {
                                dialog.cancel();
                            }
                        });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void buildAlertMessageSaveClicked() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity());
        builder.setTitle("Save Trip");
        builder.setMessage("Do you want to save this trip?");
        builder.setNegativeButton("Save",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        // save
                        // If we have points, go to the save-trip activity
                        // trip.numpoints > 0
                        if (trip.numpoints > 0) {
                            // Handle pause time gracefully
                            if (trip.pauseStartedAt > 0) {
                                trip.totalPauseTime += (System
                                        .currentTimeMillis() - trip.pauseStartedAt);
                            }
                            if (trip.totalPauseTime > 0) {
                                trip.endTime = System.currentTimeMillis()
                                        - trip.totalPauseTime;
                            }
                            // Save trip so far (points and extent, but no
                            // purpose or
                            // notes)
                            fi = new Intent(getActivity(),
                                    TripPurposeActivity.class);
                            trip.updateTrip("", "", "", "");

                            startActivity(fi);
                            getActivity().overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left);
                            getActivity().finish();
                        }
                        // Otherwise, cancel and go back to main screen
                        else {
                            Toast.makeText(getActivity(),
                                    "No GPS data acquired; nothing to submit.",
                                    Toast.LENGTH_SHORT).show();

                            cancelRecording();
                        }
                    }
                });

        builder.setNeutralButton("Discard",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        // discard
                        cancelRecording();
                    }
                });

        builder.setPositiveButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        // continue
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    void updateTimer() {
        if (trip != null && isRecording) {
            double dd = System.currentTimeMillis() - trip.startTime
                    - trip.totalPauseTime;

            txtDuration.setText(sdf.format(dd));

        }
    }

    // onResume is called whenever this activity comes to foreground.
    // Use a timer to update the trip duration.
    @Override
    public void onResume() {
        super.onResume();

        mLocationHelper.onResume();

        Log.v("Jason", "Cycle: MainInput onResume");

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(mUpdateTimer);
            }
        }, 0, 1000); // every second

        setUpMapIfNeeded();
        if (mMap != null) {
            // Keep the UI Settings state in sync with the checkboxes.
            mUiSettings.setZoomControlsEnabled(true);
            mUiSettings.setCompassEnabled(true);
            mUiSettings.setMyLocationButtonEnabled(true);
            mMap.setMyLocationEnabled(true);
            mUiSettings.setScrollGesturesEnabled(true);
            mUiSettings.setZoomGesturesEnabled(true);
            mUiSettings.setTiltGesturesEnabled(true);
            mUiSettings.setRotateGesturesEnabled(true);
        }
    }

    // Don't do pointless UI updates if the activity isn't being shown.
    @Override
    public void onPause() {
        super.onPause();
        Log.v("Jason", "Cycle: MainInput onPause");
        mLocationHelper.onPause();
        // Background GPS.
        if (timer != null)
            timer.cancel();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.v("Jason", "Cycle: MainInput onDestroyView");
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the
        // mMap.
        if (mMap == null) {
            // Try to obtain the mMap from the SupportMapFragment.

            mMap = getMapFragment().getMap();
            // Check if we were successful in obtaining the mMap.
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.setOnMyLocationButtonClickListener(this);
                mUiSettings = mMap.getUiSettings();
                // centerMapOnMyLocation();
            }
        }
    }

    private SupportMapFragment getMapFragment() {
        FragmentManager fm = null;

        Log.d(TAG, "sdk: " + Build.VERSION.SDK_INT);
        Log.d(TAG, "release: " + Build.VERSION.RELEASE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Log.d(TAG, "using getFragmentManager");
            fm = getFragmentManager();
        } else {
            Log.d(TAG, "using getChildFragmentManager");
            fm = getChildFragmentManager();
        }

        return (SupportMapFragment) fm.findFragmentById(R.id.map);
    }

    /**
     * Implementation of {@link LocationListener}.
     */
    @Override
    public void onLocationChanged(Location location) {
        // onMyLocationButtonClick();
        currentLocation = location;

        // Log.v("Jason", "Current Location: "+currentLocation);

        if (zoomFlag == 1) {
            LatLng myLocation;

            if (location != null) {
                myLocation = new LatLng(location.getLatitude(),
                        location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation,
                        16));
                zoomFlag = 0;
            }
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        // Toast.makeText(getActivity(), "MyLocation button clicked",
        // Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default
        // behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onRegionTaskFinished(boolean currentRegionChanged) {
        if (currentRegionChanged
                && Application
                .getLastKnownLocation(this.getActivity(), mLocationHelper.getGoogleApiClient())
                == null) {
            // Move mMap view after a new region has been selected, if we don't have user location
            zoomToRegion();
        }

        // If region changed and was auto-selected, show user what region we're using
        if (currentRegionChanged
                && Application.getPrefs()
                .getBoolean(getString(R.string.preference_key_auto_select_region), true)
                && Application.get().getCurrentRegion() != null
                && UIUtils.canManageDialog(getActivity())) {
            Toast.makeText(getActivity(), getString(R.string.region_region_found,
                            Application.get().getCurrentRegion().getName()),
                    Toast.LENGTH_LONG
            ).show();
        }

    }

    void zoomToRegion() {
        // If we have a region, then zoom to it.
        ObaRegion region = Application.get().getCurrentRegion();

        if (region != null && mMap != null) {
            LatLngBounds b = MapHelpV2.getRegionBounds(region);
            int padding = 0;
            mMap.animateCamera((CameraUpdateFactory.newLatLngBounds(b, padding)));
        }
    }
}