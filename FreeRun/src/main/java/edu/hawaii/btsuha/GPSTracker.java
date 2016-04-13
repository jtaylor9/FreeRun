package edu.hawaii.btsuha;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

public class GPSTracker extends Service implements LocationListener {

	private final Context context;
	
	boolean isGPSEnabled = false;
	boolean isNetworkEnabled = false;
	boolean canGetLocation = false;
	
	Location location, last;
	int distance = 0;
	int missedLocation = 0;
	
	double latitude;
	double longitude;
	
	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; //0 meters
	private static final long MIN_TIME_BW_UPDATES = 1000*30; //15 seconds
	
	protected LocationManager locationManager;
	
	public GPSTracker(Context context) {


		this.context = context;
		getLocation();
	}
	
	public Location getLocation() {
		try {
			locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
			
			isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			
			isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			
			if(isNetworkEnabled) {   //Changed
				this.canGetLocation = true;
					
				locationManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER,   //Changed
						MIN_TIME_BW_UPDATES,
						MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

				if (locationManager != null) {
					location = locationManager
							.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);  //Changed

					if (location != null) {
						latitude = location.getLatitude();
						longitude = location.getLongitude();
					}
				}
 			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return location;
	}
	
	
	public void stopUsingGPS() {
		if(locationManager != null) {
			locationManager.removeUpdates(GPSTracker.this);
		}
	}
	
	public double getLatitude() {
		if(location != null) {
			latitude = location.getLatitude();
		}
		return latitude;
	}
	
	public double getLongitude() {
		if(location != null) {
			longitude = location.getLongitude();
		}
		
		return longitude;
	}
	
	public boolean canGetLocation() {
		return this.canGetLocation;
	}

	
	public void showSettingsAlert() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
		
		alertDialog.setTitle("GPS is settings");
		
		alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");
		
		alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				context.startActivity(intent);
			}
		});
		
		alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		
		alertDialog.show();
	}

	//Finds distance between point A and point B with latitude and longitude
	public float getDistanceTo(double latA, double lngA, double latB, double lngB) {
		Location locationA = new Location("Point A");

		locationA.setLatitude(latA);
		locationA.setLongitude(lngA);

		Location locationB = new Location("Point B");

		locationB.setLatitude(latB);
		locationB.setLongitude(lngB);

		float distance = locationA.distanceTo(locationB);
		return distance;
	}

	public int getDistance() {
		return distance;
	}

	public int getMissedLocation() { return missedLocation; }

	public void resetDistance() {
		distance = 0;
	}

	@Override
	public void onLocationChanged(Location arg0) {

		if(last != null) {
			if (arg0.getAccuracy() < 40) {
				distance += arg0.distanceTo(last);
				last = new Location(arg0);
			}
			else {
				missedLocation++;
			}
		}
		else {
			last = new Location(arg0);
		}
		Log.i("Info", "arg0 accuracy: " + arg0.getAccuracy());
		Log.i("Info", "Distance to last " + arg0.distanceTo(last));
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
