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

public class SimpleStateBeanImportExportGeneratorExtension implements GeneratorExtension 
{
    int ctor_modifiers = Modifier.PUBLIC;

    public Collection extraGeneralImports()
    { return Collections.EMPTY_SET; }

    public Collection extraSpecificImports()
    { return Collections.EMPTY_SET; }

    public Collection extraInterfaceNames()
    { return Collections.EMPTY_SET; }

    static class SimplePropertyMask implements Property
    {
	Property p;

	SimplePropertyMask(Property p)
	{ this.p = p; }

	public int getVariableModifiers()
	{ return Modifier.PRIVATE; }

	public String  getName()
	{ return p.getName(); }

	public String  getSimpleTypeName()
	{ return p.getSimpleTypeName(); }

	public String getDefensiveCopyExpression()
	{ return null; }

	public String getDefaultValueExpression()
	{ return null; }

	public int getGetterModifiers()
	{ return Modifier.PUBLIC; }

	public int getSetterModifiers()
	{ return Modifier.PUBLIC; }

	public boolean isReadOnly()
	{ return false; }

	public boolean isBound()
	{ return false; }

	public boolean isConstrained()
	{ return false; }
    }

    public void generate(ClassInfo info, Class superclassType, Property[] props, Class[] propTypes, IndentedWriter iw)
	throws IOException
    {
	int num_props = props.length;
	Property[] masked = new Property[ num_props ];
	for (int i = 0; i < num_props; ++i)
	    masked[i] = new SimplePropertyMask( props[i] );

	iw.println("protected static class SimpleStateBean implements ExportedState");
	iw.println("{");
	iw.upIndent();

	for (int i = 0; i < num_props; ++i)
	    {
		masked[i] = new SimplePropertyMask( props[i] );
		BeangenUtils.writePropertyMember( masked[i], iw );
		iw.println();
		BeangenUtils.writePropertyGetter( masked[i], iw );
		iw.println();
		BeangenUtils.writePropertySetter( masked[i], iw );
	    }

	iw.downIndent();
	iw.println("}");
    }
}
