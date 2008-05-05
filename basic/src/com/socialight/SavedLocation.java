package com.socialight;

/**
 * holds a location object (different from low-level jsr179 Location. this one
 * is meant to abstract both auto-location/gps as well as manual location entry
 * 
 * @author naveen
 */
public class SavedLocation {
	
	private final String RECORD_STORE_DELIMITER = "|";
	
	// TODO might want to store GPS geocoded location here as well

	private int timestamp = 0;
	private String address = "";
	private String latitude = "";
	private String longitude = "";
	
	public SavedLocation() {}
	
	public SavedLocation(String _a, double _lat, double _long) {
		this(_a, Double.toString(_lat), Double.toString(_long));
	}
	
	public SavedLocation(String _a, String _lat, String _long) {
		address = _a;
		latitude = _lat;
		longitude = _long;
	}

	/**
	 * string is in this format: 202 1st Avenue, New York, NY|40.35|-73.89
	 */
	public SavedLocation(String _locationAsString) {
		if (_locationAsString != null && !_locationAsString.equals("")) {
			int index = _locationAsString.indexOf(RECORD_STORE_DELIMITER);
			address = _locationAsString.substring(0, index);
			_locationAsString = _locationAsString.substring(index+1);

			index = _locationAsString.indexOf("|");
			latitude = _locationAsString.substring(0, index);
			_locationAsString = _locationAsString.substring(index+1);
			
			longitude = _locationAsString;
		}
	}
	
	// TODO remove this style of doing things
	public String toString() {
		return address + "|" + latitude + "|" + longitude;
	}
	
	public int getTimestamp() { return timestamp; }
	public void setTimestamp(int _t) { timestamp = _t; }
	
	public void setAddress(String _address) {
		if (_address != null) { address = _address; } 
	}
	
	public void setLatitude(String _lat) {
		if (_lat != null) { latitude = _lat; }
	}
	
	public void setLongitude(String _lon) {
		if (_lon != null) { longitude = _lon; }
	}
	
	public String getAddress() { return address; }
	public String getLatitude() { return latitude; }
	public String getLongitude() { return longitude; }
	
	public boolean isValid() {
		return (latitude != "" && longitude != "");
	}
}
