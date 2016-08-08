package edu.gatech.ppl.cycleatlanta;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.security.MessageDigest;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import edu.gatech.ppl.cycleatlanta.provider.ObaContract;
import edu.gatech.ppl.cycleatlanta.region.ObaApi;
import edu.gatech.ppl.cycleatlanta.region.elements.ObaRegion;
import edu.gatech.ppl.cycleatlanta.region.utils.LocationUtils;
import edu.gatech.ppl.cycleatlanta.region.utils.PreferenceUtils;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;

public class Application extends android.app.Application{

    public static final String APP_UID = "app_uid";

    // Region preference (long id)
    private static final String TAG = "Application";

    private SharedPreferences mPrefs;

    private static Application mApp;

    private static final String HEXES = "0123456789abcdef";

    // Magnetic declination is based on location, so track this centrally too.
    static GeomagneticField mGeomagneticField = null;

    /**
     * We centralize location tracking in the Application class to allow all objects to make
     * use of the last known location that we've seen.  This is more reliable than using the
     * getLastKnownLocation() method of the location providers, and allows us to track both
     * Location
     * API v1 and fused provider.  It allows us to avoid strange behavior like animating a mMap view
     * change when opening a new Activity, even when the previous Activity had a current location.
     */
    private static Location mLastKnownLocation = null;

    @Override
    public void onCreate() {
        super.onCreate();

        mApp = this;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        initObaRegion();
    }

    public static Application get() {
        return mApp;
    }

    public synchronized ObaRegion getCurrentRegion() {
        return ObaApi.getDefaultContext().getRegion();
    }

    public synchronized void setCurrentRegion(ObaRegion region) {
        if (region != null) {
            // First set it in preferences, then set it in OBA.
            ObaApi.getDefaultContext().setRegion(region);
            PreferenceUtils
                    .saveLong(mPrefs, getString(R.string.preference_key_region), region.getId());
            //We're using a region, so clear the custom API URL preference
            setCustomApiUrl(null);
        } else {
            //User must have just entered a custom API URL via Preferences, so clear the region info
            ObaApi.getDefaultContext().setRegion(null);
            PreferenceUtils.saveLong(mPrefs, getString(R.string.preference_key_region), -1);
        }
    }

    /**
     * Gets the date at which the region information was last updated, in the number of
     * milliseconds
     * since January 1, 1970, 00:00:00 GMT
     * Default value is 0 if the region info has never been updated.
     *
     * @return the date at which the region information was last updated, in the number of
     * milliseconds since January 1, 1970, 00:00:00 GMT.  Default value is 0 if the region info has
     * never been updated.
     */
    public long getLastRegionUpdateDate() {
        SharedPreferences preferences = getPrefs();
        return preferences.getLong(getString(R.string.preference_key_last_region_update), 0);
    }

    /**
     * Sets the date at which the region information was last updated
     *
     * @param date the date at which the region information was last updated, in the number of
     *             milliseconds since January 1, 1970, 00:00:00 GMT
     */
    public void setLastRegionUpdateDate(long date) {
        PreferenceUtils
                .saveLong(mPrefs, getString(R.string.preference_key_last_region_update), date);
    }

    /**
     * Returns the custom URL if the user has set a custom API URL manually via Preferences, or
     * null
     * if it has not been set
     *
     * @return the custom URL if the user has set a custom API URL manually via Preferences, or null
     * if it has not been set
     */
    public String getCustomApiUrl() {
        SharedPreferences preferences = getPrefs();
        return preferences.getString(getString(R.string.preference_key_oba_api_url), null);
    }

    /**
     * Sets the custom URL used to reach a OBA REST API server that is not available via the
     * Regions
     * REST API
     *
     * @param url the custom URL
     */
    public void setCustomApiUrl(String url) {
        PreferenceUtils.saveString(getString(R.string.preference_key_oba_api_url), url);
    }

    public static SharedPreferences getPrefs() {
        return get().mPrefs;
    }

    /**
     * Returns the last known location that the application has seen, or null if we haven't seen a
     * location yet.  When trying to get a most recent location in one shot, this method should
     * always be called.
     *
     * @param cxt    The Context being used, or null if one isn't available
     * @param client The GoogleApiClient being used to obtain fused provider updates, or null if
     *               one
     *               isn't available
     * @return the last known location that the application has seen, or null if we haven't seen a
     * location yet
     */
    public static synchronized Location getLastKnownLocation(Context cxt, GoogleApiClient client) {
        if (mLastKnownLocation == null) {
            // Try to get a last known location from the location providers
            mLastKnownLocation = getLocation2(cxt, client);
        }
        // Pass back last known saved location, hopefully from past location listener updates
        return mLastKnownLocation;
    }

    private static Location getLocation2(Context cxt, GoogleApiClient client) {
        Location playServices = null;
        if (client != null &&
                cxt != null &&
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(cxt)
                        == ConnectionResult.SUCCESS
                && client.isConnected()) {
            playServices = FusedLocationApi.getLastLocation(client);
            Log.d(TAG, "Got location from Google Play Services, testing against API v1...");
        }
        Location apiV1 = getLocationApiV1(cxt);

        if (LocationUtils.compareLocationsByTime(playServices, apiV1)) {
            Log.d(TAG, "Using location from Google Play Services");
            return playServices;
        } else {
            Log.d(TAG, "Using location from Location API v1");
            return apiV1;
        }
    }

    /**
     * Sets the last known location observed by the application via an instance of LocationHelper
     *
     * @param l a location received by a LocationHelper instance
     */
    public static synchronized void setLastKnownLocation(Location l) {
        // If the new location is better than the old one, save it
        if (LocationUtils.compareLocations(l, mLastKnownLocation)) {
            if (mLastKnownLocation == null) {
                mLastKnownLocation = new Location("Last known location");
            }
            mLastKnownLocation.set(l);
            mGeomagneticField = new GeomagneticField(
                    (float) l.getLatitude(),
                    (float) l.getLongitude(),
                    (float) l.getAltitude(),
                    System.currentTimeMillis());
            // Log.d(TAG, "Newest best location: " + mLastKnownLocation.toString());
        }
    }

    private static Location getLocationApiV1(Context cxt) {
        if (cxt == null) {
            return null;
        }
        LocationManager mgr = (LocationManager) cxt.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = mgr.getProviders(true);
        Location last = null;
        for (Iterator<String> i = providers.iterator(); i.hasNext(); ) {
            Location loc = mgr.getLastKnownLocation(i.next());
            // If this provider has a last location, and either:
            // 1. We don't have a last location,
            // 2. Our last location is older than this location.
            if (LocationUtils.compareLocationsByTime(loc, last)) {
                last = loc;
            }
        }
        return last;
    }

    private String getAppUid() {
        try {
            final TelephonyManager telephony =
                    (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            final String id = telephony.getDeviceId();
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(id.getBytes());
            return getHex(digest.digest());
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    public static String getHex(byte[] raw) {
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4))
                    .append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    private void initObaRegion() {
        // Read the region preference, look it up in the DB, then set the region.
        long id = mPrefs.getLong(getString(R.string.preference_key_region), -1);
        if (id < 0) {
            Log.d(TAG, "Regions preference ID is less than 0, returning...");
            return;
        }

        ObaRegion region = ObaContract.Regions.get(this, (int) id);
        if (region == null) {
            Log.d(TAG, "Regions preference is null, returning...");
            return;
        }


        ObaApi.getDefaultContext().setRegion(region);
    }
}
