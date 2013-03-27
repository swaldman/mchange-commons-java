package com.mchange.v3.decode;

import java.lang.reflect.*;
import com.mchange.v2.log.*;

public final class DecodeUtils
{
    public final static String DECODER_CLASS_DOT_KEY    = ".decoderClass";
    public final static String DECODER_CLASS_NO_DOT_KEY = "decoderClass";

    private final static Object[] DECODER_CLASS_DOT_KEY_OBJ_ARRAY    = new Object[] { DECODER_CLASS_DOT_KEY };
    private final static Object[] DECODER_CLASS_NO_DOT_KEY_OBJ_ARRAY = new Object[] { DECODER_CLASS_NO_DOT_KEY };

    private final static MLogger logger = MLog.getLogger( DecodeUtils.class );

    //MT: protected by class lock
    private static boolean no_scala_logged = false;

    private static synchronized void logNoScala( Exception e )
    {
	if (! no_scala_logged)
	    {
		no_scala_logged = true;

		if ( logger.isLoggable( MLevel.INFO ) )
		    logger.log( MLevel.INFO, "Scala classes seem not to be available.", e );
	    }
    }

    final static String findDecoderClassName( Object encoded ) throws CannotDecodeException
    {
	try
	    {
		String className = null;
		if ( encoded instanceof java.util.Map )
		    {
			java.util.Map<String,Object> map = (java.util.Map<String,Object>) encoded;
			className = (String) map.get( DECODER_CLASS_DOT_KEY );
			if ( className == null )
			    className = (String) map.get( DECODER_CLASS_NO_DOT_KEY );
		if ( className == null )
		    throw new CannotDecodeException( "Could not find the decoder class for java.util.Map: " + encoded );
		    }
		else
		    {
			try
			    {
				Class<?> scalaMapClass = Class.forName("scala.collection.immutable.Map");
				if ( scalaMapClass.isAssignableFrom( encoded.getClass() ) )
				    {
					Method m = scalaMapClass.getMethod( "apply", new Class[] { Object.class } );
					try { className = (String) m.invoke( encoded, DECODER_CLASS_DOT_KEY_OBJ_ARRAY ); }
					catch (Exception e) { /* ignore */ }
					if (className == null)
					    {
						try { className = (String) m.invoke( encoded, DECODER_CLASS_NO_DOT_KEY_OBJ_ARRAY ); }
						catch (Exception e) { /* ignore */ }
					    }
					if ( className == null )
					    throw new CannotDecodeException( "Could not find the decoder class for scala.collection.immutable.Map: " + encoded );
				    }
			    }
			catch (Exception e)
			    { logNoScala(e); }
		    }
		
		if ( className == null )
		    throw new CannotDecodeException( "Could not find the decoder class for unexpected Object: " + encoded );
		else
		    return className;
	    }
	catch ( CannotDecodeException cde )
	    { throw cde; }
	catch ( Exception e )
	    { throw new CannotDecodeException( "An exception occurred while trying to find the decoder class!", e ); }
    }

    public static Object decode( String decoderClassFqcn, Object encoded ) throws CannotDecodeException
    {
	try 
	    {
		Class<?> clz = Class.forName( decoderClassFqcn );
		Decoder decoder = (Decoder) clz.newInstance();
		return decoder.decode( encoded );
	    }
	catch ( Exception e )
	    { throw new CannotDecodeException("An exception occurred while attempting to decode " + encoded, e); }
    }

    public static Object decode( Object encoded ) throws CannotDecodeException
    { return decode( findDecoderClassName( encoded ), encoded ); }

    private DecodeUtils()
    {}
}
