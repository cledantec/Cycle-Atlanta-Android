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

package edu.gatech.ppl.cycleatlanta;

import java.util.HashMap;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TripPurposeActivity extends Activity {
	// HashMap<Integer, ToggleButton> purpButtons = new HashMap<Integer,
	// ToggleButton>();
	String purpose = "";

	HashMap<Integer, String> purpDescriptions = new HashMap<Integer, String>();

	String[] values;

	private MenuItem saveMenuItem;

	// Set up the purpose buttons to be one-click only
	void preparePurposeButtons() {
		purpDescriptions
				.put(0,
						"The primary reason for this bike trip is to get between home and your primary work location.");
		purpDescriptions
				.put(1,
						"The primary reason for this bike trip is to go to or from school or college.");
		purpDescriptions
				.put(2,
						"The primary reason for this bike trip is to go to or from business-related meeting, function, or work-related errand for your job.");
		purpDescriptions
				.put(3,
						"The primary reason for this bike trip is exercise or biking for the sake of biking.");
		purpDescriptions
				.put(4,
						"The primary reason for this bike trip is going to or from a social activity (e.g. at a friend's house, the park, a restaurant, the movies).");
		purpDescriptions
				.put(5,
						"The primary reason for this bike trip is to purchase or bring home goods or groceries.");
		purpDescriptions
				.put(6,
						"The primary reason for this bike trip is to attend to personal business such as banking, doctor visit, going to the gym, etc.");
		purpDescriptions
				.put(7,
						"If none of the other reasons apply to this trip, you can enter trip comments after saving your trip to tell us more.");
	}

	// CheckListener cl = new CheckListener();
	// for (Entry<Integer, ToggleButton> e : purpButtons.entrySet()) {
	// e.getValue().setOnCheckedChangeListener(cl);
	// }

	// // Called every time a purp togglebutton is changed:
	// class CheckListener implements CompoundButton.OnCheckedChangeListener {
	// @Override
	// public void onCheckedChanged(CompoundButton v, boolean isChecked) {
	// // First, uncheck all purp buttons
	// if (isChecked) {
	// for (Entry<Integer, ToggleButton> e : purpButtons.entrySet()) {
	// e.getValue().setChecked(false);
	// }
	// v.setChecked(true);
	// purpose = v.getText().toString();
	// ((TextView) findViewById(R.id.textViewTripPurposeDesc))
	// .setText(Html.fromHtml(purpDescriptions.get(v.getId())));
	// }
	// }
	// }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_trip_purpose);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Set up trip purpose buttons
		purpose = "";
		preparePurposeButtons();

		final ListView listView = (ListView) findViewById(R.id.listViewTripPurpose);
		values = new String[] { "Commute", "School", "Work-Related",
				"Exercise", "Social", "Shopping", "Errand", "Other" };
		// final ArrayList<String> list = new ArrayList<String>();
		// for (int i = 0; i < values.length; ++i) {
		// list.add(values[i]);
		// }
		TripPurposeAdapter adapter = new TripPurposeAdapter(this, values);
		listView.setAdapter(adapter);
		// set default
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			private View oldSelection = null;

			public void clearSelection() {
				if (oldSelection != null) {
					oldSelection.setBackgroundColor(Color.parseColor("#ffffff"));
				}
			}

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				clearSelection();
				oldSelection = view;
				view.setBackgroundColor(Color.parseColor("#ff33b5e5"));
				// view.setBackgroundDrawable(parent.getResources().getDrawable(R.drawable.bg_key));
				purpose = values[position];
				// Log.v("Jason", purpose);
				((TextView) findViewById(R.id.textViewTripPurposeDesc))
						.setText(Html.fromHtml(purpDescriptions.get(position)));
				saveMenuItem.setEnabled(true);
				// highlight
			}

		});

		// // User prefs btn
		// final Button prefsButton = (Button) findViewById(R.id.ButtonPrefs);
		// final Intent pi = new Intent(this, UserInfoActivity.class);
		// prefsButton.setOnClickListener(new View.OnClickListener() {
		// public void onClick(View v) {
		// startActivity(pi);
		// }
		// });
		//
		// SharedPreferences settings = getSharedPreferences("PREFS", 0);
		// if (settings.getAll().size() >= 1) {
		// prefsButton.setVisibility(View.GONE);
		// }

		// // Discard btn - move to action bar
		// final Button btnDiscard = (Button) findViewById(R.id.ButtonDiscard);
		// btnDiscard.setOnClickListener(new View.OnClickListener() {
		// public void onClick(View v) {
		// Toast.makeText(getBaseContext(), "Trip discarded.",
		// Toast.LENGTH_SHORT).show();
		//
		// cancelRecording();
		//
		// Intent i = new Intent(TripPurposeActivity.this,
		// MainInputActivity.class);
		// i.putExtra("keepme", true);
		// startActivity(i);
		// TripPurposeActivity.this.finish();
		// }
		// });

		// // Submit btn
		// final Button btnSubmit = (Button) findViewById(R.id.ButtonSubmit);
		// btnSubmit.setEnabled(false);

		// Don't pop up the soft keyboard until user clicks!
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	void cancelRecording() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {
			}

			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				rs.cancelRecording();
				unbindService(this);
			}
		};
		// This should block until the onServiceConnected (above) completes.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}

	/* Creates the menu items */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.trip_purpose, menu);
		saveMenuItem = menu.getItem(1);
		saveMenuItem.setEnabled(false);
		return super.onCreateOptionsMenu(menu);
	}

	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_cancel_trip_purpose:
			Toast.makeText(getBaseContext(), "Trip discarded.",
					Toast.LENGTH_SHORT).show();

			cancelRecording();

			Intent i = new Intent(TripPurposeActivity.this, TabsConfig.class);
			i.putExtra("keepme", true);
			startActivity(i);
			overridePendingTransition(android.R.anim.slide_in_left,
					android.R.anim.slide_out_right);
			TripPurposeActivity.this.finish();
			return true;
		case R.id.action_save_trip_purpose:
			// move to next view
			// send purpose with intent
			Intent intentToTripDetail = new Intent(TripPurposeActivity.this,
					TripDetailActivity.class);
			intentToTripDetail.putExtra("purpose", purpose);
			startActivity(intentToTripDetail);
			overridePendingTransition(R.anim.slide_in_right,
					R.anim.slide_out_left);
			TripPurposeActivity.this.finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// 2.0 and above
	@Override
	public void onBackPressed() {
		Toast.makeText(getBaseContext(), "Trip discarded.", Toast.LENGTH_SHORT)
				.show();

		cancelRecording();

		Intent i = new Intent(TripPurposeActivity.this, TabsConfig.class);
		i.putExtra("keepme", true);
		startActivity(i);
		overridePendingTransition(android.R.anim.slide_in_left,
				android.R.anim.slide_out_right);
		TripPurposeActivity.this.finish();
	}

	// Before 2.0
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Toast.makeText(getBaseContext(), "Trip discarded.",
					Toast.LENGTH_SHORT).show();

			cancelRecording();

			Intent i = new Intent(TripPurposeActivity.this, TabsConfig.class);
			i.putExtra("keepme", true);
			startActivity(i);
			overridePendingTransition(android.R.anim.slide_in_left,
					android.R.anim.slide_out_right);
			TripPurposeActivity.this.finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
