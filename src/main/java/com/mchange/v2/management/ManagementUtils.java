/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
 */

package com.mchange.v2.management;

import javax.management.*;
import java.util.Comparator;

public class ManagementUtils
{
    public final static Comparator PARAM_INFO_COMPARATOR = new Comparator()
    {
        public int compare(Object a, Object b)
        {
            MBeanParameterInfo aa = (MBeanParameterInfo) a;
            MBeanParameterInfo bb = (MBeanParameterInfo) b;
            int out = aa.getType().compareTo(bb.getType());
            if (out == 0)
            {
                out = aa.getName().compareTo(bb.getName());
                if (out == 0)
                {
                    String aDesc = aa.getDescription();
                    String bDesc = bb.getDescription();
                    if (aDesc == null && bDesc == null)
                        out = 0;
                    else if (aDesc == null)
                        out = -1;
                    else if (bDesc == null)
                        out = 1;
                    else
                        out = aDesc.compareTo(bDesc);
                }
            }
            return out;
        }
    };
    
    public final static Comparator OP_INFO_COMPARATOR = new Comparator()
    {
        public int compare(Object a, Object b)
        {
            MBeanOperationInfo aa = (MBeanOperationInfo) a;
            MBeanOperationInfo bb = (MBeanOperationInfo) b;
            String aName = aa.getName();
            String bName = bb.getName();
            int out = String.CASE_INSENSITIVE_ORDER.compare(aName, bName);
            if (out == 0)
            {
                if (aName.equals(bName))
                {
                    MBeanParameterInfo[] aParams = aa.getSignature();
                    MBeanParameterInfo[] bParams = bb.getSignature();
                    if (aParams.length < bParams.length)
                        out = -1;
                    else if (aParams.length > bParams.length)
                        out = 1;
                    else
                    {
                        for (int i = 0, len = aParams.length; i < len; ++i)
                        {
                            out = PARAM_INFO_COMPARATOR.compare(aParams[i], bParams[i]);
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
