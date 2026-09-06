package com.mchange.v2.log.jdk14logging;

import java.util.*;
import java.util.logging.*;
import com.mchange.v2.log.*;
import com.mchange.v2.util.DoubleWeakHashMap;

public final class Jdk14MLog extends MLog
{
    final static String SUPPRESS_STACK_WALK_KEY = "com.mchange.v2.log.jdk14logging.suppressStackWalk";

    private static String[] UNKNOWN_ARRAY = new String[] {"UNKNOWN_CLASS", "UNKNOWN_METHOD"};

    private final static String CHECK_CLASS = "java.util.logging.Logger";

    private final Map namedLoggerMap = new DoubleWeakHashMap();

    private final static boolean suppress_stack_walk;

    static
    {
	String suppressVal = MLogConfig.getProperty( SUPPRESS_STACK_WALK_KEY );
	if (suppressVal == null || (suppressVal = suppressVal.trim()).length() == 0)
	    suppress_stack_walk = false;
	else
	    {
		if ( suppressVal.equalsIgnoreCase("true") )
		    suppress_stack_walk = true;
		else if  ( suppressVal.equalsIgnoreCase("false") )
		    suppress_stack_walk = false;
		else
		    {
			System.err.println("Bad value for " + SUPPRESS_STACK_WALK_KEY + ": '" + suppressVal + "'; defaulting to 'false'.");
			suppress_stack_walk = false;
		    }
	    }
    }

    MLogger global = null;

    public Jdk14MLog() throws ClassNotFoundException
    { Class.forName( CHECK_CLASS ); }

    public synchronized MLogger getMLogger(String name)
    {
        name = name.intern();

        MLogger out = (MLogger) namedLoggerMap.get( name );
        if (out == null)
        {
            Logger lg = Logger.getLogger(name);
            out = new Jdk14MLogger( lg ); 
            namedLoggerMap.put( name, out );
        }
        return out;
    }

    public synchronized MLogger getMLogger()
    {
        if (global == null)
            global = new Jdk14MLogger( LogManager.getLogManager().getLogger("global") );
        return global;
    }

    /*
     * We have to do this ourselves when class and method aren't provided, 
     * because the automatic extraction of this information will find the
     * (not very informative) calls in this class.
     */
    private static String[] findCallingClassAndMethod()
    {
	StackTraceElement[] ste = new Throwable().getStackTrace();
	for (int i = 0, len = ste.length; i < len; ++i)
	    {
		StackTraceElement check = ste[i];
		String cn = check.getClassName();
		if (cn != null && !cn.startsWith("com.mchange.v2.log.jdk14logging") && !cn.startsWith("com.mchange.sc.v1.log")) //last one is the Scala wrapper to the library
		    return new String[] { check.getClassName(), check.getMethodName() };
	    }
	return UNKNOWN_ARRAY;
    }

    private final static class Jdk14MLogger implements MLogger
    {
        final Logger logger;
	final String name;
	final ClassAndMethodFinder cmFinder;

        Jdk14MLogger( Logger logger )
        { 
            this.logger = logger; 
            //System.err.println("LOGGER: " + this.logger);

	    this.name = logger.getName();

	    if ( suppress_stack_walk == true )
		{
		    this.cmFinder = new ClassAndMethodFinder()
			{
			    String[] fakedClassAndMethod = new String[]{ name, "" };

			    public String[] find() { return fakedClassAndMethod; }
			};
		}
	    else
		{
		    this.cmFinder = new ClassAndMethodFinder()
			{
			    public String[] find() { return findCallingClassAndMethod(); }
			};
		}

        }

	interface ClassAndMethodFinder
	{
	    String[] find();
	}

        private static Level level(MLevel lvl)
        { return (Level) lvl.asJdk14Level(); }

        @Deprecated
        public ResourceBundle getResourceBundle()
        { return logger.getResourceBundle(); }

        @Deprecated
        public String getResourceBundleName()
        { return logger.getResourceBundleName(); }

        @Deprecated
        public void setFilter(Object java14Filter) throws SecurityException
        {
            if (! (java14Filter instanceof Filter))
                throw new IllegalArgumentException("MLogger.setFilter( ... ) requires a java.util.logging.Filter. " +
                "This is not enforced by the compiler only to permit building under jdk 1.3");
            logger.setFilter( (Filter) java14Filter ); 
        }

        @Deprecated
        public Object getFilter()
        { return logger.getFilter(); }

        public void log(MLevel l, String msg)
        { 
            if (! logger.isLoggable( level(l) )) return;

            String[] sa = cmFinder.find();
            logger.logp( level(l), sa[0], sa[1], msg );
        }

