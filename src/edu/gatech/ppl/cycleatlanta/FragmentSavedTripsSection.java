package edu.gatech.ppl.cycleatlanta;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class FragmentSavedTripsSection extends Fragment {

    public static final String ARG_SECTION_NUMBER = "section_number";

    ListView listSavedTrips;
    ActionMode mActionMode;
    ArrayList<Long> tripIdArray = new ArrayList<Long>();
    private MenuItem saveMenuItemDelete, saveMenuItemUpload;
    String[] values;

    Long storedID;

    Cursor allTrips;

    public SavedTripsAdapter sta;

    public FragmentSavedTripsSection() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_saved_trips, null);

        Log.v("Jason", "Cycle: SavedTrips onCreateView");

        setHasOptionsMenu(true);

        listSavedTrips = (ListView) rootView
                .findViewById(R.id.listViewSavedTrips);
        populateTripList(listSavedTrips);

        final DbAdapter mDb = new DbAdapter(getActivity());
        mDb.open();

        // Clean up any bad trips & coords from crashes
        int cleanedTrips = mDb.cleanTripsCoordsTables();
        if (cleanedTrips > 0) {
            Toast.makeText(getActivity(),
                    "" + cleanedTrips + " bad trip(s) removed.",
                    Toast.LENGTH_SHORT).show();
        }
        mDb.close();

        tripIdArray.clear();

        return rootView;
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.saved_trips_context_menu, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Log.v("Jason", "Prepare");
            saveMenuItemDelete = menu.getItem(0);
            saveMenuItemDelete.setEnabled(false);
            saveMenuItemUpload = menu.getItem(1);

            int flag = 1;
            for (int i = 0; i < listSavedTrips.getCount(); i++) {
                allTrips.moveToPosition(i);
                flag = flag
                        * (allTrips.getInt(allTrips.getColumnIndex("status")) - 1);
                if (flag == 0) {
                    storedID = allTrips.getLong(allTrips.getColumnIndex("_id"));
                    Log.v("Jason", "" + storedID);
                    break;
                }
            }
            if (flag == 1) {
                saveMenuItemUpload.setEnabled(false);
            } else {
                saveMenuItemUpload.setEnabled(true);
            }

            mode.setTitle(tripIdArray.size() + " Selected");
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete_saved_trips:
                    // delete selected trips
                    for (int i = 0; i < tripIdArray.size(); i++) {
                        deleteTrip(tripIdArray.get(i));
                    }
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.action_upload_saved_trips:
                    // upload selected trips
                    // for (int i = 0; i < tripIdArray.size(); i++) {
                    // retryTripUpload(tripIdArray.get(i));
                    // }
                    // Log.v("Jason", "" + storedID);
                    retryTripUpload(storedID);
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            tripIdArray.clear();
            for (int i = 0; i < listSavedTrips.getCount(); i++) {
                // Log.v("Jason", "Count" + listSavedTrips.getCount());
                // Log.v("Jason", "Count" + listSavedTrips.getChildCount());
                if (listSavedTrips.getChildCount() != 0) {
                    listSavedTrips.getChildAt(i).setBackgroundColor(
                            Color.parseColor("#80ffffff"));
                }
            }
        }
    };

    void populateTripList(ListView lv) {
        // Get list from the real phone database. W00t!
        final DbAdapter mDb = new DbAdapter(getActivity());
        mDb.open();

        try {
            allTrips = mDb.fetchAllTrips();

            String[] from = new String[]{"purp", "fancystart", "fancyinfo",
                    "endtime", "start", "distance", "status"};
            int[] to = new int[]{R.id.TextViewPurpose, R.id.TextViewStart,
                    R.id.TextViewInfo};

            sta = new SavedTripsAdapter(getActivity(),
                    R.layout.saved_trips_list_item, allTrips, from, to,
                    CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

            lv.setAdapter(sta);
        } catch (SQLException sqle) {
            // Do nothing, for now!
        }
        mDb.close();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int pos,
                                    long id) {
                allTrips.moveToPosition(pos);
                if (mActionMode == null) {
                    if (allTrips.getInt(allTrips.getColumnIndex("status")) == 2) {
                        Intent i = new Intent(getActivity(),
                                TripMapActivity.class);
                        i.putExtra("showtrip", id);
                        startActivity(i);
                    } else if (allTrips.getInt(allTrips
                            .getColumnIndex("status")) == 1) {
                        // Toast.makeText(getActivity(), "Unsent",
                        // Toast.LENGTH_SHORT).show();
                        buildAlertMessageUnuploadedTripClicked(id);

                        // Log.v("Jason",
                        // ""+allTrips.getLong(allTrips.getColumnIndex("_id")));
                    }

                } else {
                    // highlight
                    if (tripIdArray.indexOf(id) > -1) {
                        tripIdArray.remove(id);
                        v.setBackgroundColor(Color.parseColor("#80ffffff"));
                    } else {
                        tripIdArray.add(id);
                        v.setBackgroundColor(Color.parseColor("#ff33b5e5"));
                    }
                    // Toast.makeText(getActivity(), "Selected: " + tripIdArray,
                    // Toast.LENGTH_SHORT).show();
                    if (tripIdArray.size() == 0) {
                        saveMenuItemDelete.setEnabled(false);
                    } else {
                        saveMenuItemDelete.setEnabled(true);
                    }

                    mActionMode.setTitle(tripIdArray.size() + " Selected");
                }
            }
        });

        registerForContextMenu(lv);
    }

    private void buildAlertMessageUnuploadedTripClicked(final long position) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity());
        builder.setTitle("Upload Trip");
        builder.setMessage("Do you want to upload this trip?");
        builder.setNegativeButton("Upload",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        retryTripUpload(position);
                        // Toast.makeText(getActivity(),"Send Clicked: "+position,
                        // Toast.LENGTH_SHORT).show();
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

    private void retryTripUpload(long tripId) {
        TripUploader uploader = new TripUploader(getActivity());
        FragmentSavedTripsSection f2 = (FragmentSavedTripsSection) getActivity()
                .getSupportFragmentManager().findFragmentByTag(
                        "android:switcher:" + R.id.pager + ":1");
        uploader.setSavedTripsAdapter(sta);
        uploader.setFragmentSavedTripsSection(f2);
        uploader.setListView(listSavedTrips);
        uploader.execute();
    }

    private void deleteTrip(long tripId) {
        DbAdapter mDbHelper = new DbAdapter(getActivity());
        mDbHelper.open();
        mDbHelper.deleteAllCoordsForTrip(tripId);
        mDbHelper.deleteTrip(tripId);
        mDbHelper.close();
        listSavedTrips.invalidate();
        populateTripList(listSavedTrips);
    }

    // show edit button and hidden delete button
    @Override
    public void onResume() {
        super.onResume();
        Log.v("Jason", "Cycle: SavedTrips onResume");
        populateTripList(listSavedTrips);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v("Jason", "Cycle: SavedTrips onPause");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.v("Jason", "Cycle: SavedTrips onDestroyView");
    }

    /* Creates the menu items */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.saved_trips, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /* Handles item selections */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_edit_saved_trips:
                // edit
                if (mActionMode != null) {
                    return false;
                }

                // Start the CAB using the ActionMode.Callback defined above
                mActionMode = getActivity().startActionMode(mActionModeCallback);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
