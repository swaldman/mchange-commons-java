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
