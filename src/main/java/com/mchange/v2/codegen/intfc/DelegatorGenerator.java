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

package com.mchange.v2.codegen.intfc;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import com.mchange.v2.codegen.*;
import com.mchange.v1.lang.ClassUtils;

public class DelegatorGenerator
{
    int class_modifiers         = Modifier.PUBLIC | Modifier.ABSTRACT;
    int method_modifiers        = Modifier.PUBLIC;
    int wrapping_ctor_modifiers = Modifier.PUBLIC;
    int default_ctor_modifiers  = Modifier.PUBLIC;
    boolean wrapping_constructor = true;
    boolean default_constructor  = true;
    boolean inner_getter         = true;
    boolean inner_setter         = true;

    Class   superclass           = null;
    Class[] extraInterfaces      = null;

    // A rarely used feature, see below
    Method[]                   reflectiveDelegateMethods  = null;  //by default, none of this
    ReflectiveDelegationPolicy reflectiveDelegationPolicy = ReflectiveDelegationPolicy.USE_MAIN_DELEGATE_INTERFACE;

    final static Comparator classComp = new Comparator()
    {
       public int compare(Object a, Object b)
       { return ((Class) a).getName().compareTo(((Class) b).getName()); }
    };

    public void setGenerateInnerSetter( boolean b )
    { this.inner_setter = b; }

    public boolean isGenerateInnerSetter()
    { return inner_setter; }

    public void setGenerateInnerGetter( boolean b )
    { this.inner_getter = b; }

    public boolean isGenerateInnerGetter()
    { return inner_getter; }

    public void setGenerateNoArgConstructor( boolean b )
    { this.default_constructor = b; }

    public boolean isGenerateNoArgConstructor()
    { return default_constructor; }

    public void setGenerateWrappingConstructor( boolean b )
    { this.wrapping_constructor = b; }

    public boolean isGenerateWrappingConstructor()
    { return wrapping_constructor; }

    public void setWrappingConstructorModifiers( int modifiers )
    { this.wrapping_ctor_modifiers = modifiers; }

    public int getWrappingConstructorModifiers()
    { return wrapping_ctor_modifiers; }

    public void setNoArgConstructorModifiers( int modifiers )
    { this.default_ctor_modifiers = modifiers; }

    public int getNoArgConstructorModifiers()
    { return default_ctor_modifiers; }

    public void setMethodModifiers( int modifiers )
    { this.method_modifiers = modifiers; }

    public int getMethodModifiers()
    { return method_modifiers; }

    public void setClassModifiers( int modifiers )
    { this.class_modifiers = modifiers; }

    public int getClassModifiers()
    { return class_modifiers; }

    public void setSuperclass( Class superclass )
    { this.superclass = superclass; }

    public Class getSuperclass()
    { return superclass; }

    public void setExtraInterfaces( Class[] extraInterfaces )
    { this.extraInterfaces = extraInterfaces; }

    public Class[] getExtraInterfaces()
    { return extraInterfaces; }

    public Method[] getReflectiveDelegateMethods()
    { return reflectiveDelegateMethods; }

    /**
     *  Reflectively delegated methods are methods that are not declared in the interface at
     *  build time, but that should reflectively be forwarded at runtime to the inner delegate.
     *  This permits support of public methods not exposed via the interface, or support of
     *  methods added to versions of the interface newer than the build version.
     *
     *  Note that the declaring class of these methods is simply ignored. Methods will ve
     *  delegated solely by name and parameter.
     */
    public void setReflectiveDelegateMethods(Method[] reflectiveDelegateMethods)
    { this.reflectiveDelegateMethods = reflectiveDelegateMethods; }

    public ReflectiveDelegationPolicy getReflectiveDelegationPolicy()
    { return reflectiveDelegationPolicy; }

