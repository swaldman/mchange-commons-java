package com.mchange.v2.codegen.bean;

import java.util.*;
import java.io.IOException;
import com.mchange.v2.io.IndentedWriter;

/**
 * By the time generate(...) is called, all extra interfaces and imports from all
 * GeneratorExtensions should be incorporated into the passed-in ClassInfo object.
 */
public interface GeneratorExtension
{
    public Collection extraGeneralImports();
    public Collection extraSpecificImports();
    public Collection extraInterfaceNames();

    public void generate(ClassInfo info, Class superclassType, Property[] props, Class[] propTypes, IndentedWriter iw)
	throws IOException;
}
