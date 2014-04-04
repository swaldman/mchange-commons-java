/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
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
