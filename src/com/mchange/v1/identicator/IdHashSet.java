package com.mchange.v1.identicator;

import java.util.*;
import com.mchange.v1.util.WrapperIterator;

public class IdHashSet<T> extends AbstractSet<T> implements Set<T>
{
    HashSet<IdHashKey> inner;
    Identicator id;

    /** See IdList.unwrap: IdHashKey is untyped plumbing, and only a T is ever wrapped. */
    @SuppressWarnings("unchecked")
    private T unwrap( IdHashKey ik )
    { return ik == null ? null : (T) ik.getKeyObj(); }

    private IdHashSet(HashSet<IdHashKey> inner, Identicator id)
    {
	this.inner = inner;
	this.id = id;
    }

    public IdHashSet(Identicator id)
    { this( new HashSet<IdHashKey>(), id ); }

    public IdHashSet(Collection<? extends T> c, Identicator id) 
    { this( new HashSet<IdHashKey>(2 * c.size()), id ); } 

    public IdHashSet(int initialCapacity, float loadFactor, Identicator id) 
    { this( new HashSet<IdHashKey>( initialCapacity, loadFactor ), id ); }

    public IdHashSet(int initialCapacity, Identicator id) 
    { this(new HashSet<IdHashKey>( initialCapacity, 0.75f ), id); }

    @Override
    public Iterator<T> iterator()
    {
	return new WrapperIterator<T>(inner.iterator(), true)
	    {
		@Override
		protected Object transformObject(Object o)
		{
		    IdHashKey idKey = (IdHashKey) o;
		    return idKey.getKeyObj();
		}
	    };
    }

    @Override
    public int size()
    { return inner.size(); }

    @Override
    public boolean contains(Object o)
    { return inner.contains( createKey( o ) ); }

    @Override
    public boolean add(T o)
    { return inner.add( createKey( o ) ); }

    @Override
    public boolean remove(Object o)
    { return inner.remove( createKey( o ) ); }

    @Override
    public void clear()
    { inner.clear(); }

    private IdHashKey createKey(Object o)
    { return new StrongIdHashKey( o, id ); }
}
