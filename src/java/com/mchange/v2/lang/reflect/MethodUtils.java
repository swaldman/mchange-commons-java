/*
 * Distributed as part of mchange-commons-java v.0.2.3.3
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


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
