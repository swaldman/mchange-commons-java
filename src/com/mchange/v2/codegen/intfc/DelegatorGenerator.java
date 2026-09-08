package com.mchange.v2.codegen.intfc;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import com.mchange.v2.codegen.*;
import com.mchange.v2.io.IndentedWriter;
import com.mchange.v1.lang.ClassUtils;

import static com.mchange.v2.codegen.CodegenUtils.METHOD_COMPARATOR;

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

    // at most one of superclass and superclass by name should be set
    Class<?>  superclass = null;
    String superclassByName = null;

    Class<?>[] extraInterfaces = null;

    // A rarely used feature, see below
    Method[]                   reflectiveDelegateMethods  = null;  //by default, none of this
    ReflectiveDelegationPolicy reflectiveDelegationPolicy = ReflectiveDelegationPolicy.USE_MAIN_DELEGATE_INTERFACE;

    final static Comparator<Class<?>> classComp = new Comparator<Class<?>>()
    {
       @Override
       public int compare(Class<?> a, Class<?> b)
       { return a.getName().compareTo(b.getName()); }
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

    public void setSuperclass( Class<?> superclass )
    { this.superclass = superclass; }

    public Class<?> getSuperclass()
    { return superclass; }

    /** can be a simple name of FQCN, but if a simple name, you may need to override generateExtraImports */
    public void setSuperclassByName( String superclassByName )
    { this.superclassByName = superclassByName; }

    public String getSuperclassByName()
    { return superclassByName; }

    public void setExtraInterfaces( Class<?>[] extraInterfaces )
    { this.extraInterfaces = extraInterfaces; }

    public Class<?>[] getExtraInterfaces()
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

    public void writeDelegator(Class<?> intfcl, String genclass, Writer w) throws IOException
    {
        if (superclass != null && superclassByName != null)
            throw new IllegalStateException("A delegator generator may specify a superclass by class or by name, but not both! superclass: " + superclass + "; superclassByName: " + superclassByName);

	IndentedWriter iw = CodegenUtils.toIndentedWriter(w);

	String   pkg      = genclass.substring(0, genclass.lastIndexOf('.'));
	String   sgc      = CodegenUtils.fqcnLastElement( genclass );
	String   scn      = (superclass != null ? ClassUtils.simpleClassName( superclass ) : superclassByName); // may be, often is null!
	String   sin      = ClassUtils.simpleClassName( intfcl );
	String[] eins     = null;
	if (extraInterfaces != null)
	    {
		eins = new String[ extraInterfaces.length ];
		for (int i = 0, len = extraInterfaces.length; i < len; ++i)
		    eins[i] = ClassUtils.simpleClassName( extraInterfaces[i] );
	    }

	Set<Class<?>> imports  = new TreeSet<Class<?>>( classComp );

	Method[] methods = intfcl.getMethods();
        Arrays.sort( methods, METHOD_COMPARATOR );

        Method[] sortedReflectiveDelegateMethods = null;
	if ( reflectiveDelegateMethods != null )
        {
            sortedReflectiveDelegateMethods = reflectiveDelegateMethods.clone();
            Arrays.sort( sortedReflectiveDelegateMethods, METHOD_COMPARATOR );
        }


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
			Class<?> checkMe = extraInterfaces[i];
			if (! CodegenUtils.inSamePackage( checkMe.getName(), genclass ) )
			    imports.add( checkMe );
		    }
	    }

	ensureImports(genclass, imports, methods );

	if ( sortedReflectiveDelegateMethods != null )
	    ensureImports(genclass, imports, sortedReflectiveDelegateMethods );

	if ( reflectiveDelegationPolicy.delegateClass != null && !CodegenUtils.inSamePackage( reflectiveDelegationPolicy.delegateClass.getName(), genclass ) )
	    imports.add( reflectiveDelegationPolicy.delegateClass );

	generateBannerComment( iw );
	iw.println("package " + pkg + ';');
	iw.println();
	for (Class<?> imported : imports)
	    iw.println("import "+ imported.getName() + ';');
	generateExtraImports( iw );
	iw.println();
	generateClassJavaDocComment( iw );
	iw.print(CodegenUtils.getModifierString( class_modifiers ) + " class " + sgc);
	if (scn != null)
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

	if (sortedReflectiveDelegateMethods != null)
	    iw.println("protected Class<?> __delegateClass = null;");
	iw.println();

	iw.println("private void __setInner( " + sin + " inner )");
	iw.println("{");
	iw.upIndent();
	iw.println("this.inner = inner;");
	if (sortedReflectiveDelegateMethods != null)
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
                generateFullDelegateMethod( intfcl, genclass, method, iw );
	    }

	if ( sortedReflectiveDelegateMethods != null )
	{
	    iw.println("// Methods not in core interface to be delegated via reflection");
	    for (int i = 0, len = sortedReflectiveDelegateMethods.length; i < len; ++i)
	    {
		Method method  = sortedReflectiveDelegateMethods[i];

		if (i != 0) iw.println();
		// the reflective path can only cast Object to a method type variable unchecked
		if ( CodegenUtils.mentionsMethodTypeVariable( method.getGenericReturnType() ) )
		    iw.println("@SuppressWarnings(\042unchecked\042)");
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

    protected void generateFullDelegateMethod(Class<?> intfcl, String genclass, Method method, IndentedWriter iw) throws IOException
    {
        generateMethodAnnotations( intfcl, genclass, method, iw );
        iw.println( CodegenUtils.methodSignature( method_modifiers, method, null ) );
        iw.println("{");
        iw.upIndent();

        generatePreDelegateCode( intfcl, genclass, method, iw );
        generateDelegateCode( intfcl, genclass, method, iw );
        generatePostDelegateCode( intfcl, genclass, method, iw );

        iw.downIndent();
        iw.println("}");
    }

    /**
     *  Every method generated by the main loop implements a method of the delegate
     *  interface, so all of them take {@literal @}Override -- which is what makes a
     *  later drift in that interface a compile error here rather than a silent
     *  overload. Deprecation is carried through from the interface so that callers of
     *  the delegator are warned exactly as callers of the delegate would be.
     */
    protected void generateMethodAnnotations(Class<?> intfcl, String genclass, Method method, IndentedWriter iw) throws IOException
    {
        iw.println("@Override");
        if ( method.isAnnotationPresent( Deprecated.class ) )
            iw.println("@Deprecated");
    }

    private void ensureImports(String genclass, Set<Class<?>> imports, Method[] methods )
    {
	// Collect from the generic types, not their erasures: signatures are generated
	// generically, so a type argument like the Class<?> in Map<String,Class<?>> must
	// be importable too.
	Set<Class<?>> named = new HashSet<Class<?>>();
	for (int i = 0, len = methods.length; i < len; ++i)
	{
	    for ( Type arg : methods[i].getGenericParameterTypes() )
		CodegenUtils.collectNamedClasses( arg, named );
	    for ( Type exc : methods[i].getGenericExceptionTypes() )
		CodegenUtils.collectNamedClasses( exc, named );
	    CodegenUtils.collectNamedClasses( methods[i].getGenericReturnType(), named );
	    for ( TypeVariable<Method> tv : methods[i].getTypeParameters() )
		for ( Type bound : tv.getBounds() )
		    if ( bound != Object.class )
			CodegenUtils.collectNamedClasses( bound, named );
	}
	for ( Class<?> checkMe : named )
	    if (! CodegenUtils.inSamePackage( checkMe.getName(), genclass ) )
		imports.add( checkMe );
    }

    protected void generateDelegateCode( Class<?> intfcl, String genclass, Method method, IndentedWriter iw ) throws IOException
    {
	Class<?>  retType = method.getReturnType();

	iw.println( (retType == void.class ? "" : "return " ) + "inner." + CodegenUtils.methodCall( method ) + ";" );
    }

    protected void generateReflectiveDelegateCode( Class<?> intfcl, String genclass, Method method, IndentedWriter iw ) throws IOException
    {
	Class<?>  retType = method.getReturnType();

	String paramTypesArrayStr = CodegenUtils.reflectiveMethodParameterTypeArray( method );
	String argArrayStr = CodegenUtils.reflectiveMethodObjectArray( method );

	Class<?>[] exceptionsArray = method.getExceptionTypes();
	Set<Class<?>> exceptionsSet = new HashSet<Class<?>>();
	exceptionsSet.addAll( Arrays.asList( exceptionsArray ) );

	iw.println("try");
	iw.println("{");
	iw.upIndent();
	iw.println("Method m = __delegateClass.getMethod(\042" + method.getName() + "\042, " + paramTypesArrayStr + ");");
	iw.println( (retType == void.class ? "" : "return (" + CodegenUtils.typeString( method.getGenericReturnType() ) + ") ") +
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
        // support deterministic builds, see https://reproducible-builds.org/docs/source-date-epoch/
        String sde = System.getenv("SOURCE_DATE_EPOCH");
        Date timestamp;
        if (sde == null)
            timestamp = new Date();
        else
            timestamp = new Date( Long.parseLong(sde) * 1000 );

	iw.println("/*");
	iw.println(" * This class generated by " + this.getClass().getName());
	iw.println(" * " + timestamp);
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
    protected void generatePreDelegateCode( Class<?> intfcl, String genclass, Method method, IndentedWriter iw ) throws IOException {}
    protected void generatePostDelegateCode( Class<?> intfcl, String genclass, Method method, IndentedWriter iw ) throws IOException {}
    protected void generateExtraDeclarations( Class<?> intfcl, String genclass, IndentedWriter iw ) throws IOException {}
}
