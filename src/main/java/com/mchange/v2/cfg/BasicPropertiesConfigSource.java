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

package com.mchange.v2.cfg;

import java.util.*;
import java.io.*;

import static com.mchange.v2.cfg.DelayedLogItem.*;

public final class BasicPropertiesConfigSource implements PropertiesConfigSource
{
    public Parse propertiesFromSource( String identifier ) throws FileNotFoundException, Exception
    {
	InputStream rawStream = MultiPropertiesConfig.class.getResourceAsStream( identifier );
	if ( rawStream != null )
	{
	    InputStream pis = new BufferedInputStream( rawStream );
	    Properties p = new Properties();
	    List<DelayedLogItem> messages = new LinkedList<DelayedLogItem>();
	    try
	    { p.load( pis ); }
	    finally
	    {
		try { if ( pis != null ) pis.close(); } //ensures closuer of nested rawStream as well
		catch (IOException e) 
		    { messages.add( new DelayedLogItem( Level.WARNING, "An IOException occurred while closing InputStream from resource path '" + identifier + "'.", e ) ); }
	    }
	    return new Parse(p, messages);
	}
	else
	    throw new FileNotFoundException( String.format("Resource not found at path '%s'.", identifier) );
    }
}

