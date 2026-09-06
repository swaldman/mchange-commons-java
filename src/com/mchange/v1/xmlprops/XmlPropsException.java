package com.mchange.v1.xmlprops;

/**
 *  Extends the deprecated com.mchange.lang.PotentiallySecondaryException, which
 *  predates the cause field Throwable acquired in jdk 1.4. Retaining it costs
 *  nothing, and clients may catch it, so the superclass stays and the warning is
 *  suppressed instead.
 *
 *  The superclass is named in full because it is deprecated by javadoc tag rather
 *  than annotation: an import declaration lies outside the class body, where
 *  @SuppressWarnings cannot reach it.
 */
@SuppressWarnings("deprecation")
public class XmlPropsException extends com.mchange.lang.PotentiallySecondaryException
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
