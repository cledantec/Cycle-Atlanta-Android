/**  Cycle Atlanta, Copyright 2014 Georgia Institute of Technology
 *                                    Atlanta, GA. USA
 *
 *   @author Christopher Le Dantec <ledantec@gatech.edu>
 *   @author Anhong Guo <guoanhong@gatech.edu>
 *
 *   Updated/Modified for Atlanta's app deployment. Based on the
 *   CycleTracks codebase for SFCTA.
 *
 *   CycleTracks, Copyright 2009,2010 San Francisco County Transportation Authority
 *                                    San Francisco, CA, USA
 *
 *   @author Billy Charlton <billy.charlton@sfcta.org>
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
//
package edu.gatech.ppl.cycleatlanta;

import java.util.ArrayList;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class TripMapActivity extends Activity {
	// private MapView mapView;
	GoogleMap map;
	// List<Overlay> mapOverlays;
	// xwDrawable drawable;
	ArrayList<CyclePoint> gpspoints;
	// float[] lineCoords;
	Polyline polyline;

	private LatLngBounds.Builder bounds;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_trip_map);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Toast.makeText(this, "trip mMap", Toast.LENGTH_LONG).show();

		try {
			// Set zoom controls
			map = ((MapFragment) getFragmentManager().findFragmentById(
					R.id.tripMap)).getMap();
			// mapView = (MapView) findViewById(R.id.tripMap);
			// mapView.setBuiltInZoomControls(true);

			// // Set up the point layer
			// mapOverlays = mapView.getOverlays();
			// if (mapOverlays != null) mapOverlays.clear();

			Bundle cmds = getIntent().getExtras();
			long tripid = cmds.getLong("showtrip");

			TripData trip = TripData.fetchTrip(this, tripid);

			// Show trip details
			TextView t1 = (TextView) findViewById(R.id.TextViewMapPurpose);
			TextView t2 = (TextView) findViewById(R.id.TextViewMapInfo);
			TextView t3 = (TextView) findViewById(R.id.TextViewMapFancyStart);
			t1.setText(trip.purp);
			t2.setText(trip.info);
			t3.setText(trip.fancystart);

			// Center & zoom the mMap
			// int latcenter = (trip.lathigh + trip.latlow) / 2;
			// int lgtcenter = (trip.lgthigh + trip.lgtlow) / 2;
			// LatLng center = new LatLng(latcenter, lgtcenter);

			// mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center,16));

			// trip = trips[0]; // always get just the first trip

			gpspoints = trip.getPoints();

			Log.v("Jason", gpspoints.toString());

			Log.v("Jason", String.valueOf(trip.startpoint.latitude * 1E-6));
			Log.v("Jason", String.valueOf(trip.startpoint.longitude * 1E-6));
			Log.v("Jason", String.valueOf(trip.endpoint.latitude * 1E-6));
			Log.v("Jason", String.valueOf(trip.endpoint.longitude * 1E-6));

			if (trip.startpoint != null) {
				map.addMarker(new MarkerOptions()
						.icon(BitmapDescriptorFactory
								.fromResource(R.drawable.pingreen))
						.anchor(0.0f, 1.0f) // Anchors the marker on the bottom
											// left
						.position(
								new LatLng(trip.startpoint.latitude * 1E-6,
										trip.startpoint.longitude * 1E-6)));

				// mapOverlays.add(new PushPinOverlay(trip.startpoint,
				// R.drawable.pingreen));
			}
			if (trip.endpoint != null) {
				map.addMarker(new MarkerOptions()
						.icon(BitmapDescriptorFactory
								.fromResource(R.drawable.pinpurple))
						.anchor(0.0f, 1.0f) // Anchors the marker on the bottom
											// left
						.position(
								new LatLng(trip.endpoint.latitude * 1E-6,
										trip.endpoint.longitude * 1E-6)));

				// mapOverlays.add(new PushPinOverlay(trip.endpoint,
				// R.drawable.pinpurple));
			}

			bounds = new LatLngBounds.Builder();

			PolylineOptions rectOptions = new PolylineOptions();
			rectOptions.geodesic(true).color(Color.BLUE);

			Log.v("Jason", String.valueOf(gpspoints.size()));

			// //startpoint
			// mMap.addMarker(new MarkerOptions()
			// .icon(BitmapDescriptorFactory.fromResource(R.drawable.pingreen))
			// .anchor(0.0f, 1.0f) // Anchors the marker on the bottom left
			// .position(new LatLng(gpspoints.get(0).latitude*1E-6,
			// gpspoints.get(0).longitude*1E-6)));
			//
			// //endpoint
			// mMap.addMarker(new MarkerOptions()
			// .icon(BitmapDescriptorFactory.fromResource(R.drawable.pinpurple))
			// .anchor(0.0f, 1.0f) // Anchors the marker on the bottom left
			// .position(new
			// LatLng(gpspoints.get(gpspoints.size()-1).latitude*1E-6,
			// gpspoints.get(gpspoints.size()-1).longitude*1E-6)));

			for (int i = 0; i < gpspoints.size(); i++) {
				LatLng point = new LatLng(gpspoints.get(i).latitude * 1E-6,
						gpspoints.get(i).longitude * 1E-6);
				// Log.v("Jason",String.valueOf(gpspoints.get(i).latitude*1E-6));
				// Log.v("Jason",String.valueOf(gpspoints.get(i).longitude*1E-6));
				bounds.include(point);
				rectOptions.add(point);
			}

			polyline = map.addPolyline(rectOptions);

			// mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(),
			// 480, 320, 10));

			map.setOnCameraChangeListener(new OnCameraChangeListener() {

				@Override
				public void onCameraChange(CameraPosition arg0) {
					// Move camera.
					map.moveCamera(CameraUpdateFactory.newLatLngBounds(
							bounds.build(), 50));
					// Remove listener to prevent position reset on camera move.
					map.setOnCameraChangeListener(null);
				}
			});

			// MapController mc = mapView.getController();
			// mc.animateTo(center);
			// Add 500 to mMap span, to guarantee pins fit on mMap
			// mc.zoomToSpan(500+trip.lathigh - trip.latlow, 500+trip.lgthigh -
			// trip.lgtlow);

			// if (gpspoints == null) {
			// AddPointsToMapLayerTask maptask = new AddPointsToMapLayerTask();
			// maptask.execute(trip);
			// } else {
			// mapOverlays.add(gpspoints);
			// }

			if (trip.status < TripData.STATUS_SENT && cmds != null
					&& cmds.getBoolean("uploadTrip", false)) {
				// And upload to the cloud database, too! W00t W00t!
				TripUploader uploader = new TripUploader(TripMapActivity.this);
				uploader.execute(trip.tripid);
			}

		} catch (Exception e) {
			Log.e("GOT!", e.toString());
		}
	}

	// @Override
	// protected boolean isRouteDisplayed() {
	// // Auto-generated method stub
	// return false;
	// }

	// Make sure overlays get zapped when we go BACK
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && map != null) {
			// mMap.getOverlays().clear();
			polyline.remove();
		}
		return super.onKeyDown(keyCode, event);
	}

	/* Creates the menu items */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.trip_map, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_close_trip_map:
			// close -> go back to FragmentMainInput
			if (map != null) {
				polyline.remove();
			}

			onBackPressed();

			// Intent i = new Intent(TripMapActivity.this, TabsConfig.class);
			// //i.putExtra("keepme", true);
			// startActivity(i);
			// overridePendingTransition(android.R.anim.fade_in,
			// R.anim.slide_out_down);
			// TripMapActivity.this.finish();

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// private class AddPointsToMapLayerTask extends AsyncTask <TripData,
	// Integer, ArrayList<CyclePoint>> {
	// TripData trip;
	//
	// @Override
	// protected ArrayList<CyclePoint> doInBackground(TripData... trips) {
	// trip = trips[0]; // always get just the first trip
	//
	// //drawable = getResources().getDrawable(R.drawable.point);
	// TripMapActivity.this.gpspoints = trip.getPoints();
	//
	// return TripMapActivity.this.gpspoints;
	// }
	//
	// @Override
	// protected void onPostExecute(ArrayList<CyclePoint> gpspoints) {
	// // Add the points
	// mapOverlays.add(ShowMap.this.gpspoints);
	//
	// // Add the lines! W00t!
	// mapOverlays.add(new LineOverlay(ShowMap.this.gpspoints));
	//
	// // Add start & end pins
	// if (trip.startpoint != null) {
	// mapOverlays.add(new PushPinOverlay(trip.startpoint,
	// R.drawable.pingreen));
	// }
	// if (trip.endpoint != null) {
	// mapOverlays.add(new PushPinOverlay(trip.endpoint, R.drawable.pinpurple));
	// }
	//
	// // Redraw the mMap
	// mapView.invalidate();
	// }
	// }

	// class LineOverlay extends com.google.android.maps.Overlay
	// {
	// ArrayList<CyclePoint> track;
	//
	// public LineOverlay(ArrayList<CyclePoint> track) {
	// super();
	// this.track = track;
	// }
	//
	// @Override
	// public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long
	// when) {
	// super.draw(canvas, mapView, shadow);
	//
	// // Need at least two points to draw a line, duh
	// if (track.size()<2) return true;
	//
	// // Build array of points
	// float[] points = new float[4 * track.size()];
	// int segments = 0;
	// int startx = -1; int starty = -1;
	//
	// for (int i=0; i<track.size(); i++) {
	// CyclePoint z = (CyclePoint) track.get(i);
	//
	// // Skip lousy points
	// if (z.accuracy > 8) {
	// startx = -1;
	// continue;
	// }
	//
	// // If this is the beginning of a new segment, great
	// Point screenPoint = new Point();
	// mapView.getProjection().toPixels(z, screenPoint);
	//
	// if (startx == -1) {
	// startx = screenPoint.x;
	// starty = screenPoint.y;
	// continue;
	// }
	// int numpts = segments*4;
	// points[numpts] = startx;
	// points[numpts+1] = starty;
	// points[numpts+2] = startx = screenPoint.x;
	// points[numpts+3] = starty = screenPoint.y;
	// segments++;
	// }
	//
	// // Line style
	// Paint paint = new Paint();
	// paint.setARGB(255,0,0,255);
	// paint.setStrokeWidth(5);
	// paint.setStyle(Style.FILL_AND_STROKE);
	//
	// canvas.drawLines(points, 0, segments*4, paint);
	// return false;
	// }
	// }
	//
	// class PushPinOverlay extends com.google.android.maps.Overlay
	// {
	// LatLng p;
	// int d;
	//
	// public PushPinOverlay(LatLng p, int drawrsrc) {
	// super();
	// this.p=p;
	// this.d=drawrsrc;
	// }
	//
	// @Override
	// public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long
	// when) {
	// super.draw(canvas, mapView, shadow);
	//
	// //---translate the GeoPoint to screen pixels---
	// Point screenPoint = new Point();
	// mapView.getProjection().toPixels(p, screenPoint);
	//
	// //---add the marker---
	// Bitmap bmp = BitmapFactory.decodeResource(getResources(), d);
	// int height = bmp.getScaledHeight(canvas);
	// int width = (int)(0.133333 * bmp.getScaledWidth(canvas)); // 4/30 pixels:
	// how far right we want the pushpin
	//
	// canvas.drawBitmap(bmp, screenPoint.x-width, screenPoint.y-height, null);
	// return true;
	// }
	// }
}
