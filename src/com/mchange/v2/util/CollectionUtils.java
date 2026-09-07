package com.mchange.v2.util;

import java.util.*;
import java.lang.reflect.*;

/*
 * Note: This class makes assumptions about the implementation of Collections.unmodifiableXXX( ... )
 * and Collections.synchronizedXXX( ... ) that could conceivably not hold in some Java std class
 * implementation... but the implementation is robust to the most likely implementations of these
 * methods.
 */
public final class CollectionUtils
{
    /**
     *  Left raw deliberately, as java.util.Collections leaves EMPTY_LIST and EMPTY_SET raw:
     *  a raw empty constant can be assigned to a SortedSet of any element type, where a
     *  SortedSet<?> could not be assigned to anything. The initializer is parameterized so
     *  that the rawness costs no unchecked operation.
     */
    public final static SortedSet EMPTY_SORTED_SET = Collections.unmodifiableSortedSet( new TreeSet<Object>() );

    final static Class<?>[]  EMPTY_ARG_CLASSES = { };
    final static Object[] EMPTY_ARGS        = { };

    final static Class<?>[] COMPARATOR_ARG_CLASSES = { Comparator.class };
    final static Class<?>[] COLLECTION_ARG_CLASSES = { Collection.class };
    final static Class<?>[] SORTED_SET_ARG_CLASSES = { SortedSet.class };
    final static Class<?>[] MAP_ARG_CLASSES        = { Map.class };
    final static Class<?>[] SORTED_MAP_ARG_CLASSES = { SortedMap.class };

    final static Class<?> STD_UNMODIFIABLE_COLLECTION_CL;
    final static Class<?> STD_UNMODIFIABLE_SET_CL;
    final static Class<?> STD_UNMODIFIABLE_LIST_CL;
    final static Class<?> STD_UNMODIFIABLE_RA_LIST_CL;
    final static Class<?> STD_UNMODIFIABLE_SORTED_SET_CL;
    final static Class<?> STD_UNMODIFIABLE_MAP_CL;
    final static Class<?> STD_UNMODIFIABLE_SORTED_MAP_CL;
    final static Class<?> STD_SYNCHRONIZED_COLLECTION_CL;
    final static Class<?> STD_SYNCHRONIZED_SET_CL;
    final static Class<?> STD_SYNCHRONIZED_LIST_CL;
    final static Class<?> STD_SYNCHRONIZED_RA_LIST_CL;
    final static Class<?> STD_SYNCHRONIZED_SORTED_SET_CL;
    final static Class<?> STD_SYNCHRONIZED_MAP_CL;
    final static Class<?> STD_SYNCHRONIZED_SORTED_MAP_CL;

    final static Set<Class<?>> UNMODIFIABLE_WRAPPERS;
    final static Set<Class<?>> SYNCHRONIZED_WRAPPERS;
    final static Set<Class<?>> ALL_COLLECTIONS_WRAPPERS;

