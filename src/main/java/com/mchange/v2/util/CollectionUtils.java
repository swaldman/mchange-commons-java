/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
 */

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
    public final static SortedSet EMPTY_SORTED_SET = Collections.unmodifiableSortedSet( new TreeSet() );

    final static Class[]  EMPTY_ARG_CLASSES = { };
    final static Object[] EMPTY_ARGS        = { };

    final static Class[] COMPARATOR_ARG_CLASSES = { Comparator.class };
    final static Class[] COLLECTION_ARG_CLASSES = { Collection.class };
    final static Class[] SORTED_SET_ARG_CLASSES = { SortedSet.class };
    final static Class[] MAP_ARG_CLASSES        = { Map.class };
    final static Class[] SORTED_MAP_ARG_CLASSES = { SortedMap.class };

    final static Class STD_UNMODIFIABLE_COLLECTION_CL;
    final static Class STD_UNMODIFIABLE_SET_CL;
    final static Class STD_UNMODIFIABLE_LIST_CL;
    final static Class STD_UNMODIFIABLE_RA_LIST_CL;
    final static Class STD_UNMODIFIABLE_SORTED_SET_CL;
    final static Class STD_UNMODIFIABLE_MAP_CL;
    final static Class STD_UNMODIFIABLE_SORTED_MAP_CL;
    final static Class STD_SYNCHRONIZED_COLLECTION_CL;
    final static Class STD_SYNCHRONIZED_SET_CL;
    final static Class STD_SYNCHRONIZED_LIST_CL;
    final static Class STD_SYNCHRONIZED_RA_LIST_CL;
    final static Class STD_SYNCHRONIZED_SORTED_SET_CL;
    final static Class STD_SYNCHRONIZED_MAP_CL;
    final static Class STD_SYNCHRONIZED_SORTED_MAP_CL;

    final static Set UNMODIFIABLE_WRAPPERS;
    final static Set SYNCHRONIZED_WRAPPERS;
    final static Set ALL_COLLECTIONS_WRAPPERS;

    static
    {
	HashSet hs = new HashSet();
	TreeSet ts = new TreeSet();
	LinkedList ll = new LinkedList();
	ArrayList al = new ArrayList();
	HashMap hm = new HashMap();
	TreeMap tm = new TreeMap();

	HashSet tmp0 = new HashSet();
	HashSet tmp1 = new HashSet();

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

	HashSet tmp2 = new HashSet( tmp0 );
	tmp2.addAll( tmp1 );
	ALL_COLLECTIONS_WRAPPERS = Collections.unmodifiableSet( tmp2 );
    }

    public static boolean isCollectionsWrapper( Class cl )
    { return ALL_COLLECTIONS_WRAPPERS.contains( cl ); }

    public static boolean isCollectionsWrapper( Collection c )
    { return isCollectionsWrapper( c.getClass() ); }

    public static boolean isCollectionsWrapper( Map m )
    { return isCollectionsWrapper( m.getClass() ); }

    public static boolean isSynchronizedWrapper( Class cl )
    { return SYNCHRONIZED_WRAPPERS.contains( cl ); }

    public static boolean isSynchronizedWrapper( Collection c )
    { return isSynchronizedWrapper( c.getClass() ); }

    public static boolean isSynchronizedWrapper( Map m )
    { return isSynchronizedWrapper( m.getClass() ); }

    public static boolean isUnmodifiableWrapper( Class cl )
    { return UNMODIFIABLE_WRAPPERS.contains( cl ); }

    public static boolean isUnmodifiableWrapper( Collection c )
    { return isUnmodifiableWrapper( c.getClass() ); }

    public static boolean isUnmodifiableWrapper( Map m )
    { return isUnmodifiableWrapper( m.getClass() ); }

    /*
     * should we worry about the case where an Object (bizarrely)
     * implements both Set and List? don't think so...
     */
    public static Collection narrowUnmodifiableCollection( Collection c )
    {
	if (c instanceof SortedSet)
	    return Collections.unmodifiableSortedSet( (SortedSet) c );
	else if (c instanceof Set)
	    return Collections.unmodifiableSet( (Set) c );
	else if (c instanceof List)
	    return Collections.unmodifiableList( (List) c );
	else
	    return Collections.unmodifiableCollection( c );
    }

    /*
     * should we worry about the case where an Object (bizarrely)
     * implements both Set and List? don't think so...
     */
    public static Collection narrowSynchronizedCollection( Collection c )
    {
	if (c instanceof SortedSet)
	    return Collections.synchronizedSortedSet( (SortedSet) c );
	else if (c instanceof Set)
	    return Collections.synchronizedSet( (Set) c );
	else if (c instanceof List)
	    return Collections.synchronizedList( (List) c );
	else
	    return Collections.synchronizedCollection( c );
    }

    public static Map narrowUnmodifiableMap( Map m )
    {
	if (m instanceof SortedMap)
	    return Collections.unmodifiableSortedMap( (SortedMap) m );
	else
	    return Collections.unmodifiableMap( m );
    }

    public static Map narrowSynchronizedMap( Map m )
    {
	if (m instanceof SortedMap)
	    return Collections.synchronizedSortedMap( (SortedMap) m );
	else
	    return Collections.synchronizedMap( m );
    }

    /**
     *  Attempts to find a public clone() method or a copy constructor, in that
     *  order, and calls what it finds. If neither is available, throws a NoSuchMethodException.
     */
    public static Collection attemptClone( Collection c ) throws NoSuchMethodException
    {
	if (c instanceof Vector) return (Collection) ((Vector) c).clone();
	else if (c instanceof ArrayList) return (Collection) ((ArrayList) c).clone();
	else if (c instanceof LinkedList) return (Collection) ((LinkedList) c).clone();
	else if (c instanceof HashSet) return (Collection) ((HashSet) c).clone();
	else if (c instanceof TreeSet) return (Collection) ((TreeSet) c).clone();
	else
	    {
		Collection out = null;
		Class colClass = c.getClass();
		try
		    {
			Method m = colClass.getMethod("clone", EMPTY_ARG_CLASSES);
			out = (Collection) m.invoke( c, EMPTY_ARGS );
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
				Constructor ctor = colClass.getConstructor( (c instanceof SortedSet) ? SORTED_SET_ARG_CLASSES : COLLECTION_ARG_CLASSES );
				out = (Collection) ctor.newInstance( new Object[] { c } );
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
				Constructor ctor = colClass.getConstructor( new Class[] { colClass } );
				out = (Collection) ctor.newInstance( new Object[] { c } );
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
     */
    public static Map attemptClone( Map m ) throws NoSuchMethodException
    {
	if (m instanceof Properties) return (Map) ((Properties) m).clone();
	else if (m instanceof Hashtable) return (Map) ((Hashtable) m).clone();
	else if (m instanceof HashMap) return (Map) ((HashMap) m).clone();
	else if (m instanceof TreeMap) return (Map) ((TreeMap) m).clone();
	else
	    {
		Map out = null;
		Class mapClass = m.getClass();
		try
		    {
			Method meth = mapClass.getMethod("clone", EMPTY_ARG_CLASSES);
			out = (Map) meth.invoke( m, EMPTY_ARGS );
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
				Constructor ctor = mapClass.getConstructor( (m instanceof SortedMap) ? SORTED_MAP_ARG_CLASSES : MAP_ARG_CLASSES );
				out = (Map) ctor.newInstance( new Object[] { m } );
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
				Constructor ctor = mapClass.getConstructor( new Class[] { mapClass } );
				out = (Map) ctor.newInstance( new Object[] { m } );
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
    public static void add(Collection c, Object o)
    { c.add( o ); }

    public static void remove(Collection c, Object o)
    { c.remove( o ); }

    public static int size( Object o )
    {
	if (o instanceof Collection)
	    return ((Collection) o).size();
	else if (o instanceof Map)
	    return ((Map) o).size();
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
