package com.socialight;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.ContentConnection;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Image;

import org.kxml.Xml;
import org.kxml.io.ParseException;
import org.kxml.parser.ParseEvent;
import org.kxml.parser.XmlParser;

/**
 * helps with server communication, XML model handling and image uploads
 * 
 * @author michael.sharon
 * @author naveen
 */
public class HttpThread extends Thread {
	private Socialight s;

	private String mURL;

	public HttpConnection con;

	private boolean mTrucking;

	private String mode;

	// vector that contains all the incoming notes
	private Vector notes = new Vector(5);

	private Note note = new Note();
	private ListItem li = new ListItem();
	
	private SavedLocation sl = new SavedLocation();
	private String geoCodingError = "";
	
	private boolean inside = false;

	// booleans for the authentication
	private boolean id = false;
	private boolean nickname = false;
	private boolean firstname = false;
	private boolean lastname = false;
	private boolean photo = false;

	private boolean error = false;

	private boolean item = false;

	private boolean link;
	private boolean title;
	private boolean address;

	private boolean author = false;
	private boolean created;
	private boolean text;
	private boolean longitude;
	private boolean latitude;
	private boolean place;
	private boolean distance;
	private boolean tags;
	private boolean imageLink;
	private boolean mapLink;
	
	private boolean layer;

	private boolean imageSent;

	// create the constructor
	public HttpThread(Socialight _s) {
		s = _s;
		mTrucking = true;
	}

	public synchronized void restart() {
		mTrucking = true;
		yield();
		System.out.println("restart: mTrucking = " + mTrucking);
	}

	public synchronized void run() {
		System.out.println("naveen: " + toString() + ".run()");
		System.out.println("naveen: active threads: " + Thread.activeCount());
		
		while (mTrucking) {
			try {
				wait();
			} catch (InterruptedException ie) {
				// ie.printStackTrace();
				s.notice(ie.toString());
			}
			if (mTrucking) {
				connect();
			}
			System.out.println("mTrucking = " + mTrucking);
		}
	}

	public synchronized void go(String url, String theMode) {
		System.out.println("HTTP: " + toString() + ".go(" + url + ", " + theMode + ")");
		mURL = url;
		mode = theMode;
		notify();
	}
	
	public synchronized void cancel() {
		mTrucking = false;
		try {
			if (con != null)
				con.close();
			Thread.yield();
		} catch (IOException ignored) {
			ignored.printStackTrace();
		}
		notify();
	}

	public Vector getNotes() {
		return notes;
	}

	public void clearLocalNotesCache() {
		notes.removeAllElements();
	}

	public void setImageSent(boolean _imageSent) {
		this.imageSent = _imageSent;
	}

	public synchronized Image getURLImage(String theURL) {
		Image im = null;
		ContentConnection connection;
		DataInputStream iStrm;
		System.out.println("Image URL is " + theURL);
		try {
			connection = (ContentConnection) Connector.open(theURL);
			iStrm = connection.openDataInputStream();

			byte imageData[];
			ByteArrayOutputStream bStrm = new ByteArrayOutputStream();
			int ch;
			while ((ch = iStrm.read()) != -1) {
				bStrm.write(ch);
				// System.out.println(ch);
			}
			imageData = bStrm.toByteArray();
			bStrm.close();
			im = Image.createImage(imageData, 0, imageData.length);

			if (iStrm != null) {
				iStrm.close();
			}
			if (connection != null) {
				connection.close();
			}
			
			System.out.println("the image has been successfully downloaded.");
		} catch (IllegalArgumentException iae) {
			s.notice(iae.toString());
		} catch (IOException ioe) {
			s.notice(ioe.toString());
		}

		return im;
	}
	
	public void setNote(Note _n) {
		note = _n;
	}
	
	public Image getNoteImage() {
		return (note != null) ? note.image : null;
	}

	public Image getNoteMap() {
		return (note != null) ? note.map : null;
	}
	
