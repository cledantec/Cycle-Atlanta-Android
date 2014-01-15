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

package org.phillyopen.mytracks.cyclephilly;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class TripData {
	long tripid;
	double startTime = 0;
	double endTime = 0;
	int numpoints = 0;
	double lathigh, lgthigh, latlow, lgtlow, latestlat, latestlgt;
	int status;
	float distance;
	String purp, fancystart, info;

	List<LatLng> myPts;
	List<String> myTimesAcc;
	CyclePoint startpoint, endpoint;

	double totalPauseTime = 0;
	double pauseStartedAt = 0;

	DbAdapter mDb;

    public static int STATUS_INCOMPLETE = 0;
    public static int STATUS_COMPLETE = 1;
    public static int STATUS_SENT = 2;

	public static TripData createTrip(Context c) {
		TripData t = new TripData(c.getApplicationContext(), 0);
		t.createTripInDatabase(c);
        t.initializeData();
		return t;
	}

	public static TripData fetchTrip(Context c, long tripid) {
		TripData t = new TripData(c.getApplicationContext(), tripid);
		t.populateDetails();
		return t;
	}

	public TripData (Context ctx, long tripid) {
		Context context = ctx.getApplicationContext();
		this.tripid = tripid;
		mDb = new DbAdapter(context);
	}

	void initializeData() {
		startTime = System.currentTimeMillis();
		endTime = System.currentTimeMillis();
        numpoints = 0;
        latestlat = 800; latestlgt = 800;
        distance = 0;

        lathigh = (int) (-100 * 1E6);
		latlow = (int) (100 * 1E6);
		lgtlow = (int) (180 * 1E6);
		lgthigh = (int) (-180 * 1E6);
		purp = fancystart = info = "";
		myPts = new ArrayList<LatLng>();
		myTimesAcc = new ArrayList<String>();

		updateTrip();
	}

    // Get lat/long extremes, etc, from trip record
	void populateDetails() {

	    mDb.openReadOnly();

	    Cursor tripdetails = mDb.fetchTrip(tripid);
	    startTime = tripdetails.getDouble(tripdetails.getColumnIndex("start"));
	    lathigh = tripdetails.getDouble(tripdetails.getColumnIndex("lathi"));
	    latlow =  tripdetails.getDouble(tripdetails.getColumnIndex("latlo"));
	    lgthigh = tripdetails.getDouble(tripdetails.getColumnIndex("lgthi"));
	    lgtlow =  tripdetails.getDouble(tripdetails.getColumnIndex("lgtlo"));
	    status =  tripdetails.getInt(tripdetails.getColumnIndex("status"));
	    endTime = tripdetails.getDouble(tripdetails.getColumnIndex("endtime"));
	    distance = tripdetails.getFloat(tripdetails.getColumnIndex("distance"));

	    purp = tripdetails.getString(tripdetails.getColumnIndex("purp"));
	    fancystart = tripdetails.getString(tripdetails.getColumnIndex("fancystart"));
	    info = tripdetails.getString(tripdetails.getColumnIndex("fancyinfo"));

	    tripdetails.close();

		Cursor points = mDb.fetchAllCoordsForTrip(tripid);
		if (points != null) {
	        numpoints = points.getCount();
	        points.close();
		}

	    mDb.close();
	}

	void createTripInDatabase(Context c) {
		mDb.open();
		tripid = mDb.createTrip();
		mDb.close();
	}

	void dropTrip() {
	    mDb.open();
		mDb.deleteAllCoordsForTrip(tripid);
		mDb.deleteTrip(tripid);
		mDb.close();
	}
	
	private void readPoints() {
		try {
			mDb.openReadOnly();

			Cursor points = mDb.fetchAllCoordsForTrip(tripid);
            int COL_LAT = points.getColumnIndex("lat");
            int COL_LGT = points.getColumnIndex("lgt");
            int COL_TIME = points.getColumnIndex("time");
            int COL_ACC  = points.getColumnIndex(DbAdapter.K_POINT_ACC);

            numpoints = points.getCount();
            myPts = new ArrayList<LatLng>(numpoints);
            myTimesAcc = new ArrayList<String>(numpoints);

            points.moveToLast();
            this.endpoint   = new CyclePoint(points.getDouble(COL_LAT), points.getDouble(COL_LGT), points.getDouble(COL_TIME));

            points.moveToFirst();
            this.startpoint = new CyclePoint(points.getDouble(COL_LAT), points.getDouble(COL_LGT), points.getDouble(COL_TIME));

			while (!points.isAfterLast()) {
                double lat = points.getDouble(COL_LAT);
                double lgt = points.getDouble(COL_LGT);
                double time = points.getDouble(COL_TIME);
                float acc = (float) points.getDouble(COL_ACC);
                
                myPts.add(new LatLng(lat, lgt));
                myTimesAcc.add(DateFormat.getInstance().format(time) + '\n' + String.format("%1.1f mph", acc));
                
				points.moveToNext();
			}
			points.close();
			mDb.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<LatLng> getPoints() {
		// If already built, don't build again!
		if (myPts != null && myPts.size() > 0) {
			return myPts;
		}

		// Otherwise, we need to query DB and build points from scratch.
		readPoints();
		return myPts;
	}
	
	public List<String> getTimesAcc() {
		// If already built, don't build again!
		if (myTimesAcc != null && myTimesAcc.size() > 0) {
			return myTimesAcc;
		}

		// Otherwise, we need to query DB and build points from scratch.
		readPoints();
		return myTimesAcc;
	}

	boolean addPointNow(Location loc, double currentTime, float dst) {
		double lat = loc.getLatitude();
		double lgt = loc.getLongitude();

		// Skip duplicates
		if (latestlat == lat && latestlgt == lgt)
			return true;

		float accuracy = loc.getAccuracy();
		double altitude = loc.getAltitude();
		float speed = loc.getSpeed();

		CyclePoint pt = new CyclePoint(lat, lgt, currentTime, accuracy,
				altitude, speed);

        numpoints++;
        endTime = currentTime - this.totalPauseTime;
        distance = dst;

		latlow = Math.min(latlow, lat);
		lathigh = Math.max(lathigh, lat);
		lgtlow = Math.min(lgtlow, lgt);
		lgthigh = Math.max(lgthigh, lgt);

		latestlat = lat;
		latestlgt = lgt;

        mDb.open();
        boolean rtn = mDb.addCoordToTrip(tripid, pt);
        rtn = rtn && mDb.updateTrip(tripid, "", startTime, "", "", "",
                lathigh, latlow, lgthigh, lgtlow, distance);
        mDb.close();

        return rtn;
	}

	public boolean updateTripStatus(int tripStatus) {
		boolean rtn;
		mDb.open();
		rtn = mDb.updateTripStatus(tripid, tripStatus);
		mDb.close();
		return rtn;
	}

	public boolean getStatus(int tripStatus) {
		boolean rtn;
		mDb.open();
		rtn = mDb.updateTripStatus(tripid, tripStatus);
		mDb.close();
		return rtn;
	}

	public void updateTrip() { updateTrip("","","",""); }
	public void updateTrip(String purpose, String fancyStart, String fancyInfo, String notes) {
		// Save the trip details to the phone database. W00t!
		mDb.open();
		mDb.updateTrip(tripid, purpose,	startTime, fancyStart, fancyInfo, notes,
				lathigh, latlow, lgthigh, lgtlow, distance);
		mDb.close();
	}
}

