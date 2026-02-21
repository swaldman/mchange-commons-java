package com.mchange.v2.codegen.bean;

import java.util.*;
import java.lang.reflect.Modifier;
import java.io.IOException;
import com.mchange.v2.codegen.CodegenUtils;
import com.mchange.v2.io.IndentedWriter;

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
