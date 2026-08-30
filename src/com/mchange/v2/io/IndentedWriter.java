package com.mchange.v2.io;

import java.io.*;

public class IndentedWriter extends FilterWriter
{
    final static String EOL;

    static
    {
	String eol = System.getProperty( "line.separator" );
	EOL = ( eol != null ? eol : "\r\n" );
    }

    int indent_level = 0;
    boolean at_line_start = true;
    String indentSpacing;

    public IndentedWriter( Writer out, String indentSpacing )
    {
	super( out );
	this.indentSpacing = indentSpacing;
    }

    public IndentedWriter( Writer out )
    { this( out, "\t" ); }
    

    private boolean isEol( char c )
    { return ( c == '\r' || c == '\n' ); }

    public void upIndent()
    { ++indent_level; }

    public void downIndent()
    { --indent_level; }

    public void write( int c ) throws IOException
    { 
	out.write( c );
	at_line_start = isEol( (char) c );
    }

    public void write( char[] chars, int off, int len ) throws IOException
    {
	out.write( chars, off, len );
	at_line_start = isEol( chars[ off + len - 1] );
    }

    public void write( String s, int off, int len ) throws IOException
    {
	if (len > 0)
	    {
		out.write( s, off, len );
		at_line_start = isEol( s.charAt( off + len - 1) );
	    }
    }

    private void printIndent() throws IOException
    {
	for (int i = 0; i < indent_level; ++i)
	    out.write( indentSpacing );
    }

    public void print( String s ) throws IOException
    {
	if ( at_line_start )
	    printIndent();
	out.write(s);
	char last = s.charAt( s.length() - 1 );
	at_line_start = isEol( last );
    }

    public void println( String s ) throws IOException
    {
	if ( at_line_start )
	    printIndent();
	out.write(s);
	out.write( EOL );
	at_line_start = true;
    }

    public void print( boolean x ) throws IOException
    { print( String.valueOf(x) ); }

    public void print( byte x ) throws IOException
    { print( String.valueOf(x) ); }

    public void print( char x ) throws IOException
    { print( String.valueOf(x) ); }

    public void print( short x ) throws IOException
    { print( String.valueOf(x) ); }

    public void print( int x ) throws IOException
    { print( String.valueOf(x) ); }

    public void print( long x ) throws IOException
    { print( String.valueOf(x) ); }

    public void print( float x ) throws IOException
    { print( String.valueOf(x) ); }

    public void print( double x ) throws IOException
    { print( String.valueOf(x) ); }

    public void print( Object x ) throws IOException
    { print( String.valueOf(x) ); }

    public void println( boolean x ) throws IOException
    { println( String.valueOf(x) ); }

    public void println( byte x ) throws IOException
    { println( String.valueOf(x) ); }

    public void println( char x ) throws IOException
    { println( String.valueOf(x) ); }

    public void println( short x ) throws IOException
    { println( String.valueOf(x) ); }

    public void println( int x ) throws IOException
    { println( String.valueOf(x) ); }

    public void println( long x ) throws IOException
    { println( String.valueOf(x) ); }

    public void println( float x ) throws IOException
    { println( String.valueOf(x) ); }

    public void println( double x ) throws IOException
    { println( String.valueOf(x) ); }

    public void println( Object x ) throws IOException
    { println( String.valueOf(x) ); }

    public void println() throws IOException
    { println( "" ); }
}
