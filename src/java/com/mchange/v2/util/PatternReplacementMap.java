/*
 * Distributed as part of mchange-commons-java v.0.2.1
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
