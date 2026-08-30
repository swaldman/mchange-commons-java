package com.mchange.v2.cmdline;

public class UnexpectedSwitchArgumentException extends BadCommandLineException
{
    String sw;
    String arg;

    UnexpectedSwitchArgumentException(String msg, String sw, String arg)
    {
	super(msg);
	this.sw = sw;
	this.arg = arg;
    }

    public String getSwitch()
    { return sw; }

    public String getUnexpectedArgument()
    { return arg; }
}
