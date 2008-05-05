package com.socialight;

import java.io.IOException;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

/**
 * provides helpers for all low-level elements (fonts, icons, scrolling, softkeys)
 * 
 * @author naveen
 */
public class BaseCanvas extends Canvas {
	
	protected final int CANVAS_COLOR_TEXT = 0xFFFFFF;
	protected final int CANVAS_COLOR_TEXT_LIGHT = 0xDDDDDD;
	protected final int CANVAS_COLOR_BACKGROUND = 0x000000;
	protected final int CANVAS_COLOR_BORDER = 0x878787;
	
	public static final String SOFTKEY_TEXT_MENU = "Menu";
	public static final String SOFTKEY_TEXT_REFRESH = "Refresh";
	public static final String SOFTKEY_TEXT_BACK = "Back";
	public static final String SOFTKEY_TEXT_OK = "OK";
	public static final String SOFTKEY_TEXT_NEXT = "Next";
	public static final String SOFTKEY_TEXT_CANCEL = "Cancel";
	public static final String SOFTKEY_TEXT_CREATE = "Create";
	public static final String SOFTKEY_TEXT_CLOSE = "Close";
	
	public static final Font FONT_PLAIN = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    public static final Font FONT_BOLD = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL);

    protected final int SCROLL_INCREMENT = 30;
    protected int scroll = 0;
    protected int maxScrollHeight = 0;
	
	protected Image appLogo = null;
	protected Image iconGpsActive = null, iconGpsInactive = null;
	
	protected Image imageNote = null;
	protected Image imageMenu = null;
	protected Image imageBack = null;
	protected Image imageNext = null;
	protected Image imageCancel = null;
	protected Image imageUp = null, imageDown = null;
	
	private final int SOFTKEY_LEFT = 0;
	private final int SOFTKEY_RIGHT = 1;
	
	public BaseCanvas() {
		try {
			imageNote = Image.createImage("/note.png");
			imageMenu = Image.createImage("/sk_menu.png");
			imageBack = Image.createImage("/sk_back.png");
			imageNext = Image.createImage("/sk_next.png");
			imageCancel = Image.createImage("/sk_cancel.png");
			imageUp = Image.createImage("/nav_up.png");
			imageDown = Image.createImage("/nav_down.png");
			appLogo = Image.createImage("/logo_small.png");
			
			iconGpsActive = Image.createImage("/icon_gps_active.png");
			iconGpsInactive = Image.createImage("/icon_gps_inactive.png");
		} catch (IOException ioe) {
			System.out.println("IOException: " + ioe.toString());
		}
	}

	protected void paint(Graphics g) {
		// Clear out the display
        g.setColor(0x000000);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        drawHeader(g);
	}
	
	protected void drawHeader(Graphics g) {
		g.setColor(CANVAS_COLOR_BACKGROUND);
		g.fillRect(0, 0, getWidth(), 20);
		
        if (appLogo != null) {
        	g.drawImage(appLogo, getWidth() - 16 - 2, 2, Graphics.TOP | Graphics.LEFT);
        }
        
        int iconGpsStatus = Socialight.getGpsStatus();
        
        switch (iconGpsStatus) {
        case 2:
        	drawGpsStatus(g, true);
        	break;
        case 1:
        	drawGpsStatus(g, false);
        	break;
        case 0:
        default:
        	break;
        }
        
        g.setColor(CANVAS_COLOR_BORDER);
        g.drawLine(0, 20, getWidth(), 20);
	}
	
	protected void drawHeader(Graphics g, String _title) {
		// TODO move this text width variable somewhere else higher up
		int textWidth = getWidth() - 8;
		
		drawHeader(g);
		
        g.setColor(CANVAS_COLOR_TEXT);
        g.setFont(FONT_BOLD);
        g.drawString(TextUtilities.fitToLine(_title, true, FONT_BOLD, textWidth - 16), 4, (20 - FONT_BOLD.getHeight() - 2), Graphics.TOP|Graphics.LEFT);
	}
	
	protected void drawGpsStatus(Graphics g, boolean gpsIsActive) {
		if (gpsIsActive && iconGpsActive != null) {
			g.drawImage(iconGpsActive, getWidth() - 32 - 4, 2, Graphics.TOP | Graphics.LEFT);
		} else {
			if (iconGpsInactive != null) {
				g.drawImage(iconGpsInactive, getWidth() - 32 - 4, 2, Graphics.TOP | Graphics.LEFT);
			}
		}
	}
	
	protected void drawFooter(Graphics g, String _left, String _right) {
		g.setColor(CANVAS_COLOR_BACKGROUND);
		g.fillRect(0, getHeight() - 20, getWidth(), 20);
		
		drawSoftkey(g, _left);
		drawSoftkey(g, _right);
	}
	
	protected void drawScroll(Graphics g, boolean up, boolean down) {
        if (up && imageUp != null) {
        	g.drawImage(imageUp, getWidth()/2, getHeight() - 12, Graphics.VCENTER|Graphics.HCENTER);
        }
        
        if (down && imageDown != null) {
        	g.drawImage(imageDown, getWidth()/2, getHeight() - 8, Graphics.VCENTER|Graphics.HCENTER);
        }
	}
	
	protected void drawSoftkey(Graphics g, String key) {
		if (g != null && key != null) {
			// TODO maybe bottom border Y should be based on text+icon height instead
	        g.setColor(CANVAS_COLOR_BORDER);
	        g.drawLine(0, getHeight() - 20, getWidth(), getHeight() - 20);
	        g.setColor(CANVAS_COLOR_TEXT);
	        
			Image currentImage = null;
			int location = SOFTKEY_LEFT;
			
	        // consolidate icon and position
	        if (key.equals(SOFTKEY_TEXT_MENU)) {
	        	currentImage = imageMenu;
	        	location = SOFTKEY_LEFT;
			} else if (key.equals(SOFTKEY_TEXT_BACK)) {
	        	currentImage = imageBack;
	        	location = SOFTKEY_RIGHT;
			} else if (key.equals(SOFTKEY_TEXT_CLOSE)) {
				currentImage = imageBack;
				location = SOFTKEY_LEFT;
	        } else if (key.equals(SOFTKEY_TEXT_OK) || key.equals(SOFTKEY_TEXT_NEXT)) {
	        	currentImage = imageNext;
	        	location = SOFTKEY_LEFT;
	        } else if (key.equals(SOFTKEY_TEXT_CANCEL)) {
	        	currentImage = imageCancel;
	        	location = SOFTKEY_RIGHT;
	        } else if (key.equals(SOFTKEY_TEXT_CREATE)) {
	        	currentImage = imageNote;
	        	location = SOFTKEY_LEFT;
	        } else if (key.equals(SOFTKEY_TEXT_REFRESH)) {
	        	currentImage = imageNote;
	        	location = SOFTKEY_RIGHT;
	        }

	        // these are useful for spacing text and icons
			int horizontalGap = 4;
			int verticalGap = 2;
			
			// these auto-computes will help position the data
	        int imageWidth = currentImage != null ? currentImage.getWidth() : 16;
			int rightTextX = getWidth() - g.getFont().stringWidth(key) - 4;
			int textY = getHeight() - g.getFont().getHeight();
	        
	        // draw the soft key and text in the appropriate location
	        if (location == SOFTKEY_LEFT) {       	
        		g.drawString(key, horizontalGap + imageWidth + horizontalGap, textY, Graphics.TOP|Graphics.LEFT);

        		if (currentImage != null) {
	        		g.drawImage(currentImage, horizontalGap, getHeight() - currentImage.getHeight() - verticalGap, Graphics.TOP|Graphics.LEFT);
	        	}	        	
	        } else if (location == SOFTKEY_RIGHT) {
	        	g.drawString(key, rightTextX, textY, Graphics.TOP|Graphics.LEFT);
	        	
	        	if (currentImage != null) {
	        		g.drawImage(currentImage, rightTextX - imageWidth - horizontalGap, getHeight() - currentImage.getHeight() - verticalGap, Graphics.TOP|Graphics.LEFT);
	        	}
	        }
		}
	}
}
