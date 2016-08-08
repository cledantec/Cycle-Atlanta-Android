/*
 * Copyright (C) 2012 Paul Watts (paulcwatts@gmail.com)
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
package edu.gatech.ppl.cycleatlanta.region;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

import edu.gatech.ppl.cycleatlanta.Application;
import edu.gatech.ppl.cycleatlanta.R;
import edu.gatech.ppl.cycleatlanta.region.elements.ObaRegion;

public class ObaContext {

    private static final String TAG = "ObaContext";

    private String mApiKey = "v1_BktoDJ2gJlu6nLM6LsT9H8IUbWc=cGF1bGN3YXR0c0BnbWFpbC5jb20=";

    private int mAppVer = 0;

    private String mAppUid = null;

    private ObaConnectionFactory mConnectionFactory = ObaDefaultConnectionFactory.getInstance();

    private ObaRegion mRegion;

    public ObaContext() {
    }

    public void setAppInfo(int version, String uuid) {
        mAppVer = version;
        mAppUid = uuid;
    }

    public void setAppInfo(Uri.Builder builder) {
        if (mAppVer != 0) {
            builder.appendQueryParameter("app_ver", String.valueOf(mAppVer));
        }
        if (mAppUid != null) {
            builder.appendQueryParameter("app_uid", mAppUid);
        }
    }

    public void setApiKey(String apiKey) {
        mApiKey = apiKey;
    }

    public String getApiKey() {
        return mApiKey;
    }

    public void setRegion(ObaRegion region) {
        mRegion = region;
    }

    public ObaRegion getRegion() {
        return mRegion;
    }

    /**
     * Connection factory
     *
     */
    public ObaConnectionFactory setConnectionFactory(ObaConnectionFactory factory) {
        ObaConnectionFactory prev = mConnectionFactory;
        mConnectionFactory = factory;
        return prev;
    }

    public ObaConnectionFactory getConnectionFactory() {
        return mConnectionFactory;
    }

    public void setBaseUrl(Context context, Uri.Builder builder) {
        // If there is a custom preference, then use that.
        String serverName = Application.get().getCustomApiUrl();

        if (!TextUtils.isEmpty(serverName) || mRegion != null) {
            Uri baseUrl = null;
            if (!TextUtils.isEmpty(serverName)) {
                Log.d(TAG, "Using custom API URL set by user '" + serverName + "'.");

                try {
                    // URI.parse() doesn't tell us if the scheme is missing, so use URL() instead (#126)
                    URL url = new URL(serverName);
                } catch (MalformedURLException e) {
                    // Assume HTTP scheme, since without a scheme the Uri won't parse the authority
                    serverName = context.getString(R.string.http_prefix) + serverName;
                }

                baseUrl = Uri.parse(serverName);
            } else if (mRegion != null) {
                Log.d(TAG, "Using region base URL '" + mRegion.getBaseUrl() + "'.");

                baseUrl = Uri.parse(mRegion.getBaseUrl());
            }

            // Copy partial path (if one exists) from the base URL
            Uri.Builder path = new Uri.Builder();
            path.encodedPath(baseUrl.getEncodedPath());

            // Then, tack on the rest of the REST API method path from the Uri.Builder that was passed in
            path.appendEncodedPath(builder.build().getPath());

            // Finally, overwrite builder that was passed in with the full URL
            builder.scheme(baseUrl.getScheme());
            builder.encodedAuthority(baseUrl.getEncodedAuthority());
            builder.encodedPath(path.build().getEncodedPath());
        } else {
            String fallBack = "api.pugetsound.onebusaway.org";
            Log.e(TAG, "Accessing default fallback '" + fallBack + "' ...this is wrong!!");
            // Current fallback for existing users?
            builder.scheme("http");
            builder.authority(fallBack);
        }
    }

    @Override
    public ObaContext clone() {
        ObaContext result = new ObaContext();
        result.setApiKey(mApiKey);
        result.setAppInfo(mAppVer, mAppUid);
        result.setConnectionFactory(mConnectionFactory);
        return result;
    }
}