    /**
     *  If ReflectiveDelegationPolicy.USE_MAIN_DELEGATE_INTERFACE, delegate via the same interface we are generating methods against.
     *  (This is useful for supporting methods in versions of the interface with methods that don't appear in the version we are generating against.)
     *
     *  If ReflectiveDelegationPolicy.USE_RUNTIME_CLASS, delegate via the runtime class of the delegate. (This is useful if
     *  the methods come from multiple interfaces, or we want to be able to forward to methods of the delegate class not captured
     *  by an interface.
     *
     *  Otherwise, use the delegateClass set in the constructor of ReflectiveDelegationPolicy.
     *
     *  Note that if the delegate class is not public or otherwise accessible to the generated proxy, IllegalAccessExceptions may ensue.
     */
    public void setReflectiveDelegationPolicy(ReflectiveDelegationPolicy reflectiveDelegationPolicy)
    { this.reflectiveDelegationPolicy = reflectiveDelegationPolicy; }

    // public boolean isDelegateViaRuntimeClass()
    // { return delegate_via_runtime_class; }

    // /**
    //  * If true, reflective delegate methods are reflected via the runtime Class of the delegate object,
    //  * rather than via an interface. Nice because the runtime class hopefully supports all the reflective
    //  * delegates. Not so nice because the runtime class may not be accessible, so reflection may fail
    //  * with IllegalAccessExceptions.
    //  */
    // public void setDelegateRuntimeClass( boolean delegate_via_runtime_class )
    // { this.delegate_via_runtime_class = delegate_via_runtime_class; }

