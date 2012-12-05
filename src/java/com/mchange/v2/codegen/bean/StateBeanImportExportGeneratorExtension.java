/*
 * Distributed as part of mchange-commons-java v.0.2.3.2
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
