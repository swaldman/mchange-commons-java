/*
 * Distributed as part of mchange-commons-java v.0.2.1
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
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


package com.mchange.util.impl;

import com.mchange.util.*;

/* This code modified from class swaldman.util.IntChecklist */
/* which was originally written at the Media Lab            */

public class LinkedListIntChecklistImpl implements IntChecklist
{
  private final LLICIRecord headRecord = new LLICIRecord();
  private int num_checked = 0;

  public void check(int num)
  {
    LLICIRecord finger = findPrevious(num);
    if (finger.next == null || finger.next.contained != num)
      {
	LLICIRecord newRec = new LLICIRecord();
	newRec.next = finger.next;
	newRec.contained = num;
	finger.next = newRec;
	++num_checked;
      }
  }

  public void uncheck(int num)
  {
    LLICIRecord finger = findPrevious(num);
    if (finger.next != null && finger.next.contained == num)
      {
	finger.next = finger.next.next;
	--num_checked;
      }
  }

  public boolean isChecked(int num)
  {
    LLICIRecord finger = findPrevious(num);
    return (finger.next != null && finger.next.contained == num);
  }

  public void clear()
  {
    headRecord.next = null;
    num_checked = 0;
  }

  public int countChecked()
  {return num_checked;}

  public int[] getChecked()
  {
    LLICIRecord finger = headRecord;
    int[] out = new int[num_checked];
    int i = 0;
    while (finger.next != null)
      {
	out[i++] = finger.next.contained;
	finger = finger.next;
      }
    return out;
  }

  public IntEnumeration checked()
    {
      return new IntEnumerationHelperBase()
	{
	  LLICIRecord finger = headRecord;
	  
	  public int nextInt()
	    {
	      try 
		{
		  finger = finger.next;
		  return finger.contained;
		}
	      catch (NullPointerException e)
		{throw new java.util.NoSuchElementException();}
	    }

	  public boolean hasMoreInts()
	    {return (finger.next != null);}
	};
    }

  private LLICIRecord findPrevious(int num)
  {
    LLICIRecord finger = headRecord;
    while (finger.next != null && finger.next.contained < num)
      finger = finger.next;
    return finger;
  }
}

class LLICIRecord
{
  int         contained = 0;
  LLICIRecord next      = null;
}