    public void writeDelegator(Class intfcl, String genclass, Writer w) throws IOException
    {
	IndentedWriter iw = CodegenUtils.toIndentedWriter(w);
	
	String   pkg      = genclass.substring(0, genclass.lastIndexOf('.'));
	String   sgc      = CodegenUtils.fqcnLastElement( genclass );
	String   scn      = (superclass != null ? ClassUtils.simpleClassName( superclass ) : null);
	String   sin      = ClassUtils.simpleClassName( intfcl );
	String[] eins     = null;
	if (extraInterfaces != null)
	    {
		eins = new String[ extraInterfaces.length ];
		for (int i = 0, len = extraInterfaces.length; i < len; ++i)
		    eins[i] = ClassUtils.simpleClassName( extraInterfaces[i] );
	    }

	Set    imports  = new TreeSet( classComp );
	
	Method[] methods = intfcl.getMethods();
	
	//TODO: don't add array classes!
	//build import set
	if (! CodegenUtils.inSamePackage( intfcl.getName(), genclass ) )
	    imports.add( intfcl );
	if (superclass != null && ! CodegenUtils.inSamePackage( superclass.getName(), genclass ) )
	    imports.add( superclass );
	if (extraInterfaces != null)
	    {
		for (int i = 0, len = extraInterfaces.length; i < len; ++i)
		    {
			Class checkMe = extraInterfaces[i];
			if (! CodegenUtils.inSamePackage( checkMe.getName(), genclass ) )
			    imports.add( checkMe );
		    }
	    }

	ensureImports(genclass, imports, methods );
	
	if ( reflectiveDelegateMethods != null )
	    ensureImports(genclass, imports, reflectiveDelegateMethods );

	if ( reflectiveDelegationPolicy.delegateClass != null && !CodegenUtils.inSamePackage( reflectiveDelegationPolicy.delegateClass.getName(), genclass ) )
	    imports.add( reflectiveDelegationPolicy.delegateClass );	   

	generateBannerComment( iw );
	iw.println("package " + pkg + ';');
	iw.println();
	for (Iterator ii = imports.iterator(); ii.hasNext(); )
	    iw.println("import "+ ((Class) ii.next()).getName() + ';');
	generateExtraImports( iw );
	iw.println();
	generateClassJavaDocComment( iw );
	iw.print(CodegenUtils.getModifierString( class_modifiers ) + " class " + sgc);
	if (superclass != null)
	    iw.print(" extends " + scn);
	iw.print(" implements " + sin);
	if (eins != null)
	    for (int i = 0, len = eins.length; i < len; ++i)
		iw.print(", " + eins[i]);
	iw.println();
	iw.println("{");
	iw.upIndent();

	iw.println("protected " + sin + " inner;");
	iw.println();

	if (reflectiveDelegateMethods != null)
	    iw.println("protected Class __delegateClass = null;");
	iw.println();

	iw.println("private void __setInner( " + sin + " inner )");
	iw.println("{");
	iw.upIndent();
	iw.println("this.inner = inner;");
	if (reflectiveDelegateMethods != null)
	{
	    String delegateClassExpr;

	    if ( reflectiveDelegationPolicy == ReflectiveDelegationPolicy.USE_MAIN_DELEGATE_INTERFACE )
		delegateClassExpr = sin + ".class";
	    else if ( reflectiveDelegationPolicy == ReflectiveDelegationPolicy.USE_RUNTIME_CLASS )
		delegateClassExpr = "inner.getClass()";
	    else
		delegateClassExpr = ClassUtils.simpleClassName( reflectiveDelegationPolicy.delegateClass ) + ".class";
	        
	    iw.println("this.__delegateClass = inner == null ? null : " + delegateClassExpr + ";");
        }
	iw.downIndent();
	iw.println("}");
	iw.println();
	
	if ( wrapping_constructor )
	    {
		//System.err.println("WRAPPING CTOR MODIFIERS: " + CodegenUtils.getModifierString( wrapping_ctor_modifiers ) + " (intval: " + wrapping_ctor_modifiers + ")");
		iw.println(CodegenUtils.getModifierString( wrapping_ctor_modifiers ) + ' ' + sgc + '(' + sin + " inner)");
		iw.println("{ __setInner( inner ); }");
	    }

	if (default_constructor)
	    {
		iw.println();
		iw.println(CodegenUtils.getModifierString( default_ctor_modifiers ) + ' ' + sgc + "()");
		iw.println("{}");
	    }

	if (inner_setter)
	    {
		iw.println();
		iw.println( CodegenUtils.getModifierString( method_modifiers ) + " void setInner( " + sin + " inner )");
		iw.println( "{ __setInner( inner ); }" );
	    }
	if (inner_getter)
	    {
		iw.println();
		iw.println( CodegenUtils.getModifierString( method_modifiers ) + ' ' + sin + " getInner()");
		iw.println( "{ return inner; }" );
	    }
	iw.println();
	for (int i = 0, len = methods.length; i < len; ++i)
	    {
		Method method  = methods[i];

		if (i != 0) iw.println();
		iw.println( CodegenUtils.methodSignature( method_modifiers, method, null ) );
		iw.println("{");
		iw.upIndent();

		generatePreDelegateCode( intfcl, genclass, method, iw );
		generateDelegateCode( intfcl, genclass, method, iw );
		generatePostDelegateCode( intfcl, genclass, method, iw );
	    
		iw.downIndent();
		iw.println("}");
	    }
	
	if ( reflectiveDelegateMethods != null )
	{
	    iw.println("// Methods not in core interface to be delegated via reflection");
	    for (int i = 0, len = reflectiveDelegateMethods.length; i < len; ++i)
	    {
		Method method  = reflectiveDelegateMethods[i];

		if (i != 0) iw.println();
		iw.println( CodegenUtils.methodSignature( method_modifiers, method, null ) );
		iw.println("{");
		iw.upIndent();

		generatePreDelegateCode( intfcl, genclass, method, iw );
		generateReflectiveDelegateCode( intfcl, genclass, method, iw );
		generatePostDelegateCode( intfcl, genclass, method, iw );
	    
		iw.downIndent();
		iw.println("}");
	    }
	}

	iw.println();
	generateExtraDeclarations( intfcl, genclass, iw );

	iw.downIndent();
    	iw.println("}");
    }

    private void ensureImports(String genclass, Set imports, Method[] methods )
    {
	for (int i = 0, len = methods.length; i < len; ++i)
	{
	    Class[] args = methods[i].getParameterTypes();
	    for (int j = 0, jlen = args.length; j < jlen; ++j)
		{
		    if (! CodegenUtils.inSamePackage( args[j].getName(), genclass ) )
			imports.add( CodegenUtils.unarrayClass( args[j] ) );
		}       
	    Class[] excClasses = methods[i].getExceptionTypes();
	    for (int j = 0, jlen = excClasses.length; j < jlen; ++j)
		{
		    if (! CodegenUtils.inSamePackage( excClasses[j].getName(), genclass ) )
			{
			    //System.err.println("Adding exception type: " + excClasses[j]);
			    imports.add( CodegenUtils.unarrayClass( excClasses[j] ) );
			}
		}       
	    if (! CodegenUtils.inSamePackage( methods[i].getReturnType().getName(), genclass ) )
		imports.add( CodegenUtils.unarrayClass( methods[i].getReturnType() ) );
	}
    }

