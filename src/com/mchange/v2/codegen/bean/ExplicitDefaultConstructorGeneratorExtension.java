package com.mchange.v2.codegen.bean;

import java.util.*;
import java.lang.reflect.Modifier;
import java.io.IOException;
import com.mchange.v2.codegen.CodegenUtils;
import com.mchange.v2.io.IndentedWriter;

public class ExplicitDefaultConstructorGeneratorExtension implements GeneratorExtension 
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
    { BeangenUtils.writeExplicitDefaultConstructor( ctor_modifiers, info, iw); }
}
