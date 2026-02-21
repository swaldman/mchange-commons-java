package com.mchange.util;

/* inspired by ML code, swaldman.util.IntChecklist */

public interface IntChecklist
{
  public void           check(int num);
  public void           uncheck(int num);
  public boolean        isChecked(int num);
  public void           clear();
  public int            countChecked();
  public int[]          getChecked();
  public IntEnumeration checked();
}