	/**
	 * opens an HTTP connection to Database
	 */
	private synchronized void connect() {

		InputStream is = null;
		OutputStream out = null;
		String xml = "";
		boolean status = false;

		System.out.println("URL is " + mURL);
		
		try {
			if (mode == "downloadImage") {				
				note.image = getURLImage(mURL);
			} else if (mode == "downloadMap") {
				note.map = getURLImage(mURL);
			} else if ((mode == "ssImage") && (!imageSent)) {
				System.out.println("Uploading Image");
				s.progress.updateMessage("Uploading note");
				xml = sendHTTPPostImage(mURL, s.raw);
				
				// call XML parsing class
				// clear the image byte array
									
				// empty the current vector of objects
				clearLocalNotesCache();
				
				// grab any new notes that are available for us
				status = updateLoc(xml);

				if (status) {
					// add output to form
					s.createNewNoteList();
				}
				
				System.out.println("after update, in mode: " + mode);

				// check that we successfully added the note
				s.progress.updateMessage("Your note has been uploaded");
				System.out.println("Your note has been uploaded. Mode = " + mode);
				// set display to xml output. reset fields
				s.createNoteResult(mode);
			} else {
				System.out.println("Sending...");
				
				con = (HttpConnection) Connector.open(mURL,
							Connector.READ_WRITE);
					// prevent outgoing connections from being reused
					con.setRequestProperty("Connection", "close");
					// allow outgoing connections to be reused
					// con.setRequestProperty("Connection", "keep-alive");
	
					// set the request method to GET
					con.setRequestMethod(HttpConnection.GET);
					con.setRequestProperty("Accept", "text/xml");
					con.setRequestProperty("User-Agent", "Profile/J2ME; Socialight MIDP2 v0.1");
					con.setRequestProperty("Authorization", "Token " + s.getAuthToken());
					is = con.openInputStream();
				
				// Pull back a response from the server
				System.out.println("receiving from server");

				// Read the file from the server
				ByteArrayOutputStream bas = new ByteArrayOutputStream();
				int ch;
				while ((ch = is.read()) != -1) {
					bas.write(ch);
					System.out.print(".");
				}
				System.out.println("done receiving");

				// turn the bytes into XML
				xml = bas.toString();

				if (mode == "login") {
					// check if the user has been authenticated
					System.out.println("XML = " + xml);
					s.authenticateUser(getUserXML(xml));
				} else if (mode == "addss") {
					s.progress.updateMessage("Receiving data from server");
					// call XML parsing class

					// empty the current vector of objects
					clearLocalNotesCache();

					// grab any new notes that are available for us
					status = updateLoc(xml);

					if (status) {
						// add output to form
						s.createNewNoteList();
						s.vibrate();
					} else {
						// some error occured
						System.out.println("Error occured after create note/update");
						s.currentMode = Socialight.UPDATE_LOC_MODE;
						mode = "";
						mURL = "";
					}

					s.progress.updateMessage("A new note has been added");
					System.out.println("A new note has been added.");

					// set display to xml output. reset fields
					s.createNoteResult(mode);
				} else if (mode == "uLoc") {
					// empty the current notes vector of objects
					clearLocalNotesCache();

					// grab any new notes that are available for us
					status = updateLoc(xml);

					if (status) {
						// display incoming notification
						s.createNewNoteList();
						s.vibrate();
					}

					s.showNoteList();
				} else if (mode == "geocode") {
					geocode(xml);
				} else if (mode == "checkin") {
					// TODO catch errors on checkin? if successful, do nothing (silent) and perhaps update local lat/lon
					try {
						parseCheckinXML(new XmlParser(new InputStreamReader(new ByteArrayInputStream(xml.getBytes()))));
					} catch (IOException ioe) {
						s.notice(ioe.toString());
					}
				} else if (mode == "layers") {
					
					System.out.println("attempting to fetch layers");
					
					try {
						parseLayersXML(new XmlParser(new InputStreamReader(new ByteArrayInputStream(xml.getBytes()))));
					} catch (IOException ioe) {
						s.notice(ioe.toString());
					}
					
					s.createLayers();
				}
			}
		} catch (IOException ioe) {
			// TODO this will probably occur if the remote host can't be
			// reached or if it sends back invalid data; show a message to the user
			// but allow then to continue
			System.out.println("Exception " + ioe.getMessage());
			s.notice(ioe.toString());
		} catch (SecurityException se) {
			s.fatal("The application cannot access Socialight's servers and therefore has quit.");
		} finally {
			try {
				if (null != out)
					out.close();
				if (null != is)
					is.close();
				if (null != con)
					con.close();
			} catch (Exception ex) {
				System.out.println("Exception while closing connection.");
				if ((null != out) || (null != is) || (null != con))
					try {
						if (null != out)
							out.close();
						if (null != is)
							is.close();
						if (null != con)
							con.close();
					} catch (IOException e) {
						s.notice(e.toString());
					}
			}
		}
	}

