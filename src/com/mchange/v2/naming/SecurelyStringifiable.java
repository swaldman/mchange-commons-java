package com.mchange.v2.naming;

import com.mchange.v2.log.*;

import com.mchange.v2.cfg.PropertiesConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import com.mchange.v2.cfg.SealedSystemPropertiesWhitelistManager;
import com.mchange.v2.cfg.WhitelistInfo;

// we might consider caching Method objects here, but we expect this to be a rare,
// not-performace-critical application, so for now we'll just lookup on demand
public final class SecurelyStringifiable
{
    private final static MLogger logger = MLog.getLogger( SecurelyStringifiable.class );

    public final static String SECURELY_STRINGIFY_METHOD_NAME = "securelyStringify";
    public final static String CONSTRUCT_SECURELY_STRINGIFIED_METHOD_NAME = "constructSecurelyStringified";

    private final static Class<?>[] CONSTRUCT_SECURELY_STRINGIFIED_METHOD_ARGS = new Class<?>[]{String.class,PropertiesConfig.class};

    private final static String SECURELY_STRINGIFIED_PFX     = "Securely Stringified: ";
    private final static int    SECURELY_STRINGIFIED_PFX_LEN = SECURELY_STRINGIFIED_PFX.length();

    private final static SealedSystemPropertiesWhitelistManager whitelistManager = new SealedSystemPropertiesWhitelistManager( SecurityConfigKey.SECURELY_STRINGIFIABLE_BASE_KEY, null );

    private static Method getExpectedPublicStaticMethod(Class<?> cl, String methodName, Class<?>[] argTypes, Class<?> expectedReturnType)
    {
        try
        {
            Method m = cl.getMethod( methodName, argTypes );
            int modifiers = m.getModifiers();
            if ((modifiers & Modifier.PUBLIC) != 0)
            {
                if ((modifiers & Modifier.STATIC) != 0)
                {
                    if (m.getReturnType() == expectedReturnType)
                        return m;
                    else
                    {
                        if (logger.isLoggable(MLevel.WARNING))
                            logger.log(
                                MLevel.WARNING,
                                "Although a public static method '" + methodName + "' exists on class '" + cl.getName() +"', " +
                                "its return type '" + m.getReturnType() + "' is not the expected '" + expectedReturnType + "'."
                            );
                        return null;
                    }
                }
                else
                {
                    if (logger.isLoggable(MLevel.WARNING))
                        logger.log(
                            MLevel.WARNING,
                            "Although a public method '" + methodName + "' exists on class '" + cl.getName() +"', " +
                            "it is not static, and so does not fulfil the contract of a SecurelyStringifiable."
                        );
                    return null;
                }
            }
            else
            {
                if (logger.isLoggable(MLevel.WARNING))
                    logger.log(
                        MLevel.WARNING,
                        "Although the method '" + methodName + "' exists on class '" + cl.getName() +"', " +
                        "it is not public, and so does not fulfil the contract of a SecurelyStringifiable."
                    );
                return null;
            }
        }
        catch (NoSuchMethodException nsme)
        {
            if (logger.isLoggable(MLevel.DEBUG))
                logger.log(
                    MLevel.DEBUG,
                    "Class '" + cl.getName() + "' does not contain a public static method '" + SECURELY_STRINGIFY_METHOD_NAME +
                    "', so it is not SecurelyStringifiable.",
                    nsme
                );
            return null;
        }
    }

    private static Method getGoodSecurelyStringifyMethod(Class<?> cl)
    { return getExpectedPublicStaticMethod( cl, SECURELY_STRINGIFY_METHOD_NAME, new Class<?>[]{cl}, String.class ); }

    private static Method getGoodConstructSecurelyStringifiedMethod(Class<?> cl)
    { return getExpectedPublicStaticMethod( cl, CONSTRUCT_SECURELY_STRINGIFIED_METHOD_NAME, CONSTRUCT_SECURELY_STRINGIFIED_METHOD_ARGS, cl ); }

    public static boolean isSecurelyStringifiable(Class<?> cl)
    { return getGoodSecurelyStringifyMethod(cl) != null && getGoodConstructSecurelyStringifiedMethod(cl) != null; }

