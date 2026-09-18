package com.mchange.v2.naming;

import java.net.*;
import java.util.*;
import javax.naming.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import com.mchange.v2.cfg.PropertiesConfig;
import com.mchange.v2.cfg.WhitelistInfo;
import com.mchange.v2.cfg.WhitelistManager;
import com.mchange.v2.log.MLevel;
import com.mchange.v2.log.MLog;
import com.mchange.v2.log.MLogger;
import com.mchange.v2.util.IterableUtils;
import javax.naming.spi.ObjectFactory;

import static com.mchange.v2.cfg.PropertiesConfigUtils.securitySensitiveFalseBiasedLookupSyspropsPropertiesConfig;

public final class ReferenceableUtils
{
    final static MLogger logger = MLog.getLogger( ReferenceableUtils.class );

    /* don't worry -- References can have duplicate RefAddrs (I think!) */
    final static String REFADDR_VERSION                = "version";
    final static String REFADDR_CLASSNAME              = "classname";
    final static String REFADDR_FACTORY                = "factory";
    final static String REFADDR_FACTORY_CLASS_LOCATION = "factoryClassLocation";
    final static String REFADDR_SIZE                   = "size";

    final static int CURRENT_REF_VERSION = 1;

    // This set is a special case, a token.
    //
    // Semantically, it does not mean empty set (which would imply nothing passes the white list),
    // but it means NO WHITELIST, any factoryClassName is accepted.
    //
    // It is and must be tested by reference identity, not semantic equality
    public final static Set<String> ALL_FACTORY_CLASS_NAMES = Collections.unmodifiableSet(new HashSet<String>());

    private final static Set<String> ACCEPT_ANY_WHITELIST;

    private final static Set<WhitelistInfo.Source> ACCEPTABLE_WHITELIST_SOURCES;

    static
    {
        Set<String> tmp0 = new HashSet<String>();
        tmp0.add("*");
        ACCEPT_ANY_WHITELIST = Collections.unmodifiableSet(tmp0);

        Set<WhitelistInfo.Source> tmp1 = new HashSet<WhitelistInfo.Source>();
        tmp1.add(WhitelistInfo.Source.MAIN_WHITELIST);
        tmp1.add(WhitelistInfo.Source.OVERRIDE);
        tmp1.add(WhitelistInfo.Source.DEPRECATED);
        ACCEPTABLE_WHITELIST_SOURCES = Collections.unmodifiableSet(tmp1);
    }

