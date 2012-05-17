/*
 * Distributed as part of mchange-commons-java v.0.2.1
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


package com.mchange.v1.xml;

import java.io.InputStream;
import java.io.IOException;
import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

public class ResourceEntityResolver implements EntityResolver
{
    ClassLoader cl;
    String prefix;

    public ResourceEntityResolver(ClassLoader cl, String rsrcPrefix)
    { 
	///System.err.println("rsrcPrefix: " + rsrcPrefix);
	this.cl = cl;
	this.prefix = rsrcPrefix;
    }

    public ResourceEntityResolver(Class loadSibling)
    { this(loadSibling.getClassLoader(), classToPrefix(loadSibling)); }

    public InputSource resolveEntity(String publicId, String systemId)
	throws SAXException, IOException
    {
//  	System.err.println("publicId: " + publicId);
//  	System.err.println("systemId: " + systemId);

	if (systemId == null)
	    return null;

	int last_slash = systemId.lastIndexOf('/');
	String systemIdFilePart = (last_slash >= 0 ? systemId.substring(last_slash + 1) : systemId);
	InputStream is = cl.getResourceAsStream(prefix + systemIdFilePart);
	return (is == null ? null : new InputSource(is));
    }

    private static String classToPrefix(Class c)
    {
	String className = c.getName();
	int last_dot = className.lastIndexOf('.');
	String pkgName = (last_dot > 0 ? className.substring(0, last_dot) : null);
	StringBuffer sb = new StringBuffer(256);
	/* ClassLoader.getResourceAsStream() takes an arg always relative to classloader root. */
	//sb.append('/');
	if (pkgName != null)
	    {
		sb.append(pkgName);
		for (int i = 0, len = sb.length(); i < len; ++i)
		    if (sb.charAt(i) == '.') sb.setCharAt(i, '/');
		sb.append('/');
	    }
	return sb.toString();
    }
}


	
//  	/* DEBUG CRAP */
//    	System.err.println();
//    	System.err.println();
//    	System.err.println();
//    	System.err.println("RESOLVING ENTITY");
//    	System.err.println();
//    	System.err.println();
//    	System.err.println(cl);
	
//    	InputStream test = cl.getResourceAsStream(prefix + systemIdFilePart);
//    	System.err.println("test: " + test);
//    	if (test == null)
//    	    System.err.println("NULL DTD!!! (" + prefix + systemIdFilePart + ')');
//    	else
//    	    {
//    	 	for (int b = test.read(); b >= 0; b = test.read()) System.err.write(b);
//    	 	System.err.flush();
//    	    }
//          /* END DEBUG CRAP */




