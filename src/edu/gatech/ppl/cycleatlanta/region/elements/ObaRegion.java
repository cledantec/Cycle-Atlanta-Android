/*
 * Copyright (C) 2013 Paul Watts (paulcwatts@gmail.com)
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
package edu.gatech.ppl.cycleatlanta.region.elements;


/**
 * Specifies a region in the OneBusAway multi-region system.
 */
public interface ObaRegion {

    /**
     * Specifies a single bound rectangle within this region.
     */
    public interface Bounds {

        public double getLowerLeftLatitude();

        public double getLowerLeftLongitude();

        public double getUpperRightLatitude();

        public double getUpperRightLongitude();
    }

    public interface Open311Servers {

        public String getJuridisctionId();

        public String getApiKey();

        public String getBaseUrl();
    }

    /**
     * @return The ID of this region.
     */
    public long getId();

    /**
     * @return The name of the region.
     */
    public String getName();

    /**
     * @return true if this server is active and should be presented in a list of working servers,
     * false otherwise.
     */
    public boolean getActive();

    /**
     * @return The base OBA URL for this region, or null if it doesn't have a base OBA URL.
     */
    public String getBaseUrl();


    /**
     * @return An array of bounding boxes for the region.
     */
    public Bounds[] getBounds();

    /**
     * @return The email of the party responsible for this region's OBA server.
     */
    public String getContactEmail();

    public Open311Servers[] getOpen311Servers();

    /**
     * @return The Twitter URL for the region
     */
    public String getTwitterUrl();

    public String getFacebookUrl();
    /**
     * @return true if this server is experimental, false if its production.
     */
    public boolean getExperimental();

    public String getTutorialUrl();
}
