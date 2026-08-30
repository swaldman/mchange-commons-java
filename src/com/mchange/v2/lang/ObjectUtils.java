package com.mchange.v2.lang;

public final class ObjectUtils
{
    public static boolean eqOrBothNull(Object a, Object b)
    {
	if (a == b)
	    return true;
	else if (a == null)
	    return false;
	else
	    return a.equals(b);
    }
	
    /**
     *  Note -- if you are using Arrays.equals( ... ) or similar
     *  and want a compatible hash method, see methods in 
     * {@link com.mchange.v1.util.ArrayUtils#hashOrZeroArray ArrayUtils}.
     */
    public static int hashOrZero(Object o)
    { return (o == null ? 0 : o.hashCode()); }

    private ObjectUtils()
    {}
}
