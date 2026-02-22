package com.mchange.v2.naming;

import java.net.*;
import java.util.*;
import javax.naming.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import com.mchange.v2.cfg.PropertiesConfig;
import com.mchange.v2.log.MLevel;
import com.mchange.v2.log.MLog;
import com.mchange.v2.log.MLogger;
import com.mchange.v2.util.IterableUtils;
import javax.naming.spi.ObjectFactory;

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
    public final static Set ALL_FACTORY_CLASS_NAMES = Collections.unmodifiableSet(new HashSet());

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

    public static Object referenceToObject( Reference ref, Name name, Context nameCtx, Hashtable env )
	throws NamingException
    {
        Set allowedFactoryClassNames = findMandatoryObjectFactoryWhitelist( null );
        return referenceToObject( ref, name, nameCtx, env, allowedFactoryClassNames, null );
    }

    public static Object referenceToObject( Reference ref, Name name, Context nameCtx, Hashtable env, PropertiesConfig pcfg )
	throws NamingException
    {
        Set allowedFactoryClassNames = findMandatoryObjectFactoryWhitelist( pcfg );
        return referenceToObject( ref, name, nameCtx, env, allowedFactoryClassNames, pcfg );
    }

    /**
     * The allowedFactoryClassNames whitelist test can be (but generally should not be) circumvented by explicitly providing ReferenceUtils.ALL_FACTORY_CLASS_NAMES
     * as the argument allowedFactoryClassNames. allowedFactoryClassNames must not be null. (A NullPointerException will be provoked if it is.)
     */
    public static Object referenceToObject( Reference ref, Name name, Context nameCtx, Hashtable env, Set allowedFactoryClassNames )
	throws NamingException
    { return referenceToObject( ref, name, nameCtx, env, allowedFactoryClassNames, null ); }

    /**
     * The allowedFactoryClassNames whitelist test can be (but generally should not be) circumvented by explicitly providing ReferenceUtils.ALL_FACTORY_CLASS_NAMES
     * as the argument allowedFactoryClassNames. allowedFactoryClassNames must not be null. (A NullPointerException will be provoked if it is.)
     */
    public static Object referenceToObject( Reference ref, Name name, Context nameCtx, Hashtable env, Set allowedFactoryClassNames, PropertiesConfig pcfg )
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
                                   "The factoryClassLocation property of the reference will be ignored, and the reference will atempt to dereference " +
                                   "using the calling Thread's context ClassLoader or else the ClassLoader that loaded com.mchange.v2.naming.ReferenceableUtils. " +
                                   "Reference: " + ref
                                );
                            cl = defaultClassLoader;
                        }
		    }

		Class fClass = Class.forName( fClassName, true, cl );
		ObjectFactory of = (ObjectFactory) fClass.newInstance();
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

    /*
    public static boolean nameLocalityIsAcceptable( Object jndiName, PropertiesConfig pcfg )
    {
        boolean resolveNonlocal = permitNonlocalJndiNames( pcfg );
        if ( jndiName instanceof String )
            return resolveNonlocal || jndiNameIsLocal((String) jndiName);
        else if ( jndiName instanceof Name )
            return resolveNonlocal || jndiNameIsLocal((Name) jndiName);
        else
        {
            if ( logger.isLoggable( MLevel.WARNING ) )
                logger.log(
                   MLevel.WARNING,
                   "Putative JNDI name of unexpected type. We expect String or javax.naming.Name. " +
                   "We conservatively, redundantly, disallow any attempt to lookup of jndi names of unknown types. There is no API to do so. " +
                   "Putative JNDI name: " + jndiName
                );
            return false;
        }
    }

    public static boolean jndiNameIsLocal( String name )
    { return name.startsWith("java:"); }

    public static boolean jndiNameIsLocal( Name name )
    { return !name.isEmpty() && name.get(0).startsWith("java:"); }

    public static boolean permitNonlocalJndiNames( PropertiesConfig pcfg )
    { return falseBiasedLookupSyspropsPropertiesConfig( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, pcfg, "Looking up nonlocal (or not provably local) JNDI names"); }
    */

    private final static String DEFAULT_NAME_GUARD_CLASS_NAME = "com.mchange.v2.naming.ApparentlyLocalNameGuard";

    // for now we'll just use a simple HashMap, synchronizing access, to cache Constructors.
    // there should be very few values looked up, so soft-reference-ing seems like overkill
    //
    // MT: Synchronized on own lock
    private final static Map nameGuardClassNameToConstructor = new HashMap();

    private final static NameGuard nameGuardForClassName(String fqcn)
        throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        synchronized (nameGuardClassNameToConstructor)
        {
            Constructor ctor = (Constructor) nameGuardClassNameToConstructor.get(fqcn);
            if (ctor == null)
            {
                Class cl = Class.forName(fqcn);
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

    public static boolean supportReferenceRemoteFactoryClassLocation( PropertiesConfig pcfg )
    { return falseBiasedLookupSyspropsPropertiesConfig( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION, pcfg, "Loading of remote factory classes when resolving javax.naming.Reference instances" ); }

    public static boolean acceptDeserializedInitialContextEnvironment( PropertiesConfig pcfg )
    { return falseBiasedLookupSyspropsPropertiesConfig( SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT, pcfg, "Acceptance of deserialized InitialContext environment"); }

    private static boolean falseBiasedLookupSyspropsPropertiesConfig( String propStyleKey, PropertiesConfig pcfg, String whatWillBeDisabled )
    {
        String systemPropertiesBasedShouldSupportStr = System.getProperty( propStyleKey );
        Boolean systemPropertiesBasedShouldSupport = systemPropertiesBasedShouldSupportStr == null ? null : Boolean.valueOf( systemPropertiesBasedShouldSupportStr );

        Boolean pcfgBasedShouldSupport;
        if ( pcfg != null )
        {
            String pcfgBasedShouldSupportStr = pcfg.getProperty( propStyleKey );
            pcfgBasedShouldSupport = pcfgBasedShouldSupportStr == null ? null : Boolean.valueOf( pcfgBasedShouldSupportStr );
        }
        else
            pcfgBasedShouldSupport = null;

        boolean out;
        if ( Boolean.FALSE.equals( systemPropertiesBasedShouldSupport ) )
        {
            if (Boolean.TRUE.equals(pcfgBasedShouldSupport))
            {
                if ( logger.isLoggable( MLevel.WARNING ) )
                    logger.log(
                       MLevel.WARNING,
                       "Security-sensitive property '" + propStyleKey +
                       "' has been set to 'false' in System properties. Disabling this functionality in System properties conservatively " +
                       "OVERRIDES any configuration of this property set elsewhere, regardless of any alternative prioritization of system properties you may have configured. " +
                       "Please resolve the inconsistency of configuration." +
                       whatWillBeDisabled + " will be disabled!"
                    );
            }
            out = false;
        }
        else if ( Boolean.TRUE.equals( systemPropertiesBasedShouldSupport ) )
        {
            if ( Boolean.FALSE.equals( pcfgBasedShouldSupport ) )
            {
                if ( logger.isLoggable( MLevel.WARNING ) )
                    logger.log(
                       MLevel.WARNING,
                       "Security-sensitive property '" + propStyleKey +
                       "' has been set to 'true' in System properties, however it has been set to 'false' in other configuration supplied. Disabling this functionality in  " +
                       "supplied configuration overrides permission granted in System properties. " +
                       "Please resolve the inconsistency of configuration." +
                       whatWillBeDisabled + " will be disabled!"
                    );
                out = false;
            }
            else // System prop is explicitly set to true, MConfig value is either unset or set to true
            {
                out = true;
            }
        }
        else // property unset in System properties, defer to pcfg, only support if explicitly set to true there
        {
            out = Boolean.TRUE.equals( pcfgBasedShouldSupport );
        }
        return out;
    }

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

    private static Set commaSeparatedStringListToModifiableSet( String csList )
    {
        String[] items = csList.split("\\s*,\\s*");
        return new HashSet(Arrays.asList(items));
    }

    private static Set commaSeparatedStringListToSet( String csList )
    { return Collections.unmodifiableSet(commaSeparatedStringListToModifiableSet(csList)); }

    // pcfg can be null
    private static Set findMandatoryObjectFactoryWhitelist( PropertiesConfig pcfg ) throws NamingException
    {
        Set narrowest = narrowestStringListPropertiesConfigSystemProperties( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, pcfg );
        if (narrowest == null)
            throw new NamingException(
                "No ObjectFactory whitelist found. " +
                "When calling referenceToObject(...) using overloads that lack an explicit allowedFactoryClassNames Set, a '" +
                SecurityConfigKey.OBJECT_FACTORY_WHITELIST + "' must be provided either as a System property or a provided com.mchange.v2.PropertiesConfig instance. " +
                "If you really want to live dangerously and accept any ObjectFactory (why?!?), you must call an overload of referenceToObject(...) that accepts " +
                "an explicit allowedFactoryClassNames Set, and then provide it as ReferenceableUtils..ALL_FACTORY_CLASS_NAMES"
            );
        return narrowest;
    }

    // pcfg can be null
    private static Set narrowestStringListPropertiesConfigSystemProperties( String propStyleKey, PropertiesConfig pcfg )
    {
        String rawSysProp = System.getProperty( propStyleKey );
        String rawPropsConfigProp = pcfg == null ? null : pcfg.getProperty( propStyleKey );

        if (rawSysProp == null && rawPropsConfigProp == null)
            return null;
        else if (rawSysProp != null && rawPropsConfigProp == null)
            return commaSeparatedStringListToSet( rawSysProp );
        else if (rawSysProp == null && rawPropsConfigProp != null)
            return commaSeparatedStringListToSet( rawPropsConfigProp );
        else
        {
            Set sysPropSet = commaSeparatedStringListToModifiableSet( rawSysProp );
            Set propsConfigSet = commaSeparatedStringListToModifiableSet( rawPropsConfigProp );

            if (sysPropSet.equals(propsConfigSet))
                return Collections.unmodifiableSet(sysPropSet);
            else
            {
                sysPropSet.retainAll(propsConfigSet);
                Set out = Collections.unmodifiableSet(sysPropSet);

                if ( logger.isLoggable( MLevel.WARNING ) )
                    logger.log(
                        MLevel.WARNING,
                        "Inconsistent values of '" + propStyleKey + "' were found in System properties and the provided configuration. " +
                        "We are conservatively using the *intersection* of those values. " +
                        "Value in System properties: '" + rawSysProp + "'; Value in PropertiesConfig: '" + rawPropsConfigProp + "'; " +
                        "Value of intersection: '" + IterableUtils.joinAsString(",",out)
                    );

                return out;
            }
        }
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
