package com.socialight;

import javax.microedition.lcdui.Graphics;

/**
 * draws progress dots on the screen to indicate action
 * 
 * TODO: provide ability to redraw small portion of screen to indicate action
 * instead of clearing the full screen before showing progress. (for example, blinking dot)
 * 
 * @author naveen
 */
public class ProgressCanvas extends BaseCanvas implements Runnable {
	private Socialight s;
    private String message = "";
    private int counter = 0;
    
    private Thread t = null;
    private boolean active;
    
    public ProgressCanvas(Socialight _s){
		s = _s;

		setFullScreenMode(true);
    }
    
    protected void paint(Graphics g){
    	if (counter < 4) {
    		counter++;
    	} else { counter = 0; }
    	
        g.setColor(CANVAS_COLOR_BACKGROUND);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        int px = getWidth()/2;
		int py = getHeight()/2 - getHeight()/5;
		int diam = (getWidth()+getHeight())/60;
		
		g.translate(px,py);
				
		g.setColor(CANVAS_COLOR_BORDER);
		g.fillArc(-diam/2,-diam/2,diam,diam,0,360);
		g.fillArc((-diam/2)-25,-diam/2,diam,diam,0,360);
		g.fillArc((-diam/2)-50,-diam/2,diam,diam,0,360);
		g.fillArc((-diam/2)+25,-diam/2,diam,diam,0,360);
		g.fillArc((-diam/2)+50,-diam/2,diam,diam,0,360);
		
		g.setColor(0xFFF791);
		
		if (counter == 0) {
			g.fillArc((-diam/2)-50,-diam/2,diam,diam,0,360);
		} else if (counter == 1) {
			g.fillArc((-diam/2)-25,-diam/2,diam,diam,0,360);
		} else if (counter == 2) {
			g.fillArc(-diam/2,-diam/2,diam,diam,0,360);
		} else if (counter == 3) {
			g.fillArc((-diam/2)+25,-diam/2,diam,diam,0,360);
		} else if (counter == 4) {
			g.fillArc((-diam/2)+50,-diam/2,diam,diam,0,360);
		}
        
		g.setColor(CANVAS_COLOR_TEXT);
		g.setFont(FONT_PLAIN);
		g.translate(-g.getTranslateX(), -g.getTranslateY());
		g.translate(getWidth()/2, getHeight()-16);

    	g.drawString(message, 0, 0, Graphics.HCENTER|Graphics.BASELINE);
    }
    
    
    /** Used to update the message displayed on the ProgressCanvas.  This method
     * does not provide any text wrapping functionality.
     * @param _m The progress message to be displayed.
     */
    public void updateMessage(String _m){
    	if (_m != null) { message = _m; }
        
        repaint();
        serviceRepaints();
    }

    /** The implementation calls showNotify() immediately prior to this Canvas
     * being made visible on the display. Canvas subclasses may override this
     * method to perform tasks before being shown, such as setting up animations,
     * starting timers, etc. The default implementation of this method in class
     * Canvas is empty.
     */
    protected void showNotify(){
		active = true;
		t = new Thread(this);
		t.start();
    }

    /** The implementation calls hideNotify() shortly after the Canvas has been
     * removed from the display. Canvas subclasses may override this method to
     * pause animations, revoke timers, etc. The default implementation of this
     * method in Canvas is empty.
     */
    protected void hideNotify(){
		active = false;
		t = null;
    }

    /** When an object implementing interface Runnable is used to create a thread,
     * starting the thread causes the object's run method to be called in that
     * separately executing thread.
     */
    public void run() {
		while(active) {
			try {
			    /** Paint the display while this canvas is still shown */
				Thread.sleep(100);
				if (isShown()) {
					repaint();
				}
			} catch(Exception e) {
				s.notice(e.toString());
			}
		}
    }
}
