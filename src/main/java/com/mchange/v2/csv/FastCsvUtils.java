/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
 */

package com.mchange.v2.csv;

import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.IOException;

public final class FastCsvUtils
{
    private final static int ESCAPE_BIT = 1 << 24;
    private final static int SHIFT_BIT  = 1 << 25;
    private final static int SHIFT_OFFSET = 8;

    //we can ignore escaped quotes. since they must be paired (""), they don't affect the even/odd count
    //TODO: it might be a good idea to detect illegal quoting and report errors...
    public static String csvReadLine(BufferedReader br) throws IOException
    {
	String s = br.readLine();

	String out;
	if ( s != null )
	    {
		int quoteCount = countQuotes(s);
		if (quoteCount % 2 != 0) 
		    {
			StringBuilder sb = new StringBuilder( s );
			do
			    {
				s = br.readLine();
				sb.append( s );
				quoteCount += countQuotes(s);
			    }
			while( quoteCount % 2 != 0 );
			out = sb.toString();
		    }
		else
		    out = s;
	    }
	else
	    out = null;

	return out;
    }

    private static int countQuotes(String s)
    {
	char[] chars = s.toCharArray();
	int count = 0;
	for (int i = 0, len = chars.length; i < len; ++i) 
	    {
		if (chars[i] == '"') ++count;
	    }
	return count;
    }

    public static String[] splitRecord( String csvRecord ) throws MalformedCsvException
    {
	int[] upshifted = upshiftQuoteString( csvRecord );
	//debugPrint( upshifted );
	List upshiftedSplit = splitShifted( upshifted );
	int len = upshiftedSplit.size();
	String[] out = new String[ len ];
	for (int i = 0; i < len; ++i)
	    out[i] = downshift( (int[]) upshiftedSplit.get(i) );
	return out;
    }

    private static void debugPrint(int[] arr)
    {
	int len = arr.length;
	char[] cbuf = new char[len];
	for (int i = 0; i < len; ++i)
	    cbuf[i] = isShifted( arr[i] ) ? '_' : (char) arr[i];
	System.err.println( new String(cbuf) );
    }

    private static List splitShifted(int[] shiftedQuoteString)
    {
	List out = new ArrayList();
	
	int sstart = 0;
	for (int finger = 0, len = shiftedQuoteString.length; finger <= len; ++finger)
	    {
		if ( finger == len || shiftedQuoteString[finger] == ',')
		    {
			int slen = finger - sstart;
			
			// trim unquoted whitespace next to commas
			// note that whitespace chars in quotes will be shifted, so won't look like whitespace chars
			int tstart;
			int tlen = -1;
			for (tstart = sstart; tstart <= finger; ++tstart)
			    {
				if (tstart == finger)
				    {
					tlen = 0;
					break;
				    }
				else if (shiftedQuoteString[tstart] != ' ' && shiftedQuoteString[tstart] != '\t')
				    break;
			    }
			if (tlen < 0)
			    {
				if (tstart == finger - 1)
				    tlen = 1;
				else 
				    {
					for (tlen = finger - tstart; tlen > 0; --tlen)
					    {
						int index = tstart + tlen - 1;
						if (shiftedQuoteString[index] != ' ' && shiftedQuoteString[index] != '\t')
						    break;
					    }
				    }
			
			    }
			
			//DEBUG
			//tlen = slen;
			//tstart = sstart;
			//END DEBUG

			int[] trimsplit = new int[ tlen ];
			if ( tlen > 0 )
			    System.arraycopy( shiftedQuoteString, tstart, trimsplit, 0, tlen );
			out.add( trimsplit );
			sstart = finger + 1;
		    }
	    }
	return out;
    }

    private static String downshift(int[] maybeShifted)
    {
	int len = maybeShifted.length;
	char[] cbuf = new char[ len ];
	for (int i = 0; i < len; ++i)
	    {
		int c = maybeShifted[i];
		cbuf[i] = (char) (isShifted( c ) ? c >>> SHIFT_OFFSET : c); //cast eliminates shift bit
	    }
	return new String( cbuf );
    }

