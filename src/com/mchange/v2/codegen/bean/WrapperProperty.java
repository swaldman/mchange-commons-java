package com.mchange.v2.codegen.bean;

import java.lang.reflect.Modifier;

public abstract class WrapperProperty implements Property
{
    Property p;

    public WrapperProperty(Property p)
    { this.p = p; }

    protected Property getInner()
    { return p; }

    @Override
    public int getVariableModifiers()
    { return p.getVariableModifiers(); }

    @Override
    public String  getName()
    { return p.getName(); }

    @Override
    public String  getSimpleTypeName()
    { return p.getSimpleTypeName(); }

    @Override
    public String getDefensiveCopyExpression()
    { return p.getDefensiveCopyExpression(); }

    @Override
    public String getDefaultValueExpression()
    { return p.getDefaultValueExpression(); }

    @Override
    public int getGetterModifiers()
    { return p.getGetterModifiers(); }

    @Override
    public int getSetterModifiers()
    { return p.getSetterModifiers(); }

    @Override
    public boolean isReadOnly()
    { return p.isReadOnly(); }

    @Override
    public boolean isBound()
    { return p.isBound(); }

    @Override
    public boolean isConstrained()
    { return p.isConstrained(); }
}
