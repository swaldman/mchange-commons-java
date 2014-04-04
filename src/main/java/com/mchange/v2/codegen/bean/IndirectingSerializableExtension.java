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

import java.util.*;
import java.io.Serializable;
import java.io.IOException;
import com.mchange.v2.codegen.IndentedWriter;
import com.mchange.v2.ser.IndirectPolicy;

public class IndirectingSerializableExtension extends SerializableExtension
{
    protected String findIndirectorExpr;
    protected String indirectorClassName;

    /**
     * We expect this indirector to be a public class with a public no_arg ctor;
     * If you need the indirector initialized somehow, you'll have to extend
     * the class.
     *
     * @see #writeInitializeIndirector
     * @see #writeExtraDeclarations
     */
    public IndirectingSerializableExtension( String indirectorClassName )
    { 
	this.indirectorClassName = indirectorClassName;
	this.findIndirectorExpr = "new " + indirectorClassName + "()";
    }

    protected IndirectingSerializableExtension()
    {}

    public Collection extraSpecificImports()
    {
	Collection col = super.extraSpecificImports();
	col.add( indirectorClassName );
	col.add( "com.mchange.v2.ser.IndirectlySerialized" );
	col.add( "com.mchange.v2.ser.Indirector" );
	col.add( "com.mchange.v2.ser.SerializableUtils" );
	col.add( "java.io.NotSerializableException" );
	return col;
    }

    protected IndirectPolicy indirectingPolicy( Property prop, Class propType )
    {
	if (Serializable.class.isAssignableFrom( propType ))
	    return IndirectPolicy.DEFINITELY_DIRECT;
	else
	    return IndirectPolicy.INDIRECT_ON_EXCEPTION;
    }

    /**
     * hook method... does nothing by default... override at will.
     * The indirector will be called, uh, "indirector".
     * You are in the middle of a method when you define this.
     */
    protected void writeInitializeIndirector( Property prop, Class propType, IndentedWriter iw ) throws IOException
    {}

    protected void writeExtraDeclarations(ClassInfo info, Class superclassType, Property[] props, Class[] propTypes, IndentedWriter iw)
	throws IOException
    {}

    public void generate(ClassInfo info, Class superclassType, Property[] props, Class[] propTypes, IndentedWriter iw)
	throws IOException
    {
	super.generate( info, superclassType, props, propTypes, iw);
	writeExtraDeclarations( info, superclassType, props, propTypes, iw);
    }

    protected void writeStoreObject( Property prop, Class propType, IndentedWriter iw ) throws IOException
    {
	IndirectPolicy policy = indirectingPolicy( prop, propType );
	if (policy == IndirectPolicy.DEFINITELY_INDIRECT)
	    writeIndirectStoreObject( prop, propType, iw );
	else if (policy == IndirectPolicy.INDIRECT_ON_EXCEPTION)
	    {
		iw.println("try");
		iw.println("{");
		iw.upIndent();
		iw.println("//test serialize");
		iw.println("SerializableUtils.toByteArray(" + prop.getName() + ");");
		super.writeStoreObject( prop, propType, iw );
		iw.downIndent();
		iw.println("}");
		iw.println("catch (NotSerializableException nse)");
		iw.println("{");
		iw.upIndent();
		writeIndirectStoreObject( prop, propType, iw );
		iw.downIndent();
		iw.println("}");
	    }
	else if (policy == IndirectPolicy.DEFINITELY_DIRECT)
	    super.writeStoreObject( prop, propType, iw );
	else
	    throw new InternalError("indirectingPolicy() overridden to return unknown policy: " + policy);
    }

    protected void writeIndirectStoreObject( Property prop, Class propType, IndentedWriter iw ) throws IOException
    {
	iw.println("try");
	iw.println("{");
	iw.upIndent();

	iw.println("Indirector indirector = " + findIndirectorExpr + ';');
	writeInitializeIndirector( prop, propType, iw );
	iw.println("oos.writeObject( indirector.indirectForm( " + prop.getName() + " ) );");

	iw.downIndent();
	iw.println("}");
	iw.println("catch (IOException indirectionIOException)");
	iw.println("{ throw indirectionIOException; }");
	iw.println("catch (Exception indirectionOtherException)");
	iw.println("{ throw new IOException(\"Problem indirectly serializing " + prop.getName() + ": \" + indirectionOtherException.toString() ); }");
    }

    protected void writeUnstoreObject( Property prop, Class propType, IndentedWriter iw ) throws IOException
    {
	IndirectPolicy policy = indirectingPolicy( prop, propType );
	if (policy == IndirectPolicy.DEFINITELY_INDIRECT || policy == IndirectPolicy.INDIRECT_ON_EXCEPTION)
	    {
		iw.println("// we create an artificial scope so that we can use the name o for all indirectly serialized objects.");
		iw.println("{");
		iw.upIndent();
		iw.println("Object o = ois.readObject();");
		iw.println("if (o instanceof IndirectlySerialized) o = ((IndirectlySerialized) o).getObject();");
		iw.println("this." + prop.getName() + " = (" + prop.getSimpleTypeName() + ") o;");
		iw.downIndent();
		iw.println("}");
	    }
	else if (policy == IndirectPolicy.DEFINITELY_DIRECT)
	    super.writeUnstoreObject( prop, propType, iw );
	else
	    throw new InternalError("indirectingPolicy() overridden to return unknown policy: " + policy);
    }

}
