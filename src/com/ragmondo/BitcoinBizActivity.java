package com.ragmondo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.location.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class BitcoinBizActivity extends Activity implements LocationListener{

	private static final int MENU_ITEM_SETTINGS = 0;
	private static final int MENU_ITEM_ABOUT = 1;
	private static final int MENU_ITEM_LOCATE_ME = 2;
	static String Tag = "BitcoinBizActivity";
	private Tracker mGaTracker;
	private GoogleAnalytics mGaInstance;

	private LocationManager locationManager;
	private String provider;
	  
	private GoogleMap mMap;

//	private WebView mWebView;
	private AlertDialog alertDialog;
private int locationLat;
private int locationLng;
private Location position;
private Marker positionMarker;

//	public static class BusinessDisplayFragment extends DialogFragment {
//		@Override
//		public Dialog onCreateDialog(Bundle savedInstanceState) {
//			// Use the Builder class for convenient dialog construction
//			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//			builder.setMessage("more details")
//					.setPositiveButton("ring",
//							new DialogInterface.OnClickListener() {
//								public void onClick(DialogInterface dialog,
//										int id) {
//									// FIRE ZE MISSILES!
//								}
//							})
//					.setNegativeButton("cancel",
//							new DialogInterface.OnClickListener() {
//								public void onClick(DialogInterface dialog,
//										int id) {
//									// User cancelled the dialog
//								}
//							});
//			// Create the AlertDialog object and return it
//			return builder.create();
//		}
//	}

	private class LoadData extends AsyncTask<Void, Void, Boolean> {

		private List<BCLocation> locations;
		private boolean networkConnected;

		public LoadData() {
			this.locations = new ArrayList<BCLocation>();
		}

		@Override
		protected void onPreExecute() {
			showLoadingDialog();
			networkConnected = isNetworkConnected();
			super.onPreExecute();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			if (networkConnected) {
				getUpdatedLocations(locations);
				return locations.size() > 0;
			} else {
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				displayUpdatedLocations(locations, mMap);
				cancelLoadingDialog();
			} else {
				cancelLoadingDialog();
				Toast.makeText(
						BitcoinBizActivity.this,
						"Problem while loading data, please check your internet connection and try again",
						Toast.LENGTH_LONG).show();
			}
			super.onPostExecute(result);
		}

	}

	private boolean isNetworkConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni == null) {
			// There are no active networks.
			return false;
		} else
			return true;
	}

	public void showLoadingDialog() {
		alertDialog = new AlertDialog.Builder(BitcoinBizActivity.this)
				.setTitle("Loading...").setMessage("Updating latest locations")
				.setCancelable(false).create();
		alertDialog.show();
	}

	public void cancelLoadingDialog() {
		alertDialog.cancel();
		alertDialog = null;
	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Get the GoogleAnalytics singleton. Note that the SDK uses
		// the application context to avoid leaking the current context.
		mGaInstance = GoogleAnalytics.getInstance(this);

		// Use the GoogleAnalytics singleton to get a Tracker.
		mGaTracker = mGaInstance.getTracker("UA-296782-18"); // Placeholder
																// tracking ID.

		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
				.getMap();

		// display updated locations
		mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
			@Override
			public void onInfoWindowClick(Marker marker) {
				// mGaTracker.sendView("Detail:" + marker.getTitle());
				mGaTracker.sendEvent("MainMap", "button_press",
						marker.getTitle(), new Long(0));
				String snippet = marker.getSnippet();
				Log.d(Tag, snippet);
				// 1. Instantiate an AlertDialog.Builder with its constructor
				AlertDialog.Builder builder = new AlertDialog.Builder(
						BitcoinBizActivity.this);

				WebView wv = new WebView(BitcoinBizActivity.this);

				wv.loadData(snippet, "text/html", null);

				builder.setView(wv);

				// builder.setMessage(Html.fromHtml(marker.getSnippet()))
				// .setTitle(marker.getTitle());

				AlertDialog dialog = builder.create();
				dialog.show();

			}
		});

		mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(this));
		new LoadData().execute();

	    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    // Define the criteria how to select the locatioin provider -> use
	    // default
	    Criteria criteria = new Criteria();
	    criteria.setAccuracy(Criteria.ACCURACY_COARSE); 
	    criteria.setAltitudeRequired(false); 
	    criteria.setBearingRequired(false); 
	    criteria.setCostAllowed(true); 
	    criteria.setPowerRequirement(Criteria.POWER_LOW); 
	    provider = locationManager.getBestProvider(criteria, false);
	    position = locationManager.getLastKnownLocation(provider);
	    if (position != null) {
	        System.out.println("Provider " + provider + " has been selected.");
	        onLocationChanged(position);
	    }

	}

	private void getUpdatedLocations(List<BCLocation> locations) {
		locations.clear();
		MerchantLocations ml = new MerchantLocations();
		String s = MerchantLocations.getRawJsonData();
		for (BCLocation bcl : MerchantLocations.getLocationsFromJsonString(s)) {
			locations.add(bcl);
		}
	}

	private void displayUpdatedLocations(List<BCLocation> locations, GoogleMap m) {
		for (BCLocation b : locations) {
			m.addMarker(new MarkerOptions().position(b.m_latlng)
					.title(b.m_title).snippet(b.getSnippet()));
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this); // Add this method.
	}

	@Override
	protected void onResume() {
		super.onResume();
	    locationManager.requestLocationUpdates(provider, 400, 1, this);
        locate();
	}

	@Override
	protected void onPause() {
		super.onPause();
		locationManager.removeUpdates(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this); // Add this method.
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menuItem = menu.add(0, MENU_ITEM_SETTINGS, 0, "Settings");
		menuItem.setIcon(android.R.drawable.ic_menu_preferences);
		menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menuItem.setEnabled(false);
		
		menuItem = menu.add(0, 1, 1, "About");
		menuItem.setIcon(android.R.drawable.ic_menu_info_details);
		menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

		menuItem = menu.add(0, 2, 2, "Locate me");
		menuItem.setIcon(android.R.drawable.ic_menu_mylocation);
		menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_ABOUT:
            displayAbout();
			break;
		case MENU_ITEM_LOCATE_ME:
			locate();
			break;
		case MENU_ITEM_SETTINGS:

			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

    private void displayAbout() {

        mGaTracker.sendEvent("MainMap", "button_press","about", new Long(0));

        new AlertDialog.Builder(this)
                .setTitle("About bitcoin businesses")
                .setMessage(
                        "For the details about the contributors, visit https://github.com/ragmondo/bitcoinbiz or http://www.getbitcoin.info")
                .setPositiveButton("bitcoinbiz @ github", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String url = "https://github.com/ragmondo/bitcoinbiz";
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);

                    }
                }).setNegativeButton("www.getbitcoin.info", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String url = "http://www.getbitcoin.info";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);

            }
        }).create().show();
    }

    private void locate() {
		Log.d(Tag, "locate");
        mGaTracker.sendEvent("MainMap", "button_press","locate", new Long(0));

		boolean enabled = locationManager
		  .isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager
		  .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		if (!enabled) {
			new AlertDialog.Builder(this)
					.setTitle("Location access not enabled")
					.setMessage(
							"Would you like to go into your location settings now to enable it?")
					.setPositiveButton("Yes", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							Intent intent = new Intent(
									Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							startActivity(intent);

						}
					}).setNegativeButton("No", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(position!=null) {
								addMarkerAndShow();
								Toast.makeText(BitcoinBizActivity.this, "Showing last known location", Toast.LENGTH_SHORT).show();
							}
						}
					}).create().show();
		} else {
			if(position!=null) {
				addMarkerAndShow();
			} else {
				Log.d(Tag, "Location is null ");
			}
		}
	}
	
	private void addMarkerAndShow(){
		LatLng latLng = new LatLng(position.getLatitude(), position.getLongitude());
		positionMarker = mMap.addMarker(new MarkerOptions()
        .position(latLng)
        .title("Melbourne")
        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
		moveMapToLocation(latLng);
	}
	
	private void moveMapToLocation(LatLng latLng){
		Log.d(Tag, "moveMapToLocation  "+latLng);
		CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, 7.5f);
		mMap.animateCamera(update);
	}

	@Override
	public void onLocationChanged(Location location) {
		this.position = location;
		positionMarker = mMap.addMarker(new MarkerOptions()
        .position(new LatLng(position.getLatitude(), position.getLongitude()))
        .title("You")
        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
	}

	@Override
	public void onProviderDisabled(String provider) {
	    Toast.makeText(this, "Disabled provider " + provider,
	            Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onProviderEnabled(String provider) {
	    Toast.makeText(this, "Enabled new provider " + provider,
	        Toast.LENGTH_SHORT).show();
	}

	//FIXME react to status changes of the provider
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		switch (status) {
		case LocationProvider.AVAILABLE:
			
			break;
		case LocationProvider.OUT_OF_SERVICE:
			
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			
			break;
		default:
			break;
		}
	}
}
