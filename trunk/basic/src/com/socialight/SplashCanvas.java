package com.socialight;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * a canvas which displays an image for a short time
 * 
 * @author michael.sharon
 * @author naveen
 */

public class SplashCanvas extends Canvas {
    private Display display;
    private Displayable next;
    
    private Timer timer = new Timer();
    
    private Image image;
    private String imageLocation = "";
    
    //requires current display, next display, location of splash screen
    public SplashCanvas(Display _d, Displayable _n, String _i) {
        display = _d;
        setNext(_n);
        
        imageLocation = _i;
        setFullScreenMode(true);
        
        display.setCurrent(this);
        System.out.println("Current image = " + _i + " | Set to " + imageLocation);         
    }

    public SplashCanvas(Display _d, Displayable _n, Image _i) {
    	display = _d;
    	setNext(_n);
    	
    	image = _i;
    	setFullScreenMode(true);
    	display.setCurrent(this);
    }
    
    protected void paint(Graphics g){        
        g.setColor(0x000000);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // Load an image from the given location
        if (image == null) {
            try {
            	image = Image.createImage(imageLocation);          	 
            } catch (IOException ex) {
                g.setColor(0xFFFFFF);
                g.drawString("Socialight starting...", 0, 0, Graphics.TOP | Graphics.LEFT);
                System.out.println(ex); 
                return;
            }
        }        

        g.drawImage(image, getWidth()/2, getHeight()/2, Graphics.HCENTER | Graphics.VCENTER);
    }
    
    public void setNext(Displayable _n) {
    	if (_n != null) { next = _n; }
    }

    protected void showNotify(){
    	// TODO this ISE only happened after using LayerList as home view instead of NoteList. why?
    	try {
    		timer.schedule( new CountDown(), 2000 );
    	} catch (IllegalStateException ise) {
    		System.out.println("IllegalStateException: " + ise.toString());
    	}
    }

    protected void keyPressed( int keyCode ){
        dismiss();
    }
    
    protected void pointerPressed( int x, int y ){
        dismiss();
    }    
    
    private void dismiss() {
        timer.cancel();
        if (next != null) {
        	display.setCurrent( next );	
        }         
    }
    
    private class CountDown extends TimerTask {
        public void run(){
            dismiss();
        }
    }
}