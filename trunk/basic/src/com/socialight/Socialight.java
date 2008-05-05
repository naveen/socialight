package com.socialight;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.location.Location;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.GUIControl;
import javax.microedition.media.control.VideoControl;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.RecordStoreException;

/**
 * init code: handles settings, keys, screens, navigation
 * processes login/authentication, user
 * deals with check-in (passive)
 * 
 * @author michael.sharon
 * @author naveen
 */
public class Socialight extends MIDlet implements CommandListener {
	private String CURRENT_HOST = "socialight.com";
	
	public static final int ADD_NOTE_MODE = 0;
	public static final int UPDATE_LOC_MODE = 1;

	private static Display display;

	private final static long locationTimeChangeAllowed = 1000 * 60 * 15; // 15 minutes

	private MenuList mediaChoiceMenu;
	private MenuList visibilityMenu;
	private MenuList settingsMenu;
	private MenuList locationSelectMenu;

	private MenuList mainMenu;
	
	private NoteListCanvas noteList;
	private LayerList layerList;

	public ProgressCanvas progress;
	private MessageCanvas messageCanvas;

	private Form noteTextScreen;

	// set current mode to update
	public int currentMode = UPDATE_LOC_MODE;

	// form used to show image for image note
	private Form imageCaptureForm;

	private Form geoCodeForm;
	private TextField address;

	// TODO may have to be replaced with stack
	private Displayable previousScreen;

	private Command exitCommand = new Command("Exit", Command.EXIT, 0);
	private Command backCommand = new Command("Back", Command.BACK, 0);
	private Command cancelCommand = new Command("Cancel", Command.CANCEL, 0);
	private Command createCommand = new Command("Create", Command.ITEM, 0);
	private Command nextCommand = new Command("Next", Command.ITEM, 0);
	private Command captureCommand = new Command("Capture", Command.ITEM, 0);

	private String[] mainOptions = { "Change layer", "Change location", "Create a note", "Settings", "Help", "Exit" };
	private String[] settingsOptions = { "User details", "Reset application" };
	private String[] visibilityOptions = { "Everyone", "All my contacts", "Only myself" };
	private String[] mediaOptions = { "Text", "Picture" }; // TODO: , "Pictures", "Sound", "Video" };
	
	/** Data capture / display elements */

	private StringItem geoStatusText;

	// image text
	public TextField imgText;
	private TextField txtText;

	// capturing images
	private Player mPlayer;
	private VideoControl mVideoControl;

	public byte[] raw;

	private static int MEDIA_TYPE_TEXT = 0;
	private static int MEDIA_TYPE_IMAGE = 1;
	//private static int MEDIA_TYPE_AUDIO = 2;
	//private static int MEDIA_TYPE_VIDEO = 3;

	// tracks which media has been chosen
	private int mediaType = MEDIA_TYPE_TEXT;

	private Form noteImageForm;

	private SavedLocation lastUsedLocation = new SavedLocation();

	private static Location latestGpsLocation = null;
	private String latestLocationAddress = "";
	
	public User user = new User();

	// TODO bad. testing public for image downloads
	public HttpThread http;
	
	private PassiveLocator pl;
	
	public static final int GPS_INVALID = 0;
	public static final int GPS_LAST = 1;
	public static final int GPS_LATEST = 2;

	private int selectedVisibility;

	public boolean uploadFlag = false;
	
	private ListItem currentLayerItem;
	
	public String authURL, checkinURL, layersURL,
					addNoteURL, updateURL,
					geoCodeURL = "";
	
	private String geoSecretKey = "";
	private String authToken = "";
	private String appid = "";
	
	private Vector layers = new Vector(5);
	
	private int KEYCODE_LEFT = -3;
	private int KEYCODE_RIGHT = -4;
	private int KEYCODE_OK = -5;
	private int KEYCODE_LEFT_SOFTKEY = -6;
	private int KEYCODE_RIGHT_SOFTKEY = -7;
	
	public Socialight() {
		readCustomSettings();
		setupUrls();

		if (hasLocationAPI()) {
			// TODO: catch LocationException here instead so that we can show warnings?
			try {
				pl = new PassiveLocator(this, (float)0.5);
			} catch (SecurityException se) {
				System.out.println("This application does not have permission to access your handset's location information.");
			}
		}

		// instantiate display instance
		display = Display.getDisplay(this);
	}
	
	private void readCustomSettings() {
		if (getAppProperty("Setting-Keycode-Ok") != null) { KEYCODE_OK = Integer.parseInt(getAppProperty("Setting-Keycode-Ok")); }
		if (getAppProperty("Setting-Keycode-Left-Softkey") != null) { KEYCODE_LEFT_SOFTKEY = Integer.parseInt(getAppProperty("Setting-Keycode-Left-Softkey")); }
		if (getAppProperty("Setting-Keycode-Right-Softkey") != null) { KEYCODE_RIGHT_SOFTKEY = Integer.parseInt(getAppProperty("Setting-Keycode-Right-Softkey")); }
	}
	
