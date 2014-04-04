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

import java.io.IOException;
import com.mchange.v2.codegen.IndentedWriter;

public class PropsToStringGeneratorExtension implements GeneratorExtension 
{
    private Collection excludePropNames = null;

    public void setExcludePropertyNames( Collection excludePropNames )
    { this.excludePropNames = excludePropNames; }

    public Collection getExcludePropertyNames()
    { return excludePropNames; }

    public Collection extraGeneralImports()
    { return Collections.EMPTY_SET; }

    public Collection extraSpecificImports()
    { return Collections.EMPTY_SET; }

    public Collection extraInterfaceNames()
    { return Collections.EMPTY_SET; }

    public void generate(ClassInfo info, Class superclassType, Property[] props, Class[] propTypes, IndentedWriter iw)
	throws IOException
    {
	iw.println("public String toString()");
	iw.println("{");
	iw.upIndent();

	iw.println("StringBuffer sb = new StringBuffer();");
	iw.println("sb.append( super.toString() );");
	iw.println("sb.append(\" [ \");");

	for (int i = 0, len = props.length; i < len; ++i)
	    {
		Property prop = props[i];

		if ( excludePropNames != null && excludePropNames.contains( prop.getName() ) )
		    continue;

		iw.println("sb.append( \"" + prop.getName() + " -> \"" + " + " + prop.getName() + " );");
		if ( i != len - 1 )
		    iw.println("sb.append( \", \");");
	    }

	iw.println();
	iw.println("String extraToStringInfo = this.extraToStringInfo();");
	iw.println("if (extraToStringInfo != null)");
	iw.upIndent();
	iw.println("sb.append( extraToStringInfo );");
	iw.downIndent();


	iw.println("sb.append(\" ]\");");
	iw.println("return sb.toString();");
	iw.downIndent();
	iw.println("}");
	iw.println();
	iw.println("protected String extraToStringInfo()");
	iw.println("{ return null; }");
    }
}
