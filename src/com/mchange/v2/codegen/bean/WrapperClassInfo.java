package com.mchange.v2.codegen.bean;

public abstract class WrapperClassInfo implements ClassInfo
{
    ClassInfo inner;

    public WrapperClassInfo(ClassInfo info)
    { this.inner = info; }

    @Override
    public String   getPackageName() { return inner.getPackageName(); }
    @Override
    public int      getModifiers() { return inner.getModifiers(); }
    @Override
    public String   getClassName() { return inner.getClassName(); }
    @Override
    public String   getSuperclassName() { return inner.getSuperclassName(); }
    @Override
    public String[] getInterfaceNames() { return inner.getInterfaceNames(); }
    @Override
    public String[] getGeneralImports() { return inner.getGeneralImports(); }
    @Override
    public String[] getSpecificImports() { return inner.getSpecificImports(); }
}
