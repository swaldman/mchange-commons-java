package com.mchange.v2.codegen.bean;

import java.lang.reflect.Modifier;

public class SimpleClassInfo implements ClassInfo
{
    String   packageName;
    int      modifiers;
    String   className;
    String   superclassName;
    String[] interfaceNames;
    String[] generalImports;
    String[] specificImports;

    @Override
    public String   getPackageName()          { return packageName; }
    @Override
    public int      getModifiers()            { return modifiers; }
    @Override
    public String   getClassName()            { return className; }
    @Override
    public String   getSuperclassName()       { return superclassName; }
    @Override
    public String[] getInterfaceNames()       { return interfaceNames; }
    @Override
    public String[] getGeneralImports()       { return generalImports; }
    @Override
    public String[] getSpecificImports()      { return specificImports; }

    public SimpleClassInfo( String   packageName,
			    int      modifiers,
			    String   className,
			    String   superclassName,
			    String[] interfaceNames,
			    String[] generalImports,
			    String[] specificImports )
    {
	this.packageName = packageName;
	this.modifiers = modifiers;
	this.className = className;
	this.superclassName = superclassName;
	this.interfaceNames = interfaceNames;
	this.generalImports = generalImports;
	this.specificImports = specificImports;
    }
}
