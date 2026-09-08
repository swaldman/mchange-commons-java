package com.mchange.v1.cachedstore;

import java.util.*;
import java.lang.ref.*;
import com.mchange.v1.util.WrapperIterator;

class ManualCleanupSoftSet extends AbstractSet<Object> implements Vacuumable
{
    HashSet<SoftKey> inner = new HashSet<SoftKey>();
    ReferenceQueue<Object> queue = new ReferenceQueue<Object>();

    @Override
    public Iterator<Object> iterator()
    {
	return new WrapperIterator<Object>( inner.iterator(), true )
	    {
		@Override
		protected Object transformObject(Object o)
		{
		    SoftKey sk = (SoftKey) o;
		    Object out = sk.get();
		    return (out == null ? SKIP_TOKEN : out);
		}
	    };
    }

    /** 
     * this size may not be completely accurate, because
     * keys in the set may have cleared. In general with
     * this call, one must presume that elements may at
     * unpredictable times, simply "disappear".
     */
    @Override
    public int size()
    { return inner.size(); }

    @Override
    public boolean contains(Object o)
    { return inner.contains( new SoftKey( o, null ) ); }

    private ArrayList<Object> toArrayList()
    {
	ArrayList<Object> out = new ArrayList<Object>( this.size() );
	for (Iterator<Object> ii = this.iterator(); ii.hasNext();)
	    out.add( ii.next() );
	return out;
    }

    @Override
    public Object[] toArray() 
    { return this.toArrayList().toArray(); }

    @Override
    public <T> T[] toArray(T[] a) 
    { return this.toArrayList().toArray(a); }

    @Override
    public boolean add(Object o) 
    { return inner.add( new SoftKey(o, queue) ); }

    @Override
    public boolean remove(Object o) 
    { return inner.remove( new SoftKey( o, null ) ); }

    @Override
    public void clear()
    { inner.clear(); }

    @Override
    public void vacuum() throws CachedStoreException
    { 
	SoftKey key;
	while ((key = (SoftKey) queue.poll()) != null)
	    inner.remove( key );
    }
}
