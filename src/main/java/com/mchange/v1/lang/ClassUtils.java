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

package com.mchange.v1.lang;

import java.util.*;
import com.mchange.v1.jvm.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 *
 * See also com.mchange.v2.codegen.ClassUtils for related methods.
 *
 */
public final class ClassUtils
{
    final static String[] EMPTY_SA = new String[0];

    static Map primitivesToClasses;

    static
    {
	HashMap tmp = new HashMap();
	tmp.put( "boolean", boolean.class );
	tmp.put( "int", int.class );
	tmp.put( "char", char.class );
	tmp.put( "short", short.class );
	tmp.put( "int", int.class );
	tmp.put( "long", long.class );
	tmp.put( "float", float.class );
	tmp.put( "double", double.class );
	tmp.put( "void", void.class );

	primitivesToClasses = Collections.unmodifiableMap( tmp );
    }

    public static Set publicSupertypesForMethods(Class cl, Method[] methods)
    {
	Set testClasses = allAssignableFrom( cl );
	Set out = new HashSet();
	for (Iterator ii = testClasses.iterator(); ii.hasNext(); )
	{
	    Class check = (Class) ii.next();
	    if ( isPublic( check ) && hasAllMethodsAsSupertype( check, methods ) )
		out.add(check);
	}
	return Collections.unmodifiableSet( out );
    }

    public static boolean isPublic( Class cl )
    { return ( (cl.getModifiers() & Modifier.PUBLIC) != 0 ); }

    public static boolean hasAllMethodsAsSupertype(Class cl, Method[] methods)
    { return hasAllMethods( cl, methods, true ); }

    public static boolean hasAllMethodsAsSubtype(Class cl, Method[] methods)
    { return hasAllMethods( cl, methods, false ); }

    private static boolean hasAllMethods(Class cl, Method[] methods, boolean cl_as_supertype)
    {
	for (int i = 0, len = methods.length; i < len; ++i)
	    if ( !containsMethod( cl, methods[i], cl_as_supertype) )
		return false;
	return true;
    }

    public static boolean containsMethodAsSupertype(Class cl, Method m)
    { return containsMethod( cl, m, true ); }

    public static boolean containsMethodAsSubtype(Class cl, Method m)
    { return containsMethod( cl, m, false ); }

    private static boolean containsMethod(Class cl, Method m, boolean cl_as_supertype)
    {
	Method check;

	try 
	{ 
	    check = cl.getMethod( m.getName(), m.getParameterTypes() );
	    Class mRetType = m.getReturnType();
	    Class clRetType = check.getReturnType();

	    // we deal with potentially covariant return types
	    return ( ( mRetType.equals( clRetType ) ) ||
		     ( cl_as_supertype && clRetType.isAssignableFrom( mRetType ) ) ||
		     ( !cl_as_supertype && mRetType.isAssignableFrom( clRetType ) ) );
	}
	catch (NoSuchMethodException e)
	{ return false; }
    }

    public static Set allAssignableFrom(Class type)
    {
	Set out = new HashSet();

	//type itself and superclasses (if any)
	for (Class cl = type; cl != null; cl = cl.getSuperclass())
	    out.add( cl );

	//super interfaces (if any)
	addSuperInterfacesToSet( type, out );
	return out;
    }

    public static String simpleClassName(Class cl)
    {
	String scn;
	int array_level = 0;
	while (cl.isArray())
	    {
		++array_level;
		cl = cl.getComponentType();
	    }
	scn = simpleClassName( cl.getName() );
	if ( array_level > 0 )
	    {
		StringBuffer sb = new StringBuffer(16);
		sb.append( scn );
		for( int i = 0; i < array_level; ++i)
		    sb.append("[]");
		return sb.toString();
	    }
	else
	    return scn;
    }

