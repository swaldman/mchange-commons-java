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

package com.mchange.v2.log;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public final class LogUtils
{
    public static String createParamsList(Object[] params)
    {
	StringBuffer sb = new StringBuffer(511);
	LogUtils.appendParamsList( sb, params );
	return sb.toString();
    }

    public static void appendParamsList(StringBuffer sb, Object[] params)
    {
	sb.append("[params: ");
	if ( params != null )
	{
	    for (int i = 0, len = params.length; i < len; ++i)
	    {
		if (i != 0) sb.append(", ");
		sb.append( params[i] );
	    }
	}
	sb.append(']');
    }

    public static String createMessage(String srcClass, String srcMeth, String msg)
    {
	StringBuffer sb = new StringBuffer(511);
	sb.append("[class: ");
	sb.append( srcClass );
	sb.append("; method: ");
	sb.append( srcMeth );
	if (! srcMeth.endsWith(")"))
	    sb.append("()");
	sb.append("] ");
	sb.append( msg );
	return sb.toString();
    }
    
    public static String createMessage(String srcMeth, String msg)
    {
	StringBuffer sb = new StringBuffer(511);
	sb.append("[method: ");
	sb.append( srcMeth );
	if (! srcMeth.endsWith(")"))
	    sb.append("()");
	sb.append("] ");
	sb.append( msg );
	return sb.toString();
    }

    public static String formatMessage( String rbname, String msg, Object[] params )
    {
        if ( msg == null )
        {
            if (params == null)
                return "";
            else
                return LogUtils.createParamsList( params );
        }
        else
        {
	    if ( rbname != null )
	    {
		ResourceBundle rb = ResourceBundle.getBundle( rbname );
		if (rb != null)
		{
		    String check = rb.getString( msg );
		    if (check != null)
			msg = check;
		}
	    }
            return (params == null ? msg : MessageFormat.format( msg, params ));
        }
    } 

    private LogUtils()
    {}
}