    private final static WhitelistManager objectFactoryWhitelistManager = new WhitelistManager( SecurityConfigKey.OBJECT_FACTORY_BASE_KEY, SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
    private final static WhitelistManager referenceableJavaBeanClassWhitelistManager = new WhitelistManager( SecurityConfigKey.REFERENCEABLE_JAVA_BEAN_CLASS_BASE_KEY, SecurityConfigKey.REFERENCEABLE_JAVA_BEAN_CLASS_WHITELIST );

    /**
     * A null string value in a Reference sometimes goes to the literal
     * "null". Sigh. We convert this string to a Java null.
     */
    public static String literalNullToNull( String s )
    {
	if (s == null || "null".equals( s ))
	    return null;
	else
	    return s;
    }

    public static Object referenceToObject( Reference ref, Name name, Context nameCtx, Hashtable<?,?> env )
	throws NamingException
    { return referenceToObject( ref, name, nameCtx, env, (PropertiesConfig) null ); }

    public static Object referenceToObject( Reference ref, Name name, Context nameCtx, Hashtable<?,?> env, PropertiesConfig pcfg )
	throws NamingException
    {
        Set<String> allowedFactoryClassNames = findMandatoryObjectFactoryWhitelist( pcfg );
        return referenceToObject( ref, name, nameCtx, env, allowedFactoryClassNames, pcfg );
    }

    /**
     * The allowedFactoryClassNames whitelist test can be (but generally should not be) circumvented by explicitly providing ReferenceUtils.ALL_FACTORY_CLASS_NAMES
     * as the argument allowedFactoryClassNames. allowedFactoryClassNames must not be null. (A NullPointerException will be provoked if it is.)
     */
    public static Object referenceToObject( Reference ref, Name name, Context nameCtx, Hashtable<?,?> env, Set<String> allowedFactoryClassNames )
	throws NamingException
    { return referenceToObject( ref, name, nameCtx, env, allowedFactoryClassNames, null ); }

    /**
     * The allowedFactoryClassNames whitelist test can be (but generally should not be) circumvented by explicitly providing ReferenceUtils.ALL_FACTORY_CLASS_NAMES
     * as the argument allowedFactoryClassNames. allowedFactoryClassNames must not be null. (A NullPointerException will be provoked if it is.)
     */
    public static Object referenceToObject( Reference ref, Name name, Context nameCtx, Hashtable<?,?> env, Set<String> allowedFactoryClassNames, PropertiesConfig pcfg )
	throws NamingException
    {
	try
	    {
                // name and nameCtx are optional parameters. name can just be null
                //
                // this function isn't really a JNDI lookup, but we are erring on the side of
                // conservatism with this stuff now.

                if (name != null) assertAcceptableName(name,pcfg);

		String fClassName = ref.getFactoryClassName();
		String fClassLocation = ref.getFactoryClassLocation();

                // for now, we simply do not support null factoryClassName
                //
                // if ever there is a need to, we could adopt the behavior of, or delegate to, javax.naming.spi.NamingManager
                // see https://docs.oracle.com/en/java/javase/11/docs/api/java.naming/javax/naming/spi/NamingManager.html
                // but as this is likely legacy functionality, for now we'll just reject such References
                if (fClassName == null)
                    throw new NamingException(
                        "A null factoryClassName was encountered. ReferenceableUtils.referenceToObject(...) does not support null factory class names. " +
                        "If the null is intentional, consider using javax.naming.spi.NamingManager.getObjectInstance(...) " +
                        "which employs certain conventions to dereference with an unspecified factoryClassName. Reference: " + ref
                    );

                // note that the test of reference identity, rather than semantic equality, against token ALL_FACTORY_CLASS_NAMES is essential!
                if (allowedFactoryClassNames != ALL_FACTORY_CLASS_NAMES && !allowedFactoryClassNames.contains(fClassName))
                    throw new NamingException(
                        "factoryClassName '" + fClassName + "' is not in allowedFactoryClassNames [" + IterableUtils.joinAsString(",",allowedFactoryClassNames) + "]"
                    );

		ClassLoader defaultClassLoader = Thread.currentThread().getContextClassLoader();
		if ( defaultClassLoader == null ) defaultClassLoader = ReferenceableUtils.class.getClassLoader();

		ClassLoader cl;
		if ( fClassLocation == null )
		    cl = defaultClassLoader;
		else
		    {
                        if ( supportReferenceRemoteFactoryClassLocation( pcfg ) )
                        {
                            URL u = new URL( fClassLocation );
                            cl = new URLClassLoader( new URL[] { u }, defaultClassLoader );
                        }
                        else
                        {
                            if ( logger.isLoggable( MLevel.WARNING ) )
                                logger.log(
                                   MLevel.WARNING,
                                   "A javax.naming.Reference we have been tasked to dereference specifies a potentially remote factory class location. " +
                                   "This is dangerous. A malicious reference could load and execute arbitrary code. " +
                                   "To prevent this, the factoryClassLocation property of the reference will be ignored, and the reference will attempt to dereference " +
                                   "using the calling Thread's context ClassLoader or else the ClassLoader that loaded com.mchange.v2.naming.ReferenceableUtils. " +
                                   "If you really mean to allow references to download remote code, you can set '" + SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION +
                                   "'. But it is strongly disrecommended. Reference: " + ref
                                );
                            cl = defaultClassLoader;
                        }
		    }

		Class<?> fClass = Class.forName( fClassName, true, cl );
		ObjectFactory of;
		try
		    { of = (ObjectFactory) fClass.getDeclaredConstructor().newInstance(); }
		catch (InvocationTargetException ite)
		    {
			// reflective construction wraps whatever the constructor threw, while
			// the Class.newInstance() this replaces let it propagate. Keep propagating
			// it, so the NamingException test below still sees the original.
			Throwable t = ite.getCause();
			if (t instanceof Exception) throw (Exception) t;
			else if (t instanceof Error) throw (Error) t;
			else throw ite;
		    }
		return of.getObjectInstance( ref, name, nameCtx, env );
	    }
	catch ( Exception e )
	    {
		if (Debug.DEBUG)
		    {
			//e.printStackTrace();
			if ( logger.isLoggable( MLevel.FINE ) )
			    logger.log( MLevel.FINE, "Could not resolve Reference to Object!", e);
		    }
                if (e instanceof NamingException)
                    throw (NamingException) e;
                else
                {
                    NamingException ne = new NamingException("Could not resolve Reference to Object!");
                    ne.setRootCause( e );
                    throw ne;
                }
	    }
    }

    private final static String DEFAULT_NAME_GUARD_CLASS_NAME = "com.mchange.v2.naming.ApparentlyLocalNameGuard";

    // for now we'll just use a simple HashMap, synchronizing access, to cache Constructors.
    // there should be very few values looked up, so soft-reference-ing seems like overkill
    //
    // MT: Synchronized on own lock
    private final static Map<String,Constructor<?>> nameGuardClassNameToConstructor = new HashMap<String,Constructor<?>>();

    private final static NameGuard nameGuardForClassName(String fqcn)
        throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        synchronized (nameGuardClassNameToConstructor)
        {
            Constructor<?> ctor = nameGuardClassNameToConstructor.get(fqcn);
            if (ctor == null)
            {
                Class<?> cl = Class.forName(fqcn);
                ctor = cl.getDeclaredConstructor();
                nameGuardClassNameToConstructor.put(fqcn,ctor);
            }
            return (NameGuard) ctor.newInstance();
        }
    }