	private boolean getUserXML(String xml) {
		ByteArrayInputStream bin = new ByteArrayInputStream(xml.getBytes());
		boolean authenticated = false;
		
		try {
			XmlParser parser = new XmlParser(new InputStreamReader(bin));
			authenticated = parseProfileXML(parser, false);
		} catch (IOException e) {
			System.out.println("IOException while parsing XML file.");
			e.printStackTrace();
		}
		return authenticated;
	}
	
	private void geocode(String xml) {
		ByteArrayInputStream bin = new ByteArrayInputStream(xml.getBytes());
		try {
			XmlParser parser = new XmlParser(new InputStreamReader(bin));
			parseGeocodeXML(parser);
			s.handleGeocodeResult(sl, geoCodingError);
		} catch (Exception e) {
			s.notice(e.toString());
		}
	}

	private boolean updateLoc(String xml) {
		boolean status = false;
		ByteArrayInputStream bin = new ByteArrayInputStream(xml.getBytes());
		
		// reset contents of local note var (and inside parseUpdate, reset it between note objects)
		note.clear();

		try {
			XmlParser parser = new XmlParser(new InputStreamReader(bin));
			status = parseUpdateXML(parser);
		} catch (ParseException pe) {
			System.out.println("ParseException: " + pe.toString());
		} catch (Exception e) {
			System.out.println("Exception while parsing XML file.");
			e.printStackTrace();
		}
		
		return status;
	}

	private void parseGeocodeXML(XmlParser parser) throws IOException {
		boolean leave = false;

		do {
			ParseEvent event = parser.read();
			switch (event.getType()) {
			case Xml.START_TAG:
				if (event.getName().equals("latitude")) {
					inside = true;
					latitude = true;
				} else if (event.getName().equals("longitude")) {
					inside = true;
					longitude = true;
				} else if (event.getName().equals("error")) {
					inside = true;
					error = true;
				}

				parseGeocodeXML(parser);
				break;

			case Xml.END_TAG:
				if (event.getName().equals("latitude")) {
					latitude = false;
				} else if (event.getName().equals("longitude")) {
					longitude = false;
				} else if (event.getName().equals("error")) {
					error = false;
				}

				inside = false;
				break;

			case Xml.END_DOCUMENT:
				leave = true;
				break;

			case Xml.TEXT:
				if (inside) {

					if (latitude) {
						sl.setLatitude(event.getText());
						latitude = false;
					} else if (longitude) {
						sl.setLongitude(event.getText());
						longitude = false;
					} else if (error) {
						geoCodingError = event.getText();
						error = false;
					}
				}
				break;

			case Xml.WHITESPACE:
				break;

			default:
			}
		} while (!leave);
	}
	
	private void parseCheckinXML(XmlParser parser) throws IOException {
		boolean leave = false;

		do {
			ParseEvent event = parser.read();
			switch (event.getType()) {
			case Xml.START_TAG:
				if (event.getName().equals("address")) {
					inside = true;
					address = true;
				} else if (event.getName().equals("error")) {
					inside = true;
					error = true;
				}

				parseCheckinXML(parser);
				break;

			case Xml.END_TAG:
				if (event.getName().equals("address")) {
					address = false;
				} else if (event.getName().equals("error")) {
					error = false;
				}

				inside = false;
				break;

			case Xml.END_DOCUMENT:
				leave = true;
				break;

			case Xml.TEXT:
				if (inside) {
					if (address) {
						s.setLatestLocationAddress(event.getText());
						address = false;
					} else if (error) {
						System.out.println("Error in checkin: " + event.getText());
						error = false;
					}
				}
				break;

			case Xml.WHITESPACE:
				break;

			default:
			}
		} while (!leave);
	}
	
