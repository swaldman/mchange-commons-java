package com.mchange.lang;

import java.io.*;

public class PotentiallySecondaryRuntimeException extends RuntimeException implements PotentiallySecondary
{
    final static String NESTED_MSG = ">>>>>>>>>> NESTED EXCEPTION >>>>>>>>";

    Throwable nested;

    public PotentiallySecondaryRuntimeException(String msg, Throwable t)
    {
	super(msg);
	this.nested = t;
    }

    public PotentiallySecondaryRuntimeException(Throwable t)
    {this("", t);}

    public PotentiallySecondaryRuntimeException(String msg)
    {this(msg, null);}

    public PotentiallySecondaryRuntimeException()
    {this("", null);}

    @Override
    public Throwable getNestedThrowable()
    {return nested;}

    @Override
    public void printStackTrace(PrintWriter pw)
    {
	super.printStackTrace(pw);
	if (nested != null)
	    {
		pw.println(NESTED_MSG);
		nested.printStackTrace(pw);
	    }
    }

    @Override
    public void printStackTrace(PrintStream ps)
    {
	super.printStackTrace(ps);
	if (nested != null)
	    {
		ps.println("NESTED_MSG");
		nested.printStackTrace(ps);
	    }
    }

    @Override
    public void printStackTrace()
    {printStackTrace(System.err);}
}
