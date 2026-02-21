package com.mchange.v1.util;

public class BrokenObjectException extends Exception
{
    Object broken;

    public BrokenObjectException(Object broken, String msg)
    {
	super(msg);
	this.broken = broken;
    }

    public BrokenObjectException(Object broken)
    {
	super();
	this.broken = broken;
    }

    public Object getBrokenObject()
    {return broken;}
}
