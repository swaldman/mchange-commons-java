package com.mchange.v2.codegen.bean;

public interface Property
{
    public int     getVariableModifiers();
    public String  getName();
    public String  getSimpleTypeName();
    public String  getDefensiveCopyExpression();
    public String  getDefaultValueExpression();
    public int     getGetterModifiers();
    public int     getSetterModifiers();
    public boolean isReadOnly();
    public boolean isBound();
    public boolean isConstrained();

    //indexed properties not supported... use array or List valued props
}