    public static String securelyStringify(Object o) throws SecurelyStringifiableException
    {
        Class<?> cl = o.getClass();

        // always check both!
        Method mStringify = getGoodSecurelyStringifyMethod(cl);
        Method mConstruct = getGoodConstructSecurelyStringifiedMethod(cl);
        if (mStringify == null || mConstruct == null)
            throw new SecurelyStringifiableException("'" + cl.getName() + "' is not SecurelyStringifiable.");
        else
        {
            try
            {
                String fqcn = cl.getName();
                return SECURELY_STRINGIFIED_PFX + fqcn + '\n' + (String) mStringify.invoke(null, new Object[]{o});
            }
            catch (Exception e)
            {
                // System.err.println("IN securelyStringify EXCEPTION:");
                // e.printStackTrace();
                throw new SecurelyStringifiableException( "Attempt to securely stringify " + o + " failed with an Exception. [" + e + "]", e );
            }
        }
    }

    public static WhitelistInfo whitelistInfo(PropertiesConfig pcfg)
    { return whitelistManager.collectWhitelistInfoSyspropsPropertiesConfig(pcfg, logger); }

    public static Object constructSecurelyStringified( String stringified, PropertiesConfig pcfg ) throws SecurelyStringifiableException
    {
        try
        {
            if (!stringified.startsWith(SECURELY_STRINGIFIED_PFX))
                throw new SecurelyStringifiableException( "Not a SecurelyStringified, does not begin with header " + SECURELY_STRINGIFIED_PFX + "<fqcn> -- " + stringified);
            else
            {
                String noPfx = stringified.substring( SECURELY_STRINGIFIED_PFX_LEN );
                int newLineIndex = noPfx.indexOf('\n');
                if (newLineIndex <= 0)
                    throw new SecurelyStringifiableException("Bad SecurelyStringified header, unterminated or no class name -- " + stringified);
                else
                {
                    String fqcn = noPfx.substring(0, newLineIndex);

                    String whyNot = whitelistManager.whitelistWhyNot(fqcn, pcfg, logger, "classes acceptable to reconstitute via SecurelyStringifiable");
                    if (whyNot != null)
                        throw new SecurelyStringifiableConstructionForbiddenException(whyNot);
                    else
                    {
                        String stringifiedPostHeader = noPfx.substring(newLineIndex+1);

                        // we refrain from initializing the class until it duck-types as something we can reconstruct
                        // no need to run potentially dangerous static initializers if it's visibly an unsuitable class
                        Class<?> uninitializedClass = Class.forName(fqcn, false, SecurelyStringifiable.class.getClassLoader());

                        return constructSecurelyStringifiedPostHeader( uninitializedClass, stringified, stringifiedPostHeader, pcfg );
                    }
                }
            }
        }
        catch (SecurelyStringifiableException e)
        { throw e; }
        catch (Exception e)
        { throw new SecurelyStringifiableException( "An Exception occurred while trying to reconstruct a SecurelyStringified object.", e ); }
    }

    private static Object constructSecurelyStringifiedPostHeader( Class<?> cl, String stringified, String stringifiedPostHeader, PropertiesConfig pcfg ) throws SecurelyStringifiableException
    {
        // always check both!
        Method mStringify = getGoodSecurelyStringifyMethod(cl);
        Method mConstruct = getGoodConstructSecurelyStringifiedMethod(cl);
        if (mStringify == null || mConstruct == null)
            throw new SecurelyStringifiableException("'" + cl.getName() + "' is not SecurelyStringifiable.");
        else
        {
            try { return mConstruct.invoke(null, new Object[]{stringifiedPostHeader, pcfg}); }
            catch (InvocationTargetException e)
            {
                Throwable te = e.getTargetException();
                if (te instanceof SecurelyStringifiableException)
                    throw (SecurelyStringifiableException) te;
                else
                    throw new SecurelyStringifiableException(
                        "Attempt to securely construct " + cl.getName() +
                        " from stringified representation  failed with an Exception. Stringified:\n" + stringified,
                        te
                    );
            }
            catch (Exception e)
            {
                throw new SecurelyStringifiableException(
                    "Attempt to securely construct " + cl.getName() +
                    " from stringified representation  failed with an Exception. Stringified:\n" + stringified,
                    e
                );
            }
        }
    }

    private SecurelyStringifiable()
    {}
}

