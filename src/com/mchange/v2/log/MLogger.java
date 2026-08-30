package com.mchange.v2.log;

import java.util.*;

/**
 * This is an interface designed to wrap around the JDK1.4 logging API, without
 * having any compilation dependencies on that API, so that applications that use
 * MLogger in a non JDK1.4 environment, or where some other logging library is
 * prefrerred, may do so.
 *
 * Calls to handler and filter related methods may be ignored if some logging
 * system besides jdk1.4 logging is the underlying library.
 */
public interface MLogger
{
    public String getName();

    public void log(MLevel l, String msg);
    public void log(MLevel l, String msg, Object param);
    public void log(MLevel l,String msg, Object[] params);
    public void log(MLevel l, String msg,Throwable t);
    public void logp(MLevel l, String srcClass, String srcMeth, String msg);
    public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object param);
    public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object[] params);
    public void logp(MLevel l, String srcClass, String srcMeth, String msg, Throwable t);
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg);
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object param);
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object[] params);
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Throwable t);
    public void entering(String srcClass, String srcMeth);
    public void entering(String srcClass, String srcMeth, Object param);
    public void entering(String srcClass, String srcMeth, Object params[]);
    public void exiting(String srcClass, String srcMeth);
    public void exiting(String srcClass, String srcMeth, Object result);
    public void throwing(String srcClass, String srcMeth, Throwable t);
    public void severe(String msg);
    public void warning(String msg);
    public void info(String msg);
    public void config(String msg);
    public void fine(String msg);
    public void finer(String msg);
    public void finest(String msg);

    public boolean isLoggable(MLevel l);

    /** @deprecated stick to common denominator logging through MLog facade */
    public ResourceBundle getResourceBundle();

    /** @deprecated stick to common denominator logging through MLog facade */
    public String getResourceBundleName();

    /** @deprecated stick to common denominator logging through MLog facade */
    public void setFilter(Object java14Filter) throws SecurityException;

    /** @deprecated stick to common denominator logging through MLog facade */
    public Object getFilter();

    /** @deprecated stick to common denominator logging through MLog facade */
    public void setLevel(MLevel l) throws SecurityException;

    /** @deprecated stick to common denominator logging through MLog facade */
    public MLevel getLevel();

    /** @deprecated stick to common denominator logging through MLog facade */
    public void addHandler(Object h) throws SecurityException;

    /** @deprecated stick to common denominator logging through MLog facade */
    public void removeHandler(Object h) throws SecurityException;

    /** @deprecated stick to common denominator logging through MLog facade */
    public Object[] getHandlers();

    /** @deprecated stick to common denominator logging through MLog facade */
    public void setUseParentHandlers(boolean uph);

    /** @deprecated stick to common denominator logging through MLog facade */
    public boolean getUseParentHandlers();
 }

