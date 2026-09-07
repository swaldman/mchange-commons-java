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
		@Override
		public boolean hasNext()
		{ return ii.hasNext(); }

		@Override
		public Object  next()
		{ return ii.next(); }

		@Override
		public void    remove()
		{ ii.remove(); }

		@Override
		public void close()
		{}
	    };
    }
}
