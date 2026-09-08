package com.mchange.v1.util;

import java.util.Collection;
import java.util.Iterator;

public class UIteratorUtils
{
    public static <T> void addToCollection(Collection<? super T> c, UIterator<? extends T> uii) throws Exception
    {
	while (uii.hasNext())
	    c.add( uii.next() );
    }

    public static <T> UIterator<T> uiteratorFromIterator(final Iterator<T> ii)
    {
	return new UIterator<T>()
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