        public void log(MLevel l, String msg, Object param)
        { 
            if (! logger.isLoggable( level(l) )) return;

            String[] sa = cmFinder.find();
            logger.logp( level(l), sa[0], sa[1], msg, param );
        }

        public void log(MLevel l,String msg, Object[] params)
        { 
            if (! logger.isLoggable( level(l) )) return;

            String[] sa = cmFinder.find();
            logger.logp( level(l), sa[0], sa[1], msg, params );
        }

        public void log(MLevel l, String msg, Throwable t)
        { 
            if (! logger.isLoggable( level(l) )) return;

            String[] sa = cmFinder.find();
            logger.logp( level(l), sa[0], sa[1], msg, t );
        }

        public void logp(MLevel l, String srcClass, String srcMeth, String msg)
        {
            if (! logger.isLoggable( level(l) )) return;

            if (srcClass == null && srcMeth == null)
            {
                String[] sa = cmFinder.find();
                srcClass = sa[0];
                srcMeth = sa[1];
            }
            logger.logp( level(l), srcClass, srcMeth, msg ); 
        }

        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object param)
        { 
            if (! logger.isLoggable( level(l) )) return;

            if (srcClass == null && srcMeth == null)
            {
                String[] sa = cmFinder.find();
                srcClass = sa[0];
                srcMeth = sa[1];
            }
            logger.logp( level(l), srcClass, srcMeth, msg, param ); 
        }

        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object[] params)
        { 
            if (! logger.isLoggable( level(l) )) return;

            if (srcClass == null && srcMeth == null)
            {
                String[] sa = cmFinder.find();
                srcClass = sa[0];
                srcMeth = sa[1];
            }
            logger.logp( level(l), srcClass, srcMeth, msg, params ); 
        }

        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Throwable t)
        { 
            if (! logger.isLoggable( level(l) )) return;

            if (srcClass == null && srcMeth == null)
            {
                String[] sa = cmFinder.find();
                srcClass = sa[0];
                srcMeth = sa[1];
            }
            logger.logp( level(l), srcClass, srcMeth, msg, t ); 
        }

        //
        // MLogger.logrb takes the resource bundle by NAME, so these delegate to
        // java.util.logging.Logger's name-taking logrb, which jdk 9 deprecated in
        // favor of an overload taking a ResourceBundle instance.
        //
        // We keep the deprecated calls deliberately. Migrating would mean resolving
        // the name to a bundle here, and jdk logging's own resolution is not what a
        // straightforward rewrite produces:
        //
        //   - Logger resolves the name against the thread context ClassLoader.
        //     ResourceBundle.getBundle(name) resolves against the caller's loader,
        //     which here is whatever loaded mchange-commons-java. Where an
        //     application's bundles live in a loader the library cannot see -- a
        //     servlet container, say -- bundles that resolve today would stop
        //     resolving. The one argument getBundle does not consult the context
        //     loader even when one is set.
        //
        //   - A name that does not resolve is a non-event today: the record is
        //     logged unlocalized. ResourceBundle.getBundle throws
        //     MissingResourceException for an unknown name, and
        //     NullPointerException for a null one, so a rewrite would let logging
        //     throw into calling code.
        //
        // Reproducing the current behavior means reimplementing Logger's internal
        // findResourceBundle, in a logging hot path, and keeping it in step with
        // the jdk. Delegating to the deprecated method gets it right for free.
        //
        @SuppressWarnings("deprecation")
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg)
        { 
            if (! logger.isLoggable( level(l) )) return;

            if (srcClass == null && srcMeth == null)
            {
                String[] sa = cmFinder.find();
                srcClass = sa[0];
                srcMeth = sa[1];
            }
            logger.logrb( level(l), srcClass, srcMeth, rb, msg ); 
        }

        @SuppressWarnings("deprecation")
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object param)
        { 
            if (! logger.isLoggable( level(l) )) return;

            if (srcClass == null && srcMeth == null)
            {
                String[] sa = cmFinder.find();
                srcClass = sa[0];
                srcMeth = sa[1];
            }
            logger.logrb( level(l), srcClass, srcMeth, rb, msg, param ); 
        }

        @SuppressWarnings("deprecation")
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object[] params)
        { 
            if (! logger.isLoggable( level(l) )) return;

            if (srcClass == null && srcMeth == null)
            {
                String[] sa = cmFinder.find();
                srcClass = sa[0];
                srcMeth = sa[1];
            }
            logger.logrb( level(l), srcClass, srcMeth, rb, msg, params ); 
        }

        @SuppressWarnings("deprecation")
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Throwable t)
        { 
            if (! logger.isLoggable( level(l) )) return;

            if (srcClass == null && srcMeth == null)
            {
                String[] sa = cmFinder.find();
                srcClass = sa[0];
                srcMeth = sa[1];
            }
            logger.logrb( level(l), srcClass, srcMeth, rb, msg, t ); 
        }

        public void entering(String srcClass, String srcMeth)
        { 
            if (! logger.isLoggable( Level.FINER )) return;

            logger.entering( srcClass, srcMeth ); 
        }

        public void entering(String srcClass, String srcMeth, Object param)
        { 
            if (! logger.isLoggable( Level.FINER )) return;

            logger.entering( srcClass, srcMeth, param ); 
        }

        public void entering(String srcClass, String srcMeth, Object params[])
        { 
            if (! logger.isLoggable( Level.FINER )) return;

            logger.entering( srcClass, srcMeth, params ); 
        }

        public void exiting(String srcClass, String srcMeth)
        { 
            if (! logger.isLoggable( Level.FINER )) return;

            logger.exiting( srcClass, srcMeth ); 
        }

        public void exiting(String srcClass, String srcMeth, Object result)
        { 
            if (! logger.isLoggable( Level.FINER )) return;

            logger.exiting( srcClass, srcMeth, result ); 
        }

        public void throwing(String srcClass, String srcMeth, Throwable t)
        { 
            if (! logger.isLoggable( Level.FINER )) return;

            logger.throwing( srcClass, srcMeth, t ); 
        }

        public void severe(String msg)
        { 
            if (! logger.isLoggable( Level.SEVERE )) return;

            String[] sa = cmFinder.find();
            logger.logp( Level.SEVERE, sa[0], sa[1], msg );
        }

        public void warning(String msg)
        { 
            if (! logger.isLoggable( Level.WARNING )) return;

            String[] sa = cmFinder.find();
            logger.logp( Level.WARNING, sa[0], sa[1], msg );
        }

        public void info(String msg)
        { 
            if (! logger.isLoggable( Level.INFO )) return;

            String[] sa = cmFinder.find();
            logger.logp( Level.INFO, sa[0], sa[1], msg );
        }

        public void config(String msg)
        {
            if (! logger.isLoggable( Level.CONFIG )) return;

            String[] sa = cmFinder.find();
            logger.logp( Level.CONFIG, sa[0], sa[1], msg );
        }

        public void fine(String msg)
        { 
            if (! logger.isLoggable( Level.FINE )) return;

            String[] sa = cmFinder.find();
            logger.logp( Level.FINE, sa[0], sa[1], msg );
        }

        public void finer(String msg)
        { 
            if (! logger.isLoggable( Level.FINER )) return;

            String[] sa = cmFinder.find();
            logger.logp( Level.FINER, sa[0], sa[1], msg );
        }

        public void finest(String msg)
        { 
            if (! logger.isLoggable( Level.FINEST )) return;

            String[] sa = cmFinder.find();
            logger.logp( Level.FINEST, sa[0], sa[1], msg );
        }

        @Deprecated
        public void setLevel(MLevel l) throws SecurityException
        { logger.setLevel( level(l) ); }

        @Deprecated
        public MLevel getLevel()
        { return MLevel.fromIntValue( logger.getLevel().intValue() ); }

        public boolean isLoggable(MLevel l)
        { return logger.isLoggable( level(l) ); }

        public String getName()
        { return name; }

        @Deprecated
        public void addHandler(Object h) throws SecurityException
        { 
            if (! (h instanceof Handler))
                throw new IllegalArgumentException("MLogger.addHandler( ... ) requires a java.util.logging.Handler. " +
                "This is not enforced by the compiler only to permit building under jdk 1.3");
            logger.addHandler( (Handler) h ); 
        }

        @Deprecated
        public void removeHandler(Object h) throws SecurityException
        {
            if (! (h instanceof Handler))
                throw new IllegalArgumentException("MLogger.removeHandler( ... ) requires a java.util.logging.Handler. " +
                "This is not enforced by the compiler only to permit building under jdk 1.3");
            logger.removeHandler( (Handler) h ); 
        }

        @Deprecated
        public Object[] getHandlers()
        { return logger.getHandlers(); }

        @Deprecated
        public void setUseParentHandlers(boolean uph)
        { logger.setUseParentHandlers( uph ); }

        @Deprecated
        public boolean getUseParentHandlers()
        { return logger.getUseParentHandlers(); }
    }
}
