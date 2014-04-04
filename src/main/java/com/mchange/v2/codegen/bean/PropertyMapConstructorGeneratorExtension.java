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

package com.mchange.v2.codegen.bean;

import java.util.*;

import java.lang.reflect.Modifier;
import java.io.IOException;
import com.mchange.v2.codegen.CodegenUtils;
import com.mchange.v2.codegen.IndentedWriter;

public class PropertyMapConstructorGeneratorExtension implements GeneratorExtension 
{
    int ctor_modifiers = Modifier.PUBLIC;

    public Collection extraGeneralImports()
    { return Collections.EMPTY_SET; }

    public Collection extraSpecificImports()
    {
	Set set = new HashSet();
	set.add("java.util.Map");
	return set;
    }

    public Collection extraInterfaceNames()
    { return Collections.EMPTY_SET; }

    public void generate(ClassInfo info, Class superclassType, Property[] props, Class[] propTypes, IndentedWriter iw)
	throws IOException
    {
	iw.print( CodegenUtils.getModifierString( ctor_modifiers ) );
	iw.print(' ' + info.getClassName() + "( Map map )");
	iw.println("{");
	iw.upIndent();

	iw.println( "Object raw;" );
	for (int i = 0, len = props.length; i < len; ++i)
	    {
		Property prop   = props[i];
		String propName = prop.getName();
		Class propType  = propTypes[i];
		iw.println("raw = map.get( \"" + propName + "\" );");
		iw.println("if (raw != null)");
		iw.println("{");
		iw.upIndent();

		iw.print("this." + propName + " = ");
		if ( propType == boolean.class )
		    iw.println( "((Boolean) raw ).booleanValue();" );
		else if ( propType == byte.class )
		    iw.println( "((Byte) raw ).byteValue();" );
		else if ( propType == char.class )
		    iw.println( "((Character) raw ).charValue();" );
		else if ( propType == short.class )
		    iw.println( "((Short) raw ).shortValue();" );
		else if ( propType == int.class )
		    iw.println( "((Integer) raw ).intValue();" );
		else if ( propType == long.class )
		    iw.println( "((Long) raw ).longValue();" );
		else if ( propType == float.class )
		    iw.println( "((Float) raw ).floatValue();" );
		else if ( propType == double.class )
		    iw.println( "((Double) raw ).doubleValue();" );
		iw.println("raw = null;");

		iw.downIndent();
		iw.println("}");
	    }

	iw.downIndent();
	iw.println("}");
    }
}
