package com.mchange.v1.util;

import java.util.Collection;
import java.util.Iterator;

public class UIteratorUtils
{
    public static void addToCollection(Collection c, UIterator uii) throws Exception
    {
	while (uii.hasNext())
	    c.add( uii.next() );
    }

    public static UIterator uiteratorFromIterator(final Iterator ii)
    {
	return new UIterator()
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
