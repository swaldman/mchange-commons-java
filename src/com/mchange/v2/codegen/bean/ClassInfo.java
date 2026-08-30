package com.mchange.v2.codegen.bean;

public interface ClassInfo
{
    public String   getPackageName();
    public int      getModifiers();
    public String   getClassName();
    public String   getSuperclassName();
    public String[] getInterfaceNames();
    public String[] getGeneralImports();
    public String[] getSpecificImports();
}
