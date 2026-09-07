package com.mchange.v1.identicator;

import java.util.*;
import com.mchange.v1.util.*;

public class IdList<T> implements List<T>
{
    Identicator id;
    List<IdHashKey> inner;

    /**
     *  IdHashKey and its subclasses are package-private plumbing, and getKeyObj() is
     *  typed Object, so unwrapping one is the single place this class cannot be checked.
     *  Nothing but a T is ever wrapped, by add, set and addAll; confining the cast here
     *  keeps the suppression to one method.
     */
    @SuppressWarnings("unchecked")
    private T unwrap( IdHashKey ik )
    { return ik == null ? null : (T) ik.getKeyObj(); }

    public IdList(Identicator id, List<IdHashKey> inner)
    {
	this.id = id;
	this.inner = inner;
    }

    @Override
    public int size()
    { return inner.size(); }

    @Override
    public boolean isEmpty()
    { return inner.isEmpty(); }

    @Override
    public boolean contains(Object o)
    {
	IdHashKey wrappedO = new StrongIdHashKey(o, id);
	return inner.contains(wrappedO);
    }

    @Override
    public Iterator<T> iterator()
    {
	return new WrapperIterator<T>( inner.iterator(), true )
	    {
		@Override
		protected Object transformObject(Object o)
		{
		    if (o instanceof IdHashKey)
			{
			    IdHashKey ik = (IdHashKey) o;
			    return ik.getKeyObj();
			}
		    else //we expect that o is null then... 
			return o; 
		}
	    };
    }

    @Override
    public Object[] toArray()
    { return this.toArray( new Object[ this.size() ] ); }

    @Override
    @SuppressWarnings("unchecked") // IteratorUtils.toArray returns Object[]; the array handed back is the caller's own E[] when it fits
    public <E> E[] toArray(E[] space)
    { return (E[]) IteratorUtils.toArray( this.iterator(), this.size(), space ); }

    @Override
    public boolean add(T o)
    { return inner.add( new StrongIdHashKey( o, id ) ); }

    @Override
    public boolean remove(Object o)
    { return inner.remove( new StrongIdHashKey( o, id ) ); }

    @Override
    public boolean containsAll(Collection<?> c)
    {
	Iterator<?> ii = c.iterator();
	while (ii.hasNext())
	    {
		IdHashKey test = new StrongIdHashKey( ii.next(), id );
		if (! inner.contains( test ))
		    return false;
	    }
	return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c)
    {
	Iterator<?> ii = c.iterator();
	boolean changed = false;
	while (ii.hasNext())
	    {
		IdHashKey ik = new StrongIdHashKey( ii.next(), id );
		changed |= inner.add( ik );
	    }
	return changed;
    }

    @Override
    public boolean addAll(int i, Collection<? extends T> c)
    {
	Iterator<?> ii = c.iterator();
	while (ii.hasNext())
	    {
		IdHashKey ik = new StrongIdHashKey( ii.next(), id );
		inner.add( i, ik );
		++i;
	    }
	return (c.size() > 0);
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
	Iterator<?> ii = c.iterator();
	boolean changed = false;
	while (ii.hasNext())
	    {
		IdHashKey ik = new StrongIdHashKey( ii.next(), id );
		changed |= inner.remove( ik );
	    }
	return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
	Iterator<?> ii = inner.iterator();
	boolean changed = false;
	while (ii.hasNext())
	    {
		IdHashKey ours = (IdHashKey) ii.next();
		if (! c.contains( ours.getKeyObj() ))
		    {
			inner.remove( ours );
			changed = true;
		    }
	    }
	return changed;
    }

    @Override
    public void clear()
    { inner.clear(); }

    //TODO: should I make some version of this that pays attention to identicator?
    @Override
    public boolean equals(Object o)
    { 
	if (o instanceof List)
	    return ListUtils.equivalent( this, (List<?>) o );
	else
	    return false;
    }

    @Override
    public int hashCode()
    { return ListUtils.hashContents( this ); }

    @Override
    public T get(int i)
    { return unwrap( inner.get(i) ); }

    @Override
    public T set(int i, T o)
    { return unwrap( inner.set( i, new StrongIdHashKey( o, id ) ) ); }

    @Override
    public void add(int i, T o)
    {
	inner.add(  i, new StrongIdHashKey( o, id ) );
    }

    @Override
    public T remove(int i)
    { return unwrap( inner.remove(i) ); }

    @Override
    public int indexOf(Object o)
    { return inner.indexOf( new StrongIdHashKey( o, id ) ); }

    @Override
    public int lastIndexOf(Object o)
    { return inner.lastIndexOf( new StrongIdHashKey( o, id ) ); }

    //TODO: make a more efficient implementation...
    @Override
    public ListIterator<T> listIterator()
    { return new LinkedList<T>(this).listIterator(); }

    //TODO: make a more efficient implementation...
    @Override
    public ListIterator<T> listIterator(int i)
    { return new LinkedList<T>(this).listIterator(i); }

    @Override
    public List<T> subList(int a, int b)
    { return new IdList<T>(id, inner.subList(a, b)); }

}
