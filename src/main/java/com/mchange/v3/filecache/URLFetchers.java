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

package com.mchange.v3.filecache;

import java.net.URL;

import java.io.*;
import com.mchange.v1.io.*;
import com.mchange.v2.log.*;

public enum URLFetchers implements URLFetcher
{

    DEFAULT
    {
	public InputStream openStream( URL u, MLogger logger ) throws IOException
	{ return u.openStream(); }
    },
	
    BUFFERED_WGET
    {
	public InputStream openStream( URL u, MLogger logger ) throws IOException
	{ 
	    Process p = new ProcessBuilder("wget", "-O", "-", u.toString()).start();
	    
	    InputStream is = null;
	    
	    try
		{
		    is = new BufferedInputStream( p.getInputStream(), 1048576 );
		    ByteArrayOutputStream baos = new ByteArrayOutputStream( 1048576 ); //1 MB, but expandable
		    
		    for (int b = is.read(); b >= 0; b = is.read())
			baos.write(b);
		    
		    return new ByteArrayInputStream( baos.toByteArray() );
		}
	    finally
		{
		    InputStreamUtils.attemptClose( is );
		    
		    if ( logger.isLoggable( MLevel.FINER ) ) //log the error stream
			{
			    Reader errReader = null;
			    
			    try
				{
				    errReader = new BufferedReader( new InputStreamReader( p.getErrorStream() ), 1048576 );
				    StringWriter writer = new StringWriter( 1048576 );
				    
				    for (int c = errReader.read(); c >= 0; c = errReader.read())
					writer.write( c );
				    
				    logger.log( MLevel.FINER, "wget error stream for '" + u + "':\n " + writer.toString());
				}
			    finally
				{ ReaderUtils.attemptClose( errReader ); }
			}
		    
		    try
			{ 
			    int check = p.waitFor(); 
			    if (check != 0) // something went wrong
				throw new IOException("wget process terminated abnormally [return code: " + check + "]");
			}
		    catch (InterruptedException e)
			{
			    if ( logger.isLoggable( MLevel.FINER ) )
				logger.log(MLevel.FINER, "InterruptedException while waiting for wget to complete.", e);

			    throw new IOException("Interrupted while waiting for wget to complete: " + e);
			}
		    
		}
	}
    }
    ;

}
