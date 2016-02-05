/*
 * Copyright (C) 2010-2015 Paul Watts (paulcwatts@gmail.com),
 * University of South Florida (sjbarbeau@gmail.com),
 * Benjamin Du (bendu@me.com)
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
package edu.gatech.ppl.cycleatlanta.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import edu.gatech.ppl.cycleatlanta.BuildConfig;
import edu.gatech.ppl.cycleatlanta.region.elements.ObaRegion;
import edu.gatech.ppl.cycleatlanta.region.elements.ObaRegionElement;

/**
 * The contract between clients and the ObaProvider.
 *
 * This really needs to be documented better.
 *
 * NOTE: The AUTHORITY names in this class cannot be changed.  They need to stay under the
 * BuildConfig.DATABASE_AUTHORITY namespace (for the original OBA brand, "com.joulespersecond.oba")
 * namespace to support backwards compatibility with existing installed apps
 *
 * @author paulw
 */
public final class ObaContract {

    public static final String TAG = "ObaContract";

    /** The authority portion of the URI - defined in build.gradle */
    public static final String AUTHORITY = BuildConfig.DATABASE_AUTHORITY;

    /** The base URI for the Oba provider */
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    protected interface RegionsColumns {

        /**
         * The name of the region.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String NAME = "name";

        /**
         * The base OBA URL.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String BASE_URL = "oba_base_url";

        /**
         * The email of the person responsible for this server.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CONTACT_EMAIL = "contact_email";


        /**
         * The Twitter URL for the region.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String TWITTER_URL = "twitter_url";

        public static final String FACEBOOK_URL = "facebook_url";

        /**
         * Whether or not the server is experimental (i.e., not production).
         * <P>
         * Type: BOOLEAN
         * </P>
         */
        public static final String EXPERIMENTAL = "experimental";

        /**
         * The StopInfo URL for the region (see #103)
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String TUTORIAL_URL = "tutorial_url";
    }

    protected interface RegionBoundsColumns {

        /**
         * The region ID
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String REGION_ID = "region_id";

        /**
         * The latitude center of the agencies coverage area
         * <P>
         * Type: REAL
         * </P>
         */
        public static final String LOWER_LEFT_LATITUDE = "lowerLeftLatitude";

        /**
         * The longitude center of the agencies coverage area
         * <P>
         * Type: REAL
         * </P>
         */
        public static final String LOWER_LEFT_LONGITUDE = "lowerLeftLongitude";

        /**
         * The height of the agencies bounding box
         * <P>
         * Type: REAL
         * </P>
         */
        public static final String UPPER_RIGHT_LATITUDE = "upperRightLatitude";

        /**
         * The width of the agencies bounding box
         * <P>
         * Type: REAL
         * </P>
         */
        public static final String UPPER_RIGHT_LONGITUDE = "upperRightLongitude";

    }

    protected interface RegionOpen311ServersColumns {

        /**
         * The region ID
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String REGION_ID = "region_id";

        /**
         * The jurisdiction id of the open311 server
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String JURISDICTION = "jurisdiction";

        /**
         * The api key of the open311 server
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String API_KEY = "api_key";

        /**
         * The url of the open311 server
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String BASE_URL = "open311_base_url";

    }

    public static class Regions implements BaseColumns, RegionsColumns {

        // Cannot be instantiated
        private Regions() {
        }

        /** The URI path portion for this table */
        public static final String PATH = "regions";