    static
    {
	HashSet<Object> hs = new HashSet<Object>();
	TreeSet<Object> ts = new TreeSet<Object>();
	LinkedList<Object> ll = new LinkedList<Object>();
	ArrayList<Object> al = new ArrayList<Object>();
	HashMap<Object,Object> hm = new HashMap<Object,Object>();
	TreeMap<Object,Object> tm = new TreeMap<Object,Object>();

	HashSet<Class<?>> tmp0 = new HashSet<Class<?>>();
	HashSet<Class<?>> tmp1 = new HashSet<Class<?>>();

	tmp0.add( STD_UNMODIFIABLE_COLLECTION_CL = Collections.unmodifiableCollection( al ).getClass() );
	tmp0.add( STD_UNMODIFIABLE_SET_CL = Collections.unmodifiableSet( hs ).getClass() );
	tmp0.add( STD_UNMODIFIABLE_LIST_CL = Collections.unmodifiableList( ll ).getClass() );
	tmp0.add( STD_UNMODIFIABLE_RA_LIST_CL = Collections.unmodifiableList( al ).getClass() );
	tmp0.add( STD_UNMODIFIABLE_SORTED_SET_CL = Collections.unmodifiableSortedSet( ts ).getClass() );
	tmp0.add( STD_UNMODIFIABLE_MAP_CL = Collections.unmodifiableMap( hm ).getClass() );
	tmp0.add( STD_UNMODIFIABLE_SORTED_MAP_CL = Collections.unmodifiableSortedMap( tm ).getClass() );

	tmp1.add( STD_SYNCHRONIZED_COLLECTION_CL = Collections.synchronizedCollection( al ).getClass() );
	tmp1.add( STD_SYNCHRONIZED_SET_CL = Collections.synchronizedSet( hs ).getClass() );
	tmp1.add( STD_SYNCHRONIZED_LIST_CL = Collections.synchronizedList( ll ).getClass() );
	tmp1.add( STD_SYNCHRONIZED_RA_LIST_CL = Collections.synchronizedList( al ).getClass() );
	tmp1.add( STD_SYNCHRONIZED_SORTED_SET_CL = Collections.synchronizedSortedSet( ts ).getClass() );
	tmp1.add( STD_SYNCHRONIZED_MAP_CL = Collections.synchronizedMap( hm ).getClass() );
	tmp1.add( STD_SYNCHRONIZED_SORTED_MAP_CL = Collections.synchronizedMap( tm ).getClass() );

	UNMODIFIABLE_WRAPPERS = Collections.unmodifiableSet( tmp0 );

	SYNCHRONIZED_WRAPPERS = Collections.unmodifiableSet( tmp1 );

	HashSet<Class<?>> tmp2 = new HashSet<Class<?>>( tmp0 );
	tmp2.addAll( tmp1 );
	ALL_COLLECTIONS_WRAPPERS = Collections.unmodifiableSet( tmp2 );
    }

    public static boolean isCollectionsWrapper( Class<?> cl )
    { return ALL_COLLECTIONS_WRAPPERS.contains( cl ); }

    public static boolean isCollectionsWrapper( Collection<?> c )
    { return isCollectionsWrapper( c.getClass() ); }

    public static boolean isCollectionsWrapper( Map<?,?> m )
    { return isCollectionsWrapper( m.getClass() ); }

    public static boolean isSynchronizedWrapper( Class<?> cl )
    { return SYNCHRONIZED_WRAPPERS.contains( cl ); }

    public static boolean isSynchronizedWrapper( Collection<?> c )
    { return isSynchronizedWrapper( c.getClass() ); }

    public static boolean isSynchronizedWrapper( Map<?,?> m )
    { return isSynchronizedWrapper( m.getClass() ); }

    public static boolean isUnmodifiableWrapper( Class<?> cl )
    { return UNMODIFIABLE_WRAPPERS.contains( cl ); }

    public static boolean isUnmodifiableWrapper( Collection<?> c )
    { return isUnmodifiableWrapper( c.getClass() ); }

    public static boolean isUnmodifiableWrapper( Map<?,?> m )
    { return isUnmodifiableWrapper( m.getClass() ); }

    /*
     * should we worry about the case where an Object (bizarrely)
     * implements both Set and List? don't think so...
     */
    public static <T> Collection<T> narrowUnmodifiableCollection( Collection<T> c )
    {
	if (c instanceof SortedSet)
	    return Collections.unmodifiableSortedSet( (SortedSet<T>) c );
	else if (c instanceof Set)
	    return Collections.unmodifiableSet( (Set<T>) c );
	else if (c instanceof List)
	    return Collections.unmodifiableList( (List<T>) c );
	else
	    return Collections.unmodifiableCollection( c );
    }

    /*
     * should we worry about the case where an Object (bizarrely)
     * implements both Set and List? don't think so...
     */
    public static <T> Collection<T> narrowSynchronizedCollection( Collection<T> c )
    {
	if (c instanceof SortedSet)
	    return Collections.synchronizedSortedSet( (SortedSet<T>) c );
	else if (c instanceof Set)
	    return Collections.synchronizedSet( (Set<T>) c );
	else if (c instanceof List)
	    return Collections.synchronizedList( (List<T>) c );
	else
	    return Collections.synchronizedCollection( c );
    }

