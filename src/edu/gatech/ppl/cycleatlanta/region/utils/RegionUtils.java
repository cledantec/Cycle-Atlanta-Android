/*
 * Copyright (C) 2012-2013 Paul Watts (paulcwatts@gmail.com) 
 * and individual contributors
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
 */
package edu.gatech.ppl.cycleatlanta.region.utils;

import com.google.android.gms.maps.model.LatLng;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import edu.gatech.ppl.cycleatlanta.Application;
import edu.gatech.ppl.cycleatlanta.BuildConfig;
import edu.gatech.ppl.cycleatlanta.R;
import edu.gatech.ppl.cycleatlanta.provider.ObaContract;
import edu.gatech.ppl.cycleatlanta.region.ObaRegionsRequest;
import edu.gatech.ppl.cycleatlanta.region.ObaRegionsResponse;
import edu.gatech.ppl.cycleatlanta.region.elements.ObaRegion;
import edu.gatech.ppl.cycleatlanta.region.elements.ObaRegionElement;

/**
 * A class containing utility methods related to handling multiple regions in OneBusAway
 */
public class RegionUtils {

    private static final String TAG = "RegionUtils";

    public static final double METERS_TO_MILES = 0.000621371;

    private static final int DISTANCE_LIMITER = 100;  // miles

    /**
     * Get the closest region from a list of regions and a given location
     *
     * This method also enforces the constraints in isRegionUsable() to
     * ensure the returned region is actually usable by the app
     *
     * @param regions          list of regions
     * @param loc              location
     * @param enforceThreshold true if the DISTANCE_LIMITER threshold should be enforced, false if
     *                         it should not
     * @return the closest region to the given location from the list of regions, or null if a
     * enforceThreshold is true and the closest region exceeded DISTANCE_LIMITER threshold or a
     * region couldn't be found
     */
    public static ObaRegion getClosestRegion(ArrayList<ObaRegion> regions, Location loc,
                                             boolean enforceThreshold) {
        if (loc == null) {
            return null;
        }
        float minDist = Float.MAX_VALUE;
        ObaRegion closestRegion = null;
        Float distToRegion;

        NumberFormat fmt = NumberFormat.getInstance();
        if (fmt instanceof DecimalFormat) {
            ((DecimalFormat) fmt).setMaximumFractionDigits(1);
        }
        double miles;

        Log.d(TAG, "Finding region closest to " + loc.getLatitude() + "," + loc.getLongitude());

        for (ObaRegion region : regions) {
            if (!isRegionUsable(region)) {
                Log.d(TAG,
                        "Excluding '" + region.getName() + "' from 'closest region' consideration");
                continue;
            }

            distToRegion = getDistanceAway(region, loc.getLatitude(), loc.getLongitude());
            if (distToRegion == null) {
                Log.e(TAG, "Couldn't measure distance to region '" + region.getName() + "'");
                continue;
            }
            miles = distToRegion * METERS_TO_MILES;
            Log.d(TAG, "Region '" + region.getName() + "' is " + fmt.format(miles) + " miles away");
            if (distToRegion < minDist) {
                closestRegion = region;
                minDist = distToRegion;
            }
        }

        if (enforceThreshold) {
            if (minDist * METERS_TO_MILES < DISTANCE_LIMITER) {
                return closestRegion;
            } else {
                return null;
            }
        }
        return closestRegion;
    }

    /**
     * Returns the distance from the specified location
     * to the center of the closest bound in this region.
     *
     * @return distance from the specified location to the center of the closest bound in this
     * region, in meters
     */
    public static Float getDistanceAway(ObaRegion region, double lat, double lon) {
        ObaRegion.Bounds[] bounds = region.getBounds();
        if (bounds == null) {
            return null;
        }
        float[] results = new float[1];
        float minDistance = Float.MAX_VALUE;
        for (ObaRegion.Bounds bound : bounds) {

            LatLng midpoint = midPoint(bound.getLowerLeftLatitude(), bound.getLowerLeftLongitude(),
                    bound.getUpperRightLatitude(), bound.getUpperRightLongitude());
            Location.distanceBetween(lat, lon, midpoint.latitude, midpoint.longitude, results);
            if (results[0] < minDistance) {
                minDistance = results[0];
            }
        }
        return minDistance;
    }

