/*
 * Copyright (C) 2012-2013 Paul Watts (paulcwatts@gmail.com)
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


import java.util.Arrays;

public class ObaRegionElement implements ObaRegion {

    public static final ObaRegionElement[] EMPTY_ARRAY = new ObaRegionElement[]{};

    public static class Bounds implements ObaRegion.Bounds {

        public static final Bounds[] EMPTY_ARRAY = new Bounds[]{};

        private final double lowerLeftLatitude;

        private final double upperRightLatitude;

        private final double lowerLeftLongitude;

        private final double upperRightLongitude;

        Bounds() {
            lowerLeftLatitude = 0;
            upperRightLatitude = 0;
            lowerLeftLongitude = 0;
            upperRightLongitude = 0;
        }

        public Bounds(double lowerLeftLatitude,
                      double upperRightLatitude,
                      double lowerLeftLongitude,
                      double upperRightLongitude) {
            this.lowerLeftLatitude = lowerLeftLatitude;
            this.upperRightLatitude = upperRightLatitude;
            this.lowerLeftLongitude = lowerLeftLongitude;
            this.upperRightLongitude = upperRightLongitude;
        }


        @Override
        public double getLowerLeftLatitude() {
            return lowerLeftLatitude;
        }

        @Override
        public double getLowerLeftLongitude() {
            return lowerLeftLongitude;
        }

        @Override
        public double getUpperRightLatitude() {
            return upperRightLatitude;
        }

        @Override
        public double getUpperRightLongitude() {
            return upperRightLongitude;
        }
    }

    public static class Open311Servers implements ObaRegion.Open311Servers {

        public static final Open311Servers[] EMPTY_ARRAY = new Open311Servers[]{};

        private final String jurisdictionId;

        private final String apiKey;

        private final String baseUrl;

        Open311Servers() {
            jurisdictionId = "";
            apiKey = "";
            baseUrl = "";
        }

        public Open311Servers(String jurisdictionId, String apiKey, String baseUrl) {

            this.jurisdictionId = jurisdictionId;
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
        }

        @Override
        public String getJuridisctionId() {
            return jurisdictionId;
        }

        @Override
        public String getApiKey() {
            return apiKey;
        }

        @Override
        public String getBaseUrl() {
            return baseUrl;
        }
    }

    private final long id;

    private final String regionName;

    private final boolean active;

    private final String baseUrl;

    private final Bounds[] bounds;

    private final Open311Servers[] open311Servers;

    private final String contactEmail;

    private final String twitterUrl;

    private final String facebookUrl;

    private final boolean experimental;

    private final String tutorialUrl;

    ObaRegionElement() {
        id = 0;
        regionName = "";
        baseUrl = null;
        active = false;
        bounds = Bounds.EMPTY_ARRAY;
        open311Servers = Open311Servers.EMPTY_ARRAY;
        contactEmail = "";
        twitterUrl = "";
        facebookUrl = "";
        experimental = true;
        tutorialUrl = "";
    }

    public ObaRegionElement(long id,
                            String name,
                            boolean active,
                            String baseUrl,
                            Bounds[] bounds,
                            Open311Servers[] open311Servers,
                            String contactEmail,
                            String twitterUrl,
                            String facebookUrl,
                            boolean experimental,
                            String tutorialUrl) {
        this.id = id;
        this.regionName = name;
        this.active = active;
        this.baseUrl = baseUrl;
        this.bounds = bounds;
        this.open311Servers = open311Servers;
        this.contactEmail = contactEmail;
        this.twitterUrl = twitterUrl;
        this.facebookUrl = facebookUrl;
        this.experimental = experimental;
        this.tutorialUrl = tutorialUrl;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return regionName;
    }

    @Override
    public boolean getActive() {
        return active;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public Bounds[] getBounds() {
        return bounds;
    }

    @Override
    public String getContactEmail() {
        return contactEmail;
    }

    @Override
    public Open311Servers[] getOpen311Servers() {
        return open311Servers;
    }

    @Override
    public String getTwitterUrl() {
        return twitterUrl;
    }

    @Override
    public String getFacebookUrl() {
        return facebookUrl;
    }

    @Override
    public boolean getExperimental() {
        return experimental;
    }

    @Override
    public String getTutorialUrl() {
        return tutorialUrl;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == 0) ? 0 : Long.valueOf(id).hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ObaRegionElement)) {
            return false;
        }
        ObaRegionElement other = (ObaRegionElement) obj;
        if (id == 0) {
            if (other.getId() != 0) {
                return false;
            }
        } else if (id != other.getId()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ObaRegionElement{" +
                "id=" + id +
                ", regionName='" + regionName + '\'' +
                ", active=" + active +
                ", BaseUrl='" + baseUrl + '\'' +
                ", bounds=" + Arrays.toString(bounds) +
                ", contactEmail='" + contactEmail + '\'' +
                ", twitterUrl='" + twitterUrl + '\'' +
                ", experimental=" + experimental +
                '}';
    }
}