	private void parseLayersXML(XmlParser parser) throws IOException {
		boolean leave = false;

		do {
			// TODO this can throw a ParseException ($DefaultParserException)
			// if the rails side dumps a 404 or something.
			ParseEvent event = parser.read();
			
			switch (event.getType()) {
			case Xml.START_TAG:
				if (event.getName().equals("layer")) {
					layer = true;
				} else if (event.getName().equals("name") && (layer == true)) {
					inside = true;
					title = true;
				} else if (event.getName().equals("link") && (layer == true)) {
					inside = true;
					link = true;
				}

				parseLayersXML(parser);
				break;

			case Xml.END_TAG:
				if (event.getName().equals("layer")) {
					s.addLayer(li.clone());
					layer = false;
				} else if (event.getName().equals("name") && (layer == true)) {
					title = false;
				} else if (event.getName().equals("link") && (layer == true)) {
					link = false;
				}

				inside = false;
				break;

			case Xml.END_DOCUMENT:
				leave = true;
				break;

			case Xml.TEXT:
				if (inside) {
					if (title) {
						li.setTitle(event.getText());
						title = false;
					} else if (link) {
						li.setKey(event.getText());
						link = false;
					}
					
					inside = false;
				}
				break;
				
			default:
			}
		} while (!leave);
	}

	// gives back user profile object that helps auto-login user
	private boolean parseProfileXML(XmlParser parser, boolean wasAuthenticated)
			throws IOException {
		boolean leave = false;
		boolean authenticated = wasAuthenticated;
		
		System.out.println("naveen: parseProfileXML");

		do {
			ParseEvent event = parser.read();
			switch (event.getType()) {
			case Xml.START_TAG:
				if (event.getName().equals("id")) {
					System.out.println("Got ID Start");
					inside = true;
					id = true;
					authenticated = true;
				} else if (event.getName().equals("nickname")) {
					System.out.println("Got Nickname Start");
					inside = true;
					nickname = true;
				} else if (event.getName().equals("firstname")) {
					System.out.println("Got FIrstname Start");
					inside = true;
					firstname = true;
				} else if (event.getName().equals("lastname")) {
					System.out.println("Got Lastname start");
					inside = true;
					lastname = true;
				} else if (event.getName().equals("photo")) {
					System.out.println("Got PHoto start");
					inside = true;
					photo = true;
				} else if (event.getName().equals("timecreated")) {
					System.out.println("got timecreated start");
					inside = true;
					created = true;
				} else if (event.getName().equals("error")) {
					System.out.println("got error start");
					inside = true;
					error = true;
					break;
				}
				authenticated = parseProfileXML(parser, authenticated);
				break;

			case Xml.END_TAG:
				if (event.getName().equals("id")) {
					System.out.println("got id end");
					id = false;
				} else if (event.getName().equals("nickname")) {
					System.out.println("got nickname ending");
					nickname = false;
				} else if (event.getName().equals("firstname")) {
					System.out.println("got firstname ending");
					firstname = false;
				} else if (event.getName().equals("lastname")) {
					System.out.println("got lastname ending");
					lastname = false;
				} else if (event.getName().equals("photo")) {
					System.out.println("got photo ending");
					photo = false;
				} else if (event.getName().equals("timecreated")) {
					System.out.println("got timecreated ending");
					created = false;
				} else if (event.getName().equals("error")) {
					System.out.println("got error ending");
					error = false;
				}

				// I could be wrong but I don't think you want to leave here
				// leave = true;
				inside = false;
				break;

			case Xml.END_DOCUMENT:
				leave = true;
				// authenticated = true; // Are we really authenticated here?
				System.out.println("Ending the XML parsing");
				break;

			case Xml.TEXT:
				if (inside) {
					// add the relevant details to the user object
					if (id) {
						s.user.userId = event.getText();
						System.out.println("mainApp.user.userId = " + s.user.userId);
						id = false;
					} else if (nickname) {
						s.user.nickName = event.getText();
						System.out.println("xml.nickname = " + event.getText());
						nickname = false;
					} else if (firstname) {
						s.user.firstName = event.getText();
						System.out.println("xml.firstname = " + s.user.firstName);
						firstname = false;
					} else if (lastname) {
						s.user.lastName = event.getText();
						System.out.println("xml.lastname = " + s.user.lastName);
						lastname = false;
					} else if (photo) {
						s.user.photo = event.getText();
						System.out.println("xml.photo = " + s.user.photo);
						photo = false;
					} else if (created) {
						System.out.println("xml.timeCreated= " + event.getText());
						created = false;
					} else if (error) {
						System.out.println("Error! " + event.getText());
						s.fatal(event.getText());
						error = false;
					}
				}
				break;

			case Xml.WHITESPACE:
				break;

			default:
			}
		} while (!leave);
		return authenticated;
	}