        /**
         * The content:// style URI for this table URI is of the form
         * content://<authority>/regions/<id>
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(
                AUTHORITY_URI, PATH);

        public static final String CONTENT_TYPE
                = "vnd.android.cursor.item/" + BuildConfig.DATABASE_AUTHORITY + ".region";

        public static final String CONTENT_DIR_TYPE
                = "vnd.android.dir/" + BuildConfig.DATABASE_AUTHORITY + ".region";

        public static final Uri buildUri(int id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri insertOrUpdate(Context context,
                int id,
                ContentValues values) {
            return insertOrUpdate(context.getContentResolver(), id, values);
        }

        public static Uri insertOrUpdate(ContentResolver cr,
                int id,
                ContentValues values) {
            final Uri uri = Uri.withAppendedPath(CONTENT_URI, String.valueOf(id));
            Cursor c = cr.query(uri, new String[]{}, null, null, null);
            Uri result;
            if (c != null && c.getCount() > 0) {
                cr.update(uri, values, null, null);
                result = uri;
            } else {
                values.put(_ID, id);
                result = cr.insert(CONTENT_URI, values);
            }
            if (c != null) {
                c.close();
            }
            return result;
        }

        public static ObaRegion get(Context context, int id) {
            return get(context.getContentResolver(), id);
        }

        public static ObaRegion get(ContentResolver cr, int id) {
            final String[] PROJECTION = {
                    _ID,
                    NAME,
                    BASE_URL,
                    CONTACT_EMAIL,
                    TWITTER_URL,
                    FACEBOOK_URL,
                    EXPERIMENTAL,
                    TUTORIAL_URL

            };

            Cursor c = cr.query(buildUri((int) id), PROJECTION, null, null, null);
            if (c != null) {
                try {
                    if (c.getCount() == 0) {
                        return null;
                    }
                    c.moveToFirst();
                    return new ObaRegionElement(id,   // id
                            c.getString(1),             // Name
                            true,                       // Active
                            c.getString(2),             // OBA Base URL
                            RegionBounds.getRegion(cr, id), // Bounds
                            RegionOpen311Servers.getOpen311Server(cr, id), // Open311 servers
                            c.getString(3),             // Contact Email
                            c.getString(4),              // Twitter URL
                            c.getString(5),              // Fb URL
                            c.getInt(6) > 0,               // Experimental
                            c.getString(7)
                    );
                } finally {
                    c.close();
                }
            }
            return null;
        }
    }

    public static class RegionBounds implements BaseColumns, RegionBoundsColumns {

        // Cannot be instantiated
        private RegionBounds() {
        }

        /** The URI path portion for this table */
        public static final String PATH = "region_bounds";

        /**
         * The content:// style URI for this table URI is of the form
         * content://<authority>/region_bounds/<id>
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(
                AUTHORITY_URI, PATH);

        public static final String CONTENT_TYPE
                = "vnd.android.cursor.item/" + BuildConfig.DATABASE_AUTHORITY + ".region_bounds";

        public static final String CONTENT_DIR_TYPE
                = "vnd.android.dir/" + BuildConfig.DATABASE_AUTHORITY + ".region_bounds";

        public static final Uri buildUri(int id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static ObaRegionElement.Bounds[] getRegion(ContentResolver cr, int regionId) {
            final String[] PROJECTION = {
                    LOWER_LEFT_LATITUDE,
                    UPPER_RIGHT_LATITUDE,
                    LOWER_LEFT_LONGITUDE,
                    UPPER_RIGHT_LONGITUDE
            };
            Cursor c = cr.query(CONTENT_URI, PROJECTION,
                    "(" + RegionBounds.REGION_ID + " = " + regionId + ")",
                    null, null);
            if (c != null) {
                try {
                    ObaRegionElement.Bounds[] results = new ObaRegionElement.Bounds[c.getCount()];
                    if (c.getCount() == 0) {
                        return results;
                    }

                    int i = 0;
                    c.moveToFirst();
                    do {
                        results[i] = new ObaRegionElement.Bounds(
                                c.getDouble(0),
                                c.getDouble(1),
                                c.getDouble(2),
                                c.getDouble(3));
                        i++;
                    } while (c.moveToNext());

                    return results;
                } finally {
                    c.close();
                }
            }
            return null;
        }
    }

    public static class RegionOpen311Servers implements BaseColumns, RegionOpen311ServersColumns {

        // Cannot be instantiated
        private RegionOpen311Servers() {
        }

        /** The URI path portion for this table */
        public static final String PATH = "open311_servers";

        /**
         * The content:// style URI for this table URI is of the form
         * content://<authority>/region_open311_servers/<id>
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(
                AUTHORITY_URI, PATH);

        public static final String CONTENT_TYPE
                = "vnd.android.cursor.item/" + BuildConfig.DATABASE_AUTHORITY + ".open311_servers";

        public static final String CONTENT_DIR_TYPE
                = "vnd.android.dir/" + BuildConfig.DATABASE_AUTHORITY + ".open311_servers";

        public static final Uri buildUri(int id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static ObaRegionElement.Open311Servers[] getOpen311Server
                (ContentResolver cr, int regionId) {
            final String[] PROJECTION = {
                    JURISDICTION,
                    API_KEY,
                    BASE_URL
            };
            Cursor c = cr.query(CONTENT_URI, PROJECTION,
                    "(" + RegionOpen311Servers.REGION_ID + " = " + regionId + ")",
                    null, null);
            if (c != null) {
                try {
                    ObaRegionElement.Open311Servers[] results = new ObaRegionElement.
                            Open311Servers[c.getCount()];
                    if (c.getCount() == 0) {
                        return results;
                    }

                    int i = 0;
                    c.moveToFirst();
                    do {
                        results[i] = new ObaRegionElement.Open311Servers(
                                c.getString(0),
                                c.getString(1),
                                c.getString(2));
                        i++;
                    } while (c.moveToNext());

                    return results;
                } finally {
                    c.close();
                }
            }
            return null;
        }
    }
}
