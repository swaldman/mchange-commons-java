package com.mchange.v1.util;

import java.util.Collection;
import java.util.Iterator;

public class UnreliableIteratorUtils
{
    public static <T> void addToCollection(Collection<? super T> c, UnreliableIterator<? extends T> uii) 
				throws UnreliableIteratorException
    {
	while (uii.hasNext())
	    c.add( uii.next() );
    }

    public static <T> UnreliableIterator<T> unreliableIteratorFromIterator(final Iterator<T> ii)
    {
	return new UnreliableIterator<T>()
	    {
		@Override
		public boolean hasNext()
		{ return ii.hasNext(); }

		@Override
		public T  next()
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
