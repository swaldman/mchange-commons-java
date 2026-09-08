package com.mchange.v2.util;

import java.util.*;
import java.util.regex.*;
import com.mchange.v1.util.WrapperIterator;

public final class PatternReplacementMap
{
    List<Mapping> mappings = new LinkedList<Mapping>();

    public synchronized void addMapping( Pattern pattern, String replacement )
    { mappings.add( new Mapping( pattern, replacement) ); }

    public synchronized void removeMapping( Pattern pattern )
    {
	for (int i = 0, len = mappings.size(); i < len; ++i)
	    if ( mappings.get(i).getPattern().equals( pattern ) )
		mappings.remove( i );
    }

    public synchronized Iterator<Pattern> patterns()
    {
	return new WrapperIterator<Pattern>( mappings.iterator(), true )
	    {
		@Override
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
	for (Iterator<Mapping> ii = mappings.iterator(); ii.hasNext(); )
	    {
		Mapping mapping = ii.next();
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
