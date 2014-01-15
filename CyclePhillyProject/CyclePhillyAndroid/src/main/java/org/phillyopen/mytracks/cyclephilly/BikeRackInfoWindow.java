package org.phillyopen.mytracks.cyclephilly;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

public class BikeRackInfoWindow implements InfoWindowAdapter {
	LayoutInflater inflater=null;
	
	BikeRackInfoWindow(LayoutInflater inflater) {
		this.inflater=inflater;
	}

	@Override
	public View getInfoContents(Marker marker) {
		View popup=inflater.inflate(R.layout.popup, null);

	    TextView tv=(TextView)popup.findViewById(R.id.title);

	    tv.setText(marker.getTitle());
	    tv=(TextView)popup.findViewById(R.id.snippet);
	    tv.setText(marker.getSnippet());

	    return(popup);
	}

	@Override
	public View getInfoWindow(Marker marker) {
		return null;
	}
	
}