	private boolean parseUpdateXML(XmlParser parser) throws IOException, ParseException {
		boolean returnStatus = true;
		boolean leave = false;

		do {
			ParseEvent event = parser.read();
			switch (event.getType()) {
			case Xml.START_TAG:
				if (event.getName().equals("note")) {
					item = true;
				} else if ((event.getName().equals("id")) && (item == true)) {
					inside = true;
					id = true;
				} else if ((event.getName().equals("title")) && (item == true)) {
					inside = true;
					title = true;
				} else if ((event.getName().equals("link")) && (item == true)) {
					inside = true;
					link = true;
				} else if ((event.getName().equals("author")) && (item == true)) {
					inside = true;
					author = true;
					note.setAuthorId(event.getValue("id"));
				} else if ((event.getName().equals("created")) && (item == true)) {
					inside = true;
					created = true;
				} else if ((event.getName().equals("text")) && (item == true)) {
					inside = true;
					text = true;
				} else if ((event.getName().equals("latitude")) && (item == true)) {
					inside = true;
					latitude = true;
				} else if ((event.getName().equals("longitude")) && (item == true)) {
					inside = true;
					longitude = true;
				} else if ((event.getName().equals("address")) && (item == true)) {
					inside = true;
					address = true;
				} else if ((event.getName().equals("place")) && (item == true)) {
					inside = true;
					place = true;
				} else if ((event.getName().equals("distance")) && (item == true)) {
					inside = true;
					distance = true;
				} else if (event.getName().equals("error")) {
					inside = true;
					error = true;
					break;
				} else if (event.getName().equals("tags")) {
					inside = true;
					tags = true;
				} else if (event.getName().equals("imageLink")) {
					inside = true;
					imageLink = true;
				} else if (event.getName().equals("mapLink")) {
					inside = true;
					mapLink = true;
				}
				parseUpdateXML(parser);
				break;

			case Xml.END_TAG:
				if (event.getName().equals("note")) {
					notes.addElement(note.copy());
					note.clear();

					inside = false;
					item = false;
					break;
				} else if ((event.getName().equals("id")) && (item == true)) {
					inside = false;
					id = false;
				} else if ((event.getName().equals("title")) && (item == true)) {
					inside = false;
					title = false;
				} else if ((event.getName().equals("link")) && (item == true)) {
					inside = false;
					link = false;
				} else if ((event.getName().equals("author")) && (item == true)) {
					inside = false;
					author = false;
				} else if ((event.getName().equals("created")) && (item == true)) {
					inside = false;
					created = false;
				} else if ((event.getName().equals("text")) && (item == true)) {
					inside = false;
					text = false;
				} else if ((event.getName().equals("latitude")) && (item == true)) {
					inside = false;
					latitude = false;
				} else if ((event.getName().equals("longitude")) && (item == true)) {
					inside = false;
					longitude = false;
				} else if ((event.getName().equals("address")) && (item == true)) {
					inside = false;
					address = false;
				} else if ((event.getName().equals("place")) && (item == true)) {
					inside = false;
					place = false;
				} else if ((event.getName().equals("distance")) && (item == true)) {
					inside = false;
					distance = false;
				} else if (event.getName().equals("tags")) {
					inside = false;
					tags = false;
				} else if (event.getName().equals("imageLink")) {
					inside = false;
					imageLink = false;
				} else if (event.getName().equals("mapLink")) {
					inside = false;
					mapLink = false;
				} else if (event.getName().equals("error")) {
					inside = false;
					error = false;
					break;
				}
				break;

			case Xml.END_DOCUMENT:
				leave = true;
				break;

			case Xml.TEXT:
				if (inside) {
					// add the relevant details to the list vector
					// this parsing isn't working right
					if (id) {
						note.setId(event.getText());
						id = false;
						inside = false;
						break;
					} else if (title) {
						note.setTitle(event.getText());
						title = false;
						inside = false;
						break;
					} else if (link) {
						note.setLink(event.getText());
						link = false;
						inside = false;
						break;
					} else if (author) {
						note.setAuthor(event.getText());
						link = false;
						inside = false;
						break;
					} else if (created) {
						note.setSet(event.getText());
						created = false;
						inside = false;
						break;
					} else if (text) {
						note.setText(event.getText());
						text = false;
						inside = false;
						break;
					} else if (latitude) {
						note.setLatitude(event.getText());
						latitude = false;
						inside = false;
						break;
					} else if (longitude) {
						note.setLongitude(event.getText());
						longitude = false;
						inside = false;
						break;
					} else if (address) {
						note.setAddress(event.getText());
						address = false;
						inside = false;
					} else if (place) {
						note.setPlace(event.getText());
						place = false;
						inside = false;
					} else if (distance) {
						note.setDistance(event.getText());
						distance = false;
						inside = false;
					} else if (tags) {
						note.setTags(event.getText());
						tags = false;
						inside = false;
						break;
					} else if (imageLink) {
						note.setImageLink(event.getText());
						imageLink = false;
						inside = false;
					} else if (mapLink) {
						note.setMapLink(event.getText());
						mapLink = false;
						inside = false;
					} else if (error) {
						System.out.println("Error: " + event.getText());
						returnStatus = false;
						error = false;
						break;
					}
				}
				break;

			case Xml.WHITESPACE:
				break;

			default:
			}
		} while (!leave);

		return returnStatus;
	}

