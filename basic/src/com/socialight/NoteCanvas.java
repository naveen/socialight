package com.socialight;

import java.util.Enumeration;

import javax.microedition.lcdui.Graphics;

/**
 * provides an on-screen visualization for a note object (details screen, map screen, actions, ...)
 * 
 * @author naveen
 */
public class NoteCanvas extends BaseCanvas implements Runnable {
    private Socialight s;
    private Note note;
    
    private boolean active;
    private Thread t = null;
    
    private int view = 0;

    public NoteCanvas(Socialight _s, Note _n) {
    	s = _s;
    	note = _n;
    	setFullScreenMode(true);
    }
    
    protected void paint(Graphics g) {
    	super.paint(g);
		
        int textLeftX = 4;
		int textTopY = 22; // originally 37 and Graphics.BOTTOM
		int textWidth = getWidth() - 8;
		
        g.setColor(CANVAS_COLOR_TEXT);
        g.setFont(FONT_PLAIN);
    	g.translate(g.getTranslateX(), g.getTranslateY() - scroll);

        if (view == 0) {
        	
        	if (note.getImageLink() != "") {
        		textTopY += FONT_PLAIN.getHeight();
        		
    	    	if (note.image == null) {
    	        	g.setColor(CANVAS_COLOR_TEXT_LIGHT);
    	        	g.drawString("[loading image...]", getWidth()/2, textTopY, Graphics.TOP|Graphics.HCENTER);
    	        	textTopY += FONT_PLAIN.getHeight() * 2;
    	        	g.setColor(CANVAS_COLOR_TEXT);
    				
    				s.http.setNote(note);
    				s.http.restart();
    				s.http.go(note.getImageLink(), "downloadImage");
    				
    				note.image = s.http.getNoteImage();
    			} else {
    				g.drawImage(note.image, getWidth()/2, textTopY, Graphics.TOP|Graphics.HCENTER);
    				textTopY += note.image.getHeight() + FONT_PLAIN.getHeight();
    			}
        	}
        	
        	// TODO the second part of this clause is a hack. Note should handle this...
        	if (!note.getTitle().equals("") && !note.getTitle().equals(note.getText())) {
        		g.setFont(FONT_BOLD);
        		
        		for (Enumeration e = TextUtilities.wrap(note.getTitle(), FONT_BOLD, textWidth).elements(); e.hasMoreElements(); ) {
        			g.drawString(e.nextElement().toString(), textLeftX, textTopY, Graphics.TOP|Graphics.LEFT);
        			textTopY += FONT_BOLD.getHeight();
        		}

        		g.setFont(FONT_PLAIN);
        	}
        	
        	textTopY += FONT_PLAIN.getHeight();

        	if (!note.getText().equals("")) {
	    		for (Enumeration e = TextUtilities.wrap(note.getText(), FONT_PLAIN, textWidth).elements(); e.hasMoreElements(); ) {
	    			g.drawString(e.nextElement().toString(), textLeftX, textTopY, Graphics.TOP|Graphics.LEFT);
	    			textTopY += FONT_PLAIN.getHeight();
	    		}
	    	}
        	
        	// place and map blurb
        	textTopY += FONT_PLAIN.getHeight();
        	if (!note.getPlace().equals("")) {
        		g.drawString("Location: " + note.getPlace(), textLeftX, textTopY, Graphics.TOP|Graphics.LEFT);
        		textTopY += FONT_PLAIN.getHeight();
        	}

        	g.setColor(CANVAS_COLOR_TEXT_LIGHT);
    		g.drawString("[press > to view map & address]", textLeftX, textTopY, Graphics.TOP|Graphics.LEFT);
    		textTopY += FONT_PLAIN.getHeight();
    		g.setColor(CANVAS_COLOR_TEXT);
	    	
	        // author + tag information
	    	g.setColor(CANVAS_COLOR_TEXT);
	    	
	    	textTopY += FONT_PLAIN.getHeight();
	    	g.drawString("by [" + note.getAuthor() + "]", textLeftX, textTopY, Graphics.TOP|Graphics.LEFT);
	    	
	    	if (!note.getTags().equals("")) {
	    		for (Enumeration e = TextUtilities.wrap("Tags: " + note.getTags(), FONT_PLAIN, textWidth).elements(); e.hasMoreElements();) {
	    			textTopY += FONT_PLAIN.getHeight();
	    			g.drawString(e.nextElement().toString(), textLeftX, textTopY, Graphics.TOP|Graphics.LEFT);
	    		}
	    	}
	    	
        } else {
       		paintViewMap(g, textLeftX, textTopY);
        }

        if (textTopY > getHeight() - 40) {
        	maxScrollHeight = (textTopY - 30) < 0 ? 0 : textTopY - getHeight() + 40;
        } else {
        	maxScrollHeight = 0;
        }
        g.translate(0, scroll);
        
        drawHeader(g, note.getTitle());
        
        g.setColor(CANVAS_COLOR_BORDER);
        g.drawLine(0, getHeight() - 20, getWidth(), getHeight() - 20);
        
        g.setColor(CANVAS_COLOR_TEXT);
        g.setFont(FONT_PLAIN);
        
        drawFooter(g, SOFTKEY_TEXT_MENU, SOFTKEY_TEXT_BACK);
        drawScroll(g, scroll != 0, scroll < maxScrollHeight);
    }
    
