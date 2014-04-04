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

    public IndentedWriter( Writer out )
    { super( out ); }

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
	    out.write( '\t' );
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
