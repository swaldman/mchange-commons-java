package com.mchange.v2.log;

import java.lang.ref.WeakReference;
import java.util.*;

final class RedirectableMLogger implements MLogger
{
    // MT: protected by class' lock
    private static HashSet weakRefSet = new HashSet();

    synchronized static RedirectableMLogger wrap( MLogger mlogger )
    {
	RedirectableMLogger out = new RedirectableMLogger( mlogger );
	weakRefSet.add( new WeakReference( out ) );
	return out;
    }

    synchronized static void resetAll()
    {
	HashSet cloneSet = (HashSet) weakRefSet.clone();
	for ( Iterator ii = cloneSet.iterator(); ii.hasNext(); )
	{
	    WeakReference wr = (WeakReference) ii.next();
	    RedirectableMLogger registered = (RedirectableMLogger) wr.get();
	    if ( registered == null ) weakRefSet.remove( wr );
	    else registered.reset();
	}
    }

    // MT: Constant post construction
    private String _name;
    
    // MT: protected by this' lock
    private MLogger _inner;

    private synchronized void reset() { this._inner = null; }

    private synchronized MLogger inner()
    {
	if ( this._inner == null )
	    this._inner = MLog.getLogger( this._name );
	return this._inner;
    }

    private RedirectableMLogger( MLogger _inner )
    {
	this._inner = _inner;
	this._name  = _inner.getName();
    }
    
    @Override
    public String getName() { return inner().getName(); }

    @Override
    public void log(MLevel l, String msg)                                                                { inner().log( l, msg ); }
    @Override
    public void log(MLevel l, String msg, Object param)                                                  { inner().log( l, msg, param ); }
    @Override
    public void log(MLevel l,String msg, Object[] params)                                                { inner().log( l, msg, params ); }
    @Override
    public void log(MLevel l, String msg,Throwable t)                                                    { inner().log( l, msg, t ); }
    @Override
    public void logp(MLevel l, String srcClass, String srcMeth, String msg)                              { inner().logp( l, srcClass, srcMeth, msg ); }
    @Override
    public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object param)                { inner().logp( l, srcClass, srcMeth, msg, param ); }
    @Override
    public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object[] params)             { inner().logp( l, srcClass, srcMeth, msg, params ); }
    @Override
    public void logp(MLevel l, String srcClass, String srcMeth, String msg, Throwable t)                 { inner().logp( l, srcClass, srcMeth, msg, t ); }
    @Override
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg)                  { inner().logrb( l, srcClass, srcMeth, rb, msg ); }
    @Override
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object param)    { inner().logrb( l, srcClass, srcMeth, rb, msg, param ); }
    @Override
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object[] params) { inner().logrb( l, srcClass, srcMeth, rb, msg, params ); }
    @Override
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Throwable t)     { inner().logrb( l, srcClass, srcMeth, rb, msg, t ); }

    @Override
    public void entering(String srcClass, String srcMeth)                  { inner().entering( srcClass, srcMeth ); }
    @Override
    public void entering(String srcClass, String srcMeth, Object param)    { inner().entering( srcClass, srcMeth, param ); }
    @Override
    public void entering(String srcClass, String srcMeth, Object[] params) { inner().entering( srcClass, srcMeth, params ); }
    @Override
    public void exiting(String srcClass, String srcMeth)                   { inner().exiting( srcClass, srcMeth ); }
    @Override
    public void exiting(String srcClass, String srcMeth, Object result)    { inner().exiting( srcClass, srcMeth, result ); }
    @Override
    public void throwing(String srcClass, String srcMeth, Throwable t)     { inner().throwing( srcClass, srcMeth, t ); }

    @Override
    public void severe(String msg)  { inner().severe( msg ); }
    @Override
    public void warning(String msg) { inner().warning( msg ); }
    @Override
    public void info(String msg)    { inner().info( msg ); }
    @Override
    public void config(String msg)  { inner().config( msg ); }
    @Override
    public void fine(String msg)    { inner().fine( msg ); }
    @Override
    public void finer(String msg)   { inner().finer( msg ); }
    @Override
    public void finest(String msg)  { inner().finest( msg ); }

    @Override
    public boolean isLoggable(MLevel l) { return inner().isLoggable( l ); }

    /** @deprecated stick to common denominator logging through MLog facade */
    @Override
    public ResourceBundle getResourceBundle() { return inner().getResourceBundle(); }

    /** @deprecated stick to common denominator logging through MLog facade */
    @Override
    public String getResourceBundleName() { return inner().getResourceBundleName(); }

    /** @deprecated stick to common denominator logging through MLog facade */
    @Override
    public void setFilter(Object java14Filter) throws SecurityException { inner().setFilter( java14Filter ); }

    /** @deprecated stick to common denominator logging through MLog facade */
    @Override
    public Object getFilter() { return inner().getFilter(); }

    /** @deprecated stick to common denominator logging through MLog facade */
    @Override
    public void setLevel(MLevel l) throws SecurityException { inner().setLevel( l ); }

    /** @deprecated stick to common denominator logging through MLog facade */
    @Override
    public MLevel getLevel() { return inner().getLevel(); }

    /** @deprecated stick to common denominator logging through MLog facade */
    @Override
    public void addHandler(Object h) throws SecurityException { inner().addHandler( h ); }

    /** @deprecated stick to common denominator logging through MLog facade */
    @Override
    public void removeHandler(Object h) throws SecurityException { inner().removeHandler( h ); }

    /** @deprecated stick to common denominator logging through MLog facade */
    @Override
    public Object[] getHandlers() { return inner().getHandlers(); }

    /** @deprecated stick to common denominator logging through MLog facade */
    @Override
    public void setUseParentHandlers(boolean uph) { inner().setUseParentHandlers(uph); }

    /** @deprecated stick to common denominator logging through MLog facade */
    @Override
    public boolean getUseParentHandlers() { return inner().getUseParentHandlers(); }
 }

