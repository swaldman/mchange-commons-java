package com.mchange.v1.lang;

public class AmbiguousClassNameException extends Exception
{
    AmbiguousClassNameException(String simpleName, Class c1, Class c2)
    { 
	super( simpleName +
	       " could refer either to " + c1.getName() + 
	       " or " + c2.getName() );
    }
}