    private static String simpleClassName(String fqcn)
    {
       int pkgdot = fqcn.lastIndexOf('.');
       if (pkgdot < 0)
          return fqcn;
       String scn = fqcn.substring(pkgdot + 1);
       if (scn.indexOf('$') >= 0)
          {
             StringBuffer sb = new StringBuffer(scn);
             for (int i = 0, len = sb.length(); i < len; ++i)
             {
                 if (sb.charAt(i) == '$')
                   sb.setCharAt(i, '.');
             }
             return sb.toString();
          }
       else
         return scn;
    }

    public static boolean isPrimitive(String typeStr)
    { return (primitivesToClasses.get( typeStr ) != null); }

    public static Class classForPrimitive(String typeStr)
    { return (Class) primitivesToClasses.get( typeStr ); }
    
    public static Class forName(String fqcnOrPrimitive ) throws ClassNotFoundException
    {
        Class out = classForPrimitive( fqcnOrPrimitive );
        if (out == null)
            out = Class.forName( fqcnOrPrimitive );
        return out;
    }

    public static Class forName( String fqOrSimple,  String[] importPkgs, String[] importClasses )
	throws AmbiguousClassNameException, ClassNotFoundException
    {
	try
	    { return Class.forName( fqOrSimple ); }
	catch ( ClassNotFoundException e )
	    { return classForSimpleName( fqOrSimple, importPkgs, importClasses ); }
    }

    public static Class classForSimpleName( String simpleName, String[] importPkgs, String[] importClasses )
	throws AmbiguousClassNameException, ClassNotFoundException
    {
	Set checkSet = new HashSet();
	Class out = classForPrimitive( simpleName );

	if (out == null)
	    {
		if (importPkgs == null)
		    importPkgs = EMPTY_SA;
		
		if (importClasses == null)
		    importClasses = EMPTY_SA;
		
		for (int i = 0, len = importClasses.length; i < len; ++i)
		    {
			String importSimpleName = fqcnLastElement( importClasses[i] );
			if (! checkSet.add( importSimpleName ) )
			    throw new IllegalArgumentException("Duplicate imported classes: " + 
							       importSimpleName);
			if ( simpleName.equals( importSimpleName ) ) 
			    //we won't duplicate assign. we'd have caught it above
			    out = Class.forName( importClasses[i] );
		    }
		if (out == null)
		    {
			try { out = Class.forName("java.lang." + simpleName); }
			catch (ClassNotFoundException e)
			    { /* just means we haven't found it yet */ }

			for (int i = 0, len = importPkgs.length; i < len; ++i)
			    {
				try
				    {
					String tryClass = importPkgs[i] + '.' + simpleName;
					Class test = Class.forName( tryClass );
					if ( out == null )
					    out = test;
					else
					    throw new AmbiguousClassNameException( simpleName, out, test );
				    }
				catch (ClassNotFoundException e)
				    { /* just means we haven't found it yet */ }
			    }
		    }
	    }
	if (out == null)
	    throw new ClassNotFoundException( "Could not find a class whose unqualified name is \042" +
					      simpleName + "\042 with the imports supplied. Import packages are " +
					      Arrays.asList( importPkgs ) + "; class imports are " +
					      Arrays.asList( importClasses ) );
	else
	    return out;
    }

    public static String resolvableTypeName( Class type, String[] importPkgs, String[] importClasses )
	throws ClassNotFoundException
    {
	String simpleName = simpleClassName( type );
	try
	    { classForSimpleName( simpleName, importPkgs, importClasses ); }
	catch ( AmbiguousClassNameException e )
	    { return type.getName(); }
	return simpleName;
    }

    public static String fqcnLastElement(String fqcn)
    {
       int pkgdot = fqcn.lastIndexOf('.');
       if (pkgdot < 0)
          return fqcn;
       return fqcn.substring(pkgdot + 1);
    }


    /* does not add type itself, only its superinterfaces */
    private static void addSuperInterfacesToSet(Class type, Set set)
    {
	Class[] ifaces = type.getInterfaces();
	for (int i = 0, len = ifaces.length; i < len; ++i)
	    {
		set.add( ifaces[i] );
		addSuperInterfacesToSet( ifaces[i], set );
	    }
    }

    private ClassUtils()
    {}
}
