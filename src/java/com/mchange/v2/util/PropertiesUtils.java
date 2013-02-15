/*
 * Distributed as part of mchange-commons-java v.0.2.4
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.v2.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties; 

public final class PropertiesUtils
{
    public static int getIntProperty( Properties props, String name, int dflt ) throws NumberFormatException
    {
	String unparsed = props.getProperty( name );
	return ( unparsed != null ? Integer.parseInt( unparsed ) : dflt );
    }

    public static Properties fromString(String propsString, String encoding) throws UnsupportedEncodingException
    {
	try
	    {
		Properties out = new Properties();
		if ( propsString != null )
		    {
			byte[] bytes = propsString.getBytes( encoding );
			out.load( new ByteArrayInputStream( bytes ) );
		    }
		return out;
	    }
	catch (UnsupportedEncodingException e)
	    { throw e; }
	catch (IOException e)
	    { throw new Error("Huh? An IOException while working with byte array streams?!", e); }
    }

    public static Properties fromString(String propsString)
    { 
	try { return fromString( propsString, "ISO-8859-1" ); }
	catch (UnsupportedEncodingException e)
	    { throw new Error("Huh? An ISO-8859-1 is an unsupported encoding?!", e); }
    }

    public static String toString(Properties props, String comment, String encoding) throws UnsupportedEncodingException
    {
	try
	    {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		props.store( baos, comment);
		baos.flush(); //superfluous but I feel better...
		return new String( baos.toByteArray(), encoding );
	    }
	catch (UnsupportedEncodingException e)
	    { throw e; }
	catch (IOException e)
	    { throw new Error("Huh? An IOException while working with byte array streams?!", e); }
    }

    public static String toString(Properties props, String comment)
    { 
	try { return toString( props, comment, "ISO-8859-1" ); }
	catch (UnsupportedEncodingException e)
	    { throw new Error("Huh? An ISO-8859-1 is an unsupported encoding?!", e); }
    }

    private PropertiesUtils()
    {}
}
