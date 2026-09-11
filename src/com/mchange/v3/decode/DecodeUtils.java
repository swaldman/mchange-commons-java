package com.mchange.v3.decode;

import java.util.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.*;
import com.mchange.v2.log.*;

import static com.mchange.v2.reflect.ByNameInstantiationUtils.instantiateByNameGated;

public final class DecodeUtils
{
    public final static String DECODER_CLASS_DOT_KEY    = ".decoderClass";
    public final static String DECODER_CLASS_NO_DOT_KEY = "decoderClass";

    private final static Object[] DECODER_CLASS_DOT_KEY_OBJ_ARRAY    = new Object[] { DECODER_CLASS_DOT_KEY };
    private final static Object[] DECODER_CLASS_NO_DOT_KEY_OBJ_ARRAY = new Object[] { DECODER_CLASS_NO_DOT_KEY };

    private final static MLogger logger = MLog.getLogger( DecodeUtils.class );

    private final static List<DecoderFinder> finders;

    private final static String[] finderClassNames = {
	"com.mchange.sc.v1.decode.ScalaMapDecoderFinder"
    };

    static 
    {
	List<DecoderFinder> tmp = new LinkedList<DecoderFinder>();
	tmp.add( new JavaMapDecoderFinder() );
	for ( int i = 0, len = finderClassNames.length; i < len; ++i )
	    {
		try { tmp.add( (DecoderFinder) instantiateByNameGated( finderClassNames[i], null ) ); }
		catch( Exception e )
		    {
			// reflective construction wraps whatever the constructor threw; report the cause
			Throwable t = ( e instanceof InvocationTargetException ? e.getCause() : e );

			if ( logger.isLoggable( MLevel.INFO ) )
			    logger.log( MLevel.INFO, "Could not load DecoderFinder '" + finderClassNames[i] + "'", t );
		    }
	    }
	finders = Collections.unmodifiableList( tmp );
    }

    static class JavaMapDecoderFinder implements DecoderFinder
    {
	@Override
	public String decoderClassName( Object encoded ) throws CannotDecodeException
	{
	    if ( encoded instanceof Map )
		{
		    String className = null;
		    Map<?,?> map = (Map<?,?>) encoded;
		    className = (String) map.get( DECODER_CLASS_DOT_KEY );
		    if ( className == null )
			className = (String) map.get( DECODER_CLASS_NO_DOT_KEY );
		    if ( className == null )
			throw new CannotDecodeException( "Could not find the decoder class for java.util.Map: " + encoded );
		    else
			return className;
		}
	    else
		return null;
	}
    }

    final static String findDecoderClassName( Object encoded ) throws CannotDecodeException
    {
	for ( DecoderFinder finder : finders )
	    {
		String check = finder.decoderClassName( encoded );
		if ( check != null ) return check;
	    }
	throw new CannotDecodeException("Could not find a decoder class name for object: " + encoded);
    }

    public static Object decode( String decoderClassFqcn, Object encoded ) throws CannotDecodeException
    {
	try 
	    {
		Decoder decoder = (Decoder) instantiateByNameGated( decoderClassFqcn, null );
		return decoder.decode( encoded );
	    }
	catch ( Exception e )
	    {
		// reflective construction wraps whatever the constructor threw; report the cause
		Throwable t = ( e instanceof InvocationTargetException ? e.getCause() : e );
		throw new CannotDecodeException("An exception occurred while attempting to decode " + encoded, t);
	    }
    }

    public static Object decode( Object encoded ) throws CannotDecodeException
    { return decode( findDecoderClassName( encoded ), encoded ); }

    private DecodeUtils()
    {}
}
