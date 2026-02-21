package com.mchange.v1.lang;

/**
 * @deprecated use com.mchange.v2.ObjectUtils.eqOrBothNull()
 */
public final class NullUtils
{
    public static boolean equalsOrBothNull(Object a, Object b)
    {
	if (a == b)
	    return true;
	else if (a == null)
	    return false;
	else
	    return a.equals( b );
    }

    private NullUtils()
    {}
}
