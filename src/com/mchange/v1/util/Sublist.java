package com.mchange.v1.util;

import java.util.*;

public class Sublist<T> extends AbstractList<T>
{

    List<T> parent;
    int  start_index;
    int  end_index;

    public Sublist()
    { this( Collections.<T>emptyList(), 0, 0 ); }

    /**
     * @param start_index index of the element of the parent list just before which the Sublist begins
     * @param end_index   index of the first element of parent excluded from the Sublist
     */
    public Sublist(List<T> parent, int start_index, int end_index)
    { setParent(parent, start_index, end_index); }

    /**
     * @param start_index index of the element of the parent list just before which the Sublist begins
     * @param end_index   index of the first element of parent excluded from the Sublist
     */
    public void setParent(List<T> parent, int start_index, int end_index)
    {
	if (start_index > end_index || end_index > parent.size())
	    throw new IndexOutOfBoundsException("start_index: " + start_index +
						" end_index: " + end_index +
						" parent.size(): " + parent.size());
	this.parent = parent;
	this.start_index  = start_index;
	this.end_index  = end_index;
    }

    @Override
    public T get(int i)
    { return parent.get( start_index + i ); }

    @Override
    public int size()
    { return end_index - start_index; }

    @Override
    public T set(int index, T element) 
    {
	if (index < this.size())
	    return parent.set(start_index + index, element);
	else
	    throw new IndexOutOfBoundsException(index + " >= " + this.size());
    }

    @Override
    public void add(int index, T element) 
    {
	if (index <= this.size())
	    {
		parent.add(start_index + index, element);
		++end_index;
	    }
	else
	    throw new IndexOutOfBoundsException(index + " > " + this.size());
	//System.err.println( parent );
    }

    @Override
    public T remove(int index) {
	if (index < this.size())
	    {
		--end_index;
		return parent.remove(start_index + index);
	    }
	else
	    throw new IndexOutOfBoundsException(index + " >= " + this.size());
    }
}
