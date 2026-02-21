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

    public int     getVariableModifiers()       { return variable_modifiers; }
    public String  getName()                    { return name; }
    public String  getSimpleTypeName()          { return simpleTypeName; }
    public String  getDefensiveCopyExpression() { return defensiveCopyExpression; }
    public String  getDefaultValueExpression()  { return defaultValueExpression; }
    public int     getGetterModifiers()         { return getter_modifiers; }
    public int     getSetterModifiers()         { return setter_modifiers; }
    public boolean isReadOnly()                 { return is_read_only; }
    public boolean isBound()                    { return is_bound; }
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