    public static Float getDistanceAway(ObaRegion region, Location loc) {
        return getDistanceAway(region, loc.getLatitude(), loc.getLongitude());
    }

    public static LatLng midPoint(double lat1,double lon1,double lat2,double lon2){

        double dLon = Math.toRadians(lon2 - lon1);

        //convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        lon1 = Math.toRadians(lon1);

        double Bx = Math.cos(lat2) * Math.cos(dLon);
        double By = Math.cos(lat2) * Math.sin(dLon);
        double lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) *
                (Math.cos(lat1) + Bx) + By * By));
        double lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);

        return new LatLng(Math.toDegrees(lat3), Math.toDegrees(lon3));
    }


    /**
     * Checks if the given region is usable by the app, based on what this app supports
     * - Is the region active?
     * - Does the region support the OBA Discovery APIs?
     * - Does the region support the OBA Realtime APIs?
     * - Is the region experimental, and if so, did the user opt-in via preferences?
     *
     * @param region region to be checked
     * @return true if the region is usable by this application, false if it is not
     */
    public static boolean isRegionUsable(ObaRegion region) {
        if (!region.getActive()) {
            Log.d(TAG, "Region '" + region.getName() + "' is not active.");
            return false;
        }
        return true;
    }

    /**
     * Gets regions from either the server, local provider, or if both fails the regions file
     * packaged
     * with the APK.  Includes fail-over logic to prefer sources in above order, with server being
     * the first preference.
     *
     * @param forceReload true if a reload from the server should be forced, false if it should not
     * @return a list of regions from either the server, the local provider, or the packaged
     * resource file
     */
    public synchronized static ArrayList<ObaRegion> getRegions(Context context,
                                                               boolean forceReload) {
        ArrayList<ObaRegion> results;
        if (!forceReload) {
            //
            // Check the DB
            //
            results = RegionUtils.getRegionsFromProvider(context);
            if (results != null) {
                Log.d(TAG, "Retrieved regions from database.");
                return results;
            }
            Log.d(TAG, "Regions list retrieved from database was null.");
        }

        results = RegionUtils.getRegionsFromServer(context);
        if (results == null || results.isEmpty()) {
            Log.d(TAG, "Regions list retrieved from server was null or empty.");

            if (forceReload) {
                //If we tried to force a reload from the server, then we haven't tried to reload from local provider yet
                results = RegionUtils.getRegionsFromProvider(context);
                if (results != null) {
                    Log.d(TAG, "Retrieved regions from database.");
                    return results;
                } else {
                    Log.d(TAG, "Regions list retrieved from database was null.");
                }
            }

            //If we reach this point, the call to the Regions REST API failed and no results were
            //available locally from a prior server request.        
            //Fetch regions from local resource file as last resort (otherwise user can't use app)
            results = RegionUtils.getRegionsFromResources(context);

            if (results == null) {
                //This is a complete failure to load region info from all sources, app will be useless
                Log.d(TAG, "Regions list retrieved from local resource file was null.");
                return results;
            }

            Log.d(TAG, "Retrieved regions from local resource file.");
        } else {
            Log.d(TAG, "Retrieved regions list from server.");
            //Update local time for when the last region info was retrieved from the server
            Application.get().setLastRegionUpdateDate(new Date().getTime());
        }

        //If the region info came from the server or local resource file, we need to save it to the local provider
        RegionUtils.saveToProvider(context, results);
        return results;
    }

    public static ArrayList<ObaRegion> getRegionsFromProvider(Context context) {
        // Prefetch the bounds to limit the number of DB calls.
        HashMap<Long, ArrayList<ObaRegionElement.Bounds>> allBounds = getBoundsFromProvider(
                context);

        HashMap<Long, ArrayList<ObaRegionElement.Open311Servers>> allOpen311Servers =
                getOpen311ServersFromProvider(context);

        Cursor c = null;
        try {
            final String[] PROJECTION = {
                    ObaContract.Regions._ID,
                    ObaContract.Regions.NAME,
                    ObaContract.Regions.BASE_URL,
                    ObaContract.Regions.CONTACT_EMAIL,
                    ObaContract.Regions.TWITTER_URL,
                    ObaContract.Regions.FACEBOOK_URL,
                    ObaContract.Regions.EXPERIMENTAL,
                    ObaContract.Regions.TUTORIAL_URL
            };

            ContentResolver cr = context.getContentResolver();
            c = cr.query(
                    ObaContract.Regions.CONTENT_URI, PROJECTION, null, null,
                    ObaContract.Regions._ID);
            if (c == null) {
                return null;
            }
            if (c.getCount() == 0) {
                c.close();
                return null;
            }
            ArrayList<ObaRegion> results = new ArrayList<ObaRegion>();

            c.moveToFirst();
            do {
                long id = c.getLong(0);
                ArrayList<ObaRegionElement.Bounds> bounds = allBounds.get(id);
                ObaRegionElement.Bounds[] bounds2 = (bounds != null) ?
                        bounds.toArray(new ObaRegionElement.Bounds[]{}) :
                        null;

                ArrayList<ObaRegionElement.Open311Servers> open311Servers = allOpen311Servers.get(id);
                ObaRegionElement.Open311Servers[] open311Servers2 = (open311Servers != null) ?
                        open311Servers.toArray(new ObaRegionElement.Open311Servers[]{}) :
                        null;

                results.add(new ObaRegionElement(id,   // id
                        c.getString(1),             // Name
                        true,                       // Active
                        c.getString(2),             // OBA Base URL
                        bounds2,                    // Bounds
                        open311Servers2,            // Open311 servers
                        c.getString(3),             // Contact Email
                        c.getString(4),              // Twitter URL
                        c.getString(5),              // FB URL
                        c.getInt(6) > 0,            // Experimental
                        c.getString(7)
                ));

            } while (c.moveToNext());

            return results;

        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private static HashMap<Long, ArrayList<ObaRegionElement.Bounds>> getBoundsFromProvider(
            Context context) {
        // Prefetch the bounds to limit the number of DB calls.
        Cursor c = null;
        try {
            final String[] PROJECTION = {
                    ObaContract.RegionBounds.REGION_ID,
                    ObaContract.RegionBounds.LOWER_LEFT_LATITUDE,
                    ObaContract.RegionBounds.UPPER_RIGHT_LATITUDE,
                    ObaContract.RegionBounds.LOWER_LEFT_LONGITUDE,
                    ObaContract.RegionBounds.UPPER_RIGHT_LONGITUDE
            };
            HashMap<Long, ArrayList<ObaRegionElement.Bounds>> results
                    = new HashMap<Long, ArrayList<ObaRegionElement.Bounds>>();

            ContentResolver cr = context.getContentResolver();
            c = cr.query(ObaContract.RegionBounds.CONTENT_URI, PROJECTION, null, null, null);
            if (c == null) {
                return results;
            }
            if (c.getCount() == 0) {
                c.close();
                return results;
            }
            c.moveToFirst();
            do {
                long regionId = c.getLong(0);
                ArrayList<ObaRegionElement.Bounds> bounds = results.get(regionId);
                ObaRegionElement.Bounds b = new ObaRegionElement.Bounds(
                        c.getDouble(1),
                        c.getDouble(2),
                        c.getDouble(3),
                        c.getDouble(4));
                if (bounds != null) {
                    bounds.add(b);
                } else {
                    bounds = new ArrayList<ObaRegionElement.Bounds>();
                    bounds.add(b);
                    results.put(regionId, bounds);
                }

            } while (c.moveToNext());

            return results;

        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private static HashMap<Long, ArrayList<ObaRegionElement.Open311Servers>> getOpen311ServersFromProvider(
            Context context) {
        // Prefetch the bounds to limit the number of DB calls.
        Cursor c = null;
        try {
            final String[] PROJECTION = {
                    ObaContract.RegionOpen311Servers.REGION_ID,
                    ObaContract.RegionOpen311Servers.JURISDICTION,
                    ObaContract.RegionOpen311Servers.API_KEY,
                    ObaContract.RegionOpen311Servers.BASE_URL
            };
            HashMap<Long, ArrayList<ObaRegionElement.Open311Servers>> results
                    = new HashMap<Long, ArrayList<ObaRegionElement.Open311Servers>>();

            ContentResolver cr = context.getContentResolver();
            c = cr.query(ObaContract.RegionOpen311Servers.CONTENT_URI, PROJECTION, null, null, null);
            if (c == null) {
                return results;
            }
            if (c.getCount() == 0) {
                c.close();
                return results;
            }
            c.moveToFirst();
            do {
                long regionId = c.getLong(0);
                ArrayList<ObaRegionElement.Open311Servers> open311Servers = results.get(regionId);
                ObaRegionElement.Open311Servers b = new ObaRegionElement.Open311Servers(
                        c.getString(1),
                        c.getString(2),
                        c.getString(3));
                if (open311Servers != null) {
                    open311Servers.add(b);
                } else {
                    open311Servers = new ArrayList<ObaRegionElement.Open311Servers>();
                    open311Servers.add(b);
                    results.put(regionId, open311Servers);
                }

            } while (c.moveToNext());

            return results;

        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private synchronized static ArrayList<ObaRegion> getRegionsFromServer(Context context) {
        ObaRegionsResponse response = ObaRegionsRequest.newRequest(context).call();
        return new ArrayList<ObaRegion>(Arrays.asList(response.getRegions()));
    }

    /**
     * Retrieves region information from a regions file bundled within the app APK
     *
     * IMPORTANT - this should be a last resort, and we should always try to pull regions
     * info from the local provider or Regions REST API instead of from the bundled file.
     *
     * This method is only intended to be a fail-safe in case the Regions REST API goes
     * offline and a user downloads and installs OBA Android during that period
     * (i.e., local OBA servers are available, but Regions REST API failure would block initial
     * execution of the app).  This avoids a potential central point of failure for OBA
     * Android installations on devices in multiple regions.
     *
     * @return list of regions retrieved from the regions file in app resources
     */
    public static ArrayList<ObaRegion> getRegionsFromResources(Context context) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_ANDROID_RESOURCE);
        builder.authority(context.getPackageName());
        builder.path(Integer.toString(R.raw.regions_v3));
        ObaRegionsResponse response = ObaRegionsRequest.newRequest(context, builder.build()).call();
        return new ArrayList<ObaRegion>(Arrays.asList(response.getRegions()));
    }

    /**
     * Retrieves hard-coded region information from the build flavor defined in build.gradle.
     * If a fixed region is defined in a build flavor, it does not allow region roaming.
     *
     * @return hard-coded region information from the build flavor defined in build.gradle
     */
    public static ObaRegion getRegionFromBuildFlavor() {
        final int regionId = Integer.MAX_VALUE; // This doesn't get used, but needs to be positive
        ObaRegionElement.Bounds[] boundsArray = new ObaRegionElement.Bounds[1];
        ObaRegionElement.Bounds bounds = new ObaRegionElement.Bounds(
                BuildConfig.FIXED_REGION_BOUNDS_LAT, BuildConfig.FIXED_REGION_BOUNDS_LON,
                BuildConfig.FIXED_REGION_BOUNDS_LAT_SPAN, BuildConfig.FIXED_REGION_BOUNDS_LON_SPAN);
        boundsArray[0] = bounds;
        ObaRegionElement region = new ObaRegionElement(regionId,
                BuildConfig.FIXED_REGION_NAME, true,
                BuildConfig.FIXED_REGION_OBA_BASE_URL,
                boundsArray, new ObaRegionElement.Open311Servers[0],
                BuildConfig.FIXED_REGION_CONTACT_EMAIL,
                BuildConfig.FIXED_REGION_TWITTER_URL,BuildConfig.FIXED_REGION_TWITTER_URL, false,
                null);
        return region;
    }

    //
    // Saving
    //
    public synchronized static void saveToProvider(Context context, List<ObaRegion> regions) {
        // Delete all the existing regions
        ContentResolver cr = context.getContentResolver();

        cr.delete(ObaContract.Regions.CONTENT_URI, null, null);
        // Should be a no-op?
        cr.delete(ObaContract.RegionBounds.CONTENT_URI, null, null);

        for (ObaRegion region : regions) {
            if (!isRegionUsable(region)) {
                Log.d(TAG, "Skipping insert of '" + region.getName() + "' to provider...");
                continue;
            }

            cr.insert(ObaContract.Regions.CONTENT_URI, toContentValues(region));
            Log.d(TAG, "Saved region '" + region.getName() + "' to provider");
            long regionId = region.getId();
            // Bulk insert the bounds
            ObaRegion.Bounds[] bounds = region.getBounds();
            if (bounds != null) {
                ContentValues[] values = new ContentValues[bounds.length];
                for (int i = 0; i < bounds.length; ++i) {
                    values[i] = toContentValues(regionId, bounds[i]);
                }
                cr.bulkInsert(ObaContract.RegionBounds.CONTENT_URI, values);
            }

            ObaRegion.Open311Servers[] open311Servers = region.getOpen311Servers();

            if (open311Servers != null) {
                ContentValues[] values = new ContentValues[open311Servers.length];
                for (int i = 0; i < open311Servers.length; ++i) {
                    values[i] = toContentValues(regionId, open311Servers[i]);
                }
                cr.bulkInsert(ObaContract.RegionOpen311Servers.CONTENT_URI, values);
            }
        }
    }

    private static ContentValues toContentValues(ObaRegion region) {
        ContentValues values = new ContentValues();
        values.put(ObaContract.Regions._ID, region.getId());
        values.put(ObaContract.Regions.NAME, region.getName());
        String obaUrl = region.getBaseUrl();
        values.put(ObaContract.Regions.BASE_URL, obaUrl != null ? obaUrl : "");
        values.put(ObaContract.Regions.CONTACT_EMAIL, region.getContactEmail());
        values.put(ObaContract.Regions.TWITTER_URL, region.getTwitterUrl());
        values.put(ObaContract.Regions.FACEBOOK_URL, region.getFacebookUrl());
        values.put(ObaContract.Regions.EXPERIMENTAL, region.getExperimental());
        values.put(ObaContract.Regions.TUTORIAL_URL, region.getTutorialUrl());
        return values;
    }

    private static ContentValues toContentValues(long region, ObaRegion.Bounds bounds) {
        ContentValues values = new ContentValues();
        values.put(ObaContract.RegionBounds.REGION_ID, region);
        values.put(ObaContract.RegionBounds.LOWER_LEFT_LATITUDE, bounds.getLowerLeftLatitude());
        values.put(ObaContract.RegionBounds.UPPER_RIGHT_LATITUDE, bounds.getUpperRightLatitude());
        values.put(ObaContract.RegionBounds.LOWER_LEFT_LONGITUDE, bounds.getLowerLeftLongitude());
        values.put(ObaContract.RegionBounds.UPPER_RIGHT_LONGITUDE, bounds.getUpperRightLongitude());
        return values;
    }

    private static ContentValues toContentValues(long region, ObaRegion.Open311Servers open311Servers) {
        ContentValues values = new ContentValues();
        values.put(ObaContract.RegionOpen311Servers.REGION_ID, region);
        values.put(ObaContract.RegionOpen311Servers.BASE_URL, open311Servers.getBaseUrl());
        values.put(ObaContract.RegionOpen311Servers.JURISDICTION, open311Servers.getJuridisctionId());
        values.put(ObaContract.RegionOpen311Servers.API_KEY, open311Servers.getApiKey());
        return values;
    }
}
