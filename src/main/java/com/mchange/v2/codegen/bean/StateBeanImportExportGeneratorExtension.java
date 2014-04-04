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

public class StateBeanImportExportGeneratorExtension implements GeneratorExtension 
{
    int ctor_modifiers = Modifier.PUBLIC;

    public Collection extraGeneralImports()
    { return Arrays.asList( new String[] {"com.mchange.v2.bean"} ); }

    public Collection extraSpecificImports()
    { return Collections.EMPTY_SET; }

    public Collection extraInterfaceNames()
    { return Arrays.asList( new String[] {"StateBeanExporter"} ); }


    public void generate(ClassInfo info, Class superclassType, Property[] props, Class[] propTypes, IndentedWriter iw)
	throws IOException
    {
	String cn = info.getClassName();

	int num_props = props.length;
	Property[] masked = new Property[ num_props ];
	for (int i = 0; i < num_props; ++i)
	    masked[i] = new SimplePropertyMask( props[i] );

	iw.println("protected class MyStateBean implements StateBean");
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
	iw.println();
	iw.downIndent();
	iw.println("}");
	iw.println();
	iw.println("public StateBean exportStateBean()");
	iw.println("{");
	iw.upIndent();
	iw.println("MyStateBean out = createEmptyStateBean();");
	for (int i = 0; i < num_props; ++i)
	    {
		String capName = BeangenUtils.capitalize( props[i].getName() );
		iw.println("out.set" + capName + "( this." + (propTypes[i] == boolean.class ? "is" : "get") + capName + "() );");
	    }
	iw.println("return out;");
	iw.downIndent();
	iw.println("}");
	iw.println();


	iw.println("public void importStateBean( StateBean bean )");
	iw.println("{");
	iw.upIndent();
	iw.println("MyStateBean msb = (MyStateBean) bean;");
	for (int i = 0; i < num_props; ++i)
	    {
		String capName = BeangenUtils.capitalize( props[i].getName() );
		iw.println("this.set" + capName + "( msb." + (propTypes[i] == boolean.class ? "is" : "get") + capName + "() );");
	    }
	iw.downIndent();
	iw.println("}");
	iw.println();

	iw.print( CodegenUtils.getModifierString( ctor_modifiers ) );
	iw.println(" " + cn + "( StateBean bean )");
	iw.println("{ importStateBean( bean ); }");

	iw.println("protected MyStateBean createEmptyStateBean() throws StateBeanException");
	iw.println("{ return new MyStateBean(); }");
    }
}
