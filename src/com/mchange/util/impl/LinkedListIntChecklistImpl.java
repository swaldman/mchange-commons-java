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
