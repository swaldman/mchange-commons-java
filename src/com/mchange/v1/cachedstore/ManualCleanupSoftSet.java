package com.mchange.v1.cachedstore;

import java.util.*;
import java.lang.ref.*;
import com.mchange.v1.util.WrapperIterator;

class ManualCleanupSoftSet extends AbstractSet implements Vacuumable
{
    HashSet inner = new HashSet();
    ReferenceQueue queue = new ReferenceQueue();

    public Iterator iterator()
    {
	return new WrapperIterator( inner.iterator(), true )
	    {
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
    public int size()
    { return inner.size(); }

    public boolean contains(Object o)
    { return inner.contains( new SoftKey( o, null ) ); }

    private ArrayList toArrayList()
    {
	ArrayList out = new ArrayList( this.size() );
	for (Iterator ii = this.iterator(); ii.hasNext();)
	    out.add( ii.next() );
	return out;
    }

    public Object[] toArray() 
    { return this.toArrayList().toArray(); }

    public Object[] toArray(Object[] a) 
    { return this.toArrayList().toArray(a); }

    public boolean add(Object o) 
    { return inner.add( new SoftKey(o, queue) ); }

    public boolean remove(Object o) 
    { return inner.remove( new SoftKey( o, null ) ); }

    public void clear()
    { inner.clear(); }

    public void vacuum() throws CachedStoreException
    { 
	SoftKey key;
	while ((key = (SoftKey) queue.poll()) != null)
	    inner.remove( key );
    }
}
