package com.mchange.v2.codegen.bean;

import java.util.*;

import java.lang.reflect.Modifier;
import java.io.IOException;
import com.mchange.v2.codegen.CodegenUtils;
import com.mchange.v2.io.IndentedWriter;

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