    public static <K,V> Map<K,V> narrowUnmodifiableMap( Map<K,V> m )
    {
	if (m instanceof SortedMap)
	    return Collections.unmodifiableSortedMap( (SortedMap<K,V>) m );
	else
	    return Collections.unmodifiableMap( m );
    }

    public static <K,V> Map<K,V> narrowSynchronizedMap( Map<K,V> m )
    {
	if (m instanceof SortedMap)
	    return Collections.synchronizedSortedMap( (SortedMap<K,V>) m );
	else
	    return Collections.synchronizedMap( m );
    }

    /**
     *  Attempts to find a public clone() method or a copy constructor, in that
     *  order, and calls what it finds. If neither is available, throws a NoSuchMethodException.
     *
     *  Unchecked is suppressed rather than avoided here. Every route to the copy --
     *  Object clone(), Method.invoke(...), Constructor.newInstance(...) -- is typed to
     *  return Object, so the element type cannot be checked at the point of the cast,
     *  only preserved. A copy of a Collection has the element type of the original, which is what the
     *  signature says; the compiler simply cannot see it through reflection.
     */
    @SuppressWarnings("unchecked")
    public static <T> Collection<T> attemptClone( Collection<T> c ) throws NoSuchMethodException
    {
	if (c instanceof Vector) return (Collection<T>) ((Vector<T>) c).clone();
	else if (c instanceof ArrayList) return (Collection<T>) ((ArrayList<T>) c).clone();
	else if (c instanceof LinkedList) return (Collection<T>) ((LinkedList<T>) c).clone();
	else if (c instanceof HashSet) return (Collection<T>) ((HashSet<T>) c).clone();
	else if (c instanceof TreeSet) return (Collection<T>) ((TreeSet<T>) c).clone();
	else
	    {
		Collection<T> out = null;
		Class<?> colClass = c.getClass();
		try
		    {
			Method m = colClass.getMethod("clone", EMPTY_ARG_CLASSES);
			out = (Collection<T>) m.invoke( c, EMPTY_ARGS );
		    }
		catch ( Exception e )
		    { 
			/* IGNORE... just means there's no accessible clone() here */ 
			if ( Debug.DEBUG && Debug.TRACE == Debug.TRACE_MAX )
			    e.printStackTrace();
		    }

		if ( out == null )
		    {
			try
			    {
				Constructor<?> ctor = colClass.getConstructor( (c instanceof SortedSet) ? SORTED_SET_ARG_CLASSES : COLLECTION_ARG_CLASSES );
				out = (Collection<T>) ctor.newInstance( new Object[] { c } );
			    }
			catch ( Exception e )
			    {
				/* IGNORE... just means there's no accessible ctor here */
				if ( Debug.DEBUG && Debug.TRACE == Debug.TRACE_MAX )
				    e.printStackTrace();
			    }
		    }

		if ( out == null )
		    {
			try
			    {
				Constructor<?> ctor = colClass.getConstructor( new Class<?>[] { colClass } );
				out = (Collection<T>) ctor.newInstance( new Object[] { c } );
			    }
			catch ( Exception e )
			    { 
				/* IGNORE... just means there's no accessible ctor here */ 
				if ( Debug.DEBUG && Debug.TRACE == Debug.TRACE_MAX )
				    e.printStackTrace();
			    }
		    }

		if (out == null)
		    throw new NoSuchMethodException("No accessible clone() method or reasonable copy constructor could be called on Collection " + c);
		else
		    return out;
	    }
    }

