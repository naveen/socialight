package com.socialight;

import java.util.Vector;

/**
 * responsible for the display of a list of notes
 *
 * @author naveen
 */
public class NoteListCanvas extends ListCanvas {
	
	private final String MSG_NOTE_NOT_FOUND = "There are no notes around here.";
	private final String DISTANCE_UNIT = "mi"; // miles
	
	private Socialight s;

	public NoteListCanvas(Socialight _s, Vector _v) {
		super();
		
       	System.out.println("NoteList: initialize");
    
		s = _s;
		
		listTitle = "Notes nearby";
		listMsgNoElements = MSG_NOTE_NOT_FOUND;
		listSoftkeyLeft = SOFTKEY_TEXT_MENU;
		listSoftkeyRight = SOFTKEY_TEXT_REFRESH;
		setNotes(_v);
	}
	
	public void setNotes(Vector _n) {
		elements.removeAllElements();
		
		Note note = new Note();

		if (_n != null) {
			for (int vs = 0; vs < _n.size(); vs++) {
				note = (Note)_n.elementAt(vs);
				elements.addElement(new ListItem(null, note.getTitle(), note.getText(), note.getDistance() + " " + DISTANCE_UNIT));
			}
		}
	}
	
    public void itemSelected(int position, int keyCode) {
    	s.canvasCommand("listCanvas", position, keyCode);
    }
}
