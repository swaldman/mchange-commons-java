package com.mchange.v2.cmdline;

public class UnexpectedSwitchException extends BadCommandLineException
{
    String sw;

    UnexpectedSwitchException(String msg, String sw)
    {
	super(msg);
	this.sw = sw;
    }

    public String getUnexpectedSwitch()
    { return sw; }
}
