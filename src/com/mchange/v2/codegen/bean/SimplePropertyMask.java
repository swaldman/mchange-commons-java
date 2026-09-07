package com.mchange.v2.codegen.bean;

import java.lang.reflect.Modifier;

class SimplePropertyMask implements Property
{
    Property p;

    SimplePropertyMask(Property p)
    { this.p = p; }

    @Override
    public int getVariableModifiers()
    { return Modifier.PRIVATE; }

    @Override
    public String  getName()
    { return p.getName(); }

    @Override
    public String  getSimpleTypeName()
    { return p.getSimpleTypeName(); }

    @Override
    public String getDefensiveCopyExpression()
    { return null; }

    @Override
    public String getDefaultValueExpression()
    { return p.getDefaultValueExpression(); }

    @Override
    public int getGetterModifiers()
    { return Modifier.PUBLIC; }

    @Override
    public int getSetterModifiers()
    { return Modifier.PUBLIC; }

    @Override
    public boolean isReadOnly()
    { return false; }

    @Override
    public boolean isBound()
    { return false; }

    @Override
    public boolean isConstrained()
    { return false; }
}
