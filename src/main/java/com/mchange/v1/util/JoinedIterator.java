package com.mchange.v1.util;

import java.util.*;

public class JoinedIterator implements Iterator
{
    Iterator[] its;
    Iterator   removeIterator = null;
    boolean    permit_removes;
    int        cur = 0;

    public JoinedIterator(Iterator[] its, boolean permit_removes)
    {
	this.its = its;
	this.permit_removes = permit_removes;
    }

    public boolean hasNext()
    {
	if (cur == its.length)
	    return false;
	else if (its[ cur ].hasNext())
	    return true;
	else
	    {
		++cur;
		return this.hasNext();
	    }
    }

    public Object next()
    {
	if (! this.hasNext())
	    throw new NoSuchElementException();

	removeIterator = its[cur];
	return removeIterator.next();
    }

    public void remove()
    {
	if (permit_removes)
	    {
		if (removeIterator != null)
		    {
			removeIterator.remove();
			removeIterator = null;
		    }
		else
		    throw new IllegalStateException("next() not called, or element already removed.");
	    }
	else
	    throw new UnsupportedOperationException();
    }
}
