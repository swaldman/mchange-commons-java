package com.mchange.util.impl;

import com.mchange.util.IntChecklist;
import com.mchange.util.IntEnumeration;

public class HashIntChecklist implements IntChecklist
{
  private final static Object DUMMY = new Object();

  IntObjectHash ioh = new IntObjectHash();

  public void check(int num)
    {ioh.put(num, DUMMY);}

  public void uncheck(int num)
    {ioh.remove(num);}

  public boolean isChecked(int num)
    {return ioh.containsInt(num);}

  public void clear()
    {ioh.clear();}

  public int countChecked()
    {return ioh.getSize();}

  public int[] getChecked()
    {
      synchronized (ioh)
	{
	  int[] out = new int[ioh.getSize()];
	  IntEnumeration ints = ioh.ints();
	  for (int i = 0; ints.hasMoreInts(); ++i) out[i] = ints.nextInt();
	  return out;
	}
    }

  public IntEnumeration checked()
    {return ioh.ints();}

}
