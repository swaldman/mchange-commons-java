package com.mchange.v1.util;

import java.util.*;
import com.mchange.v1.util.DebugUtils;

/**
 *  This implementation does not yet support removes once hasNext() has
 *  been called... will add if necessary.
 */
public abstract class WrapperIterator implements Iterator
{
    protected final static Object SKIP_TOKEN = new Object();

    final static boolean DEBUG = true;

    Iterator inner;
    boolean  supports_remove;
    Object   lastOut = null;
    Object   nextOut = SKIP_TOKEN;
    
    public WrapperIterator(Iterator inner, boolean supports_remove)
    { 
	this.inner = inner; 
	this.supports_remove = supports_remove;
    }

    public WrapperIterator(Iterator inner)
    { this( inner, false ); }

    public boolean hasNext()
    {
	findNext();
	return nextOut != SKIP_TOKEN; 
    }

    private void findNext()
    {
	if (nextOut == SKIP_TOKEN)
	    {
		while (inner.hasNext() && nextOut == SKIP_TOKEN)
		    this.nextOut = transformObject( inner.next() );
	    }
    }

    public Object next()
    {
	findNext();
	if (nextOut != SKIP_TOKEN)
	    {
		lastOut = nextOut;
		nextOut = SKIP_TOKEN;
	    }
	else
	    throw new NoSuchElementException();

	//postcondition
	if (DEBUG)
	    DebugUtils.myAssert( nextOut == SKIP_TOKEN && lastOut != SKIP_TOKEN );
	return lastOut;
    }
    
    public void remove()
    { 
	if (supports_remove)
	    {
		if (nextOut != SKIP_TOKEN)
		    throw new UnsupportedOperationException(this.getClass().getName() +
							    " cannot support remove after" +
							    " hasNext() has been called!");
		if (lastOut != SKIP_TOKEN)
		    inner.remove();
		else
		    throw new NoSuchElementException();
	    }
	else
	    throw new UnsupportedOperationException(); 
    }

    /**
     * return SKIP_TOKEN to indicate an object should be
     * skipped, i.e., not exposed as part of the iterator.
     * (we don't use null, because we want to support iterators
     * over null-accepting Collections.)
     */
    protected abstract Object transformObject(Object o);
}






