package com.mchange.util;

/**
 * @deprecated Use com.mchange.v2.cmdline.CommandLineUtils instead
 */
public interface CommandLineParser
{
  public boolean  checkSwitch(String sw);
  public String   findSwitchArg(String sw);
  public boolean  checkArgv();
  public int      findLastSwitched();

  /**
   * Order of args is guaranteed to be maintained.
   */ 
  public String[] findUnswitchedArgs();
}


  






