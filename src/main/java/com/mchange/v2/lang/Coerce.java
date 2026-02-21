package com.mchange.v2.lang;

import java.util.*;

public final class Coerce
{
    final static Set CAN_COERCE;
    
    static
    {
	Class[] classes =
	    {
		byte.class,
		boolean.class,
		char.class,
		short.class,
		int.class,
		long.class,
		float.class,
		double.class,
		String.class,
		Byte.class,
		Boolean.class,
		Character.class,
		Short.class,
		Integer.class,
		Long.class,
		Float.class,
		Double.class
	    };
	Set tmp = new HashSet();
	tmp.addAll( Arrays.asList( classes ) );
	CAN_COERCE = Collections.unmodifiableSet( tmp );
    }

    public static boolean canCoerce( Class cl )
    { return CAN_COERCE.contains( cl ); }

    public static boolean canCoerce( Object o )
    { return canCoerce( o.getClass() ); }

    public static int toInt( String s )
    { 
	try { return Integer.parseInt( s ); }
	catch ( NumberFormatException e )
	    { return (int) Double.parseDouble( s ); }
    }

    public static long toLong( String s )
    { 
	try { return Long.parseLong( s ); }
	catch ( NumberFormatException e )
	    { return (long) Double.parseDouble( s ); }
    }

    public static float toFloat( String s )
    { return Float.parseFloat( s ); }

    public static double toDouble( String s )
    { return Double.parseDouble( s ); }

    public static byte toByte( String s )
    { return (byte) toInt(s); }

    public static short toShort( String s )
    { return (short) toInt(s); }

    public static boolean toBoolean( String s )
    { return Boolean.valueOf( s ).booleanValue(); }

    public static char toChar( String s )
    {
	s = s.trim();
	if (s.length() == 1)
	    return s.charAt( 0 );
	else
	    return (char) toInt(s);
    }

    public static Object toObject( String s, Class type )
    {
	if ( type == byte.class) type = Byte.class;
	else if ( type == boolean.class) type = Boolean.class;
	else if ( type == char.class) type = Character.class;
	else if ( type == short.class) type = Short.class;
	else if ( type == int.class) type = Integer.class;
	else if ( type == long.class) type = Long.class;
	else if ( type == float.class) type = Float.class;
	else if ( type == double.class) type = Double.class;

	if ( type == String.class )
	    return s;
	else if ( type == Byte.class )
	    return Byte.valueOf( toByte( s ) );
	else if ( type == Boolean.class )
	    return Boolean.valueOf( s );
	else if ( type == Character.class )
	    return Character.valueOf( toChar( s ) );
	else if ( type == Short.class )
	    return Short.valueOf( toShort( s ) );
	else if ( type == Integer.class )
	    return Integer.valueOf( s );
	else if ( type == Long.class )
	    return Long.valueOf( s );
	else if ( type == Float.class )
	    return Float.valueOf( s );
	else if ( type == Double.class )
	    return Double.valueOf( s );
	else
	    throw new IllegalArgumentException("Cannot coerce to type: " + type.getName());
    }

    private Coerce()
    {}
}
