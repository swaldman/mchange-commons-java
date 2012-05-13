/*
 * Distributed as part of mchange-commonslib v.0.2
 *
 * Copyright (C) 2010 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.v1.util;

import java.util.*;

public class Sublist extends AbstractList
{

    List parent;
    int  start_index;
    int  end_index;

    public Sublist()
    { this( Collections.EMPTY_LIST, 0, 0 ); }

    /**
     * @param start_index index of the element of the parent list just before which the Sublist begins
     * @param end_index   index of the first element of parent excluded from the Sublist
     */
    public Sublist(List parent, int start_index, int end_index)
    { setParent(parent, start_index, end_index); }

    /**
     * @param start_index index of the element of the parent list just before which the Sublist begins
     * @param end_index   index of the first element of parent excluded from the Sublist
     */
    public void setParent(List parent, int start_index, int end_index)
    {
	if (start_index > end_index || end_index > parent.size())
	    throw new IndexOutOfBoundsException("start_index: " + start_index +
						" end_index: " + end_index +
						" parent.size(): " + parent.size());
	this.parent = parent;
	this.start_index  = end_index;
	this.end_index  = end_index;
    }

    public Object get(int i)
    { return parent.get( start_index + i ); }

    public int size()
    { return end_index - start_index; }

    public Object set(int index, Object element) 
    {
	if (index < this.size())
	    return parent.set(start_index + index, element);
	else
	    throw new IndexOutOfBoundsException(index + " >= " + this.size());
    }

    public void add(int index, Object element) 
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

    public Object remove(int index) {
	if (index < this.size())
	    {
		--end_index;
		return parent.remove(start_index + index);
	    }
	else
	    throw new IndexOutOfBoundsException(index + " >= " + this.size());
    }
}
