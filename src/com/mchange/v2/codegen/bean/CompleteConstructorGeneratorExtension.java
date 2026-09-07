package com.mchange.v2.codegen.bean;

import java.util.*;
import java.lang.reflect.Modifier;
import java.io.IOException;
import com.mchange.v2.codegen.CodegenUtils;
import com.mchange.v2.io.IndentedWriter;

public class CompleteConstructorGeneratorExtension implements GeneratorExtension 
{
    int ctor_modifiers = Modifier.PUBLIC;

    @Override
    public Collection<String> extraGeneralImports()
    { return Collections.<String>emptySet(); }

    @Override
    public Collection<String> extraSpecificImports()
    { return Collections.<String>emptySet(); }

    @Override
    public Collection<String> extraInterfaceNames()
    { return Collections.<String>emptySet(); }

    @Override
    public void generate(ClassInfo info, Class<?> superclassType, Property[] props, Class<?>[] propTypes, IndentedWriter iw)
	throws IOException
    {
	iw.print( CodegenUtils.getModifierString( ctor_modifiers ) );
	iw.print( info.getClassName() + "( ");
	BeangenUtils.writeArgList(props, true, iw);
	iw.println(" )");
	iw.println("{");
	iw.upIndent();

	for (int i = 0, len = props.length; i < len; ++i)
	    {
		iw.print("this." + props[i].getName() + " = ");
		String setExp = props[i].getDefensiveCopyExpression();
		if (setExp == null)
		    setExp = props[i].getName();
		iw.println(setExp + ';');
	    }

	iw.downIndent();
	iw.println("}");
    }
}
