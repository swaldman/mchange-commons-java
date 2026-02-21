package com.mchange.v2.cfg;

import com.mchange.v2.lang.ObjectUtils;

public final class DelayedLogItem
{
    public static enum Level
    { ALL, CONFIG, FINE, FINER, FINEST, INFO, OFF, SEVERE, WARNING }

    private Level     level;
    private String    text;
    private Throwable exception;
    
    public Level     getLevel()     { return level; }
    public String    getText()      { return text; }
    public Throwable getException() { return exception; }
    
    public DelayedLogItem(Level level, String text, Throwable exception)
    {
	this.level     = level;
	this.text      = text;
	this.exception = exception;
    }

    public DelayedLogItem(Level level, String text)
    { this( level, text, null ); }

    public boolean equals( Object o )
    {
	if (o instanceof DelayedLogItem)
	{
	    DelayedLogItem other = (DelayedLogItem) o;
	    return
		this.level.equals( other.level ) &&
		this.text.equals( other.text ) &&
		ObjectUtils.eqOrBothNull( this.exception, other.exception );
	}
	else
	    return false;
    }

    public int hashCode()
    {
	return
	    this.level.hashCode() ^
	    this.text.hashCode() ^
	    ObjectUtils.hashOrZero( this.exception );
    }

    public String toString()
    {
	StringBuffer sb = new StringBuffer();
	sb.append( this.getClass().getName() );
	sb.append( String.format(" [ level -> %s, text -> \042%s\042, exception -> %s]", level, text, exception ) );
	return sb.toString();
    }
}
