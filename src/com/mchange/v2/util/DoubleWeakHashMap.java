package com.mchange.v2.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


import com.mchange.v1.util.AbstractMapEntry;
import com.mchange.v1.util.WrapperIterator;

//TODO -- ensure that cleanCleared() gets called only once, even in methods implemented
//        as loops. (cleanCleared() is idempotent, so the repeated calls are okay,
//        but they're wasteful.

/**
 * <p>This class is <u>not</u> Thread safe.
 * Use in single threaded contexts, or contexts where
 * single threaded-access can be guaranteed, or
 * wrap with Collections.synchronizedMap().</p>
 * 
 * <p>This class does not accept null keys or values.</p>
 */
public class DoubleWeakHashMap<K,V> implements Map<K,V>
{
    HashMap<Object,WVal> inner;                                  // keys are WKey; lookups pass a CheckKeyHolder
    ReferenceQueue<Object> keyQ = new ReferenceQueue<Object>();
    ReferenceQueue<Object> valQ = new ReferenceQueue<Object>();
    
    CheckKeyHolder holder = new CheckKeyHolder();
    
    Set<K> userKeySet = null;
    Collection<V> valuesCollection = null;
    
    public DoubleWeakHashMap()
    { this.inner = new HashMap<Object,WVal>(); }
    
    public DoubleWeakHashMap(int initialCapacity)
    { this.inner = new HashMap<Object,WVal>( initialCapacity ); }
    
    public DoubleWeakHashMap(int initialCapacity, float loadFactor)
    { this.inner = new HashMap<Object,WVal>( initialCapacity, loadFactor ); }
    
    public DoubleWeakHashMap(Map<? extends K,? extends V> m)
    {
        this();
        putAll(m);
    }

    /**
     *  WKey and WVal are static, so they cannot be WeakReference&lt;K&gt; and
     *  WeakReference&lt;V&gt; -- a static nested class has no access to its enclosing
     *  class's type parameters. Their referents are therefore typed Object, and these two
     *  methods are the only places that narrow one back. Nothing but a K is ever stored
     *  under a WKey, and nothing but a V under a WVal, so the casts hold; the compiler
     *  cannot see that through WeakReference&lt;Object&gt;.
     */
    @SuppressWarnings("unchecked")
    private K keyOf( WKey wk )
    { return wk == null ? null : (K) wk.get(); }

    @SuppressWarnings("unchecked")
    private V valOf( WVal wv )
    { return wv == null ? null : (V) wv.get(); }

    public void cleanCleared()
    {
        WKey wk;
        while ((wk = (WKey) keyQ.poll()) != null)
            inner.remove(wk);
        
        WVal wv;
        while ((wv = (WVal) valQ.poll()) != null)
            inner.remove(wv.getWKey());
    }
    
    @Override
    public void clear()
    {
        cleanCleared();
        inner.clear();
    }

    @Override
    public boolean containsKey(Object key)
    {
        cleanCleared();
        try 
            { return inner.containsKey( holder.set(key) ); }
        finally
            { holder.clear(); }
    }

    @Override
    public boolean containsValue(Object val)
    {
        for (WVal wval : inner.values())
        {
            if (val.equals(wval.get()))
                return true;
        }
        return false;
    }

    @Override
    public Set<Map.Entry<K,V>> entrySet()
    {
        cleanCleared();
        return new UserEntrySet();
    }

    @Override
    public V get(Object key)
    {
        try
        {
            cleanCleared();
            return valOf( inner.get(holder.set(key)) );
        }
        finally
        { holder.clear(); }
    }

    @Override
    public boolean isEmpty()
    {
        cleanCleared();
        return inner.isEmpty();
    }

    @Override
    public Set<K> keySet()
    {
        cleanCleared();
        if (userKeySet == null)
            userKeySet = new UserKeySet();
        return userKeySet;
    }

    @Override
    public V put(K key, V val)
    {
        cleanCleared();
        return valOf( doPut(key, val) );
    }
    
    private WVal doPut(Object key, Object val)
    {
        WKey wk = new WKey(key, keyQ);
        WVal wv = new WVal(wk, val, valQ);
        return inner.put(wk, wv);
    }

    @Override
    public void putAll(Map<? extends K,? extends V> m)
    {
       cleanCleared();
       for (Map.Entry<? extends K,? extends V> entry : m.entrySet())
           this.doPut( entry.getKey(), entry.getValue() );
    }

    @Override
    public V remove(Object key)
    {
        try
        {
            cleanCleared();
            return valOf( inner.remove( holder.set(key) ) );
        }
        finally
        { holder.clear(); }
    }

    @Override
    public int size()
    {
        cleanCleared();
        return inner.size();
    }

    @Override
    public Collection<V> values()
    {
        if (valuesCollection == null)
            this.valuesCollection = new ValuesCollection();
        return valuesCollection;
    }
    
