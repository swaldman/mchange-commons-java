package com.mchange.v2.csv;

import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.IOException;

// RFC 4180-style CSV,
// though accepts (unquoted) CR only or LF only in addition to CRLF as line terminator
public final class FastCsvUtils
{
    private final static int ESCAPE_BIT = 1 << 24;
    private final static int SHIFT_BIT  = 1 << 25;
    private final static int SHIFT_OFFSET = 8;

    private final static int CR = '\r';
    private final static int LF = '\n';

    private final static int EOF = -1;

    private final static int CRLF_TOKEN = 999;

    private final static String CRLF = "\r\n";

    private final static int GUESSED_LINE_LEN = 512;

    public static String generateQuotedCsvItem(String s)
    {
        int quoteCount = countQuotes(s);
        if ( quoteCount == 0 )
            return "\"" + s + "\"";
        else
        {
            int len = s.length();
            StringBuilder sb = new StringBuilder(len + quoteCount + 2);
            sb.append('\"');
            for (int i = 0; i < len; ++i)
            {
                char c = s.charAt(i);
                if (c == '"')
                    sb.append("\"\"");
                else
                    sb.append(c);
            }
            sb.append('\"');
            return sb.toString();
        }
    }

    public static String[] generateCsvItemsAlwaysQuoted(String[] ss)
    {
        int len = ss.length;
        String[] out = new String[len];
        for (int i = 0; i < len; ++i)
            out[i] = generateQuotedCsvItem(ss[i]);
        return out;
    }

    public static String generateCsvLineQuotedUnterminated(String[] ss)
    {
        int len = ss.length;
        StringBuffer sb = new StringBuffer( GUESSED_LINE_LEN );
        for (int i = 0; i < len; ++i)
        {
            sb.append(generateQuotedCsvItem(ss[i]));
            if (i != len-1) sb.append(',');
        }
        return sb.toString();
    }

    // read a logical line, which may span multiple physical lines
    // (because CR/LF/CRLF might be included in quoted spans)
    public static String csvReadLine(BufferedReader br) throws IOException, MalformedCsvException
    {
	int[] holder = new int[1];
	String s = readLine( br, holder );

	String out;
	if ( s != null )
	    {
                //we can ignore escaped quotes. since they must be paired (""), they don't affect the even/odd count
		int quoteCount = countQuotes(s);
		if (quoteCount % 2 != 0) 
		    {
			StringBuilder sb = new StringBuilder( s );
			do
			    {
				appendForToken( holder[0], sb );
				s = readLine( br, holder );
				if (s != null)
				{
				    sb.append( s );
				    quoteCount += countQuotes(s);
				}
				else
				    throw new MalformedCsvException("Unterminated quote at EOF: '" + sb.toString() + "'");
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

    // for joining CSV lines that span multiple physical lines
    //
    // we have to restore the token that split the physical lines
    private static void appendForToken( int token, StringBuilder sb )
    {
	switch (token ) {
	case CR:
	case LF:
	    sb.append( (char) token );
	    break;
	case CRLF_TOKEN:
	    sb.append( CRLF );
	    break;
	case EOF:
	    //do nothing
	    break;
	default:
	    throw new InternalError("Unexpected token (should never happen): " + token);
	}
    }

    // reads a line, storing the token that ended the line, which can be CR, LF, CRLF, or EOF
    //
    // outSep is a size one array which will contain the separator char or -1 for EOF
    private static String readLine(BufferedReader br, int[] outSep) throws IOException
    {
	StringBuilder sb = new StringBuilder( GUESSED_LINE_LEN );
	int i = br.read();
	if ( i < 0 ) 
	{
	    outSep[0] = EOF;
	    return null;
	}
	else 
	{
	    while( notSepOrEOF(i) ) 
	    {
		sb.append( (char) i ); 
		i = br.read();
	    }
	    if (i == CR)
	    {
		br.mark(1);
		int check = br.read();
		if ( check == LF ) outSep[0] = CRLF_TOKEN;
		else 
		{
		    br.reset();
		    outSep[0] = CR;
		}
	    }
	    else outSep[0] = i;
	    return sb.toString();
	}
    }

    private static boolean notSepOrEOF( int i ) 
    { return i >= 0 && (i != '\n' && i != '\r'); }

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

    /**
     * @deprecated("Prefer csvSplitLine.")
     */
    public static String[] splitRecord( String csvRecord ) throws MalformedCsvException
    { return csvSplitLine( csvRecord ); }

    // split a logical line, which may span multiple physical lines, into correct CSV elements
    public static String[] csvSplitLine( String csvLine ) throws MalformedCsvException
    {
	int[] upshifted = upshiftQuoteString( csvLine );
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

    // here we just split on commas and trim around them.
    // we don't have to worry about quoted commas or whitespace, because
    // that has already been shifted
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

    // to avoid splitting on quoted regions, we shift everything inside quoted strings up,
    // and mark them with SHIFT_BIT to indicate that we have done so.
    //
    // when we are done with this, all characters within double quotes are shifted,
    // while the double-quote characters that begin and end the shift region are eliminated
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

    // note that this also shifts the escape bit for escaped double-quote chars,
    // so once we've shifted, if we tested those chars for isEscaped(c), it'd show false!
    //
    // but we don't and we don't need to, the escape has already served its function, distinguishing
    // quote terminating from non-quote-terminating double quotes.
    // the escape bit is shifted beyond the high bit, so disappears.
    private static int findShiftyChar( int c, boolean shift )
    { return ( shift ? ((c << SHIFT_OFFSET) | SHIFT_BIT) : c ); }

    private static int escape( int c )
    { return c | ESCAPE_BIT;  }

    private static boolean isEscaped( int c )
    { return (c & ESCAPE_BIT) != 0; }

    // read one character at a time.
    //
    // the client tracks whether we are inside a double-quoted region and ensures that
    // if we are, shift is true, if we are not, shift is false.
    //
    // when we are in a double-quoted region,
    // we have to unescape double double-quotes ("") to single double-quote chars, but we don't want
    // these interior single quotes to be taken as delimiters.
    //
    // so we return the double-quote char with an escape bit set,
    // so it is not actually equal to '"'
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