    private static boolean isShifted( int c )
    { return ( c & SHIFT_BIT ) != 0; }

    private static int[] upshiftQuoteString(String s) throws MalformedCsvException
    {
	//System.err.printf("ENTERED upshiftQuoteString, s->%s\n", s);

	char[] chars = s.toCharArray();
	int[] buf = new int[ chars.length ];
	
	EscapedCharReader rdr = new EscapedCharReader( chars );
	int finger = 0;
	boolean shift = false;

	for (int c = rdr.read(shift); c >= 0; c = rdr.read(shift))
	    {
		//System.err.println( (char) c );
		if (c == '"') // imples an unescaped quote
		    shift = !shift;
		else
		    buf[finger++] = findShiftyChar( c, shift );
	    }
	
	int[] out = new int[ finger ];
	System.arraycopy( buf, 0, out, 0, finger );
	return out;
    }

    private static int findShiftyChar( int c, boolean shift )
    { return ( shift ? ((c << SHIFT_OFFSET) | SHIFT_BIT) : c ); }

    private static int escape( int c )
    { return c | ESCAPE_BIT;  }

    private static boolean isEscaped( int c )
    { return (c & ESCAPE_BIT) != 0; }

    private static class EscapedCharReader
    {
	char[] chars;
	int finger;

	EscapedCharReader( char[] chars )
	{
	    this.chars = chars;
	    this.finger = 0;
	}

	int read(boolean shift) throws MalformedCsvException
	{
	    if (finger < chars.length)
		{
		    char out = chars[finger++];
		    if (out == '"' && shift) //we're inside quotes, have to watch for escaped quotes
			{
			    if (finger < chars.length)
				{
				    char next = chars[ finger ];
				    if ( next == '"' )
					{
					    ++finger;
					    //System.err.println("SKIP");
					    return escape( next );
					}
				    else return out;
				}
			    else
				{
				    //this is a quote that ends a csv field
				    return out; 
				}
			}
		    else 
			return out;
		}
	    else
		return -1;
	}
    }

    private FastCsvUtils()
    {}
}

    /* WHOOPS! I thought backslashes marked escapes. Nope.

    private final static int ESCAPED_BACKSLASH = escape( (int) '\\' );

    private static int findShiftyChar( int nonQuoteChar, boolean shift )
    {
	int nqc = ( nonQuoteChar == ESCAPED_BACKSLASH ? '\\' : nonQuoteChar );
	return ( shift ? (nonQuoteChar << SHIFT_OFFSET) | SHIFT_BIT : nonQuoteChar );
    }

    private static class EscapedCharReader
    {
	char[] chars;
	int finger;

	EscapedCharReader( char[] chars )
	{
	    this.chars = chars;
	    this.finger = 0;
	}

	int read() throws MalformedCsvException
	{
	    if (finger < chars.length)
		{
		    char out = chars[finger++];
		    if (out == '\\')
			{
			    if (finger < chars.length)
				{
				    char next = chars[ finger + 1 ];
				    if ( next == '\\' || next == '"' )
					{
					    ++finger;
					    return escape( next );
					}
				    else return out;
				}
			    else
				{
				    //we consider a backslash not before a quote or another backslash
				    //just a backslash
				    return out; 
				    
				    //throw new MalformedCsvException("Escape character '\\' at end of input!");
				}
			}
		    else 
			return out;
		}
	    else
		return -1;
	}
    }
    */


/*
	char[] chars = s.toCharArray();
	int[]  out =   new int[chars.length];

	int last = -1;
	int cur  = -1;

	boolean shift = false;

	int out_finger = 0;
	for (int i = 0, len = out.length; i < len; ++i)
	    {
		last = cur;
		cur = chars[i];
		boolean is_quote = (cur == '"' && last != '\\');

		if (shift && is_quote)
		    shift = false;

		out[i] = shift ? cur << 8 : cur;

		if (!shift && is_quote)
		    shift = true;
		    
	    }
*/
