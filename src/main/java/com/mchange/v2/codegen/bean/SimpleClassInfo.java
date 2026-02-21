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

    public String   getPackageName()          { return packageName; }
    public int      getModifiers()            { return modifiers; }
    public String   getClassName()            { return className; }
    public String   getSuperclassName()       { return superclassName; }
    public String[] getInterfaceNames()       { return interfaceNames; }
    public String[] getGeneralImports()       { return generalImports; }
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
