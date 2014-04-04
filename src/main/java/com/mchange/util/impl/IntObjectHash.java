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

package com.mchange.util.impl;

import java.util.NoSuchElementException;
import com.mchange.util.*;

public class IntObjectHash implements IntObjectMap
{
  IOHRecord[] records;
  int         init_capacity;
  float       load_factor;
  int         threshold;
  int         size;


  public IntObjectHash(int init_capacity, float load_factor)
    {
      this.init_capacity = init_capacity;
      this.load_factor   = load_factor;
      this.clear();
    }

  public IntObjectHash()
    {this(101, 0.75f);} //defaults from java.util.Hashtable

  public synchronized Object get(int num)
    {
      int index  = getIndex(num);
      Object out = null;
      if (records[index] != null)
	out = records[index].get(num);
      return out;
    }
  
  public synchronized void put(int num, Object obj)
    {
      if (obj == null)
	throw new NullPointerException("Null values not permitted.");
      int index = getIndex(num);
      if (records[index] == null)
	records[index] = new IOHRecord(index);
      boolean replaced = records[index].add(num, obj, true);
      if (!replaced) ++size;
      if (size > threshold) rehash();
    }

  public synchronized boolean putNoReplace(int num, Object obj)
    {
      if (obj == null)
	throw new NullPointerException("Null values not permitted.");
      int index = getIndex(num);
      if (records[index] == null)
	records[index] = new IOHRecord(index);
      boolean needed_replace = records[index].add(num, obj, false);
      if (needed_replace)
	{return false;}
      else
	{
	  ++size;
	  if (size > threshold) rehash();
	  return true;
	}
    }

  public int getSize()
    {return size;}

  public synchronized boolean containsInt(int num)
    {
      int index = getIndex(num);
      return (records[index] != null && records[index].findInt(num) != null);
    }

  private int getIndex(int num)
    {return Math.abs(num % records.length);}

  public synchronized Object remove(int num)
    {
      IOHRecord rec = records[getIndex(num)];
      Object out = (rec == null ? null : rec.remove(num));
      if (out != null) size--;
      return out;
    }

  public synchronized void clear()
    {
      this.records       = new IOHRecord[init_capacity];
      this.threshold     = (int) (load_factor * init_capacity);
      this.size          = 0;
    }

  public synchronized IntEnumeration ints()
    {
      return new IntEnumerationHelperBase()
	{
	  int index;
	  IOHRecElem finger;

	  {
	    index = -1;
	    nextIndex();
	  }

	  public boolean hasMoreInts()
	    {return index < records.length;}

	  public int nextInt()
	    {
	      try 
		{
		  int out = finger.num;
		  findNext();
		  return out;
		}
	      catch (NullPointerException e)
		{throw new NoSuchElementException();}
	    }

	  private void findNext()
	  {
	    if (finger.next != null) finger = finger.next;
	    else nextIndex();
	  }

	  private void nextIndex()
	    {
	      try
		{
		  int len = records.length;
		  do {++index;}
		  while (records[index] == null && index <= len);
		  finger = records[index].next;
		}
	      catch (ArrayIndexOutOfBoundsException e)
		{ 
		  //just means we're done.
		  finger = null;
		}
	    }
	};
    }

  //should only be called from a sync'ed method
  protected void rehash()
    {
      IOHRecord[] newRecords = new IOHRecord[records.length * 2];
      for (int i = 0; i < records.length; ++i)
	{
	  if (records[i] != null)
	    {
	      newRecords[i]     = records[i];
	      newRecords[i * 2] = records[i].split(newRecords.length);
	    }
	}
      records = newRecords;
      threshold = (int) (load_factor * records.length);
    }
}

class IOHRecord extends IOHRecElem
{
  IntObjectHash parent;
  int size = 0;
  
  IOHRecord(int index)
    {super(index, null, null);}

  IOHRecElem findInt(int num) //retuns the RecElem previous to the one containing num
    {
      for (IOHRecElem finger = this; finger.next != null; finger = finger.next)
	if (finger.next.num == num) return finger;
      return null;
    }

  boolean add(int num, Object obj, boolean replace) //returns whether or not we would have had to replace
    {                                               //whether we did depends on the value of replace
      IOHRecElem prev = findInt(num);
      if (prev != null)
	{
	  if (replace)
	    prev.next = new IOHRecElem(num, obj, prev.next.next);
	  return true;
	}
      else
	{
	  this.next = new IOHRecElem(num, obj, this.next);
	  ++size;
	  return false; 
	}
    }

  Object remove(int num)
    {
      IOHRecElem prev = findInt(num);
      if (prev == null) return null;
      else
	{
	  Object out = prev.next.obj;
	  prev.next = prev.next.next;
	  --size;
	  if (size == 0)
	    parent.records[this.num] = null; //kamikaze!!!
	  return out;
	}
    }

  Object get(int num)
    {
      IOHRecElem prev = findInt(num);
      if (prev != null)
	return prev.next.obj;
      else return null;
    }

  IOHRecord split(int new_cap)
    {
      IOHRecord  out       = null;
      IOHRecElem outFinger = null;
      for (IOHRecElem finger = this; finger.next != null; finger = finger.next)
	{
	  if (Math.abs(finger.next.num % new_cap) != this.num)
	    {
	      if (out == null)
		{
		  out       = new IOHRecord(num * 2);
		  outFinger = out;
		}
	      outFinger.next = finger.next;
	      finger.next    = finger.next.next;
	      outFinger      = outFinger.next;
	      outFinger.next = null;
	    }
	}
      return out;
    }
}

class IOHRecElem
{
  int        num;
  Object     obj;
  IOHRecElem next;

  IOHRecElem(int num, Object obj, IOHRecElem next)
    {
      this.num  = num;
      this.obj  = obj;
      this.next = next;
    }
}
