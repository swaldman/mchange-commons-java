package com.mchange.v2.codegen.bean;

import java.lang.reflect.Modifier;

public abstract class WrapperProperty implements Property
{
    Property p;

    public WrapperProperty(Property p)
    { this.p = p; }

    protected Property getInner()
    { return p; }

    public int getVariableModifiers()
    { return p.getVariableModifiers(); }

    public String  getName()
    { return p.getName(); }

    public String  getSimpleTypeName()
    { return p.getSimpleTypeName(); }

    public String getDefensiveCopyExpression()
    { return p.getDefensiveCopyExpression(); }

    public String getDefaultValueExpression()
    { return p.getDefaultValueExpression(); }

    public int getGetterModifiers()
    { return p.getGetterModifiers(); }

    public int getSetterModifiers()
    { return p.getSetterModifiers(); }

    public boolean isReadOnly()
    { return p.isReadOnly(); }

    public boolean isBound()
    { return p.isBound(); }

    public boolean isConstrained()
    { return p.isConstrained(); }
}
