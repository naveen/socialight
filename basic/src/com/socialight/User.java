package com.socialight;

import java.util.Enumeration;
import java.util.Vector;

/**
 * model to hold user details and last three good locations
 * 
 * @author naveen
 */
public class User {

	public User() {}

	public int recordId;
	public String userId;
	public String nickName = "";
	public String firstName = "";
	public String lastName = "";
	public String photo = "";
	
	private Vector locations = new Vector(3);
 		
	public void empty() {
		userId = null;
		nickName = null;
		firstName = null;
		lastName = null;
		photo = null;
		recordId = 0;

		locations = null;
	}
	
	public void addLocation(SavedLocation _sl) {
		SavedLocation check = null;
		
		if (_sl != null && !_sl.getAddress().equals("")) {

			if (!locations.isEmpty()) {
				// make sure this address is not a duplicate - sad loop for now
				Enumeration e = locations.elements();
				while (e.hasMoreElements()) {
					check = (SavedLocation)e.nextElement();
					
					// if it turns out to be a duplicate, remove the existing one
					// and use the latest as it could have updated lat/lon data
					if (check.getAddress().equals(_sl.getAddress())) {
						System.out.println("removing " + check.getAddress());
						locations.removeElement(check);
					}
				}
				
				// TODO insert at 0; remove from end doesn't seem to work right?
				locations.addElement(_sl);

				if (locations.size() > 3) {
					locations.removeElementAt(0);
				}
			} else {
				locations.addElement(_sl);
			}
			
		}
		
		// TODO this is a hack for now to get chrono involved; see note above
		Vector rev = new Vector();
		for (int j = locations.size() - 1; j >= 0; j--) {
			rev.addElement(locations.elementAt(j));
		}
		
		locations = rev;
	}
	
	public SavedLocation getLocation(int selectedIndex) {
		SavedLocation l = null;
		
		// TODO for now, reset the index back one. in future, iterator!
		if (locations != null && locations.size() > 0) {
			try {
				l = (SavedLocation)locations.elementAt(selectedIndex-1);
			} catch (ArrayIndexOutOfBoundsException aioobe) {}
		}
		
		return l;
	}
	
	public Vector getLocations() { return locations; }
}
