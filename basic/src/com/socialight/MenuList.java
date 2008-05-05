package com.socialight;

/**
 * wrapper that is responsible for all choice-based menu screens. helps abstract soft key, behavior
 * 
 * @author naveen
 */
public class MenuList extends ListCanvas {
	private Socialight s;
	private String screen;
	
	public MenuList(Socialight _s, String _sn, String[] _items, String _m, String _l, String _r) {
		s = _s;
		screen = _sn;
        
       	System.out.println("Created " + screen);
        
        listTitle = _m;
        listSoftkeyLeft = _l;
        listSoftkeyRight = _r;
        
        if (_items != null) {
        	for (int i = 0; i < _items.length; i++) {
        		if (_items[i] != null) {
        			elements.addElement(new ListItem(null, _items[i], null, null));
        		}
        	}
        }
	}
    
    public void itemSelected(int position, int keyCode) {
    	s.canvasCommand(screen, position, keyCode);
    }
}