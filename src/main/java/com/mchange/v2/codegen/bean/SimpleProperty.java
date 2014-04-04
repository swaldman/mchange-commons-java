/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
 */

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
