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

import com.mchange.util.*;

public class LongObjectHash implements LongObjectMap
{
  LOHRecord[] records;
  float       load_factor;
  long        threshold;
  long        size;

  public LongObjectHash(int init_capacity, float load_factor)
    {
      this.records     = new LOHRecord[init_capacity];
      this.load_factor = load_factor;
      this.threshold   = (long) (load_factor * init_capacity);
    }

  public LongObjectHash()
    {this(101, 0.75f);} //defaults from java.util.Hashtable

  public synchronized Object get(long num)
    {
      int index  = (int) (num % records.length);
      Object out = null;
      if (records[index] != null)
	out = records[index].get(num);
      return out;
    }
  
  public synchronized void put(long num, Object obj)
    {
      int index = (int) (num % records.length);
      if (records[index] == null)
	records[index] = new LOHRecord(index);
      boolean replaced = records[index].add(num, obj, true);
      if (!replaced) ++size;
      if (size > threshold) rehash();
    }

  public synchronized boolean putNoReplace(long num, Object obj)
    {
      int index = (int)(num % records.length);
      if (records[index] == null)
	records[index] = new LOHRecord(index);
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

  public long getSize()
    {return size;}

  public synchronized boolean containsLong(long num)
    {
      int index = (int) (num % records.length);
      return (records[index] != null && records[index].findLong(num) != null);
    }

  public synchronized Object remove(long num)
    {
      LOHRecord rec = records[(int) (num % records.length)];
      Object out = (rec == null ? null : rec.remove(num));
      if (out != null) size--;
      return out;
    }

  //should only be called from a sync'ed method
  protected void rehash()
    {
      if ((records.length * 2L) > Integer.MAX_VALUE)
	throw new Error("Implementation of LongObjectHash allows a capacity of only " + Integer.MAX_VALUE);

      LOHRecord[] newRecords = new LOHRecord[records.length * 2];
      for (int i = 0; i < records.length; ++i)
	{
	  if (records[i] != null)
	    {
	      newRecords[i]     = records[i];
	      newRecords[i * 2] = records[i].split(newRecords.length);
	    }
	}
      records = newRecords;
      threshold = (long) (load_factor * records.length);
    }
}

class LOHRecord extends LOHRecElem
{
  LongObjectHash parent;
  int size = 0;
  
  LOHRecord(long index)
    {super(index, null, null);}

  LOHRecElem findLong(long num) //retuns the RecElem previous to the one containing num
    {
      for (LOHRecElem finger = this; finger.next != null; finger = finger.next)
	if (finger.next.num == num) return finger;
      return null;
    }

  boolean add(long num, Object obj, boolean replace) //returns whether or not we would have had to replace
    {                                               //whether we did depends on the value of replace
      LOHRecElem prev = findLong(num);
      if (prev != null)
	{
	  if (replace)
	    prev.next = new LOHRecElem(num, obj, prev.next.next);
	  return true;
	}
      else
	{
	  this.next = new LOHRecElem(num, obj, this.next);
	  ++size;
	  return false; 
	}
    }

  Object remove(long num)
    {
      LOHRecElem prev = findLong(num);
      if (prev == null) return null;
      else
	{
	  Object out = prev.next.obj;
	  prev.next = prev.next.next;
	  --size;
 	  if (size == 0)
 	    parent.records[(int) this.num] = null; //kamikaze!!!
	  return out;
	}
    }

  Object get(long num)
    {
      LOHRecElem prev = findLong(num);
      if (prev != null)
	return prev.next.obj;
      else return null;
    }

  LOHRecord split(int new_cap)
    {
      LOHRecord  out       = null;
      LOHRecElem outFinger = null;
      for (LOHRecElem finger = this; finger.next != null; finger = finger.next)
	{
	  if ((finger.next.num % new_cap) != this.num)
	    {
	      if (out == null)
		{
		  out       = new LOHRecord(num * 2);
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

class LOHRecElem
{
  long       num;
  Object     obj;
  LOHRecElem next;

  LOHRecElem(long num, Object obj, LOHRecElem next)
    {
      this.num  = num;
      this.obj  = obj;
      this.next = next;
    }
}
