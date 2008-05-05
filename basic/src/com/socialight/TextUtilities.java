package com.socialight;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Font;

/**
 * provides helper functions to encode string for URLs and to
 * wrap strings according to screen width
 * 
 * @author naveen
 */
public class TextUtilities {

	/**
	 * the characters that do not need to
	 * be converted.
	 */
    private static final String noEncode =
        "abcdefghijklmnopqrstuvwxyz" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
        "0123456789.*-_";

    /**
     * mapping value values 0 through 15 to the
     * corresponding hexadecimal character.
     */
    private static final char[] hexDigits = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    /**
     * encodes the given string as required for
     * use in a URL query string or POST data.
     * 
     * TODO: does this work with UTF-8 data?
     */
    public static String encode(String src) {
        StringBuffer result = new StringBuffer(src.length());
        int count = src.length();
        for (int i = 0; i < count; i++) {
            char c = src.charAt(i);
            if (noEncode.indexOf(c) != -1) {
                // This is a character that does not
                // need to be encoded
                result.append(c);
                continue;
            }

            // Space is converted to '+'
            if (c == ' ') {
                result.append('+');
                continue;
            }

            // The remaining characters must be converted to
            // '%XY' where 'XY' is the hexadecimal value of
            // the character itself.
            result.append('%');
            result.append(hexDigits[(c >> 4) & 0xF]);
            result.append(hexDigits[c & 0xF]);
        }
        return result.toString();
    }
    
    /**
     * when given a string, it attempts to fit as much as it can onto a single line
     */
    public static String fitToLine(String fullLine, boolean appendEllipses, Font f, int width) {
    	Enumeration e = wrap(fullLine, f, width).elements();
        String line = e.nextElement().toString();
        
        if (e.hasMoreElements() && appendEllipses) {
        	line += "...";
        }
        
        return line;
    }

	/**
	 * wraps string into parts (return parts as substrings);
	 * approach: first, split on line break; then, fit words to end of line
	 * adapted from j2me polish
	 */
	public static Vector wrap(String value, Font font, int lineWidth) {
		Vector lines = new Vector();
		
		if (lineWidth <= 0) {
			lines.addElement(value);
			return lines;
		}
		
		boolean hasLineBreaks = (value.indexOf('\n') != -1);
		int completeWidth = font.stringWidth(value);
		
		// fits on one line
		if ( (completeWidth <= lineWidth && !hasLineBreaks) ) {
			lines.addElement(value);
			return lines;
		}
		
		if (!hasLineBreaks) {
			wrap( value, font, completeWidth, lineWidth, lines );
		} else {
			char[] valueChars = value.toCharArray();
			int lastIndex = 0;
			char c =' ';
			for (int i = 0; i < valueChars.length; i++) {
				c = valueChars[i];
				if (c == '\n' || i == valueChars.length -1 ) {
					String line = new String(valueChars, lastIndex, (i + 1) - lastIndex);
					completeWidth = font.stringWidth(line);
					if (completeWidth <= lineWidth ) {
						lines.addElement(line);			
					} else {
						wrap(line, font, completeWidth, lineWidth, lines);
					}
					lastIndex = i + 1;
				} // for each line
			} // for all chars

			// in case the line ends with a newline, add one
			if (c == '\n') {
				lines.addElement("");
			}
		}
		return lines;
	}
	
	public static void wrap(String value, Font font,
			int completeWidth,	int lineWidth, Vector list) 
	{
		char[] valueChars = value.toCharArray();
		int startPos = 0;
		int lastSpacePos = -1;
		int lastSpacePosLength = 0;
		int currentLineWidth = 0;
		for (int i = 0; i < valueChars.length; i++) {
			char c = valueChars[i];
			currentLineWidth += font.charWidth( c );
			if (c == '\n') {
				list.addElement( new String( valueChars, startPos, i - startPos ) );
				lastSpacePos = -1;
				startPos = i+1;
				currentLineWidth = 0;
				i = startPos;
			} else if (currentLineWidth >= lineWidth && i > 0) {
				if ( lastSpacePos == -1 ) {
					i--;
					list.addElement( new String( valueChars, startPos, i - startPos ) );
					startPos =  i;
					currentLineWidth = 0;
				} else {
					currentLineWidth -= lastSpacePosLength;
					list.addElement( new String( valueChars, startPos, lastSpacePos - startPos ) );
					startPos =  lastSpacePos + 1;
					lastSpacePos = -1;
				}
			} else if (c == ' ' || c == '\t') {
				lastSpacePos = i;
				lastSpacePosLength = currentLineWidth;
			}
			
		} 

		list.addElement( new String( valueChars, startPos, valueChars.length - startPos ) );
	}

}