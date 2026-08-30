package com.mchange.v2.codegen.bean;

public abstract class WrapperClassInfo implements ClassInfo
{
    ClassInfo inner;

    public WrapperClassInfo(ClassInfo info)
    { this.inner = info; }

    public String   getPackageName() { return inner.getPackageName(); }
    public int      getModifiers() { return inner.getModifiers(); }
    public String   getClassName() { return inner.getClassName(); }
    public String   getSuperclassName() { return inner.getSuperclassName(); }
    public String[] getInterfaceNames() { return inner.getInterfaceNames(); }
    public String[] getGeneralImports() { return inner.getGeneralImports(); }
    public String[] getSpecificImports() { return inner.getSpecificImports(); }
}
