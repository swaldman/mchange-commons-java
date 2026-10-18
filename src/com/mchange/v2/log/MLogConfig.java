package com.mchange.v2.log;


import java.util.*;
import java.lang.reflect.Method;
import com.mchange.v2.cfg.MultiPropertiesConfig;
import com.mchange.v2.cfg.DelayedLogItem;
import com.mchange.v2.cfg.MConfig;

public final class MLogConfig
{
    // MT: all now mutable references, protected by class' lock
    private static MultiPropertiesConfig config              = null;
    private static List<DelayedLogItem>  bootstrapLogItems   = null;
    private static Method                delayedDumpToLogger = null;

    // this is very inelegant. in c3p0, mlog should be entirely independent of and prior to c3p0,
    // conceptually, c3p0 is just a random user of the library.
    // but in practice c3p0 is the most prominent user of the library,
    // and we want to ease the transition from MConfig.WithTraditionalDefaultSources with minimum
    // disruption to c3p0 users. so we are going to keep the default load paths between the two libraries
    // in sync
    public static String[] mlogC3p0Defaults()
    { return new String[] {"/com/mchange/v2/log/default-mchange-log.properties","/mchange-commons.properties","/mchange-log.properties","/c3p0-default.properties"}; }

    public static String[] mlogC3p0Preempts()
    { return new String[] {"hocon:/reference,/application,/c3p0,/","/c3p0.properties", "/"}; }

    public static String[] mlogC3p0Unified()
    { return new String[] {"/com/mchange/v2/log/default-mchange-log.properties","/mchange-commons.properties","/mchange-log.properties","/c3p0-default.properties","hocon:/reference,/application,/c3p0,/","/c3p0.properties","/"}; }

    public synchronized static void refresh( MultiPropertiesConfig[] overrides, String overridesDescription )
    {
	List<DelayedLogItem> bli = new ArrayList<>();

        MultiPropertiesConfig tmpConfig;

        if (MConfig.WithTraditionalDefaultSources.traditionalPathFileSourcedPathsFound(bli))
        {
            bli.add(new DelayedLogItem(DelayedLogItem.Level.WARNING,
                                       "At least one of the following resources " + MConfig.WithTraditionalDefaultSources.traditionalPathFileResourcesSearched() +
                                       " has been found, so mlog will configure itself using the traditional mechanism that honored these files. " +
                                       "However, mlog is transitioning from this mechanism, which is fragile and obscure. The supported way to customize configuration is " +
                                       "to refresh a now hardcoded configuration source list with overrides and then call MLog.refreshConfig(...) with overrides."));
            String[] defaults = mlogC3p0Defaults();
            String[] preempts = mlogC3p0Preempts();

            // note that it's important that we read the config uncached here, because we call this from MLog's class init,
            // and the cached pathway potentially hits a logger, which might lead to reentrancy for which we are not prepared
            // or deadlocks
            tmpConfig = MConfig.WithTraditionalDefaultSources.readUncachedResourceConfig( defaults, preempts, bli );
        }
        else
        {
            String[] resources = mlogC3p0Unified();

            // note that it's important that we read the config uncached here, because we call this from MLog's class init,
            // and the cached pathway potentially hits a logger, which might lead to reentrancy for which we are not prepared
            // or deadlocks
            tmpConfig = MConfig.AsProvided.readUncachedResourceConfig(null, resources, bli);
        }

	boolean firstLoad = (config == null);

	if ( overrides != null )
	{
	    int olen = overrides.length;
	    MultiPropertiesConfig[] combineMe = new MultiPropertiesConfig[ olen + 1 ];
	    combineMe[0] = tmpConfig;
	    for ( int i = 0; i < olen; ++i )
		combineMe[ i + 1 ] = overrides[i];
	    config = MConfig.combine( combineMe );
            bli.addAll( config.getDelayedLogItems() );
	    bli.add( new DelayedLogItem( DelayedLogItem.Level.INFO, (firstLoad ? "Loaded" : "Refreshed") + " MLog library log configuration, with overrides" + (overridesDescription == null ? "." : ": " + overridesDescription) ) );
	}
	else
	{
	    if ( !firstLoad )
		bli.add( new DelayedLogItem( DelayedLogItem.Level.INFO, "Refreshed MLog library log configuration, without overrides.") );
	    config = tmpConfig;
	}
        bli.add(new DelayedLogItem(DelayedLogItem.Level.INFO,
                                   "mlog config is now drawn from the following configuration sources: " +
                                   Arrays.toString(config.getPropertiesResourcePaths()) +
                                   " Later paths shadow earlier paths where values conflict."));
	bootstrapLogItems = bli;
    }

