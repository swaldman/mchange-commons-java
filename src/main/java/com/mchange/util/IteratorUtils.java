package com.mchange.util;

import java.util.Iterator;

public class IteratorUtils
{
  public static Iterator unmodifiableIterator(final Iterator ii)
    {
      return new Iterator()
	{
	  public boolean hasNext()
	    {return ii.hasNext();}
	  
	  public Object next()
	    {return ii.next();}

	  public void remove()
	    {throw new UnsupportedOperationException("This Iterator does not support the remove operation.");}
	};
    }
}

