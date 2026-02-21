package com.mchange.v1.xmlprops;

import com.mchange.lang.PotentiallySecondaryException;

public class XmlPropsException extends PotentiallySecondaryException
{
    public XmlPropsException(String msg, Throwable t)
    {super(msg, t);}

    public XmlPropsException(Throwable t)
    {super(t);}

    public XmlPropsException(String msg)
    {super(msg);}

    public XmlPropsException()
    {super();}
}
