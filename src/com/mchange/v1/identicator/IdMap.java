package com.mchange.v1.identicator;

import java.util.*;
import com.mchange.v1.util.*;

/*
 * Implementation notes: many AbstractMap methods are written in
 * terms of entrySet(). It is most important to get that right.
 */
abstract class IdMap<K,V> extends AbstractMap<K,V> implements Map<K,V>
{
    Map<IdHashKey,V> inner;
    Identicator id;

    protected IdMap(Map<IdHashKey,V> inner, Identicator id)
    {
	this.inner = inner;
	this.id = id;
    }
    
    @Override
    public V put(K key, V value) 
    { return inner.put( createIdKey( key ), value ); }

    @Override
    public boolean containsKey(Object key)
    { return inner.containsKey( createIdKey( key ) ); }

    @Override
    public V get(Object key)
    { return inner.get( createIdKey( key ) ); }

    @Override
    public V remove(Object key)
    { return inner.remove( createIdKey( key ) ); }

    protected V removeIdHashKey( IdHashKey idhk )
    { return inner.remove( idhk ); }

    @Override
    public Set<Entry<K,V>> entrySet()
    { return new UserEntrySet(); }

    protected final Set<Entry<IdHashKey,V>> internalEntrySet()
    { return inner.entrySet(); }

    protected abstract IdHashKey createIdKey(Object o);

    /** An Object that passed `instanceof Entry`; its value type is only ever compared, never stored. */
    @SuppressWarnings("unchecked")
    protected static <V> Map.Entry<?,? extends V> castEntry( Object o )
    { return (Map.Entry<?,? extends V>) o; }

    protected final Entry<IdHashKey,V> createIdEntry(Object key, V val)
    { return new SimpleMapEntry<IdHashKey,V>( createIdKey( key ), val); }
    
    protected final Entry<IdHashKey,V> createIdEntry(Entry<?,? extends V> entry)
    { return createIdEntry( entry.getKey(), entry.getValue() ); }

    private final class UserEntrySet extends AbstractSet<Entry<K,V>>
    {
	Set<Entry<IdHashKey,V>> innerEntries = inner.entrySet();

	@Override
	public Iterator<Entry<K,V>> iterator()
	{
	    return new WrapperIterator<Entry<K,V>>(innerEntries.iterator(), true)
		{
		    @Override
		    @SuppressWarnings("unchecked")
		    protected Object transformObject(Object o)
		    { return new UserEntry<K,V>( (Entry<IdHashKey,V>) o ); }
		};
	}
	
	@Override
	public int size()
	{ return innerEntries.size(); }
	
	@Override
	public boolean contains(Object o)
	{ 
	    if (o instanceof Entry)
		{
		    Entry<?,? extends V> entry = castEntry( o );
		    return innerEntries.contains( createIdEntry( entry ) ); 
		}
	    else
		return false;
	}
	
	@Override
	public boolean remove(Object o)
	{
	    if (o instanceof Entry)
		{
		    Entry<?,? extends V> entry = castEntry( o );
		    return innerEntries.remove( createIdEntry( entry ) ); 
		}
	    else
		return false;
	}

	@Override
	public void clear()
	{ inner.clear(); }
    }

    /**
     *  An entry of this map as the user sees it, with the IdHashKey wrapper stripped off
     *  its key. IdHashKey is untyped plumbing, so getKey() cannot be checked; only a K is
     *  ever wrapped, by createIdKey.
     */
    protected static class UserEntry<K,V> extends AbstractMapEntry<K,V>
    {
	private Entry<IdHashKey,V> innerEntry;

	UserEntry(Entry<IdHashKey,V> innerEntry)
	{ this.innerEntry = innerEntry; }

	@Override
	@SuppressWarnings("unchecked")
	public final K getKey()
	{ return (K) innerEntry.getKey().getKeyObj(); }

	@Override
	public final V getValue()
	{ return innerEntry.getValue(); }

	@Override
	public final V setValue(V value)
	{ return innerEntry.setValue( value ); }
    }
}