    /**
     *  Attempts to find a public clone() method or a copy constructor, in that
     *  order, and calls what it finds. If neither is available, throws a NoSuchMethodException.
     *
     *  Unchecked is suppressed rather than avoided here. Every route to the copy --
     *  Object clone(), Method.invoke(...), Constructor.newInstance(...) -- is typed to
     *  return Object, so the element type cannot be checked at the point of the cast,
     *  only preserved. A copy of a Map has the key and value types of the original, which is what the
     *  signature says; the compiler simply cannot see it through reflection.
     */
    @SuppressWarnings("unchecked")
    public static <K,V> Map<K,V> attemptClone( Map<K,V> m ) throws NoSuchMethodException
    {
	if (m instanceof Properties) return (Map<K,V>) ((Properties) m).clone();
	else if (m instanceof Hashtable) return (Map<K,V>) ((Hashtable<K,V>) m).clone();
	else if (m instanceof HashMap) return (Map<K,V>) ((HashMap<K,V>) m).clone();
	else if (m instanceof TreeMap) return (Map<K,V>) ((TreeMap<K,V>) m).clone();
	else
	    {
		Map<K,V> out = null;
		Class<?> mapClass = m.getClass();
		try
		    {
			Method meth = mapClass.getMethod("clone", EMPTY_ARG_CLASSES);
			out = (Map<K,V>) meth.invoke( m, EMPTY_ARGS );
		    }
		catch ( Exception e )
		    { 
			/* IGNORE... just means there's no accessible clone() here */ 
			if ( Debug.DEBUG && Debug.TRACE == Debug.TRACE_MAX )
			    e.printStackTrace();
		    }

		if ( out == null )
		    {
			try
			    {
				Constructor<?> ctor = mapClass.getConstructor( (m instanceof SortedMap) ? SORTED_MAP_ARG_CLASSES : MAP_ARG_CLASSES );
				out = (Map<K,V>) ctor.newInstance( new Object[] { m } );
			    }
			catch ( Exception e )
			    { 
				/* IGNORE... just means there's no accessible ctor here */ 
				if ( Debug.DEBUG && Debug.TRACE == Debug.TRACE_MAX )
				    e.printStackTrace();
			    }
		    }

		if ( out == null )
		    {
			try
			    {
				Constructor<?> ctor = mapClass.getConstructor( new Class<?>[] { mapClass } );
				out = (Map<K,V>) ctor.newInstance( new Object[] { m } );
			    }
			catch ( Exception e )
			    { 
				/* IGNORE... just means there's no accessible ctor here */ 
				if ( Debug.DEBUG && Debug.TRACE == Debug.TRACE_MAX )
				    e.printStackTrace();
			    }
		    }

		if (out == null)
		    throw new NoSuchMethodException("No accessible clone() method or reasonable copy constructor could be called on Map " + m);
		else
		    return out;
	    }
    }

    /*
     * These functions are primarily motivated by a desire
     * to manipulate Collections from JSP 2.0 Expression
     * Language functions, which must be mapped to public 
     * static functions.
     */ 
    public static <T> void add(Collection<? super T> c, T o)
    { c.add( o ); }

    public static void remove(Collection<?> c, Object o)
    { c.remove( o ); }

    public static int size( Object o )
    {
	if (o instanceof Collection)
	    return ((Collection<?>) o).size();
	else if (o instanceof Map)
	    return ((Map<?,?>) o).size();
	else if (o instanceof Object[])
	    return ((Object[]) o).length;
	else if (o instanceof boolean[])
	    return ((boolean[]) o).length;
	else if (o instanceof byte[])
	    return ((byte[]) o).length;
	else if (o instanceof char[])
	    return ((char[]) o).length;
	else if (o instanceof short[])
	    return ((short[]) o).length;
	else if (o instanceof int[])
	    return ((int[]) o).length;
	else if (o instanceof long[])
	    return ((long[]) o).length;
	else if (o instanceof float[])
	    return ((float[]) o).length;
	else if (o instanceof double[])
	    return ((double[]) o).length;
	else
	    throw new IllegalArgumentException(o + " must be a Collection, Map, or array!");
    }

    private CollectionUtils()
    {}
}