    public static void assertAcceptableName( Object jndiName, PropertiesConfig pcfg ) throws NamingException
    {
        String nameGuardClassName;
        if (pcfg == null)
            nameGuardClassName = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        else
            nameGuardClassName = pcfg.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );

        try
        {
            NameGuard nameGuard;
            if (nameGuardClassName == null)
                nameGuard = nameGuardForClassName( DEFAULT_NAME_GUARD_CLASS_NAME );
            else
                nameGuard = nameGuardForClassName( nameGuardClassName );

            boolean acceptable;
            if ( jndiName instanceof String )
                acceptable = nameGuard.nameIsAcceptable((String) jndiName);
            else if ( jndiName instanceof Name )
                acceptable = nameGuard.nameIsAcceptable((Name) jndiName);
            else
            {
                throw new NamingException(
                   "Putative JNDI name of unexpected type. We expect String or javax.naming.Name. " +
                   "We conservatively, redundantly, disallow any attempt to lookup of jndi names of unknown types. There is no API to do so. " +
                   "Putative JNDI name: " + jndiName
                );
            }

            if (!acceptable)
            {
                String nameGuardDescription;
                if (nameGuardClassName == null)
                    nameGuardDescription = "default NameGuard '" + DEFAULT_NAME_GUARD_CLASS_NAME +"'";
                else
                    nameGuardDescription = "NameGuard '" + nameGuardClassName + "', currently configured via '" + SecurityConfigKey.NAME_GUARD_CLASS_NAME + "'";
                throw new NamingException(
                    "Under " + nameGuardDescription + ", names are only acceptable when " + nameGuard.onlyAcceptableWhen() + ". '" + jndiName + "' does not qualify."
                );
            }
        }
        catch (ReflectiveOperationException roe)
        {
            if (nameGuardClassName == null)
                throw new InternalError("Huh? We failed to reflectively lookup and construct default NameGuard '" + DEFAULT_NAME_GUARD_CLASS_NAME + "'?!?", roe);
            else
                throw new NamingException("We failed to reflectively lookup and construct configured NameGuard '" + nameGuardClassName + ". Cause: " + roe);
        }
    }

    public static boolean allowIndirectSerializationViaReference( PropertiesConfig pcfg )
    { return securitySensitiveFalseBiasedLookupSyspropsPropertiesConfig( SecurityConfigKey.ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE, pcfg, "Creating or decoding dangerous Java-Serialized References when objects are Referenceable but not Serializable, or ordinary Serialization fails.", logger ); }

    public static boolean generateSerializedObjectBinaryRefAddr( PropertiesConfig pcfg )
    { return securitySensitiveFalseBiasedLookupSyspropsPropertiesConfig( SecurityConfigKey.GENERATE_SERIALIZED_OBJECT_BINARY_REF_ADDR, pcfg, "Serializing, via dangerous Java Serialization, objects into references (as BinaryRefAddr)", logger ); }

    public static boolean supportReferenceRemoteFactoryClassLocation( PropertiesConfig pcfg )
    { return securitySensitiveFalseBiasedLookupSyspropsPropertiesConfig( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION, pcfg, "Loading of remote factory classes when resolving javax.naming.Reference instances", logger ); }

    public static boolean acceptDeserializedInitialContextEnvironment( PropertiesConfig pcfg )
    { return securitySensitiveFalseBiasedLookupSyspropsPropertiesConfig( SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT, pcfg, "Acceptance of deserialized InitialContext environment", logger ); }

    /**
     * @deprecated nesting references seemed useful until I realized that
     *             references are Serializable and can be stored in a BinaryRefAddr.
     *             Oops.
     */
    public static void appendToReference(Reference appendTo, Reference orig)
	throws NamingException
    {
	int len = orig.size();
	appendTo.add( new StringRefAddr( REFADDR_VERSION, String.valueOf( CURRENT_REF_VERSION ) ) );
	appendTo.add( new StringRefAddr( REFADDR_CLASSNAME, orig.getClassName() ) );
	appendTo.add( new StringRefAddr( REFADDR_FACTORY, orig.getFactoryClassName() ) );
	appendTo.add( new StringRefAddr( REFADDR_FACTORY_CLASS_LOCATION,
					 orig.getFactoryClassLocation() ) );
	appendTo.add( new StringRefAddr( REFADDR_SIZE, String.valueOf(len) ) );
	for (int i = 0; i < len; ++i)
	    appendTo.add( orig.get(i) );
    }

    /**
     * @deprecated nesting references seemed useful until I realized that
     *             references are Serializable and can be stored in a BinaryRefAddr.
     *             Oops.
     */
    public static ExtractRec extractNestedReference(Reference extractFrom, int index)
	throws NamingException
    {
	try
	    {
		int version = Integer.parseInt((String) extractFrom.get(index++).getContent());
		if (version == 1)
		    {
			String className = (String) extractFrom.get(index++).getContent();
			String factoryClassName = (String) extractFrom.get(index++).getContent();
			String factoryClassLocation = (String) extractFrom.get(index++).getContent();

			Reference outRef = new Reference( className,
							  factoryClassName,
							  factoryClassLocation );
			int size = Integer.parseInt((String) extractFrom.get(index++).getContent());
			for (int i = 0; i < size; ++i)
			    outRef.add( extractFrom.get( index++ ) );
			return new ExtractRec( outRef, index );
		    }
		else
		    throw new NamingException("Bad version of nested reference!!!");
	    }
	catch (NumberFormatException e)
	    {
		if (Debug.DEBUG)
		    {
			//e.printStackTrace();
			if ( logger.isLoggable( MLevel.FINE ) )
			    logger.log( MLevel.FINE, "Version or size nested reference was not a number!!!", e);
		    }
		throw new NamingException("Version or size nested reference was not a number!!!");
	    }
    }

    /* intentionally package-scope, accessed by JavaBeanReferenceMaker */
    /* pcfg can be null */
    static void ensureWhitelistedJavaBeanClass( Object bean, PropertiesConfig pcfg ) throws NamingException
    { ensureWhitelistedJavaBeanClass( bean.getClass().getName(), pcfg ); }

    private static boolean whitelistIsDisabled(WhitelistInfo info)
    { return info.getWhitelist().equals(ACCEPT_ANY_WHITELIST) && ACCEPTABLE_WHITELIST_SOURCES.contains(info.getSource()); }

    private static boolean objectFactoryWhitelistIsDisabled(WhitelistInfo info)
    { return whitelistIsDisabled(info); }

    private static boolean javaBeanWhitelistIsDisabled(WhitelistInfo info)
    { return whitelistIsDisabled(info); }

    /* intentionally package-scope, accessed by JavaBeanObjectFactory */
    /* pcfg can be null */
    static void ensureWhitelistedJavaBeanClass( String fqcn, PropertiesConfig pcfg ) throws NamingException
    {
        WhitelistInfo info = referenceableJavaBeanClassWhitelistManager.collectWhitelistInfoSyspropsPropertiesConfig( pcfg, logger );
        if (ACCEPTABLE_WHITELIST_SOURCES.contains(info.getSource()))
        {
            if (!javaBeanWhitelistIsDisabled(info) && !info.getWhitelist().contains(fqcn))
            {
                throw new NamingException(
                    "The whitelist of acceptable JavaBean classes to which to create or look up references does not contain referenced class '" + fqcn + "'. " +
                    "Please add that class to comma-separated list at config key '" + referenceableJavaBeanClassWhitelistManager.getTopLevelBaseKey() + "' (and/or subkeys) if " +
                    "you wish for this reference to be created or resolved. " +
                    "(If this denial is unexpected, note that if you have set the same whitelist key or subkey in both System properties and other config to distinct values, only the INTERSECTION becomes whitelisted.) " +
                    "Current whitelist: " + info + " -- " + "Missing class: " + fqcn
                );
            }
        }
        else
        {
            if (info.getSource() == WhitelistInfo.Source.MISSING)
                throw new NamingException(
                    "No whitelist is set for referenceable java beans. Please set '" + referenceableJavaBeanClassWhitelistManager.getTopLevelBaseKey() + 
                    "' (and/or subkeys) if you wish to dereference Referenceable JavaBean classes. You are currently creating or dereferencing an object of class '" + fqcn + "'. If that is intended and desirable, please include it " +
                    "in the whitelist! (If this denial is unexpected, note that if you have set the same whitelist key or subkey in both System properties and other config to distinct values, only the INTERSECTION becomes whitelisted.) " +
                    "No classes are currently whitelisted. -- Missing class: " + fqcn
                );
            else
            {
                // at present this can't happen, but in case the WhitelistInfo.Source enum grows
                throw new NamingException(
                    "The whitelist of Referenceable JavaBean classes was derived from an unexpected source, and will not be honored. Current whitelist: " + info +
                    "; source " + info.getSource()
                );
            }
        }
    }

    // pcfg can be null
    private static Set<String> findMandatoryObjectFactoryWhitelist( PropertiesConfig pcfg ) throws NamingException
    {
        WhitelistInfo info = objectFactoryWhitelistManager.collectWhitelistInfoSyspropsPropertiesConfig( pcfg, logger );
        if (!ACCEPTABLE_WHITELIST_SOURCES.contains(info.getSource()))
        {
            if (info.getWhitelist().isEmpty() && info.getSource().equals(WhitelistInfo.Source.MISSING))
                throw new NamingException(
                  "No acceptable ObjectFactory whitelist found. " +
                  "When calling referenceToObject(...) using overloads that lack an explicit allowedFactoryClassNames Set, a '" +
                  objectFactoryWhitelistManager.getTopLevelBaseKey() + "' (or a subkey) must be provided either as a System property or a provided com.mchange.v2.PropertiesConfig instance. " +
                  "If you really want to live dangerously and accept any ObjectFactory (why?!?), you may provide a whitelist with a unique entry of '*'. WhitelistInfo: " + info
                );
            else
            {
                // at present this should not happen, the only unacceptable whitelist source is MISSING, and the whitelist
                // should always be blank in that case. But, future-proofing, in case we unexpectedly find a non-empty MISSING whitelist, or
                // the enum WhitelistInfo.Source expands, or ACCEPTABLE_WHITELIST_SOURCES shrinks..
                throw new NamingException("ObjectFactory whitelist did not come from an expected source. Found whitelist: " + info + "; source " + info.getSource() + "; Acceptable sources: " + ACCEPTABLE_WHITELIST_SOURCES);
            }
        }
        if (objectFactoryWhitelistIsDisabled(info))
            return ALL_FACTORY_CLASS_NAMES;
        else
            return info.getWhitelist();
    }

    /**
     * @deprecated nesting references seemed useful until I realized that
     *             references are Serializable and can be stored in a BinaryRefAddr.
     *             Oops.
     */
    public static class ExtractRec
    {
	public Reference ref;

	/**
	 *  return the first RefAddr index that the function HAS NOT read to
	 *  extract the reference.
	 */
	public int       index;

	private ExtractRec(Reference ref, int index)
	{
	    this.ref   = ref;
	    this.index = index;
	}
    }

    private ReferenceableUtils()
    {}
}
