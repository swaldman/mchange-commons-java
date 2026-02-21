package com.mchange.v2.lang.reflect;

import java.lang.reflect.Method;
import java.util.Comparator;

public final class MethodUtils
{
    public final static Comparator METHOD_COMPARATOR = new Comparator()
    {
        public int compare(Object a, Object b)
        {
            Method aa = (Method) a;
            Method bb = (Method) b;
            String aName = aa.getName();
            String bName = bb.getName();
            int out = String.CASE_INSENSITIVE_ORDER.compare(aName, bName);
            if (out == 0)
            {
                if (aName.equals(bName))
                {
                    Class[] aParams = aa.getParameterTypes();
                    Class[] bParams = bb.getParameterTypes();
                    if (aParams.length < bParams.length)
                        out = -1;
                    else if (aParams.length > bParams.length)
                        out = 1;
                    else
                    {
                        for (int i = 0, len = aParams.length; i < len; ++i)
                        {
                            String apName = aParams[i].getName();
                            String bpName = bParams[i].getName();
                            out = apName.compareTo( bpName);
                            if (out != 0)
                                break;
                        }
                    }
                }
                else
                {
                    out = aName.compareTo(bName);
                }
            }
            return out;

        }
    };

}
