package com.mchange.v3.hocon;

import java.util.*;
import com.mchange.v2.cfg.*;
import com.typesafe.config.*;

import static com.mchange.v2.cfg.DelayedLogItem.Level.*;
import static com.typesafe.config.ConfigValueType.*;

public class HoconMultiPropertiesConfig extends MultiPropertiesConfig
{
    //MT: Immutable or never altered post ctor
    String quasiResourcePath;
    Properties props;
    List<DelayedLogItem> delayedLogItems = new LinkedList<DelayedLogItem>();

    //MT: Protected by this' lock
    Map<String,Properties> propsByPrefix = new HashMap<String,Properties>();

    HoconMultiPropertiesConfig( String quasiResourcePath, Config config )
    {
	this.quasiResourcePath = quasiResourcePath;
	this.props = propsForConfig( config );
    }
    
    private Properties propsForConfig( Config config )
    {
	Properties out = new Properties();
	for ( Iterator<Map.Entry<String,ConfigValue>> ii = config.entrySet().iterator(); ii.hasNext(); )
	{
	    Map.Entry<String,ConfigValue> entry = ii.next();
	    try { out.put( entry.getKey(), asSimpleString( entry.getValue() ) ); }
	    catch ( IllegalArgumentException e )
		{ delayedLogItems.add( new DelayedLogItem( FINE, "For property '" + entry.getKey() + "', " + e.getMessage() ) ); }
	}
	return out;
    }

    private static String asSimpleString( ConfigValue value ) throws IllegalArgumentException
    {
	ConfigValueType type = value.valueType();
	switch ( type ) 
	{
	case BOOLEAN:
	case NUMBER:
	case STRING:
	    return String.valueOf( value.unwrapped() );
	case LIST:
	    List<ConfigValue> l = (ConfigList) value;
	    for ( ConfigValue cv : l )
		if ( ! isSimple( cv ) )
		    throw new IllegalArgumentException("value is a complex list, could not be rendered as a simple property: " + value);
	    StringBuilder sb = new StringBuilder();
	    for( int i = 0, len = l.size(); i < len; ++i )
		{
		    if (i != 0) sb.append(',');
		    sb.append( asSimpleString( l.get(i) ) );
		}
	    return sb.toString();
	case OBJECT:
	    throw new IllegalArgumentException("value is a ConfigValue object rather than an atom or list of atoms: " + value);
	case NULL:
	    throw new IllegalArgumentException("value is a null; will be excluded from the MultiPropertiesConfig: " + value);
	default:
	    throw new IllegalArgumentException("value of an unexpected type: (value->" + value + ", type->" + type + ")");
	}
    }

    private static boolean isSimple( ConfigValue value )
    {
	ConfigValueType type = value.valueType();
	switch ( type ) 
	{
	case BOOLEAN:
	case NUMBER:
	case STRING:
	    return true;
	default:
	    return false;
	}
    }
    
    @Override
    public String[] getPropertiesResourcePaths() 
    { return new String[] { quasiResourcePath }; }

    @Override
    public Properties getPropertiesByResourcePath(String path)
    {
	if ( path.equals( quasiResourcePath ) )
	{
	    Properties out = new Properties();
	    out.putAll( props );
	    return out;
	}
	else
	    return null;
    }
    
    @Override
    public synchronized Properties getPropertiesByPrefix(String pfx)
    {
	Properties outish = (Properties) propsByPrefix.get( pfx );
	if ( outish == null )
	{
	    outish = new Properties();

	    String dottedPfx = pfx + '.';
	    for ( Map.Entry entry : props.entrySet() )
	    {
		String key = (String) entry.getKey();
		if ( key.startsWith( dottedPfx ) )
		    outish.put( key, entry.getValue() );
	    }

	    propsByPrefix.put( pfx, outish );
	}
	return outish;
    }

    @Override
    public String getProperty( String key )
    { return (String) props.get( key ); }

    @Override
    public List getDelayedLogItems()
    { return delayedLogItems; }
}

