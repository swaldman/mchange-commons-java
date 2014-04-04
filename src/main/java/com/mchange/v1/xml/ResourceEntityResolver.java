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




