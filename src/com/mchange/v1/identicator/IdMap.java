package com.mchange.v1.identicator;

import java.util.*;
import com.mchange.v1.util.*;

/*
 * Implementation notes: many AbstractMap methods are written in
 * terms of entrySet(). It is most important to get that right.
 */
abstract class IdMap extends AbstractMap implements Map
{
    Map         inner;
    Identicator id;

    protected IdMap(Map inner, Identicator id)
    {
	this.inner = inner;
	this.id = id;
    }
    
    @Override
    public Object put(Object key, Object value) 
    { return inner.put( createIdKey( key ), value ); }

    @Override
    public boolean containsKey(Object key)
    { return inner.containsKey( createIdKey( key ) ); }

    @Override
    public Object get(Object key)
    { return inner.get( createIdKey( key ) ); }

    @Override
    public Object remove(Object key)
    { return inner.remove( createIdKey( key ) ); }

    protected Object removeIdHashKey( IdHashKey idhk )
    { return inner.remove( idhk ); }

    @Override
    public Set entrySet()
    { return new UserEntrySet(); }

    protected final Set internalEntrySet()
    { return inner.entrySet(); }

    protected abstract IdHashKey createIdKey(Object o);

    protected final Entry createIdEntry(Object key, Object val)
    { return new SimpleMapEntry( createIdKey( key ), val); }
    
    protected final Entry createIdEntry(Entry entry)
    { return createIdEntry( entry.getKey(), entry.getValue() ); }

    private final class UserEntrySet extends AbstractSet
    {
	Set innerEntries = inner.entrySet();
	
	@Override
	public Iterator iterator()
	{
	    return new WrapperIterator(innerEntries.iterator(), true)
		{
		    @Override
		    protected Object transformObject(Object o)
		    { return new UserEntry( (Entry) o ); }
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
		    Entry entry = (Entry) o;
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
		    Entry entry = (Entry) o;
		    return innerEntries.remove( createIdEntry( entry ) ); 
		}
	    else
		return false;
	}

	@Override
	public void clear()
	{ inner.clear(); }
    }

    protected static class UserEntry extends AbstractMapEntry
    {
	private Entry innerEntry;

	UserEntry(Entry innerEntry)
	{ this.innerEntry = innerEntry; }

	@Override
	public final Object getKey()
	{ return ((IdHashKey) innerEntry.getKey()).getKeyObj(); }

	@Override
	public final Object getValue()
	{ return innerEntry.getValue(); }

	@Override
	public final Object setValue(Object value)
	{ return innerEntry.setValue( value ); }
    }
}
