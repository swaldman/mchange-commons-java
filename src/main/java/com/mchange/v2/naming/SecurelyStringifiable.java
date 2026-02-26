package com.mchange.v2.naming;

import com.mchange.v2.log.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// we might consider caching Method objects here, but we expect this to be a rare,
// not-performace-critical application, so for now we'll just lookup on demand
final class SecurelyStringifiable
{
    private final static MLogger logger = MLog.getLogger( SecurelyStringifiable.class );
    
    public final static String SECURELY_STRINGIFY_METHOD_NAME = "securelyStringify";
    public final static String CONSTRUCT_SECURELY_STRINGIFIED_METHOD_NAME = "constructSecurelyStringified";

    private final static Class[] CONSTRUCT_SECURELY_STRINGIFIED_METHOD_ARGS = new Class[]{String.class};


    private static Method getExpectedPublicStaticMethod(Class cl, String methodName, Class[] argTypes, Class expectedReturnType)
    {
        try
        {
            Method m = cl.getMethod( methodName, argTypes );
            int modifiers = m.getModifiers();
            if ((modifiers & Modifier.PUBLIC) == 0)
            {
                if ((modifiers & Modifier.STATIC) == 0)
                {
                    if (m.getReturnType() == cl)
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

    private static Method getGoodSecurelyStringifyMethod(Class cl)
    { return getExpectedPublicStaticMethod( cl, SECURELY_STRINGIFY_METHOD_NAME, new Class[]{cl}, String.class ); }

    private static Method getGoodConstructSecurelyStringifiedMethod(Class cl)
    { return getExpectedPublicStaticMethod( cl, CONSTRUCT_SECURELY_STRINGIFIED_METHOD_NAME, CONSTRUCT_SECURELY_STRINGIFIED_METHOD_ARGS, cl ); }

    public static boolean isSecurelyStringifiable(Class cl)
    { return getGoodSecurelyStringifyMethod(cl) != null && getGoodConstructSecurelyStringifiedMethod(cl) != null; }

    public static String securelyStringify(Object o) throws SecurelyStringifiableException
    {
        Class cl = o.getClass();

        // always check both!
        Method mStringify = getGoodSecurelyStringifyMethod(cl);
        Method mConstruct = getGoodConstructSecurelyStringifiedMethod(cl);
        if (mStringify == null || mConstruct == null)
            throw new SecurelyStringifiableException("'" + cl.getName() + "' is not SecurelyStringifiable.");
        else
        {
            try { return (String) mStringify.invoke(null, new Object[]{o}); }
            catch (Exception e)
            { throw new SecurelyStringifiableException( "Attempt to securely stringify " + o + " failed with an Exception.", e ); }
        }
    }

    public static Object constructSecurelyStringified( Class cl, String stringified ) throws SecurelyStringifiableException
    {
        // always check both!
        Method mStringify = getGoodSecurelyStringifyMethod(cl);
        Method mConstruct = getGoodConstructSecurelyStringifiedMethod(cl);
        if (mStringify == null || mConstruct == null)
            throw new SecurelyStringifiableException("'" + cl.getName() + "' is not SecurelyStringifiable.");
        else
        {
            try { return mConstruct.invoke(null, new Object[]{stringified}); }
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