    // should be called only from static synchronized methods
    private static void ensureLoad()
    { if (config == null) refresh( null, null); }

    // should be called only from static synchronized methods
    private static void ensureDelayedDumpToLogger()
    {
	try
	{
	    if ( delayedDumpToLogger == null )
	    {
		Class<?> mConfigClass = Class.forName( "com.mchange.v2.cfg.MConfig" );
		Class<?> delayedLogItemClass = Class.forName( "com.mchange.v2.cfg.DelayedLogItem" );
		delayedDumpToLogger = mConfigClass.getMethod("dumpToLogger", new Class<?>[] { delayedLogItemClass, MLogger.class } );
	    }
	}
	catch ( RuntimeException e )
	{ 
	    e.printStackTrace();
	    throw e; 
	}
	catch ( Exception e )
	{ 
	    e.printStackTrace();
	    throw new RuntimeException( e ); 
	}
    }

    public synchronized static String getProperty( String key )
    {
	ensureLoad();
	return config.getProperty( key ); 
    }

    public synchronized static String getPropertyOnlyIfAvailable( String key )
    {
	return (config != null ? config.getProperty( key ) : null);
    }

    public synchronized static String getPropertyIfNoFailure( String key )
    {
        try { ensureLoad(); }
        catch (Exception e)
        { /* ignore */ }
	return (config != null ? config.getProperty( key ) : null);
    }

    private static String dedupKey(DelayedLogItem dli) { return dli.getLevel().toString() + "\u0000" + dli.getText().toString(); }

    // should not be called during static init to avoid cyclic dependency issues
    //
    // this got more complicated because we strip throwables off of log items before
    // retaining them, to avoid pinning ClassLoaders in memory. so sometimes we see
    // "duplicates" that are actually the same item, just one has its exception property
    // trimmed. so, we deduplicate on only level and text, but we prefer to log items
    // with throwables if we have one.
    public synchronized static void logDelayedItems( MLogger logger )
    {
        ensureLoad();
        ensureDelayedDumpToLogger();

        if ( bootstrapLogItems != null ) // we only want to log them once, we don't want to retain their Throwables
        {
            List<DelayedLogItem> items = new ArrayList<>();
            items.addAll( bootstrapLogItems );

            Set<String>                uniquerizer = new HashSet<>();
            Map<String,DelayedLogItem> preferred   = new HashMap<>();

            for ( DelayedLogItem item : items )
            {
                String ddkey = dedupKey(item);
                uniquerizer.add( ddkey );
                if (item.getException() != null && !preferred.containsKey(ddkey))
                    preferred.put(ddkey, item);
            }

            for( Iterator<DelayedLogItem> ii = items.iterator(); ii.hasNext(); )
            {
                DelayedLogItem item = ii.next();
                String ddkey = dedupKey(item);

                if (uniquerizer.contains( ddkey ) )
                {
                    uniquerizer.remove( ddkey );

                    DelayedLogItem loggableItem = preferred.get(ddkey);
                    if (loggableItem == null) loggableItem = item;

                    try { delayedDumpToLogger.invoke( null, new Object[] { loggableItem, logger } ); }
                    catch ( Exception e )
                    {
                        // bad, bad, shouldn't happen
                        e.printStackTrace();
                        throw new Error(e);
                    }
                }
            }
            bootstrapLogItems = null;
        }
    }

    public synchronized static String dump()
    { return config.toString(); }

    private MLogConfig()
    {}
}
