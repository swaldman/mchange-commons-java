package com.mchange.lang;

import java.io.*;
import com.mchange.v2.lang.VersionUtils;

/**
 * @deprecated jdk 1.4 mow includes this idea as part of the standard 
 *             Throwable/Exception classes.
 */            
public class PotentiallySecondaryException extends Exception implements PotentiallySecondary
{
    final static String NESTED_MSG = ">>>>>>>>>> NESTED EXCEPTION >>>>>>>>";

    Throwable nested;

    public PotentiallySecondaryException(String msg, Throwable t)
    {
	super(msg, t);
	this.nested = t;
    }

    public PotentiallySecondaryException(Throwable t)
    {this("", t);}

    public PotentiallySecondaryException(String msg)
    {this(msg, null);}

    public PotentiallySecondaryException()
    {this("", null);}

    public Throwable getNestedThrowable()
    {return nested;}

    private void setNested(Throwable t)
    {
	this.nested = t;
	if ( VersionUtils.isAtLeastJavaVersion1_4() )
	    this.initCause( t );
    }

    public void printStackTrace(PrintWriter pw)
    {
	super.printStackTrace(pw);
	if ( !VersionUtils.isAtLeastJavaVersion1_4() && nested != null)
	    {
		pw.println(NESTED_MSG);
		nested.printStackTrace(pw);
	    }
    }

    public void printStackTrace(PrintStream ps)
    {
	super.printStackTrace(ps);
	if ( !VersionUtils.isAtLeastJavaVersion1_4() && nested != null)
	    {
		ps.println("NESTED_MSG");
		nested.printStackTrace(ps);
	    }
    }

    public void printStackTrace()
    {
	if ( VersionUtils.isAtLeastJavaVersion1_4() )
	    super.printStackTrace();
	else
	    this.printStackTrace(System.err);
    }
}
