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

package com.mchange.v3.decode;

import java.util.*;
import java.lang.reflect.*;
import com.mchange.v2.log.*;

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
		try { tmp.add( (DecoderFinder) Class.forName( finderClassNames[i] ).newInstance() ); }
		catch( Exception e )
		    {
			if ( logger.isLoggable( MLevel.INFO ) )
			    logger.log( MLevel.INFO, "Could not load DecoderFinder '" + finderClassNames[i] + "'", e );
		    }
	    }
	finders = Collections.unmodifiableList( tmp );
    }

    static class JavaMapDecoderFinder implements DecoderFinder
    {
	public String decoderClassName( Object encoded ) throws CannotDecodeException
	{
	    if ( encoded instanceof Map )
		{
		    String className = null;
		    Map<String,Object> map = (Map<String,Object>) encoded;
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