	/*
    * Generate the boundary for post request with Content-Type multipart/form-data
    */
    private String generateMimeBoundary() {
        String boundary ="";
        Random random = new Random();
        random.setSeed(new Date().getTime());
        
        boundary += Integer.toHexString(random.nextInt());
        boundary += Integer.toHexString(random.nextInt());

        return "---------------------------" + boundary.substring(4);
    }
	
	private String sendHTTPPostImage(String url, byte[] outputArray) {
		HttpConnection c = null;
		String response = "";
		
		String boundary = generateMimeBoundary();
		
		try {
			//url = "http://shadw.net:8080/createnote.php";
			c = (HttpConnection) Connector.open(url, Connector.READ_WRITE); 
			c.setRequestMethod(HttpConnection.POST);
						
			String post = "--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"media\"; filename=\"image_file.jpg\"\r\n"
				+ "Content-Type: " + "image/jpeg\r\n"
				+ "\r\n";
						
			String image = new String(outputArray);
			post += image;

			post += "\r\n--"+ boundary + "--\r\n";

			c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			c.setRequestProperty("Authorization", "Token " + s.getAuthToken());
			
			System.out.println("Sending message with boundary (" + boundary + ")");
			
			s.progress.updateMessage("Creating");
			DataOutputStream dos = new DataOutputStream(c.openOutputStream());
			byte[] body = post.getBytes();
			for (int i = 0; i < body.length; i++) {
				dos.writeByte(body[i]);
			}
			
			s.progress.updateMessage("Uploading");
			dos.close();

			/*

			for (int i = 0; i < outputArray.length; i++) {
				//dos.writeByte(outputArray[i]);
			}
			dos.write(outputArray);
			dos.writeUTF(endBoundary);

			mainApp.myPC.updateMessage("Sending...");
			dos.close();
			
			*/
			
			s.progress.updateMessage("Receiving update");
			//is = c.openDataInputStream();

			DataInputStream dis = new DataInputStream(c.openInputStream());
			
			int ch;
			while ((ch = dis.read()) != -1) {
				response += (char)ch;
			}
			
			dis.close();
			c.close();
			
			setImageSent(true);
		} catch (IOException ioe) {
			System.out.println("IOException: " + ioe.toString());
		} catch (NullPointerException npe) {
			System.out.println("NullPointerException: " + npe.toString());
		}
		
		System.out.println("response: " + response);   
		return response;
	}
}
