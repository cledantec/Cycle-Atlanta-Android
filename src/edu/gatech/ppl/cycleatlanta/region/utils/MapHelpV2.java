/*
 * Copyright (C) 2014 University of South Florida (sjbarbeau@gmail.com)
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
import com.google.android.gms.maps.model.LatLngBounds;

import edu.gatech.ppl.cycleatlanta.region.elements.ObaRegion;

/**
 * Utilities to help process data for Android Maps API v1
 */
public class MapHelpV2 {

    public static final String TAG = "MapHelpV2";

    /**
     * Converts a latitude/longitude to a LatLng.
     *
     * @param lat The latitude.
     * @param lon The longitude.
     * @return A LatLng representing this latitude/longitude.
     */
    public static final LatLng makeLatLng(double lat, double lon) {
        return new LatLng(lat, lon);
    }


    /**
     * Returns the bounds for the entire region.
     *
     * @return LatLngBounds for the region
     */
    public static LatLngBounds getRegionBounds(ObaRegion region) {
        if (region == null) {
            throw new IllegalArgumentException("Region is null");
        }
        double latMin = 90;
        double latMax = -90;
        double lonMin = 180;
        double lonMax = -180;

        // This is fairly simplistic
        for (ObaRegion.Bounds bound : region.getBounds()) {
            // Get the top bound
            double lat1 = bound.getLowerLeftLatitude();
            double lat2 = bound.getUpperRightLatitude();
            if (lat1 < latMin) {
                latMin = lat1;
            }
            if (lat2 > latMax) {
                latMax = lat2;
            }

            double lon1 = bound.getLowerLeftLongitude();
            double lon2 = bound.getUpperRightLongitude();
            if (lon1 < lonMin) {
                lonMin = lon1;
            }
            if (lon2 > lonMax) {
                lonMax = lon2;
            }
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(MapHelpV2.makeLatLng(latMin, lonMin));
        builder.include(MapHelpV2.makeLatLng(latMax, lonMax));

        return builder.build();
    }
}
