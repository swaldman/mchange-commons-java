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

package com.mchange.v2.util;

import java.util.*;
import java.util.regex.*;
import com.mchange.v1.util.WrapperIterator;

public final class PatternReplacementMap
{
    List mappings = new LinkedList();

    public synchronized void addMapping( Pattern pattern, String replacement )
    { mappings.add( new Mapping( pattern, replacement) ); }

    public synchronized void removeMapping( Pattern pattern )
    {
	for (int i = 0, len = mappings.size(); i < len; ++i)
	    if ( ((Mapping) mappings.get(i)).getPattern().equals( pattern ) )
		mappings.remove( i );
    }

    public synchronized Iterator patterns()
    {
	return new WrapperIterator( mappings.iterator(), true )
	    {
		protected Object transformObject(Object o)
		{
		    Mapping m = (Mapping) o;
		    return m.getPattern();
		}
	    };
    }

    public synchronized int size()
    { return mappings.size(); }

    /**
     * @return null if there was no match, designated replacement otherwise.
     */
    public synchronized String attemptReplace( String testString )
    {
	String out = null;
	for (Iterator ii = mappings.iterator(); ii.hasNext(); )
	    {
		Mapping mapping = (Mapping) ii.next();
// 		System.err.println(this + " trying: pattern -- " + mapping.getPattern() + 
// 				   " ; replacement -- " + mapping.getReplacement() + 
// 				   " ; testString -- " + testString );
		Matcher matcher = mapping.getPattern().matcher( testString );
		if (matcher.matches())
		    {
			out = matcher.replaceAll( mapping.getReplacement() );
			break;
		    }
	    }
	return out;
    }

    private final static class Mapping
    {
	Pattern pattern;
	String  replacement;

	public Pattern getPattern()
	{ return pattern; }

	public String getReplacement()
	{ return replacement; }

	public Mapping( Pattern pattern, String replacement )
	{
	    this.pattern = pattern;
	    this.replacement = replacement;
	}
    }
}
