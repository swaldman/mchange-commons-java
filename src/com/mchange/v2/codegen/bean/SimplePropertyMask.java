package com.mchange.v2.codegen.bean;

import java.lang.reflect.Modifier;

class SimplePropertyMask implements Property
{
    Property p;

    SimplePropertyMask(Property p)
    { this.p = p; }

    public int getVariableModifiers()
    { return Modifier.PRIVATE; }

    public String  getName()
    { return p.getName(); }

    public String  getSimpleTypeName()
    { return p.getSimpleTypeName(); }

    public String getDefensiveCopyExpression()
    { return null; }

    public String getDefaultValueExpression()
    { return p.getDefaultValueExpression(); }

    public int getGetterModifiers()
    { return Modifier.PUBLIC; }

    public int getSetterModifiers()
    { return Modifier.PUBLIC; }

    public boolean isReadOnly()
    { return false; }

    public boolean isBound()
    { return false; }

    public boolean isConstrained()
    { return false; }
}
