package com.socialight;

import javax.microedition.location.Criteria;
import javax.microedition.location.Location;
import javax.microedition.location.LocationException;
import javax.microedition.location.LocationListener;
import javax.microedition.location.LocationProvider;

/**
 * A JSR179-based locator.
 * 
 * @author naveen
 */
public class PassiveLocator implements LocationListener {
	private Socialight s;
	
	private LocationProvider provider;
	
	private UpdateHandler handler;
	private boolean done;
	private float moveDistanceAllowed;
	
	private Location latestLocation;
	private String statusDescription = "";
	private boolean statusGps = false;
	
	/** auto-update the location every NN seconds */
	/** TODO: make it so that the initial lookup is immediate and then subsequent are 30 seconds or so */
	private int LOCATION_UPDATE_INTERVAL = 5;

	public PassiveLocator(Socialight _s, float _md) throws SecurityException {
		s = _s;
		moveDistanceAllowed = _md;
		
		createLocationProvider();

		done = false;
		
		if (provider != null) {
			handler = new UpdateHandler();
			new Thread(handler).start();
		}
	}
	
	public void createLocationProvider() throws SecurityException {
		if (provider == null) {
			Criteria cr = new Criteria();
			//cr.setPreferredPowerConsumption(Criteria.POWER_USAGE_HIGH);
			
			try {
				provider = LocationProvider.getInstance(cr);
				provider.setLocationListener(this, LOCATION_UPDATE_INTERVAL, -1, -1);
			} catch (LocationException le) {
				System.out.println("LocationException: " + le.toString());
				statusDescription = "LocationException: " + le.toString();
				statusGps = false;
			}
		}
		
		getLastKnownLocation();
	}
	
	public void pauseLocationProvider() {
		System.out.println("pausing location provider");
		done = true;

		provider.setLocationListener(null, LOCATION_UPDATE_INTERVAL, -1, -1);
		provider = null;
	}
	
	public void unpauseLocationProvider() {
		createLocationProvider();
		done = false;
	}

	/**
	 * so that we have something to start with as a default
	 * 
	 * TODO: if nothing here; then suggest pulling in a default lat/lon from
	 * properties file so that there's always a valid starting point
	 * 
	 * TODO: should we force a "notify" here?
	 */
	private void getLastKnownLocation() {
		Location lastValidLocation = null;
		
		try {
			lastValidLocation = LocationProvider.getLastKnownLocation();
		} catch (SecurityException se) {
			System.out.println("SecurityException: " + se.toString());
		}
		
		if (lastValidLocation != null && lastValidLocation.isValid()) {
			System.out.println("latestLocation = lastValidLocation");
			latestLocation = lastValidLocation;
		}
		statusDescription = "last known";
	}

	public void locationUpdated(LocationProvider provider, Location location) {
		if (handler != null) { handler.handleUpdate(location); }
		
		// notify only if change has occurred or if this is the first time
		Location last = s.getLocation();
		
		if (latestLocation != null) {
			if ((last == null) ||
				((last != null) && (last.getQualifiedCoordinates().distance(latestLocation.getQualifiedCoordinates()) > moveDistanceAllowed))) {
					s.notifyLocationUpdated(latestLocation, statusGps, statusDescription);
			}
		}
	}

	public void providerStateChanged(LocationProvider provider, int newState) {}

	class UpdateHandler implements Runnable {
		private Location updatedLocation = null;

		public void run() {
			Location locationToBeHandled = null;

			while (!done) {
				synchronized(this) {
					if (updatedLocation == null) {
						try {
							wait();
						} catch (Exception e) {
							// Handle interruption
						}
					}
					locationToBeHandled = updatedLocation;
					updatedLocation = null;
				}

				if  (locationToBeHandled != null)
					processUpdate(locationToBeHandled);
				updatedLocation = null;
			}
		}

		public synchronized void handleUpdate(Location update) {
			updatedLocation = update;
			notify();
		}

		private void processUpdate(Location update) {
			getStatus();
			
			if (update != null && update.isValid()) {
				latestLocation = update;
				statusDescription = "OK";
				statusGps = true;
			} else {
				statusDescription = getStatus() + "/no fix";
				statusGps = false;
			}
		}
	}
	
	/** TODO: right now, only a textual description of LocationProvider status; but has to
	 * take into account other statuses related to location validity and then must return
	 * something that makes sense in the context of interface updates
	 */
	public String getStatus() {
		String ret = "";
		
		if (provider != null) {
	
			if (provider.getState() == LocationProvider.AVAILABLE) {
				ret = "available";
			} else if (provider.getState() == LocationProvider.OUT_OF_SERVICE) {
				ret = "out of service";
			} else if (provider.getState() == LocationProvider.TEMPORARILY_UNAVAILABLE) {
				ret = "unavailable";
			}
		
		}
		
		return ret;
	}

	public void setUpdateInterval(int _seconds) {
		if (_seconds > 0) { LOCATION_UPDATE_INTERVAL = _seconds; }
	}
}
