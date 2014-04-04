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

/*
 * This class modified from swaldman.util.CircularList,
 * originally written by Steve Waldman at the MIT Media Lab
 */
package com.mchange.util.impl;

import java.util.*;
import com.mchange.util.*;


/**
 * <P>CircularList is a list class. Objects can be added
 * at the beginning or end; they may be enumerated through
 * forwards or backwards; they may be retrieved directly or
 * by index from the front or rear. CircularLists can also 
 * be enumerated endlessly. If the list contains any elements at
 * all, an "unterminated" Enumeration will always claim to have
 * more elements... when it's done enumerating, it will start
 * over again. The default Enumeration is of the usual,
 * terminated variety, however.</P>
 *
 * <P><B>This class is a completely UNSYNCHRONIZED implementation
 * class. Synchronize on calls where necessary.</B></P> 
 *
 */  
public class CircularList extends Object implements Cloneable
{
  CircularListRecord firstRecord;
  int size;

  public CircularList()
  {
    this.firstRecord = null;
    this.size = 0;
  }

  private void addElement(Object object, boolean first)
  {
    if (firstRecord == null)
      firstRecord = new CircularListRecord(object);
    else
      {
	CircularListRecord newRecord = new CircularListRecord(object, firstRecord.prev, firstRecord);
	firstRecord.prev.next = newRecord;
	firstRecord.prev = newRecord;
	if (first) firstRecord = newRecord;
      }
    ++size;
  }

  private void removeElement(boolean first)
  {
    if (size == 1)
      firstRecord = null;
    else
      {
	if (first) firstRecord = firstRecord.next;
	zap(firstRecord.prev);
      }
    --size;
  }

  private void zap(CircularListRecord record)
  {
    record.next.prev = record.prev;
    record.prev.next = record.next;
  }

  public void appendElement(Object object)
  {addElement(object, false);}

  public void addElementToFront(Object object)
  {addElement(object, true);}

  public void removeFirstElement()
  {removeElement(true);}

  public void removeLastElement()
  {removeElement(false);}

  public void removeFromFront(int count)
  {
    if (count > size)
      throw new IndexOutOfBoundsException(count + ">" + size);
    else for(int i = 0; i < count; ++i) removeElement(true);
  }

  public void removeFromBack(int count)
  {
    if (count > size)
      throw new IndexOutOfBoundsException(count + ">" + size);
    else for(int i = 0; i < count; ++i) removeElement(false);
  }

  public void removeAllElements()
  {
    size = 0;
    firstRecord = null;
  }

  public Object getElementFromFront(int index)
  {
    if (index >= size)
      throw new IndexOutOfBoundsException(index + ">=" + size);
    else
      {
	CircularListRecord finger = firstRecord;
	for (int i = 0; i < index; ++i)
	  finger = finger.next;
	return finger.object;
      }
  }

  public Object getElementFromBack(int index)
  {
    if (index >= size)
      throw new IndexOutOfBoundsException(index + ">=" + size);
    else
      {
	CircularListRecord finger = firstRecord.prev;
	for (int i = 0; i < index; ++i)
	  finger = finger.prev;
	return finger.object;
      }
  }

  public Object getFirstElement()
    {
      try {return firstRecord.object;}
      catch (NullPointerException e)
	{throw new IndexOutOfBoundsException("CircularList is empty.");}
    }

  public Object getLastElement()
    {
      try {return firstRecord.prev.object;}
      catch (NullPointerException e)
	{throw new IndexOutOfBoundsException("CircularList is empty.");}
    }



  public Enumeration elements(boolean forward, boolean terminated)
  {return new CircularListEnumeration(this, forward, terminated);}

  public Enumeration elements(boolean forward)
  {return elements(forward, true);}

  public Enumeration elements()
  {return elements(true, true);}

  public int size()
  {return size;}

  /**
   * Returns a <B><I>shallow</I></B> copy. The list is
   * cloned, but not the elements within it.
   */
  public Object clone()
    {
      //this could be much more effeicient...
      CircularList out = new CircularList();
      int len = this.size();
      for (int i = 0; i < len; ++i)
	out.appendElement(this.getElementFromFront(i));
      return out;
    }

  public static void main(String[] argv)
  {
    CircularList list = new CircularList();
    list.appendElement("Hello");
    list.appendElement("There");
    list.appendElement("Joe.");
    for (Enumeration e = list.elements(); e.hasMoreElements();)
      System.out.println("x " + e.nextElement());
  }
}

class CircularListEnumeration extends Object implements Enumeration
{
  boolean forward;
  boolean terminated;
  boolean done;
  CircularListRecord startRecord;
  CircularListRecord lastRecord;

  CircularListEnumeration(CircularList list, boolean forward, boolean terminated)
  {
    if (list.firstRecord == null)
      this.done = true;
    else
      {
	this.done = false;
	this.forward = forward;
	this.terminated = terminated;
	this.startRecord = (forward ? list.firstRecord : list.firstRecord.prev);
	this.lastRecord = (forward ? startRecord.prev : startRecord);
      }
  }

  public boolean hasMoreElements()
  {return !done;}

  public Object nextElement()
  {
    if (done)
      throw new NoSuchElementException();
    else
      {
	lastRecord = (forward ? lastRecord.next : lastRecord.prev);
	if (terminated && lastRecord == (forward ? startRecord.prev : startRecord))
	  done = true;
	return lastRecord.object;
      }
  }
}
      

class CircularListRecord
{
  Object object;
  CircularListRecord next;
  CircularListRecord prev;

  CircularListRecord(Object object, CircularListRecord prev, CircularListRecord next)
  {
    this.object = object;
    this.prev = prev;
    this.next = next;
  }

  CircularListRecord(Object object) //for first record
  {
    this.object = object;
    this.prev = this;
    this.next = this;
  }
}
      



