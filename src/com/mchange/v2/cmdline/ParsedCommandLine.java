package com.mchange.v2.cmdline;

public interface ParsedCommandLine
{
    public String[] getRawArgs();

    public String   getSwitchPrefix();
    public boolean  includesSwitch(String sw);
    public String   getSwitchArg(String sw);
    public String[] getUnswitchedArgs();
}
