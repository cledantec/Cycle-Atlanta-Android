/**  Cycle Altanta, Copyright 2012 Georgia Institute of Technology
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
package org.phillyopen.mytracks.cyclephilly;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.phillyopen.mytracks.cyclephilly.R;

public class ShowMap extends FragmentActivity {
	List<Polyline> mapTracks;
	Drawable drawable;
	Polyline gpspoints;
	PolylineOptions gpsoptions;
	ArrayList<MarkerOptions> markers;
	float[] lineCoords;

	private GoogleMap mMap;
	private LinearLayout layout;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapview);

		try {
			if (mapTracks != null) {
				mapTracks.clear();
			} else {
				mapTracks = new ArrayList<Polyline>();
			}

	        Bundle cmds = getIntent().getExtras();
            long tripid = cmds.getLong("showtrip");
            TripData trip = TripData.fetchTrip(this, tripid);

            // map bounds
            final LatLngBounds bounds = new LatLngBounds.Builder()
            	.include(new LatLng(trip.lathigh, trip.lgtlow))
            	.include(new LatLng(trip.latlow, trip.lgthigh))
            	.build();

			// check if already instantiated
			if (mMap == null) {
				mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
				layout = (LinearLayout)findViewById(R.id.LinearLayout01);
				ViewTreeObserver vto = layout.getViewTreeObserver();
				vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
					@Override
				    public void onGlobalLayout() {
				        layout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				        // Center & zoom the map after map layout completes
				        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 5));
				    }
				});
			} else {
				mMap.clear();
			}

			// check if got map
			if (mMap == null) {
				Log.d("Couldn't get map fragment!", "No map fragment");
				return;
			}
			
			// customize info window
			mMap.setInfoWindowAdapter(new BikeRackInfoWindow(getLayoutInflater()));

            // Show trip details
            TextView t1 = (TextView) findViewById(R.id.TextViewT1);
            TextView t2 = (TextView) findViewById(R.id.TextViewT2);
            TextView t3 = (TextView) findViewById(R.id.TextViewT3);
            t1.setText(trip.purp);
            t2.setText(trip.info);
            t3.setText(trip.fancystart);

			if (gpspoints == null) {
				AddPointsToMapLayerTask maptask = new AddPointsToMapLayerTask();
				maptask.execute(trip);
			} else {
				mapTracks.add(gpspoints);
			}

			if (trip.status < TripData.STATUS_SENT
					&& cmds != null
					&& cmds.getBoolean("uploadTrip", false)) {
			    // And upload to the cloud database, too!  W00t W00t!
                TripUploader uploader = new TripUploader(ShowMap.this);
                uploader.execute(trip.tripid);
			}

		} catch (Exception e) {
			Log.d("Map error",e.toString(), e);
		}
	}

	private class AddPointsToMapLayerTask extends AsyncTask<TripData, Integer, PolylineOptions> {
		TripData trip;

		@Override
		protected PolylineOptions doInBackground(TripData... trips) {
			trip = trips[0]; // always get just the first trip

			List<LatLng> pos = trip.getPoints();
			ShowMap.this.gpsoptions = new PolylineOptions();
			ShowMap.this.gpsoptions.addAll(pos);
			ShowMap.this.markers = new ArrayList<MarkerOptions>(pos.size());
			List<String> timesAcc = trip.getTimesAcc();
			
			// use custom icon for trip points
			Bitmap bmp = Bitmap.createBitmap(2, 2, Bitmap.Config.ALPHA_8);
			
			for (int i = timesAcc.size(); i-->0;) {
				markers.add(new MarkerOptions()
					.position(pos.get(i))
					.title("trip point")
					.snippet(timesAcc.get(i))
					.icon(BitmapDescriptorFactory.fromBitmap(bmp)));
			}
			
			return ShowMap.this.gpsoptions;
		}

		@Override
		protected void onPostExecute(PolylineOptions opts) {
			// Add the polylines
			ShowMap.this.gpspoints = ShowMap.this.mMap.addPolyline(opts);
			mapTracks.add(ShowMap.this.gpspoints);

			// Add start & end markers
			if (trip.startpoint != null) {
				mMap.addMarker(new MarkerOptions()
						.position(trip.startpoint.coords)
						.title("start")
						.snippet(trip.fancystart)
						.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
			}
			if (trip.endpoint != null) {
				mMap.addMarker(new MarkerOptions()
						.position(trip.endpoint.coords)
						.title("end")
						.snippet(DateFormat.getInstance().format(trip.endTime))
						.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
			}
			if (ShowMap.this.markers != null & ShowMap.this.markers.size() > 0) {
				for (int i = ShowMap.this.markers.size(); i-->0;) {
					mMap.addMarker(ShowMap.this.markers.get(i));
				}
			}
		}
	}
}