    private void paintViewMap(Graphics g, int textLeftX, int textTopY) {
    	g.setColor(CANVAS_COLOR_TEXT_LIGHT);
		g.drawString("[press < to view note]", textLeftX, textTopY, Graphics.TOP|Graphics.LEFT);
		textTopY += FONT_PLAIN.getHeight() * 2;
		g.setColor(CANVAS_COLOR_TEXT);
    	
    	if (note.getPlace() != "") {
    		g.drawString(note.getPlace(), textLeftX, textTopY, Graphics.TOP|Graphics.LEFT);
    		textTopY += FONT_PLAIN.getHeight();
    	}
    	
    	if (note.getAddress() != "") {
    		g.drawString(note.getAddress(), textLeftX, textTopY, Graphics.TOP | Graphics.LEFT);
    		textTopY += FONT_PLAIN.getHeight();
    	}
    	
    	g.setColor(CANVAS_COLOR_TEXT_LIGHT);
    	textTopY += FONT_PLAIN.getHeight();
    	
    	if (note.getMapLink() != "") {
	    	if (note.map == null) {
				g.drawString("[loading map...]", getWidth()/2, textTopY, Graphics.TOP | Graphics.HCENTER);
				
				s.http.setNote(note);
				//s.http.restart();
				s.http.go(note.getMapLink(), "downloadMap");

				note.map = s.http.getNoteMap();
			} else {
				g.drawImage(note.map, getWidth()/2, textTopY, Graphics.TOP | Graphics.HCENTER);
				textTopY += note.map.getHeight() + FONT_PLAIN.getHeight();
			}
    	} else {
    		for (Enumeration e = TextUtilities.wrap("[A map for this note couldn't be loaded.]", FONT_PLAIN, getWidth() - 8).elements(); e.hasMoreElements(); ) {
    			g.drawString(e.nextElement().toString(), textLeftX, textTopY, Graphics.TOP|Graphics.LEFT);
    			textTopY += FONT_PLAIN.getHeight();
    		}
    		textTopY += FONT_PLAIN.getHeight();
    	}
    	
    	g.setColor(CANVAS_COLOR_TEXT);
    }
    
    protected void keyPressed(int keyCode) {
    	int maxViews = 1;
    	
    	if (keyCode == s.getKeycodeLeftSoftkey()) {
    		s.showMainMenu();	
    	} else if (keyCode == s.getKeycodeRightSoftkey()) { // back on all views
    		dismiss();
    	} else if (getGameAction(keyCode) == LEFT) {
    		if (view > 0) {
    			view--;
    			scroll = maxScrollHeight = 0;
    			repaint();
    		} else {
    			dismiss();
    		}
    	} else if (getGameAction(keyCode) == RIGHT) {
    		if (view < maxViews) {
    			view++;
    			scroll = maxScrollHeight = 0;
    			repaint();
    		}
    	} else if (getGameAction(keyCode) == UP) {
   			if (scroll > 0) { scroll -= SCROLL_INCREMENT; }
   			repaint();
    	} else if (getGameAction(keyCode) == DOWN) {
   			if (scroll < maxScrollHeight) { scroll += SCROLL_INCREMENT; }
   			repaint();
    	}
    }
    
    protected void keyRepeated(int keyCode) {
    	keyPressed(keyCode);
    }

    protected void pointerPressed(int x, int y) {
        dismiss();
    }
    
    protected void showNotify(){
        active = true;
        t = new Thread(this);
        t.start();
    }
    
    protected void hideNotify() {
        active = false;
        t = null;
    }
    
    private void dismiss() {
    	s.showNoteList();
    }

	public void run() {
		while (active) {
			try {
				Thread.sleep(50);
				if (isShown()) {
					if (view == 0 && note.getImageLink() != "" && note.image == null) {
						repaint();
					} else if (view == 1 && note.getMapLink() != "" && note.map == null) {
						repaint();
					}
				}
			} catch (InterruptedException ie) {
				s.notice(ie.toString());
			}
		}
	}
}
