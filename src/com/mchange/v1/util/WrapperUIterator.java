package com.mchange.v1.util;

import java.util.*;
import com.mchange.v1.util.DebugUtils;

/**
 *  This implementation does not yet support removes once hasNext() has
 *  been called... will add if necessary.
 */
public abstract class WrapperUIterator<T> implements UIterator<T>
{
    protected final static Object SKIP_TOKEN = new Object();

    final static boolean DEBUG = true;

    UIterator<?> inner;
    boolean  supports_remove;
    Object   lastOut = null;
    Object   nextOut = SKIP_TOKEN;
    
    public WrapperUIterator(UIterator<?> inner, boolean supports_remove)
    { 
	this.inner = inner; 
	this.supports_remove = supports_remove;
    }

    public WrapperUIterator(UIterator<?> inner)
    { this( inner, false ); }

    @Override
    public boolean hasNext() throws Exception
    {
	findNext();
	return nextOut != SKIP_TOKEN; 
    }

    private void findNext() throws Exception
    {
	if (nextOut == SKIP_TOKEN)
	    {
		while (inner.hasNext() && nextOut == SKIP_TOKEN)
		    this.nextOut = transformObject( inner.next() );
	    }
    }

    /**
     *  nextOut and lastOut hold either an element or the SKIP_TOKEN sentinel, so they
     *  cannot be typed T. transformObject's contract is to return a T or the sentinel,
     *  and the sentinel is never what gets returned from here, so the cast holds; the
     *  compiler cannot see that through a field typed Object.
     */
    @Override
    @SuppressWarnings("unchecked")
    public T next() throws NoSuchElementException, Exception
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

	assert( nextOut == SKIP_TOKEN && lastOut != SKIP_TOKEN );

	return (T) lastOut;
    }
    
    @Override
    public void remove() throws Exception
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

    @Override
    public void close() throws Exception
    { inner.close(); }

    /**
     * return SKIP_TOKEN to indicate an object should be
     * skipped, i.e., not exposed as part of the iterator.
     * (we don't use null, because we want to support iterators
     * over null-accepting Collections.)
     */
    protected abstract Object transformObject(Object o) throws Exception;
}






