package com.socialight;

import javax.microedition.lcdui.Image;

/**
 * getters/setters are useful (though repetitive) here because they can properly handle
 * null values and empty strings. this is very useful as lack of such tests will result
 * in nullexceptions when attempting to write to screen or when trying to use recordstore.
 * (should not leave this to the caller)
 * 
 * @author michael.sharon
 * @author naveen
 */

public class Note {
	private final String MSG_DEFAULT_TITLE = "A note";
	private final int DISPLAY_MAX_TITLE_LENGTH = 50;
	
	public static final String MEDIATYPE_IMAGE = "image";
	public static final String MEDIATYPE_AUDIO = "audio";
	public static final String MEDIATYPE_VIDEO = "video";
	public static final String MEDIATYPE_TEXT = "text";
	
	private String id			= "";
	private String title		= "";
	private String link			= "";
	private String author		= "";
	private String authorId		= "";
	private String set			= "";
	private String text			= "";
	private String latitude		= "";
	private String longitude	= "";
	private String address      = "";
	private String place        = "";
	private String distance     = "";
    private String tags         = "";
    private String imageLink    = "";
    private String mapLink      = "";

    // TODO change visibility on this
    public Image image;
    public Image map;
	
	public Note() {}
	
	public Note(String _id, String _title, String _link,
						String _author, String _authorId,
						String _set, String _text,
						String _latitude, String _longitude, 
						String _address, String _place, String _distance,
						String _tags, String _imageLink, String _mapLink)
	{
		setId(_id); 
		setTitle(_title);
		setLink(_link);
		setAuthor(_author);
		setAuthorId(_authorId);
		setSet(_set);
		setText(_text);
		setLatitude(_latitude);
		setLongitude(_longitude);
		setAddress(_address);
		setPlace(_place);
		setDistance(_distance);
		setTags(_tags);
		setImageLink(_imageLink);
		setMapLink(_mapLink);
	}	
	
	// TODO this is too much work; there has to be a better way to clear everything out
	public void clear() {
		setId("");
		setTitle("");
		setLink("");
		setAuthor("");
		setAuthorId("");
		setSet("");
		setText("");
		setLatitude("");
		setLongitude("");
		setAddress("");
		setPlace("");
		setDistance("");
		setTags("");
		setImageLink("");
		setMapLink("");
		
		image = null;
		map = null;
	}
 
	public Note copy() {
		return new Note(id, title, link,
						author, authorId,
						set, text,
						latitude, longitude,
						address, place, distance,
						tags, imageLink, mapLink);
	}
	 
	public void setId(String _id) { if (_id != null) { id = _id.trim(); } }
	public String getId() { return id; }
	 
	public void setTitle(String _title) { if (_title != null) { title = _title.trim(); } }
	public String getTitle() {
		String t = title;
		
		if (t == "") {
			// try using a part of the text as the title
			t = getText().length() > DISPLAY_MAX_TITLE_LENGTH ? getText().substring(0, DISPLAY_MAX_TITLE_LENGTH) : getText();
			
			// if there is no text, then try using part of the place name as title
			if (t == "") {
				t = getPlace().length() > DISPLAY_MAX_TITLE_LENGTH ? getText().substring(0, DISPLAY_MAX_TITLE_LENGTH) : getPlace();
			}
			
			// if that doesn't work, just go with a default string
			if (t == "") { t = MSG_DEFAULT_TITLE; }
		}
		
		return t;
	}
	 
	public void setLink(String _link) { if (_link != null) { link = _link.trim(); } }
	public String getLink() { return link; }
	 
	public void setAuthor(String _author) { if (_author != null) { author = _author.trim(); } }
	public String getAuthor() { return author; }
	 
	public void setAuthorId(String _authorId) { if (_authorId != null) { authorId = _authorId.trim(); } }
	public String getAuthorId() { return authorId; }
		 
	public void setSet(String _set) { if (_set != null) { set = _set.trim(); } }
	public String getSet() { return set; }
	 
	public void setText(String _text) { if (_text != null) { text = _text.trim(); } }
	public String getText() { return text; }
	 
	public void setLatitude(String _geolat) { if (_geolat != null) { latitude = _geolat.trim(); } }
	public String getLatitude() { return latitude; }
	 
	public void setLongitude(String _geolong) { if (_geolong != null) { longitude = _geolong.trim(); } }
	public String getLongitude() { return longitude; }
	 
	public void setAddress(String _a) { if (_a != null) { address = _a.trim(); } }
	public String getAddress() { return address; }
	 
	public void setPlace(String _p) { if (_p != null) { place = _p.trim(); } }
	public String getPlace() { return place; }
	
	public void setDistance(String _d) { if (_d != null) { distance = _d.trim(); } }
	public String getDistance() { return distance; }
	 
	public void setTags(String _tags) { if (_tags != null) { tags = _tags.trim(); } }
	public String getTags() { return tags; } // TODO: maybe return this as comma-separated instead?
	 
	public void setImageLink(String _i) { if (_i != null) { imageLink = _i.trim(); } }
	public String getImageLink() { return imageLink; }
	 
	public void setMapLink(String _m) { if (_m != null) { mapLink = _m.trim(); } }
	public String getMapLink() { return mapLink; }
}
