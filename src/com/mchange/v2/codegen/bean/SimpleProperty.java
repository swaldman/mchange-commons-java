package com.mchange.v2.codegen.bean;

import java.lang.reflect.Modifier;

public class SimpleProperty implements Property
{
    int     variable_modifiers;
    String  name;
    String  simpleTypeName;
    String  defensiveCopyExpression;
    String  defaultValueExpression;
    int     getter_modifiers;
    int     setter_modifiers;
    boolean is_read_only;
    boolean is_bound;
    boolean is_constrained;

    @Override
    public int     getVariableModifiers()       { return variable_modifiers; }
    @Override
    public String  getName()                    { return name; }
    @Override
    public String  getSimpleTypeName()          { return simpleTypeName; }
    @Override
    public String  getDefensiveCopyExpression() { return defensiveCopyExpression; }
    @Override
    public String  getDefaultValueExpression()  { return defaultValueExpression; }
    @Override
    public int     getGetterModifiers()         { return getter_modifiers; }
    @Override
    public int     getSetterModifiers()         { return setter_modifiers; }
    @Override
    public boolean isReadOnly()                 { return is_read_only; }
    @Override
    public boolean isBound()                    { return is_bound; }
    @Override
    public boolean isConstrained()              { return is_constrained; }

    public SimpleProperty( int     variable_modifiers,
			   String  name,
			   String  simpleTypeName,
			   String  defensiveCopyExpression,
			   String  defaultValueExpression,
			   int     getter_modifiers,
			   int     setter_modifiers,
			   boolean is_read_only,
			   boolean is_bound,
			   boolean is_constrained )
    {
	this.variable_modifiers = variable_modifiers;
	this.name = name;
	this.simpleTypeName = simpleTypeName;
	this.defensiveCopyExpression = defensiveCopyExpression;
	this.defaultValueExpression = defaultValueExpression;
	this.getter_modifiers = getter_modifiers;
	this.setter_modifiers = setter_modifiers;
	this.is_read_only = is_read_only;
	this.is_bound = is_bound;
	this.is_constrained = is_constrained;
    }

    public SimpleProperty( String  name,
			   String  simpleTypeName,
			   String  defensiveCopyExpression,
			   String  defaultValueExpression,
			   boolean is_read_only,
			   boolean is_bound,
			   boolean is_constrained )
    {
	this ( Modifier.PRIVATE,
	       name,
	       simpleTypeName,
	       defensiveCopyExpression,
	       defaultValueExpression,
	       Modifier.PUBLIC,
	       Modifier.PUBLIC,
	       is_read_only,
	       is_bound,
	       is_constrained );
    }
}
