package com.mchange.util;

import java.util.Iterator;

public class IteratorUtils
{
  public static Iterator unmodifiableIterator(final Iterator ii)
    {
      return new Iterator()
	{
	  @Override
	  public boolean hasNext()
	    {return ii.hasNext();}
	  
	  @Override
	  public Object next()
	    {return ii.next();}

	  @Override
	  public void remove()
	    {throw new UnsupportedOperationException("This Iterator does not support the remove operation.");}
	};
    }
}

