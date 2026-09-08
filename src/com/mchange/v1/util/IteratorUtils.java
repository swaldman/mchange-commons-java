package com.mchange.v1.util;

import java.util.*;
import java.lang.reflect.Array;

public final class IteratorUtils
{
    /**
     *  Left raw deliberately, as java.util.Collections leaves EMPTY_LIST and EMPTY_SET raw:
     *  a raw empty constant can be assigned to an Iterator of any element type, where an
     *  Iterator&lt;?&gt; could not be assigned to anything. The initializer is parameterized
     *  so that the rawness costs no unchecked operation.
     */
    public final static Iterator EMPTY_ITERATOR = new Iterator<Object>()
    {
	@Override
	public boolean hasNext()
	{ return false; }

	@Override
	public Object next()
	{ throw new NoSuchElementException(); }

	@Override
	public void remove()
	{ throw new IllegalStateException(); }
    };

    public static <T> Iterator<T> oneElementUnmodifiableIterator(final T elem)
    {
	return new Iterator<T>()
	    {
		boolean shot = false;

		@Override
		public boolean hasNext() { return (!shot); }

		@Override
		public T next()
		{
		    if (shot)
			throw new NoSuchElementException();
		    else
			{
			    shot = true;
			    return elem;
			}
		}

		@Override
		public void remove()
		{ throw new UnsupportedOperationException("remove() not supported."); }
	    };
    }

    public static boolean equivalent(Iterator<?> ii, Iterator<?> jj)
    {
	while (true)
	    {
		boolean ii_hasnext = ii.hasNext();
		boolean jj_hasnext = jj.hasNext();
		if (ii_hasnext ^ jj_hasnext)
		    return false;
		else if (ii_hasnext)
		    {
			Object iiNext = ii.next();
			Object jjNext = jj.next();
			if (iiNext == jjNext)
			    continue;
			else if (iiNext == null)
			    return false;
			else if (!iiNext.equals(jjNext))
			    return false;
		    }
		else return true;
	    }
    }

    public static <T> ArrayList<T> toArrayList(Iterator<? extends T> ii, int initial_capacity)
    {
	ArrayList<T> out = new ArrayList<T>(initial_capacity);
	while (ii.hasNext())
	    out.add(ii.next());
	return out;
    }

    /**
     * Fills an array with the contents of an iterator. If the array is too small,
     * it will contain the first portion of the iterator. If the array can contain
     * more elements than the iterator, extra elements are left untouched, unless
     * null_terminate is set to true, in which case the element immediately following
     * the last from the iterator is set to null. (This method is intended to make
     * it easy to implement Collection.toArray(Object[] oo) methods...
     *
     * @param null_terminate iff there is extra space in the array, set the element
     *        immediately after the last from the iterator to null.
     */
    public static void fillArray(Iterator<?> ii, Object[] fillMe, boolean null_terminate)
    {
	int i = 0;
	int len = fillMe.length;
	while ( i < len && ii.hasNext() )
	    fillMe[ i++ ] = ii.next();
	if (null_terminate && i < len)
	    fillMe[i] = null;
    }

    public static void fillArray(Iterator<?> ii, Object[] fillMe)
    { fillArray( ii, fillMe, false); }

    /**
     * @param null_terminate iff there is extra space in the array, set the element
     *        immediately after the last from the iterator to null.
     */
    public static Object[] toArray(Iterator<?> ii, int array_size, Class<?> componentClass, boolean null_terminate)
    {
	Object[] out = (Object[]) Array.newInstance( componentClass, array_size );
	fillArray(ii, out, null_terminate);
	return out;
    }

    public static Object[] toArray(Iterator<?> ii, int array_size, Class<?> componentClass)
    { return toArray( ii, array_size, componentClass, false ); }

    /**
     * Designed to help implement Collection.toArray(Object[] )methods... does
     * the right thing if you can express an iterator and know the size of your
     * Collection.
     */
    public static Object[] toArray(Iterator<?> ii, int ii_size, Object[] maybeFillMe)
    {
	if (maybeFillMe.length >= ii_size)
	    {
		fillArray( ii, maybeFillMe, true );
		return maybeFillMe;
	    }
	else
	    {
		Class<?> componentType = maybeFillMe.getClass().getComponentType(); 
		return toArray( ii, ii_size, componentType );
	    }
    }

    private IteratorUtils()
    {}
}


