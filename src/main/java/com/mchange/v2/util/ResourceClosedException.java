package com.mchange.v2.util;

import com.mchange.v2.lang.VersionUtils;

public class ResourceClosedException extends RuntimeException
{
    //retaining 1.3.x compatability for now

//     public ResourceClosedException(String msg, Throwable t)
//     { super( msg, t ); }

//     public ResourceClosedException(Throwable t)
//     { super(t); }

    Throwable rootCause;

    public ResourceClosedException(String msg, Throwable t)
    { 
	super( msg ); 
	setRootCause( t );
    }

    public ResourceClosedException(Throwable t)
    { 
	super(); 
	setRootCause( t );
    }

    public ResourceClosedException(String msg)
    { super( msg ); }

    public ResourceClosedException()
    { super(); }

    public Throwable getCause()
    { return rootCause; }

    private void setRootCause( Throwable t )
    {
	this.rootCause = t;
	if ( VersionUtils.isAtLeastJavaVersion14() )
	    this.initCause( t );
    }
}
