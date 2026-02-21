package com.mchange.lang;

import java.io.*;

public class PotentiallySecondaryError extends Error implements PotentiallySecondary
{
    final static String NESTED_MSG = ">>>>>>>>>> NESTED THROWABLE >>>>>>>>";

    Throwable nested;

    public PotentiallySecondaryError(String msg, Throwable t)
    {
	super(msg);
	this.nested = t;
    }

    public PotentiallySecondaryError(Throwable t)
    {this("", t);}

    public PotentiallySecondaryError(String msg)
    {this(msg, null);}

    public PotentiallySecondaryError()
    {this("", null);}

    public Throwable getNestedThrowable()
    {return nested;}

    public void printStackTrace(PrintWriter pw)
    {
	super.printStackTrace(pw);
	if (nested != null)
	    {
		pw.println(NESTED_MSG);
		nested.printStackTrace(pw);
	    }
    }

    public void printStackTrace(PrintStream ps)
    {
	super.printStackTrace(ps);
	if (nested != null)
	    {
		ps.println("NESTED_MSG");
		nested.printStackTrace(ps);
	    }
    }

    public void printStackTrace()
    {printStackTrace(System.err);}
}