    final static class CheckKeyHolder
    {
        Object checkKey;
        
        public Object get()
        {return checkKey; }
        
        public CheckKeyHolder set(Object ck)
        { 
            assert this.checkKey == null : "Illegal concurrenct use of DoubleWeakHashMap!";
            
            this.checkKey = ck; 
            return this;
        }
        
        public void clear()
        { checkKey = null; }
        
        @Override
        public int hashCode()
        { return checkKey.hashCode(); }
        
        @Override
        public boolean equals(Object o)
        {
            assert this.get() != null : "CheckedKeyHolder should never do an equality check while its value is null." ;
            
            if (this == o)
                return true;
            else if (o instanceof CheckKeyHolder)
                return this.get().equals( ((CheckKeyHolder) o).get() );
            else if (o instanceof WKey)
                return this.get().equals( ((WKey) o).get() );
            else
                return false;
        }
    }
    
    final static class WKey extends WeakReference<Object>
    {
        int cachedHash;
        
        WKey(Object keyObj, ReferenceQueue<Object> rq)
        {
            super(keyObj, rq);
            this.cachedHash = keyObj.hashCode();
        }
        
        @Override
        public int hashCode()
        { return cachedHash; }
        
        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            else if (o instanceof WKey)
            {
                WKey oo = (WKey) o;
                Object myVal = this.get();
                Object ooVal = oo.get();
                if (myVal == null || ooVal == null)
                    return false;
                else
                    return myVal.equals(ooVal);
            }
            else if (o instanceof CheckKeyHolder)
            {
                CheckKeyHolder oo = (CheckKeyHolder) o;
                Object myVal = this.get();
                Object ooVal = oo.get();
                if (myVal == null || ooVal == null)
                    return false;
                else
                    return myVal.equals(ooVal);
            }
            else
                return false;
        }
    }
    
    final static class WVal extends WeakReference<Object>
    {
        WKey key;
        WVal(WKey key, Object valObj, ReferenceQueue<Object> rq)
        {
            super(valObj, rq);
            this.key = key;
        }
        
        public WKey getWKey()
        { return key; }
    }
    
    
    private final class UserEntrySet extends AbstractSet<Map.Entry<K,V>>
    {
        private Set<Map.Entry<Object,WVal>> innerEntrySet()
        {
            cleanCleared();
            return inner.entrySet();
        }

        @Override
        public Iterator<Map.Entry<K,V>> iterator()
        {
            return new WrapperIterator<Map.Entry<K,V>>(innerEntrySet().iterator(), true)
            {
                @Override
                @SuppressWarnings("unchecked") // transformObject is handed the elements of innerEntrySet(), which are exactly these
                protected Object transformObject(Object o)
                {
                    Entry<Object,WVal> innerEntry = (Entry<Object,WVal>) o;
                    K key = keyOf( (WKey) innerEntry.getKey() );
                    V val = valOf( innerEntry.getValue() );
                    
                    if (key == null || val == null)
                        return WrapperIterator.SKIP_TOKEN;
                    else
                        return new UserEntry( innerEntry, key, val ); 
                }
            };
        }
        
        @Override
        public int size()
        { return innerEntrySet().size(); }
    }
    
    class UserEntry extends AbstractMapEntry<K,V>
    {
        Entry<Object,WVal> innerEntry;
        K key;
        V val;

        UserEntry(Entry<Object,WVal> innerEntry, K key, V val)
        { 
            this.innerEntry = innerEntry; 
            this.key = key;
            this.val = val;
        }

        @Override
        public final K getKey()
        { return key; }

        @Override
        public final V getValue()
        { return val; }

        @Override
        public final V setValue(V value)
        {
            // Returns the value previously mapped, per Map.Entry.setValue. Before this class
            // was parameterized it returned innerEntry.setValue(...) directly, which is the
            // previous WVal wrapper rather than the previous value -- typing it made that
            // impossible to keep.
            return valOf( innerEntry.setValue( new WVal( (WKey) innerEntry.getKey(), value, valQ) ) );
        }
    }    
    
    class UserKeySet implements Set<K>
    {
        @Override
        public boolean add(K o)
        {
            cleanCleared();
            throw new UnsupportedOperationException("You cannot add to a Map's key set.");
        }

        @Override
        public boolean addAll(Collection<? extends K> c)
        {
            cleanCleared();
            throw new UnsupportedOperationException("You cannot add to a Map's key set.");
        }

        @Override
        public void clear()
        { DoubleWeakHashMap.this.clear(); }

        @Override
        public boolean contains(Object o)
        {
            return DoubleWeakHashMap.this.containsKey(o);
        }

        @Override
        public boolean containsAll(Collection<?> c)
        {
            for (Iterator<?> ii = c.iterator(); ii.hasNext();)
                if (! this.contains(ii.next()))
                    return false;
            return true;
        }

        @Override
        public boolean isEmpty()
        { return DoubleWeakHashMap.this.isEmpty(); }

        @Override
        public Iterator<K> iterator()
        {
            cleanCleared();
            return new WrapperIterator<K>(DoubleWeakHashMap.this.inner.keySet().iterator(), true)
            {
                @Override
                protected Object transformObject(Object o)
                {
                    K key = keyOf( (WKey) o );
                    
                    if (key == null)
                        return WrapperIterator.SKIP_TOKEN;
                    else
                        return key; 
                }
            };
        }

        @Override
        public boolean remove(Object o)
        {
            return (DoubleWeakHashMap.this.remove(o) != null);
        }

        @Override
        public boolean removeAll(Collection<?> c)
        {
            boolean out = false;
            for (Iterator<?> ii = c.iterator(); ii.hasNext();)
                out |= this.remove(ii.next());
            return out;
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
            //we implicitly cleanCleared() by calling iterator()
            boolean out = false;
            for (Iterator<?> ii = this.iterator(); ii.hasNext();)
            {
                if (!c.contains(ii.next()))
                {
                    ii.remove();
                    out = true;
                }
            }
            return out;
        }

        @Override
        public int size()
        { return DoubleWeakHashMap.this.size(); }

        @Override
        public Object[] toArray()
        { 
            cleanCleared();
            return new HashSet<K>( this ).toArray(); 
        }

        @Override
        public <T> T[] toArray(T[] array)
        {
            cleanCleared();
            return new HashSet<K>( this ).toArray(array); 
        }
    }

    class ValuesCollection implements Collection<V>
    {

        @Override
        public boolean add(V o)
        {
            cleanCleared();
            throw new UnsupportedOperationException("DoubleWeakHashMap does not support adding to its values Collection.");
        }

        @Override
        public boolean addAll(Collection<? extends V> c)
        {
            cleanCleared();
            throw new UnsupportedOperationException("DoubleWeakHashMap does not support adding to its values Collection.");
        }

        @Override
        public void clear()
        { DoubleWeakHashMap.this.clear(); }

        @Override
        public boolean contains(Object o)
        { return DoubleWeakHashMap.this.containsValue(o); }

        @Override
        public boolean containsAll(Collection<?> c)
        {
            for (Iterator<?> ii = c.iterator(); ii.hasNext();)
                if (!this.contains(ii.next()))
                    return false;
            return true;
        }

        @Override
        public boolean isEmpty()
        { return DoubleWeakHashMap.this.isEmpty(); }

        @Override
        public Iterator<V> iterator()
        {
            return new WrapperIterator<V>(inner.values().iterator(), true)
            {
                @Override
                protected Object transformObject(Object o)
                {
                    Object val = ((WVal) o).get();
                    
                    if (val == null)
                        return WrapperIterator.SKIP_TOKEN;
                    else
                        return val; 
                }
            };            
        }

        @Override
        public boolean remove(Object o)
        {
            cleanCleared();
            return removeValue(o);
        }

        @Override
        public boolean removeAll(Collection<?> c)
        {
            cleanCleared();
            boolean out = false;
            for (Iterator<?> ii = c.iterator(); ii.hasNext();)
                out |= removeValue(ii.next());
            return out;
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
            cleanCleared();
            return retainValues(c);
        }

        @Override
        public int size()
        { return DoubleWeakHashMap.this.size(); }

        @Override
        public Object[] toArray()
        { 
            cleanCleared();
            return new ArrayList<V>(this).toArray();
        }

        @Override
        public <T> T[] toArray(T[] array)
        {
            cleanCleared();
            return new ArrayList<V>(this).toArray(array);
        }

        private boolean removeValue(Object val)
        {
            boolean out = false;
            for (Iterator<?> ii = inner.values().iterator(); ii.hasNext();)
            {
                WVal wv = (WVal) ii.next();
                if (val.equals(wv.get()))
                {
                    ii.remove();
                    out = true;
                }
            }
            return out;
        }
        
        private boolean retainValues(Collection<?> c)
        {
            boolean out = false;
            for (Iterator<?> ii = inner.values().iterator(); ii.hasNext();)
            {
                WVal wv = (WVal) ii.next();
                if (! c.contains(wv.get()) )
                {
                    ii.remove();
                    out = true;
                }
            }
            return out;
        }
    }
    
    /*
    public static void main(String[] argv)
    {
        DoubleWeakHashMap m = new DoubleWeakHashMap();
        //Set keySet = new HashSet();
        //Set valSet = new HashSet();

        while (true)
        {
            System.err.println( m.inner.size() );

            //if (Math.random() < 0.1f)
            //    valSet.clear();
            
            Object key = new Object();
            Object val = new long[100000];
            //keySet.add(key);
            //valSet.add(val);
            m.put( key, val );
        }
    }
    */
}