    protected void generateDelegateCode( Class intfcl, String genclass, Method method, IndentedWriter iw ) throws IOException 
    {
	Class  retType = method.getReturnType();
	
	iw.println( (retType == void.class ? "" : "return " ) + "inner." + CodegenUtils.methodCall( method ) + ";" );
    }

    protected void generateReflectiveDelegateCode( Class intfcl, String genclass, Method method, IndentedWriter iw ) throws IOException 
    {
	Class  retType = method.getReturnType();

	String paramTypesArrayStr = CodegenUtils.reflectiveMethodParameterTypeArray( method );
	String argArrayStr = CodegenUtils.reflectiveMethodObjectArray( method );

	Class[] exceptionsArray = method.getExceptionTypes();
	Set exceptionsSet = new HashSet();
	exceptionsSet.addAll( Arrays.asList( exceptionsArray ) );

	iw.println("try");
	iw.println("{");
	iw.upIndent();
	iw.println("Method m = __delegateClass.getMethod(\042" + method.getName() + "\042, " + paramTypesArrayStr + ");");
	iw.println( (retType == void.class ? "" : "return (" + ClassUtils.simpleClassName( retType ) + ") ") + 
		    "m.invoke( inner, " + argArrayStr + " );" );
	iw.downIndent();
	iw.println("}");
	if (! exceptionsSet.contains( IllegalAccessException.class ) )
	{
	    iw.println("catch (IllegalAccessException iae)");
	    iw.println("{");
	    iw.upIndent();
	    iw.println( "throw new RuntimeException(\042A reflectively delegated method '" +
			method.getName() +
			"' cannot access the object to which the call is delegated\042, iae);" );
	    iw.downIndent();
	    iw.println("}");
	}
	iw.println("catch (InvocationTargetException ite)");
	iw.println("{");
	iw.upIndent();
	iw.println("Throwable cause = ite.getCause();");
	iw.println("if (cause instanceof RuntimeException) throw (RuntimeException) cause;");
	iw.println("if (cause instanceof Error) throw (Error) cause;");
	int len = exceptionsArray.length;
	if (len > 0)
	{
	    for (int i = 0; i < len; ++i)
	    {
		String ecn = ClassUtils.simpleClassName( exceptionsArray[i] );
		iw.println("if (cause instanceof " + ecn + ") throw (" + ecn + ") cause;");
	    }
	}
	iw.println( "throw new RuntimeException(\042Target of reflectively delegated method '" + method.getName() + "' threw an Exception.\042, cause);" );
	iw.downIndent();
	iw.println("}");
    }

    protected void generateBannerComment( IndentedWriter iw ) throws IOException 
    {
	iw.println("/*");
	iw.println(" * This class generated by " + this.getClass().getName());
	iw.println(" * " + new Date());
	iw.println(" * DO NOT HAND EDIT!!!!");
	iw.println(" */");
    }

    protected void generateClassJavaDocComment( IndentedWriter iw ) throws IOException
    {
	iw.println("/**");
	iw.println(" * This class was generated by " + this.getClass().getName() + ".");
	iw.println(" */");
    }

    protected void generateExtraImports( IndentedWriter iw ) throws IOException {}
    protected void generatePreDelegateCode( Class intfcl, String genclass, Method method, IndentedWriter iw ) throws IOException {}
    protected void generatePostDelegateCode( Class intfcl, String genclass, Method method, IndentedWriter iw ) throws IOException {}
    protected void generateExtraDeclarations( Class intfcl, String genclass, IndentedWriter iw ) throws IOException {}
}
