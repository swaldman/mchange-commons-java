package com.mchange.v1.identicator;

import java.lang.ref.*;
import java.util.*;
import com.mchange.v1.util.WrapperIterator;

/**
 * IdWeakHashMap is NOT null-accepting!
 */
public final class IdWeakHashMap<K,V> extends IdMap<K,V> implements Map<K,V>
{
    ReferenceQueue<Object> rq;

    public IdWeakHashMap(Identicator id)
    { 
	super ( new HashMap<IdHashKey,V>(), id ); 
	this.rq = new ReferenceQueue<Object>();
    }

    //all methods from Map interface
    @Override
    public int size()
    {
	// doing cleanCleared() afterwards, as with other methods
	// would be just as "correct", as weak collections
	// make no guarantees about when things disappear,
	// but for size(), it feels a little more accurate
	// this way.
	cleanCleared();
	return super.size();
    }

    @Override
    public boolean isEmpty()
    {
	try
	    { return super.isEmpty(); }
	finally
	    { cleanCleared(); }
    }

    @Override
    public boolean containsKey(Object o)
    {
	try
	    { return super.containsKey( o ); }
	finally
	    { cleanCleared(); }
    }

    @Override
    public boolean containsValue(Object o)
    {
	try
	    { return super.containsValue( o ); }
	finally
	    { cleanCleared(); }
    }

    @Override
    public V get(Object o)
    {
	try
	    { return super.get( o ); }
	finally
	    { cleanCleared(); }
    }

    @Override
    public V put(K k, V v)
    {
	try
	    { return super.put( k , v ); }
	finally
	    { cleanCleared(); }
    }

    @Override
    public V remove(Object o)
    {
	try
	    { return super.remove( o ); }
	finally
	    { cleanCleared(); }
    }

    @Override
    public void putAll(Map<? extends K,? extends V> m)
    {
	try
	    { super.putAll( m ); }
	finally
	    { cleanCleared(); }
    }

    @Override
    public void clear()
    {
	try
	    { super.clear(); }
	finally
	    { cleanCleared(); }
    }

    @Override
    public Set<K> keySet()
    {
	try
	    { return super.keySet(); }
	finally
	    { cleanCleared(); }
    }

    @Override
    public Collection<V> values()
    {
	try
	    { return super.values(); }
	finally
	    { cleanCleared(); }
    }

    /*
     * entrySet() is the basis of the implementation of the other
     * Collection returning methods. Get this right and the rest 
     * follow.
     */
    @Override
    public Set<Entry<K,V>> entrySet()
    {
	try
	    { return new WeakUserEntrySet(); }
	finally
	    { cleanCleared(); }
    }

    @Override
    public boolean equals(Object o)
    {
	try
	    { return super.equals( o ); }
	finally
	    { cleanCleared(); }
    }

    @Override
    public int hashCode()
    {
	try
	    { return super.hashCode(); }
	finally
	    { cleanCleared(); }
    }

    //internal methods
    @Override
    protected IdHashKey createIdKey(Object o)
    { return new WeakIdHashKey( o, id, rq ); }

    private void cleanCleared()
    {
	WeakIdHashKey.Ref ref;
	while ((ref = (WeakIdHashKey.Ref) rq.poll()) != null)
	    this.removeIdHashKey( ref.getKey() );
    }

    private final class WeakUserEntrySet extends AbstractSet<Entry<K,V>>
    {
	Set<Entry<IdHashKey,V>> innerEntries = internalEntrySet();
	
	@Override
	public Iterator<Entry<K,V>> iterator()
	{
	    try
		{
		    return new WrapperIterator<Entry<K,V>>(innerEntries.iterator(), true)
			{
			    @Override
			    @SuppressWarnings("unchecked")
			    protected Object transformObject(Object o)
			    {
				Entry<IdHashKey,V> innerEntry = (Entry<IdHashKey,V>) o;
				final Object userKey = innerEntry.getKey().getKeyObj();
				if (userKey == null)
				    return WrapperIterator.SKIP_TOKEN;
				else
				    return new UserEntry<K,V>( innerEntry ) 
					{ Object preventRefClear = userKey; };
			    }
			};
		}
	finally
	    { cleanCleared(); }
	}
	
	@Override
	public int size()
	{ 
	    // doing cleanCleared() afterwards, as with other methods
	    // would be just as "correct", as weak collections
	    // make no guarantees about when things disappear,
	    // but for size(), it feels a little more accurate
	    // this way.
	    cleanCleared();
	    return innerEntries.size(); 
	}
	
	@Override
	public boolean contains(Object o)
	{ 
	    try
		{
		    if (o instanceof Entry)
			{
			    Entry<?,? extends V> entry = castEntry( o );
			    return innerEntries.contains( createIdEntry( entry ) ); 
			}
		    else
			return false;
		}
	    finally
		{ cleanCleared(); }
	}
	
	@Override
	public boolean remove(Object o)
	{
	    try
		{
		    if (o instanceof Entry)
			{
			    Entry<?,? extends V> entry = castEntry( o );
			    return innerEntries.remove( createIdEntry( entry ) ); 
			}
		    else
			return false;
		}
	    finally
		{ cleanCleared(); }
	}

	@Override
	public void clear()
	{
	    try
		{ inner.clear(); }
	    finally
		{ cleanCleared(); }
	}
    }
}
