package com.socialight;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

/**
 * model to help serialize User object.
 * 
 * TODO: give recordstore the ability to store any XML blob. this way it'll store <User></User>
 * or <Location></Location> and we won't have to deal with byte ordering or parsing/checking.
 * we can just dump the full XML into and out of this serialization and can then pass it back
 * to the User object (perhaps like User.extractFromXml(urs.toXml()))
 * 
 * @author naveen
 */
public class UserRecordStore {
	private static final String STORE_NAME = "Socialight";
	private RecordStore recordStore;

	public UserRecordStore() throws RecordStoreException {
		recordStore = RecordStore.openRecordStore(STORE_NAME, true);
	   	System.out.println("Opened the " + STORE_NAME +" DB");
	}

	public void addUser(User currentUser) throws IOException, RecordStoreException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream os = new DataOutputStream(baos);

		// TODO if userId is null, this will probably screw up the record store
		if (currentUser.userId != null) {
			os.writeUTF(currentUser.userId);	
		}

		if (currentUser.nickName == null) { currentUser.nickName = "-"; }
		os.writeUTF(currentUser.nickName);
			
		if (currentUser.firstName == null) { currentUser.firstName = ""; }	
		os.writeUTF(currentUser.firstName);							
		
		if (currentUser.lastName == null) { currentUser.lastName = ""; }		
		os.writeUTF(currentUser.lastName);
		
		if (currentUser.photo == null) { currentUser.photo = ""; }
		os.writeUTF(currentUser.photo);
		os.writeInt(currentUser.recordId);
		
		// TODO this needs to move to an iterator; also store count of number locations (size) inside record store
		os.writeUTF(currentUser.getLocation(1) != null ? currentUser.getLocation(1).toString() : "");
		os.writeUTF(currentUser.getLocation(2) != null ? currentUser.getLocation(2).toString() : "");
		os.writeUTF(currentUser.getLocation(3) != null ? currentUser.getLocation(3).toString() : "");
		
		os.close();

		//get the byte array with the saved values
		byte[] data = baos.toByteArray();

		//Write the record to the record store
		int id = recordStore.addRecord(data, 0, data.length);

		System.out.println("Record added - id #" + id);
	}
	
	private User readRecord(byte[] recordData) throws IOException {
		User u = new User();
		DataInputStream is = new DataInputStream(new ByteArrayInputStream(recordData));
		
		u.userId = is.readUTF();
		System.out.println("Record User ID: " + u.userId);
		
		u.nickName = is.readUTF();
		u.firstName = is.readUTF();
		u.lastName = is.readUTF();			
		u.photo = is.readUTF();
		u.recordId = is.readInt();

		u.addLocation(new SavedLocation(is.readUTF()));
		u.addLocation(new SavedLocation(is.readUTF()));
		u.addLocation(new SavedLocation(is.readUTF()));

		is.close();
		
		return u;
	}

	public User returnLastRecord() throws IOException, RecordStoreException {
		User u = null;
		
		// TODO there seems to be no way to get the last record other than to iterate?
		RecordEnumeration e = recordStore.enumerateRecords(null, null, false);
		while (e.hasNextElement()) {
			u = readRecord(recordStore.getRecord(e.nextRecordId()));
		}

		return u;
	}

	public void close() throws RecordStoreException {
		if (recordStore != null) recordStore.closeRecordStore();
	}

	public int getNumberOfRecords() throws RecordStoreException {
		return (recordStore != null) ? recordStore.getNumRecords() : 0;
	}
	
	public void deleteAllRecords() throws RecordStoreException {
		if (recordStore != null) {
			RecordEnumeration e = recordStore.enumerateRecords(null, null, false);

			while(e.hasNextElement()) {
				recordStore.deleteRecord(e.nextRecordId());
			}
		}
	}
	
	public int getNextId() throws RecordStoreException {
		return recordStore.getNextRecordID();  
	}
}
