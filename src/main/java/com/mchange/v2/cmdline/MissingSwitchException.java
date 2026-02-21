package com.mchange.v2.cmdline;

public class MissingSwitchException extends BadCommandLineException
{
    String sw;

    MissingSwitchException(String msg, String sw)
    {
	super(msg);
	this.sw = sw;
    }

    public String getMissingSwitch()
    { return sw; }
}
