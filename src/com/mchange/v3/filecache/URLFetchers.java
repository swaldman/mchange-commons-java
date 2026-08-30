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
