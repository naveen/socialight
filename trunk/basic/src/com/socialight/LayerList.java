package com.socialight;

import java.util.Enumeration;
import java.util.Vector;

/**
 * responsible for the display of a list of layers
 *
 * @author naveen
 */
public class LayerList extends ListCanvas {
	
	private final String MSG_LAYER_NOT_FOUND = "Refreshing layers. Please wait.";
	private final String MSG_LAYER_NO_TITLE = "Layer: No Title";
	
	// needed for call back
	private Socialight mainApp;

	public LayerList(Socialight _mainApp, Vector _l) {
		super();
		
       	System.out.println("LayerList: initialize");
    
		mainApp = _mainApp;
		
		listMsgNoTitle = MSG_LAYER_NO_TITLE;
		listMsgNoElements = MSG_LAYER_NOT_FOUND;
		listSoftkeyLeft = SOFTKEY_TEXT_MENU;
		listSoftkeyRight = SOFTKEY_TEXT_BACK;
		setLayers(_l);
	}

	/**
	 * 
	 * @param _l Vector of ListItems
	 */
	public void setLayers(Vector _l) {
		elements.removeAllElements();
		
		if (_l != null) {
			Enumeration e = _l.elements();
			while (e.hasMoreElements()) {
				elements.addElement(e.nextElement());
			}
		}
		
		// reset the title because in some instances, the title is based
		// on the number of elements in the list
		setTitle();		
	}
	
	public ListItem getItem(int _i) {
		return (ListItem)elements.elementAt(_i);
	}
	
	/**
	 * use this function to set a custom home view title or to have it reset
	 * the default title based on the number of layers
	 */
	public void setTitle() {
		listTitle = "Layers";
		if (elements != null) { listTitle += " (" + elements.size() + ")"; }
	}
	
    public void itemSelected(int position, int keyCode) {
    	mainApp.canvasCommand("layer", position, keyCode);
    }
}
