package com.mchange.v2.cfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import com.mchange.v2.log.MLevel;
import com.mchange.v2.log.MLogger;

/**
 *  An MLogger that remembers what it was told, so tests can assert on warnings.
 *
 *  <p>Several of WhitelistManager's behaviors are <i>only</i> observable as warnings -- a
 *  '*' that shares a whitelist with other entries is dropped silently from the caller's
 *  point of view, and the class's whole contract with a confused operator is the text that
 *  explains which key to edit. Pinning the resulting Set alone would leave those
 *  untested.</p>
 *
 *  <p>NullMLogger cannot be subclassed (private constructor), so this implements MLogger
 *  directly. Only isLoggable and log(MLevel,String) do anything; WhitelistManager uses
 *  nothing else, and a test that starts exercising another method should teach this class
 *  about it rather than silently capture nothing.</p>
 */
public class CapturingMLogger implements MLogger
{
    private final List<String> warnings = new ArrayList<String>();
    private boolean loggable = true;

    /** with warnings off, isLoggable(WARNING) is false -- security behavior must not depend on it */
    public void setLoggable( boolean loggable )
    { this.loggable = loggable; }

    public List<String> warnings()
    { return Collections.unmodifiableList( warnings ); }

    public void clear()
    { warnings.clear(); }

    /** every captured warning containing all of the given fragments */
    public List<String> warningsContaining( String... fragments )
    {
        List<String> out = new ArrayList<String>();
        for ( String w : warnings )
        {
            boolean all = true;
            for ( String f : fragments )
                if ( ! w.contains( f ) ) { all = false; break; }
            if ( all ) out.add( w );
        }
        return out;
    }

    public boolean sawWarningContaining( String... fragments )
    { return ! warningsContaining( fragments ).isEmpty(); }

    @Override
    public boolean isLoggable(MLevel l)
    { return loggable && l != null && l.intValue() >= MLevel.WARNING.intValue(); }

    @Override
    public void log(MLevel l, String msg)
    { if ( isLoggable( l ) ) warnings.add( msg ); }

    // ---------- everything below is inert ----------

    @Override public String getName() { return CapturingMLogger.class.getName(); }
    @Override public void log(MLevel l, String msg, Object param) {}
    @Override public void log(MLevel l, String msg, Object[] params) {}
    @Override public void log(MLevel l, String msg, Throwable t) {}
    @Override public void logp(MLevel l, String srcClass, String srcMeth, String msg) {}
    @Override public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object param) {}
    @Override public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object[] params) {}
    @Override public void logp(MLevel l, String srcClass, String srcMeth, String msg, Throwable t) {}
    @Override public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg) {}
    @Override public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object param) {}
    @Override public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object[] params) {}
    @Override public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Throwable t) {}
    @Override public void entering(String srcClass, String srcMeth) {}
    @Override public void entering(String srcClass, String srcMeth, Object param) {}
    @Override public void entering(String srcClass, String srcMeth, Object[] params) {}
    @Override public void exiting(String srcClass, String srcMeth) {}
    @Override public void exiting(String srcClass, String srcMeth, Object result) {}
    @Override public void throwing(String srcClass, String srcMeth, Throwable t) {}
    @Override public void severe(String msg) {}
    @Override public void warning(String msg) {}
    @Override public void info(String msg) {}
    @Override public void config(String msg) {}
    @Override public void fine(String msg) {}
    @Override public void finer(String msg) {}
    @Override public void finest(String msg) {}
    @Override public ResourceBundle getResourceBundle() { return null; }
    @Override public String getResourceBundleName() { return null; }
    @Override public void setFilter(Object java14Filter) throws SecurityException {}
    @Override public Object getFilter() { return null; }
    @Override public void setLevel(MLevel l) throws SecurityException {}
    @Override public MLevel getLevel() { return MLevel.WARNING; }
    @Override public void addHandler(Object h) throws SecurityException {}
    @Override public void removeHandler(Object h) throws SecurityException {}
    @Override public Object[] getHandlers() { return new Object[0]; }
    @Override public void setUseParentHandlers(boolean uph) {}
    @Override public boolean getUseParentHandlers() { return false; }
}
