package com.socialight;

/**
 * holds an active element that belongs to a list
 * 
 * @author naveen
 */
public class ListItem {
	private String key = "";
	private String title = "";
	private String description = "";
	private String aux = "";
	
	public ListItem() {}
	
	public ListItem(String _k, String _t, String _d, String _a) {
		setKey(_k);
		setTitle(_t);
		setDescription(_d);
		setAux(_a);
	}
	
	public String getKey() { return key; }
	public void setKey(String _k) { if (_k != null) { key = _k; }}
	public String getTitle() { return title; }
	public void setTitle(String _t) { if (_t != null) { title = _t; }}
	public String getDescription() { return description; }
	public void setDescription(String _d) { if (_d != null) { description = _d; }}
	public String getAux() { return aux; }
	public void setAux(String _a) { if (_a != null) { aux = _a; }}

	public ListItem clone() {
		return new ListItem(key, title, description, aux);
	}
}