	public int getKeycodeOk() { return KEYCODE_OK; }
	public int getKeycodeLeftSoftkey() { return KEYCODE_LEFT_SOFTKEY; }
	public int getKeycodeRightSoftkey() { return KEYCODE_RIGHT_SOFTKEY; }
	
	public String getAuthToken() { return authToken; }
	
	private void setupUrls() {
		// setup URLs - if custom host has been specified in the JAD file, use that
		// otherwise fallback to default host
		if (getAppProperty("Socialight-App-Server") != null) {
			CURRENT_HOST = getAppProperty("Socialight-App-Server");
		}
		
		// TODO: if this doesn't exist, either show a message or show login
		// TODO: for now authToken is here, but later on we'll have to store it with the user object
		// and make use of it from there.
		authToken = getAppProperty("Socialight-Auth-Token");
		appid = getAppProperty("Socialight-App-Id");
		
		authURL = "http://" + CURRENT_HOST + "/api/profile?appid=" + appid + "&";
		
		addNoteURL = "http://" + CURRENT_HOST + "/api/createnote?appid=" + appid + "&geosecretkey=" + geoSecretKey + "&";
		
		checkinURL = "http://" + CURRENT_HOST + "/api/checkin?appid=" + appid + "&";
		geoCodeURL = "http://" + CURRENT_HOST + "/api/geocode?appid=" + appid + "&geosecretkey=" + geoSecretKey + "&address=";

		layersURL = "http://" + CURRENT_HOST + "/api/layers?appid=" + appid;
		updateURL = "http://" + CURRENT_HOST + "/api/layer?appid=" + appid + "&geosecretkey=" + geoSecretKey + "&";
	}

	protected void pauseApp() {
		System.out.println("pauseApp called");

		// TODO pause gps here as well
	}

	protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
		System.out.println("destroyApp called");
		exitMIDlet();
	}

	private void exitMIDlet() {
		notifyDestroyed();
	}

	protected void startApp() throws MIDletStateChangeException {
		try {
			initMIDlet();
		} catch (Exception ex) {
			notice(ex.toString());
		}
	}

	private void initMIDlet() {
		boolean isLoggedIn = false;
		
		System.out.println("MIDlet-Vendor: " + getAppProperty("MIDlet-Vendor"));
		
		new SplashCanvas(display, null, "/logo_splash.png");
		
		initScreens();
		initMenus();

		progress = new ProgressCanvas(this);
		addCommands(progress, "progress");

		http = new HttpThread(this);
		http.start();

		// check to see if user has logged in properly
		isLoggedIn = checkUserData();
		
		// TODO where else should this go?
		// initialize layers object
		// TODO: a layer cache might be useful for quicker startup
		// TODO: uhhh, not sure how this ended up before an attempt at loginToSocialight, but fix
		// order at some point.
		getLayers();

		if (!isLoggedIn) {
			loginToSocialight();
		} else {
			showLayers();
		}
	}

	/**
	 * 
	 * @return boolean tells you if the user is already logged in
	 * @throws IOException
	 */
	private boolean checkUserData() {
		boolean ret = false;
		
		System.out.println("Socialight.checkUserData");
		
		try {
			UserRecordStore userDb = new UserRecordStore();
			if (userDb.getNumberOfRecords() > 0) {
				user = userDb.returnLastRecord();
				
				if (user != null) {
					ret = true;
				}
			}

			userDb.close();		
		} catch (RecordStoreException e) {
			notice(e.toString());
		} catch (IOException ioe) {
			notice(ioe.toString());
		}
	
		return ret;
	}
	
	private void initMenus() {
		mainMenu = new MenuList(this, "mainMenu", mainOptions, "Main menu", null, BaseCanvas.SOFTKEY_TEXT_CLOSE);
		mediaChoiceMenu = new MenuList(this, "mediaOptions", mediaOptions, "Create a note", BaseCanvas.SOFTKEY_TEXT_NEXT, BaseCanvas.SOFTKEY_TEXT_BACK);
		visibilityMenu = new MenuList(this, "visibilityMenu", visibilityOptions, "Visibility", BaseCanvas.SOFTKEY_TEXT_CREATE, BaseCanvas.SOFTKEY_TEXT_BACK);
		settingsMenu = new MenuList(this, "settingsMenu", settingsOptions, "Settings", BaseCanvas.SOFTKEY_TEXT_MENU, BaseCanvas.SOFTKEY_TEXT_BACK);
	}

	private void initScreens() {
		noteTextScreen = createNoteTextScreen();
		initGeoCodeForm();
		
		// image capture form
		noteImageForm = new Form("Note Image");
		addCommands(noteImageForm, "noteImageForm");
	}

	private void initGeoCodeForm() {
		// TODO this is ugly
		geoCodeForm = new Form("Find Address");
		address = new TextField("Please enter an address", "", 100, TextField.ANY);
		geoStatusText = new StringItem("", "");

		geoCodeForm.append(address);
		geoCodeForm.append(geoStatusText);

		addCommands(geoCodeForm, "geoCodeForm");
	}

	private Form createNoteTextScreen() {
		Form createTextNote = new Form("Create note");

		txtText = new TextField("Message", "", 255, TextField.ANY);
		createTextNote.append(txtText);

		addCommands(createTextNote, "createTextNote");
		return createTextNote;
	}

	/**
	 * @param d
	 *            is the displayable
	 * @param whichform
	 *            is the form to add commands
	 */
	public void addCommands(Displayable d, String whichform) {
		// TODO This stuff should be re-written to use constants
		// instead of passing strings around

		if (whichform == "createTextNote") {
			d.addCommand(cancelCommand);
			d.addCommand(nextCommand);
		} else if (whichform == "imageCaptureForm") {
			d.addCommand(captureCommand);
			d.addCommand(backCommand);
		} else if (whichform == "noteImageForm") {
			d.addCommand(nextCommand);
			d.addCommand(backCommand);
		} else if (whichform == "geoCodeForm") {
			d.addCommand(backCommand);
			d.addCommand(nextCommand);
		} else if (whichform == "progress") {			
			d.addCommand(cancelCommand);			
		}
		
		d.setCommandListener(this);
	}

	/**
	 * Indicates that a command event has occurred on Displayable d. <B>Note for
	 * application developers:</B> the method should return immediately.
	 * 
	 * @param c
	 *            A Command object identifying the command. This is either one
	 *            of the Commands that have been added to Displayable with
	 *            addCommand(Command)or the implicit SELECT_COMMAND of List.
	 * @param d
	 *            The Displayable on which this event has occurred
	 */
	public void commandAction(Command c, Displayable d) {

		if (c == exitCommand) {
			try {
				destroyApp(false);
			} catch (MIDletStateChangeException e) {
				notice(e.toString());
			}
		} else if ((c == cancelCommand) && (d == noteTextScreen)) {
			// noteTextMedia -> mediaChoiceScreen
			showMediaChoices();			
		}

		else if ((c == nextCommand) && (d == noteTextScreen)) {
			display.setCurrent(visibilityMenu);
		} else if (c == createCommand) {
			createNote();
		} else if ((c == backCommand) && (d == imageCaptureForm)) {
			// insert kill the camera stuff here
			killCamera();
			showMediaChoices();
		} else if ((c == backCommand) && (d == noteImageForm)) {
			mediaType = MEDIA_TYPE_IMAGE;
			// noteImageForm --> MediaChoices
			killCamera();
			// this should probably go back to imageCaptureForm
			displayImageViewerForm();
		} else if ((c == nextCommand) && (d == noteImageForm)) {
			display.setCurrent(visibilityMenu);
		} else if ((c == backCommand) && (d == imageCaptureForm)) {
			// reset the media type
			mediaType = MEDIA_TYPE_TEXT;
			// imageCaptureForm --> MediaChoices
			showMediaChoices();
		} else if (c == captureCommand) {
			capture();
		} else if (d == geoCodeForm) {
			if (c == nextCommand) {
				geoCode(address.getString());
			} else if (c == backCommand) {
				if (previousScreen != null) {
					display.setCurrent(previousScreen);
				} else { showNoteList(); }				
			}
		} else if ((c == cancelCommand) && (d == progress)) {
			System.out.println("Cancel has been pressed. Stopping http and returning to note list."); 

			if (null != http) {
				// stop current thread operations if there are any
				http.cancel();
				http = null;
			}
			
			showNoteList();
		}

	}

	private void locationSelect(int selectedIndex) {
		SavedLocation l = new SavedLocation();

		if (selectedIndex == 0) {
			display.setCurrent(geoCodeForm);
		} else {
			// TODO hack, but what are you going to do?
			if (selectedIndex == 1 && latestGpsLocation != null) {
				l = new SavedLocation(latestLocationAddress, latestGpsLocation.getQualifiedCoordinates().getLatitude(), latestGpsLocation.getQualifiedCoordinates().getLongitude());
			} else {
				// adjust for GPS being one of the entries and lookup the data from there.
				if (latestGpsLocation != null) { selectedIndex--; }
				l = user.getLocation(selectedIndex);
			}

			if (l != null) {
				lastUsedLocation.setLatitude(l.getLatitude());
				lastUsedLocation.setLongitude(l.getLongitude());
	
				uploadFlag = true;
				sendData(currentMode);
			}
		}
	}

	private void geoCode(String address) {
		// TODO finish geocoder
		display.setCurrent(progress);
		progress.updateMessage("Finding address");
		
		// TODO let's make sure user data exists at this point
		if (user == null) {
			System.err.println("user is null");
		}
		
		try {
			user = new UserRecordStore().returnLastRecord();
		} catch (RecordStoreException ioe) {
			System.err.println("RecordStoreException on returnLastRecord");
		} catch (IOException ioe) {
			System.err.println("IOException on returnLastRecord");
		}
		if (user == null) {
			System.err.println("ERROR! userId is null. shouldn't continue with geocode; go to login");
		}

		// set the URL to equal the input fields
		String url = geoCodeURL + TextUtilities.encode(address);
		System.out.println("url: " + url);

		if ((null == http) || (!http.isAlive())) {
			System.err.println("loginThread is not alive. recreating.");
			http = new HttpThread(this);
			http.start();
		}
		http.restart();
		System.out.println("Geocode: Is the thread alive - "
				+ http.isAlive() + " | active Threads = "
				+ Thread.activeCount());
		http.go(url, "geocode");
	}

	/**
	 * Destroys any viewer windows left open
	 */
	private void killCamera() {
		if (mPlayer != null) {
			// Shut down the player.
			mPlayer.close();
			mPlayer = null;

			if (imageCaptureForm != null) {
				imageCaptureForm = null;
				// raw = null;
			}
		}
	}
	
	private void fetchNote(int selectedIndex) {
		Note note = null;
		
		// TODO getNotes shouldn't be called twice. why? save a local copy of notes instead
		// http only will store new notes that are downloaded
		Vector notes = http.getNotes();

		if (notes.size() > 0) {
			note = (Note)notes.elementAt(selectedIndex);
		
			if (note != null) {
				display.setCurrent(new NoteCanvas(this, note));
			}
		}
	}

	private void handleMediaChoiceCommand(int selectedIndex) {
		// grab the previous screen
		previousScreen = mediaChoiceMenu;

		switch (selectedIndex) {
		case 0: // text
			mediaType = MEDIA_TYPE_TEXT;
			display.setCurrent(noteTextScreen);
			break;
		case 1: // pictures
			mediaType = MEDIA_TYPE_IMAGE;
			displayImageViewerForm();
			break;
		case 2: // sound
			break;
		case 3: // video
			break;
		}

	}

	private void showMediaChoices() {
		if (null != http) {
			// stop current thread operations if there are any
			http.cancel();
		}

		display.setCurrent(mediaChoiceMenu);
	}

	/**
	 * Handles results once has been added
	 * 
	 * @param _theMode
	 */
	public void createNoteResult(String _theMode) {
		// show alert
		uploadFlag = false;
		
		if (_theMode == "addss") {
			// stop the login thread
			http.cancel();
			txtText.setString("");
			mediaType = MEDIA_TYPE_TEXT;
		} else if (_theMode == "ssImage") {
			// stop the login thread
			http.cancel();
			imgText.setString("");
			mediaType = MEDIA_TYPE_TEXT;
		}

		// not the best solution here, but sometimes multiple createnotes run one after the other,
		// so force ourselves out of addnote mode and back into updateloc mode
		// TODO fix the code so that we use some other method other than a global currentMode system
		currentMode = UPDATE_LOC_MODE;
		
		showNoteList();
	}
	
	/**
	 * TODO need to change this up to have two method: loginAutomatically + loginManually
	 * automatically is the original JAD initiated method; manually is used after a reset
	 * of the application
	 */
	private void loginToSocialight() {
		String version, model = "";

		// TODO these two might come in handy as HTTP headers
		version = getAppProperty("MIDlet-Version");
		model = System.getProperty("microedition.hostname");
		
		System.out.println("naveen: loginToSocialight");

		String url = authURL;
		System.out.println("url: " + url);

		if (http == null) {
			http = new HttpThread(this);
			http.start();
		}

		http.go(url, "login");
	}

	public void authenticateUser(boolean isAuthenticated) {
		if (isAuthenticated) {
			System.out.println("Socialight.authenticateUser");

			try {
				// add the user to the device memory so that we don't have to
				// login ever again
				UserRecordStore currentUserRecord = new UserRecordStore();

				int newid = currentUserRecord.getNextId();
				System.out.println("RecordID = " + user.recordId);
				System.out.println("newid = " + newid);

				System.out.println("user.userid = " + user.userId);
				System.out.println("user.nickName = " + user.nickName);
				System.out.println("user.firstname = " + user.firstName);
				System.out.println("user.lastName = " + user.lastName);
				System.out.println("user.photo = " + user.photo);
				System.out.println("adding user info");
	
				currentUserRecord.addUser(user);
				currentUserRecord.close();
			} catch (RecordStoreException e) { notice(e.toString());
			} catch (IOException e) { notice(e.toString()); }
			System.out.println("Finished authenticating userid #" + user.userId);

			showLayers();
		} // otherwise, notify the user if they didn't login correctly
	}

	private void createNote() {
		System.out.println("createNote called");
		// show the progress screen
		// set application mode
		currentMode = ADD_NOTE_MODE;
		locationCheck(true);
	}

	/**
	 * Sends note data to the server
	 */
	public void sendData(int _appMode) {
		if (mediaType == MEDIA_TYPE_TEXT) {
			if (_appMode == ADD_NOTE_MODE) {
				if (uploadFlag) {
					System.out.println("Add Note - Text Mode");

					// abs math here because of the visual order of these elements with respect to their values
					// Everyone (3)
					// Friends (2)
					// Myself (1)
					String visibility = Integer.toString(Math.abs(selectedVisibility - 3));

					// grab the text
					String theText = TextUtilities.encode(txtText.getString());
					System.out.println("text = " + theText);
									
					display.setCurrent(progress);
					if ((null == http) || (!http.isAlive())) {
						http = new HttpThread(this);
						http.start();
					}
					http.restart();
					// loginThread.restart();
					System.out.println("Is the thread alive - "
							+ http.isAlive() + " | active Threads = "
							+ Thread.activeCount());
					System.out.println("MediaType = " + mediaType);
					
					System.out.println("Add Text Note URL = " + addNoteURL
								+ "latitude=" + lastUsedLocation.getLatitude() + "&longitude="
								+ lastUsedLocation.getLongitude() + "&visibility=" + visibility + "&text=" + theText);
					http.go(addNoteURL + "latitude=" + lastUsedLocation.getLatitude()
							+ "&longitude=" + lastUsedLocation.getLongitude() + "&visibility=" + visibility
							+ "&text=" + theText, "addss");
				}
			} // end _appMode = ADD_NOTE_MODE
			else if (_appMode == UPDATE_LOC_MODE) {
				// TODO check to see if time, location or listkey has changed before attempting an update
				display.setCurrent(progress);
				progress.updateMessage("Searching for notes");

				// TODO set scanning

				String key = "";
				if (currentLayerItem == null || currentLayerItem.getKey() == "") {
					key = "type=recent&u=" + user.userId;
				} else {
					key = currentLayerItem.getKey();
				}

				System.out.println("Update URL = " + updateURL + key + "&latitude=" + lastUsedLocation.getLatitude() + "&longitude=" + lastUsedLocation.getLongitude());
				if ((null == http) || (!http.isAlive())) {
					http = new HttpThread(this);
					http.start();
				}
				
				// for now, clear the list manually (on first launch, this does not exist)
				if (noteList != null) { noteList.clear(); }

				http.restart();
				System.out.println("Is the thread alive - "
						+ http.isAlive() + " | active Threads = "
						+ Thread.activeCount());
				http.go(updateURL + key + "&latitude=" + lastUsedLocation.getLatitude() + "&longitude=" + lastUsedLocation.getLongitude(), "uLoc");

			} // end gpsMode 1
		} // end media type = 0
		else if (mediaType == MEDIA_TYPE_IMAGE) {
			
			if (uploadFlag) {
				display.setCurrent(progress);
				progress.updateMessage("Uploading");

				// abs math here because of the visual order of these elements with respect to their values
				// Everyone (3)
				// Friends (2)
				// Myself (1)
				String visibility = Integer.toString(Math.abs(selectedVisibility - 3));

				// grab the text
				String theText = TextUtilities.encode(imgText.getString());

				progress.updateMessage("Uploading data");

				if ((null == http) || (!http.isAlive())) {
					http = new HttpThread(this);
					http.start();
				}
				http.restart();
				http.setImageSent(false);
				System.out.println("Is the thread alive - "
						+ http.isAlive() + " | active Threads = "
						+ Thread.activeCount());
				http.go(addNoteURL + "latitude="
						+ lastUsedLocation.getLatitude() + "&longitude=" + lastUsedLocation.getLongitude() + "&visibility=" + visibility
						+ "&text=" + theText, "ssImage");

			}
		}

	}

	/**
	 * Handles commands from the settingsMenu MenuScreen
	 * 
	 * @param selectedIndex
	 */
	private void handleSettingsMenu(int selectedIndex) {

		System.out.println("Settings selectedIndex = " + selectedIndex);
		previousScreen = display.getCurrent();

		switch (selectedIndex) {
			case 0:
				showUserDetails();
				break;
			case 1:
				resetApplication();
				break;
		}
	}

	private void showUserDetails() {
		messageCanvas = new MessageCanvas(this, "message", user.nickName + "'s details", "You are logged in as " + user.nickName + ". You are connected to " + CURRENT_HOST + ".");
		messageCanvas.addSoftkey(BaseCanvas.SOFTKEY_TEXT_BACK);
		display.setCurrent(messageCanvas);
	}

	public void showNoteList() {
		if (noteList == null) { noteList = new NoteListCanvas(this, null); }
		display.setCurrent(noteList);
	}
	
	public void showLayers() {
		if (layerList != null) { display.setCurrent(layerList); }
	}

	private void showSettingScreen() {
		display.setCurrent(settingsMenu);
	}

	// TODO each list is unique as it depends on the layer
	// TODO there might be no need to store notes into the cache as you're always moving around
	public void createNewNoteList() {
		// grab the vector of notes
		Vector v = http.getNotes();
		System.out.println("Number of new notes: " + v.size());

		noteList = new NoteListCanvas(this, v);

		if (currentLayerItem != null) {
			noteList.setTitle(currentLayerItem.getTitle());
		}
	}

	public void showHelp() {
		messageCanvas = new MessageCanvas(this, "message", "Help",
				"Socialight lets you create, share and discover Sticky Notes, stuck to actual places all around you. Sounds simple right? It is! It's also very powerful.\n" +
				"The world becomes a playground of hidden messages written by your friends and the people your trust.\n" +
				"Since there could be hundreds of notes around you at any given location, the mobile client separates the content into layers. Choose a layer to see a subset of notes around you (perhaps just content from your friends or maybe content from a particular publication).\n" +
				"This mobile client will not only allow you to find these messages but it will also give you the ability to post your own notes. The 'Create note' menu item will allow you to make your own text or image notes."
				);
		messageCanvas.addSoftkey(BaseCanvas.SOFTKEY_TEXT_BACK);
		display.setCurrent(messageCanvas);
	}

	/**
	 * Remove all traces of user details, cached notes, etc. When the app is
	 * relaunched after this stage, it will behave as though it were a newly downloaded
	 * application (relogin, pull in user & note data, etc)
	 * 
	 * TODO how to handle username/password after this stage? user will have to pull
	 * down the latest JAD file again
	 */
	private void resetApplication() {
		// TODO this is dangerous, of course, and we should warn the user
		// and allow them to back out of this action
		try {
			// delete all user data
			UserRecordStore urs = new UserRecordStore();
			urs.deleteAllRecords();
			urs.close();
			
			// erase everything from current user object
			user.empty();

			// kill any threads that might be running
			http = null;
			pl = null;

			// delete all info from inbox
			if (null != noteList) {
				System.out.println("clearing noteList");
				noteList = null;
			}
			if (null != layerList) {
				System.out.println("clearing layerList");
				layerList = null;
			}

		} catch (RecordStoreException e) {
			notice(e.toString());
		} catch (NullPointerException npe) {
			System.out.println("NullPointerException: " + npe.toString());
		}

		// TODO for now, exit. in future, allow user to loginManually()
		try {
			destroyApp(false);
		} catch (MIDletStateChangeException e) {
			notice(e.toString());
		}
	}

	private void displayImageViewerForm() {
		// skip this stuff to test the uploader
		try {
			mPlayer = Manager.createPlayer("capture://video");
			mPlayer.realize();

			mVideoControl = (VideoControl) mPlayer.getControl("VideoControl");

			imageCaptureForm = new Form("Image Capture");
			Item item = (Item) mVideoControl.initDisplayMode(
					GUIControl.USE_GUI_PRIMITIVE, null);
			imageCaptureForm.append(item);
			addCommands(imageCaptureForm, "imageCaptureForm");
			display.setCurrent(imageCaptureForm);
			mPlayer.start();
		} catch (IOException ioe) {
			notice(ioe.toString());
		} catch (MediaException me) {
			// this will tell us if capture is supported or if the camera is ready
			notice(me.toString());
		} catch (SecurityException se) {
			notice(se.toString());
		}
	}

	public void capture() {
		try {
			raw = mVideoControl.getSnapshot("encoding=jpeg");
			Image image = Image.createImage(raw, 0, raw.length);
			Image thumb = createThumbnail(image);

			// Shut down the player.
			mPlayer.close();
			mPlayer = null;
			mVideoControl = null;

			// delete anything from the Form first
			noteImageForm.deleteAll();

			// Place it in the main form.
			if (noteImageForm.size() > 0
					&& noteImageForm.get(0) instanceof StringItem)
				noteImageForm.delete(0);
			noteImageForm.append(thumb);

			imgText = new TextField("Text", "", 255, TextField.ANY);
			noteImageForm.append(imgText);

			// Flip back to the main form.
			display.setCurrent(noteImageForm);

		} catch (MediaException me) {
			mPlayer.close();
			mPlayer = null;
			mVideoControl = null;
			notice(me.toString());
		}
	}

	public void notice(String e) {
		messageCanvas = new MessageCanvas(this, "message", "Error", e);
		messageCanvas.addSoftkey(BaseCanvas.SOFTKEY_TEXT_OK);
		messageCanvas.setNext(display.getCurrent());
		display.setCurrent(messageCanvas);
	}
	
	public void fatal(String e) {
		MessageCanvas mc = new MessageCanvas(this, null, "Error", e);
		display.setCurrent(mc);
	}

	private Image createThumbnail(Image image) {
		// There has to be a better way to do this
		int sourceWidth = image.getWidth();
		int sourceHeight = image.getHeight();

		int thumbWidth = 64;
		int thumbHeight = thumbWidth * sourceHeight / sourceWidth;

		Image thumb = Image.createImage(thumbWidth, thumbHeight);
		Graphics g = thumb.getGraphics();

		for (int y = 0; y < thumbHeight; y++) {
			for (int x = 0; x < thumbWidth; x++) {
				g.setClip(x, y, 1, 1);
				int dx = x * sourceWidth / thumbWidth;
				int dy = y * sourceHeight / thumbHeight;
				g.drawImage(image, x - dx, y - dy, Graphics.LEFT|Graphics.TOP);
			}
		}

		Image immutableThumb = Image.createImage(thumb);

		return immutableThumb;
	}

	public void showMainMenu() {
		previousScreen = display.getCurrent();
		display.setCurrent(mainMenu);
	}
	
	/**
	 * convenience method to handle clicks on the item in the main menu;
	 * this global menu is responsible for sending the user to various parts
	 * of the application quickly
	 * 
	 * @param index selected item
	 */
	public void handleMainMenu(int index) {
		switch (index) {
		case 0: // "Home"
			showLayers();
			break;
		case 1: // "Set location"
			locationCheck(true);
			break;
		case 2: // "Create"
			showMediaChoices();
			break;
		case 3: // "Settings"
			showSettingScreen();
			break;
		case 4: // "Help"
			showHelp();
			break;
		case 5:
			commandAction(exitCommand, null);
		}
	}

	public void canvasCommand(String cs, int selectedOption, int keyCode) {

		if (cs.equals("mediaOptions")) {
			if (keyCode == KEYCODE_OK || keyCode == KEYCODE_LEFT_SOFTKEY) {
				handleMediaChoiceCommand(selectedOption);
			} else if (keyCode == KEYCODE_RIGHT_SOFTKEY) {
				showNoteList();
			}
		} else if (cs.equals("visibilityMenu")) {
			if (keyCode == KEYCODE_OK || keyCode == KEYCODE_LEFT_SOFTKEY) {
				// TODO may not be the best way to do this
				selectedVisibility = selectedOption;
				
				// TODO uploadFlag = true is repetitive.
				// check gpsMode text / picture
				if (mediaType == MEDIA_TYPE_TEXT) {
					if (!txtText.getString().equals("")) {
						uploadFlag = true;
						createNote();
					} else {
						uploadFlag = true;
						createNote();
					}
				} else if (mediaType == MEDIA_TYPE_IMAGE) {
					if (!imgText.getString().equals("")) {
						uploadFlag = true;
						createNote();
					} else {
						uploadFlag = true;
						createNote();
					}
				}
			} else if (keyCode == KEYCODE_RIGHT_SOFTKEY) { // right softkey
				// return to the previous screen
				display.setCurrent(mediaChoiceMenu);
			}
		} else if (cs.equals("layer")) {
			if (keyCode == KEYCODE_OK || keyCode == KEYCODE_RIGHT) {
				previousScreen = layerList;
				currentLayerItem = layerList.getItem(selectedOption);

				display.setCurrent(noteList);
				locationCheck(false);
			} else if (keyCode == KEYCODE_LEFT_SOFTKEY) {
				showMainMenu();
			} else if (keyCode == KEYCODE_RIGHT_SOFTKEY) {
				showNoteList();
			}
		} else if (cs.equals("listCanvas")) {
			if (keyCode == KEYCODE_OK || keyCode == KEYCODE_RIGHT) {
				previousScreen = noteList;
				fetchNote(selectedOption);
			} else if (keyCode == KEYCODE_LEFT) {
				showLayers();
			} else if (keyCode == KEYCODE_LEFT_SOFTKEY) {
				showMainMenu();
			} else if (keyCode == KEYCODE_RIGHT_SOFTKEY) {
				// show location / refresh
				locationCheck(true);
			}
		} else if (cs.equals("settingsMenu")) {
			if (keyCode == KEYCODE_OK) {
				handleSettingsMenu(selectedOption);
			} else if (keyCode == KEYCODE_LEFT_SOFTKEY) {
				showMainMenu();
			} else if (keyCode == KEYCODE_RIGHT_SOFTKEY) {
				showNoteList();
			}
		} else if (cs.equals("mainMenu")) {
			if (keyCode == KEYCODE_OK) {
				handleMainMenu(selectedOption);
			} else if (keyCode == KEYCODE_LEFT_SOFTKEY) {
				// go back
				display.setCurrent(previousScreen);
			}
		} else if (cs.equals("locationSelectMenu")) {
			if (keyCode == KEYCODE_OK || keyCode == KEYCODE_LEFT_SOFTKEY) {
				locationSelect(selectedOption);
			} else if (keyCode == KEYCODE_RIGHT_SOFTKEY) {
				display.setCurrent(previousScreen);
			}
		} else if (cs.equals("message")) {
			if (keyCode == KEYCODE_RIGHT_SOFTKEY) {
				if (previousScreen != null) {
					display.setCurrent(previousScreen);
				} else {
					showLayers();
				}
			} else if (keyCode == KEYCODE_LEFT_SOFTKEY) {
				// get the action from the screen first
				if (messageCanvas.getNext() != null) {
					display.setCurrent(messageCanvas.getNext());
				}
			}
		}
	}

	/**
	 * Updates the current user Record
	 * 
	 * @return true on success false on fail
	 */
	private boolean updateUserRecord() {
		System.out.println("updating user record");
		boolean status = false;

		try {
			// see if we can access the recordStore
			UserRecordStore userDb = new UserRecordStore();
			System.out.println("Opened the recordstore for Update");

			// wipe existing record - create new one
			userDb.deleteAllRecords();
			userDb.addUser(user);
			userDb.close();
			status = true;
		} catch (RecordStoreException e) {
			notice(e.toString());
		} catch (IOException e) {
			notice(e.toString());
		}

		return status;
	}

	public void handleGeocodeResult(SavedLocation l, String error) {
		http = null;

		System.out.println("handling Geocode Result");

		if (error.equals("")) {

			geoStatusText.setText("");
			
			lastUsedLocation.setAddress(address.getString());
			lastUsedLocation.setLatitude(l.getLatitude());
			lastUsedLocation.setLongitude(l.getLongitude());

			user.addLocation(lastUsedLocation);
			updateUserRecord();

			uploadFlag = true;
			
			System.out.println("currentMode = " + currentMode);
			sendData(currentMode);

		} else {
			geoStatusText.setText(error);
			display.setCurrent(geoCodeForm);
			System.out.println("geocoding error = " + error);
		}

	}

	/**
	 * locationCheck - test gps, saved locations or manual input
	 * 
	 * 1. if there is a valid gps fix, show that to the user in the list
	 * 2. if there is an old gps fix, show that to the user (and indicate it as old)
	 * 3. if there are previous manually entered addresses, show them
	 * 4. ask the user if they want to manually enter a new address
	 */
	public void locationCheck(boolean force) {
		force = (!force) ? !lastUsedLocation.isValid() : true;
		
		if (force) {
			// if there is a valid gps location and it is within the time change allowed, use it
			String gpsLocationTitle = null;
			
			int s = getGpsStatus();
			
			if (s == GPS_LATEST) {
				gpsLocationTitle = "[GPS] " + latestLocationAddress;
			} else if (s == GPS_LAST) {
				// last valid, probably
				gpsLocationTitle = "[LastGPS] " + latestLocationAddress;
			}
	
			showLocationSelectMenu(gpsLocationTitle);
		} else {
			sendData(currentMode);
		}
	}

	public void showLocationSelectMenu(String _gps) {
		// TODO move into an iterator? how will array be handled with list that grows?
		String[] locationSelectOptions = new String[5];
		int i = 0;

		locationSelectOptions[i] = "+ Enter new location";
		
		if (_gps != null && _gps != "") {
			locationSelectOptions[++i] = _gps;
		}

		Enumeration e = user.getLocations().elements();
		String a = "";
		while (e.hasMoreElements()) {
			a = ((SavedLocation)e.nextElement()).getAddress();
			if (!a.equals("")) { locationSelectOptions[++i] = a; }
		}

		locationSelectMenu = new MenuList(this, "locationSelectMenu", locationSelectOptions, "Change location", BaseCanvas.SOFTKEY_TEXT_OK, BaseCanvas.SOFTKEY_TEXT_BACK);
		
		previousScreen = display.getCurrent();
		display.setCurrent(locationSelectMenu);
	}
	
	public void addLayer(ListItem _li) {
		layers.addElement(_li);
	}

	/**
	 * fetch a list of layers from the system for this current user
	 */
	public void getLayers() {
		layers = new Vector();
		layerList = new LayerList(this, null);
		
		if ((null == http) || (!http.isAlive())) {
			http = new HttpThread(this);
			http.start();
		}
		http.restart();
		http.go(layersURL, "layers");
	}
	
	public void createLayers() {
		System.out.println("Layers: " + layers.size());
		
		Enumeration e = layers.elements();
		while (e.hasMoreElements()) { System.out.println("  " + ((ListItem)e.nextElement()).getKey()); }
		
		layerList.setLayers(layers);
		if (layerList.isShown()) {
			layerList.repaint();
		}
	}
	
	public void vibrate() {
		// TODO enable this for certain actions?
		//display.vibrate(1000);
	}
	
	public boolean hasLocationAPI() {
		return (System.getProperty("microedition.location.version") != null);
	}
	
	public static int getGpsStatus() {
		int gpsStatus = GPS_INVALID;
		
		if (latestGpsLocation != null &&
				((System.currentTimeMillis() - latestGpsLocation.getTimestamp()) < locationTimeChangeAllowed)) {
			gpsStatus = GPS_LATEST;
		} else {
			if (latestGpsLocation != null) {
				gpsStatus = GPS_LAST;
			}
		}

		return gpsStatus;
	}
	
	public void notifyLocationUpdated(Location _l, boolean _statusGps, String _statusDescription) {
		if (_l != null) {
			// TODO: update the recent location variable so that it can be used for add/update actions
			// might want to check at that point to make sure timestamp still reasonable
			latestGpsLocation = _l;
			
			String url = checkinURL + "latitude=" + latestGpsLocation.getQualifiedCoordinates().getLatitude() + "&longitude=" + latestGpsLocation.getQualifiedCoordinates().getLongitude();
			System.out.println("url: " + url);
	
			if ((null == http) || (!http.isAlive())) {
				http = new HttpThread(this);
				http.start();
			}
			http.restart();
			http.go(url, "checkin");
		}
	}
	
	public Location getLocation() { return latestGpsLocation; }
	public void setLatestLocationAddress(String _a) {
		if (_a != null) { latestLocationAddress = _a; }
	}
}
