package com.mchange.v1.util;

import java.util.Collection;
import java.util.Iterator;

public class UnreliableIteratorUtils
{
    public static void addToCollection(Collection c, UnreliableIterator uii) 
				throws UnreliableIteratorException
    {
	while (uii.hasNext())
	    c.add( uii.next() );
    }

    public static UnreliableIterator unreliableIteratorFromIterator(final Iterator ii)
    {
	return new UnreliableIterator()
	    {
		public boolean hasNext()
		{ return ii.hasNext(); }

		public Object  next()
		{ return ii.next(); }

		public void    remove()
		{ ii.remove(); }

		public void close()
		{}
	    };
    }
}
