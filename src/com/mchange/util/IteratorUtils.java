package com.mchange.util;

import java.util.Iterator;

public class IteratorUtils
{
  public static <T> Iterator<T> unmodifiableIterator(final Iterator<T> ii)
    {
      return new Iterator<T>()
	{
	  @Override
	  public boolean hasNext()
	    {return ii.hasNext();}
	  
	  @Override
	  public T next()
	    {return ii.next();}

	  @Override
	  public void remove()
	    {throw new UnsupportedOperationException("This Iterator does not support the remove operation.");}
	};
    }
}

