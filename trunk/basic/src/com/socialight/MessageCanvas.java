package com.socialight;

import java.util.Enumeration;

import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;

/**
 * screen to help show notification text in a scrollable pane (help, confirmations, etc)
 * 
 * @author naveen
 */
public class MessageCanvas extends BaseCanvas {
    private Socialight s;
    private Displayable next;
    
    private String screen = null;
    private String title = "";
    private String message = "";

    private String[] softKeys = new String[2];
    
    public MessageCanvas(Socialight _s, String _sn, String _t, String _m) {
    	s = _s;
    	screen = _sn;
    	title = _t != null ? _t : "";
    	message = _m != null ? _m : "";
    	
    	setFullScreenMode(true);
    }
    
    public void addSoftkey(String _sk) {
    	if (softKeys[0] == null) {
    		softKeys[0] = _sk;
    	} else {
    		softKeys[1] = _sk;
    	}
    }
    
    protected void paint(Graphics g) {
    	super.paint(g);
		
        int textLeftX = 4;
		int textTopY = 22; // originally 37 and Graphics.BOTTOM
		int textWidth = getWidth() - 8;
        
        if (message != null) {
        	g.setColor(CANVAS_COLOR_TEXT);
            g.setFont(FONT_PLAIN);
        	g.translate(g.getTranslateX(), g.getTranslateY() - scroll);
        	
        	if (!message.equals("")) {
        		Enumeration e = TextUtilities.wrap(message, FONT_PLAIN, textWidth).elements();

        		while (e.hasMoreElements()) {
        			g.drawString(e.nextElement().toString(), textLeftX, textTopY, Graphics.TOP|Graphics.LEFT);
        			textTopY = textTopY + FONT_PLAIN.getHeight();
        		}
        	}

        	maxScrollHeight = (textTopY - 30) < 0 ? 0 : textTopY - getHeight() + 40;
            g.translate(0, scroll);
        }

        drawHeader(g, title);

        g.setColor(CANVAS_COLOR_BORDER);
        g.drawLine(0, getHeight() - 20, getWidth(), getHeight() - 20);

        g.setColor(CANVAS_COLOR_TEXT);
        g.setFont(FONT_PLAIN);

        drawFooter(g, softKeys[0], softKeys[1]);
        drawScroll(g, scroll != 0, scroll < maxScrollHeight);
    }
    
    public void setNext(Displayable _c) { next = _c; }
    public Displayable getNext() { return next; }
    
    protected void keyPressed(int keyCode) {
    	if (screen != null) {
    		if (getGameAction(keyCode) == UP) {
    			if (scroll > 0) { scroll -= SCROLL_INCREMENT; }
    			repaint();
    		} else if (getGameAction(keyCode) == DOWN) {
    			if (scroll < maxScrollHeight) { scroll += SCROLL_INCREMENT; }
    			repaint();
    		} else {
    			s.canvasCommand(screen, 0, keyCode);
    		}
    	}
    }
    
    protected void keyRepeated(int keyCode) {
    	keyPressed(keyCode);
    }
}