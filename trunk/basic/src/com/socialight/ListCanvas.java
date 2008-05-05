package com.socialight;

import java.util.Vector;

import javax.microedition.lcdui.Graphics;

/**
 * responsible for the display of a list
 * 
 * @author naveen
 */
public class ListCanvas extends BaseCanvas {
	
	/** indicates selected position in the list */
	private int position = 0;

	protected Vector elements = new Vector(5); // ListItems as array to make growth easy
	
	protected String listTitle = "";
	protected String listMsgNoTitle = "-";
	protected String listMsgNoElements = "There are no elements in this list.";
	
	// TODO actually, there is no such thing as left and right variables as BaseCanvas
	// is responsible for automatically picking the location, so change this to a Vector(2)?
	protected String listSoftkeyLeft = "";
	protected String listSoftkeyRight = "";
	
	public ListCanvas() {
		super();
		setFullScreenMode(true);
	}

	public void paint(Graphics g) {
		super.paint(g);
        
        int textLeftX = 4;
        int textWidth = getWidth() - 8;
		       
        g.setColor(CANVAS_COLOR_TEXT);
        g.setFont(FONT_BOLD);
        g.drawString(TextUtilities.fitToLine(listTitle, true, FONT_BOLD, textWidth - 16), 4, (20 - FONT_BOLD.getHeight() - 1), Graphics.TOP|Graphics.LEFT);
        
        g.setFont(FONT_PLAIN);
        drawSoftkey(g, listSoftkeyLeft);
        drawSoftkey(g, listSoftkeyRight);
		
       	int y = 21;
       	int rowHeight = FONT_PLAIN.getHeight() + 8; // originally 32
       	
       	boolean initial = true;

       	// TODO the first box seems to be 20 pixels in height, all others 19 pixels (sun color emulator). why?
        if (elements != null && elements.size() > 0) {
        	
        	// if any of the elements have items that need to be displayed on two rows, adjust row height
        	for (int i = 0; i < elements.size(); i++) {
        		if (!((ListItem)elements.elementAt(i)).getDescription().equals("")) {
        			rowHeight += FONT_PLAIN.getHeight();
        			break;
        		}
        	}

	        for (int p = 1; p <= elements.size(); p++) {      		
	        	// if this is the selected note, color it differently
	        	if ((position+1) == p) {
	        		g.setColor(0x646464);
	        	} else {
	        		g.setColor(0x2A2A2A);
	        	}
	        		        	
        		g.fillRect(1, y, getWidth()-2, rowHeight);
	        	
	        	if (!initial) {
	        		g.setColor(CANVAS_COLOR_BORDER);
	        		g.drawLine(1, y, getWidth()-2, y);
	        	} else { initial = false; }
	        	
	        	y += rowHeight;
	        }
	        
	        g.setColor(CANVAS_COLOR_TEXT);
	        
	        y = 26;
			for (int vs = 0; vs < elements.size(); vs++) {
				ListItem li = (ListItem)elements.elementAt(vs);
				String displayTitle = li.getTitle().equals("") ? "-" : li.getTitle();
				String displayAux = li.getAux();
				
				if (displayAux.length() > 8) { displayAux = displayAux.substring(0, 8); }
				if (!displayAux.equals("")) { displayAux = " [" + displayAux + "]"; }

				// if we have auxiliary text to display on the first line, then reduce the width of the title
				int titleWidth = (int)Math.floor(textWidth*(displayAux.equals("") ? 1 : .7));

				g.setColor(CANVAS_COLOR_TEXT);
	            g.drawString(TextUtilities.fitToLine(displayTitle, true, FONT_PLAIN, titleWidth), textLeftX, y, Graphics.TOP|Graphics.LEFT);
	            
	            g.setColor(CANVAS_COLOR_TEXT_LIGHT);
	            g.drawString(displayAux, textWidth+4, y, Graphics.TOP|Graphics.RIGHT);
	            
	            if (!li.getDescription().equals("")) {
	            	g.drawString(TextUtilities.fitToLine(li.getDescription(), true, FONT_PLAIN, textWidth), textLeftX, y+FONT_PLAIN.getHeight()+2, Graphics.TOP|Graphics.LEFT);
	            }
	            
	            y += rowHeight;
			}
        } else {
        	g.setColor(CANVAS_COLOR_TEXT);
        	g.drawString(listMsgNoElements, getWidth()/2, 50, Graphics.TOP|Graphics.HCENTER);
        }
	}	
	
    protected void keyPressed(int keyCode) {
    	if (getGameAction(keyCode) == DOWN) {
    		downSelector();
    	} else if (getGameAction(keyCode) == UP) {
    		upSelector();
    	} else {
    		itemSelected(position, keyCode);
    	}        
    }
    
    public void clear() { if (elements != null) { elements.removeAllElements(); } }
	public void setTitle(String _t) { if (_t != null) { listTitle = _t; } }
	
    protected void itemSelected(int position, int keyCode) {}

    // TODO selector/position only moves if elements.size > 0
	private void upSelector() {
		if (position > 0) {
			position--;  
		} else {
			position = elements.size() - 1;
		}
		
		repaint();
	}

	private void downSelector() {
		if (position < elements.size() - 1) {
			position++;
		} else {
			position = 0;
		}
		
		repaint();
	}

	protected void pointerPressed(int x, int y) {
        dismiss();
    }

    private void dismiss() {}
}
