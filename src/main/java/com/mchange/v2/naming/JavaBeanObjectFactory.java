package com.mchange.v2.naming;

import java.beans.*;
import java.util.*;
import javax.naming.*;
import com.mchange.v2.log.*;
import java.io.IOException;
import java.lang.reflect.Method;
import javax.naming.spi.ObjectFactory;
import com.mchange.v2.beans.BeansUtils;
import com.mchange.v2.cfg.CurrentConfigFinder;
import com.mchange.v2.csv.FastCsvUtils;
import com.mchange.v2.lang.Coerce;
import com.mchange.v2.ser.SerializableUtils;

public class JavaBeanObjectFactory implements ObjectFactory
{
    private final static MLogger logger = MLog.getLogger( JavaBeanObjectFactory.class );

    final static Object NULL_TOKEN = new Object();

    JavaBeanReferencePropertyOverrider overrider = null;

    CurrentConfigFinder cfgFinder = null;

    public void setReferencePropertyOverrider(JavaBeanReferencePropertyOverrider overrider)
    { this.overrider = overrider; }

    public JavaBeanReferencePropertyOverrider getReferencePropertyOverrider()
    { return this.overrider; }

    public void setConfigFinder(CurrentConfigFinder cfgFinder)
    { this.cfgFinder = cfgFinder; }

    public CurrentConfigFinder getConfigFinder()
    { return this.cfgFinder; }

    public Object getObjectInstance(Object refObj, Name name, Context nameCtx, Hashtable env)
	throws Exception
    {
	if (refObj instanceof Reference)
	    {
		Reference ref = (Reference) refObj;
		Map refAddrsMap = new HashMap();
		for (Enumeration e = ref.getAll(); e.hasMoreElements(); )
		    {
			RefAddr addr = (RefAddr) e.nextElement();
			refAddrsMap.put( addr.getType(), addr );
		    }
		Class beanClass = Class.forName( ref.getClassName() );
		Set refProps = null;
		RefAddr refPropsRefAddr = (StringRefAddr) refAddrsMap.remove( JavaBeanReferenceMaker.REF_PROPS_KEY );
		if ( refPropsRefAddr != null )
                {
                    String[] refPropsArray = FastCsvUtils.csvSplitLine((String) refPropsRefAddr.getContent());
		    refProps = new HashSet();
                    Collections.addAll(refProps, refPropsArray);
                    //System.err.println(refProps);
                }
		Map propMap = createPropertyMap( beanClass, refAddrsMap );
		return findBean( beanClass, propMap, refProps );
	    }
	else
	    return null;
    }

    private Map createPropertyMap( Class beanClass, Map refAddrsMap ) throws Exception
    {
	BeanInfo bi = Introspector.getBeanInfo( beanClass );
	PropertyDescriptor[] pds = bi.getPropertyDescriptors();

	Map out = new HashMap();
	for (int i = 0, len = pds.length; i < len; ++i)
	    {
		PropertyDescriptor pd = pds[i];
		String propertyName = pd.getName();
		Class  propertyType = pd.getPropertyType();
		RefAddr addr = (RefAddr) refAddrsMap.remove( propertyName );
		if (addr != null)
		    {
                        Object override;
                        if ( this.overrider != null & (override = this.overrider.overrideDecodeRefAddr( beanClass, cfgFinder.findCurrentConfig(), propertyName, propertyType, addr )) != null )
                            {
                                out.put( propertyName, override );
                            }
			else if ( addr instanceof StringRefAddr )
			    {
				String content = (String) ((StringRefAddr) addr).getContent();
				if ( Coerce.canCoerce( propertyType ) )
				    out.put( propertyName, Coerce.toObject( content, propertyType ) );
				else
                                {
                                    PropertyEditor pe = BeansUtils.findPropertyEditor( pd );
                                    if (pe != null)
                                    {
                                        pe.setAsText( content );
                                        out.put( propertyName, pe.getValue() );
                                    }
                                    else // the only remaining valid possibility is that it is securely Stringified
                                    {
                                        try
                                        {
                                            out.put( propertyName, SecurelyStringifiable.constructSecurelyStringified( content ) );
                                        }
                                        catch (Exception e)
                                        {
                                            if (logger.isLoggable( MLevel.WARNING ))
                                                logger.log(
                                                    MLevel.WARNING,
                                                    "Failed to find an acceptable means to decode StringRefAddr for property '" + propertyName +
                                                    "' of " + beanClass + ". Content: " + content,
                                                    e
                                                );
                                        }
                                    }
                                }
			    }
			else if ( addr instanceof BinaryRefAddr )
			    {
				byte[] content = (byte[]) ((BinaryRefAddr) addr).getContent();
				if ( content.length == 0 )
				    out.put( propertyName, NULL_TOKEN ); //we use an empty array to mean null
				else
                                    handleDeserializeBinaryRefAddressContent( propertyName, out, content );
			    }
			else
			    {
				if (logger.isLoggable( MLevel.WARNING ))
				    logger.warning(this.getClass().getName() + " -- unknown RefAddr subclass: " + addr.getClass().getName());
			    }
		    }
	    }
	for ( Iterator ii = refAddrsMap.keySet().iterator(); ii.hasNext(); )
	    {
		String type = (String) ii.next();
		if (logger.isLoggable( MLevel.WARNING ))
		    logger.warning(this.getClass().getName() + " -- RefAddr for unknown property: " + type);
	    }
	return out;
    }

    protected void handleDeserializeBinaryRefAddressContent( String propertyName, Map out, byte[] content ) throws ClassNotFoundException, IOException
    {
        if ( logger.isLoggable( MLevel.WARNING ) )
            logger.log(
                MLevel.WARNING,
                "Deserialization of BinaryRefAddr contents interpreted as Java-Serialized objects has been disabled. " +
                "The functionality still exists, but to restore it you must define your own subclass of " +
                this.getClass().getName() +
                "and override 'protected void handleDeserializeBinaryRefAddressContent( String propertyName, Map out, byte[] content ) throws ClassNotFoundException, IOException' " +
                "to call 'dangerousDeserializeBinaryRefAddressContent( propertyName, out, content )' rather than merely log this warning. " +
                "For now, property '" + propertyName + "' will be skipped. " +
                "It will take its default value upon construction and not be updated from the reference."
            );
    }

    protected void dangerousDeserializeBinaryRefAddressContent( String propertyName, Map out, byte[] content ) throws ClassNotFoundException, IOException
    {
	out.put( propertyName, SerializableUtils.fromByteArray( content ) ); //this will handle "indirectly serialized" objects.
    }

    protected Object createBlankInstance(Class beanClass) throws Exception
    { return beanClass.newInstance(); }

    protected Object findBean(Class beanClass, Map propertyMap, Set refProps ) throws Exception
    {
	Object bean = createBlankInstance( beanClass );
	BeanInfo bi = Introspector.getBeanInfo( bean.getClass() );
	PropertyDescriptor[] pds = bi.getPropertyDescriptors();

	for (int i = 0, len = pds.length; i < len; ++i)
	    {
		PropertyDescriptor pd = pds[i];
		String propertyName = pd.getName();
		Object value = propertyMap.get( propertyName );
		Method setter = pd.getWriteMethod();
		if (value != null)
		    {
			if (setter != null)
			    setter.invoke( bean, new Object[] { (value == NULL_TOKEN ? null : value) } );
			else
			    {
				//System.err.println(this.getClass().getName() + ": Could not restore read-only property '" + propertyName + "'.");
				if (logger.isLoggable( MLevel.WARNING ))
				    logger.warning(this.getClass().getName() + ": Could not restore read-only property '" + propertyName + "'.");
			    }
		    }
		else
		    {
			if (setter != null)
			    {
				if (refProps == null || refProps.contains( propertyName ))
				    {
					//System.err.println(this.getClass().getName() +
					//": WARNING -- Expected writable property '" + propertyName + "' left at default value");
					if (logger.isLoggable( MLevel.WARNING ))
					    logger.warning(this.getClass().getName() + " -- Expected writable property '" + propertyName + "' left at default value");
				    }
			    }
		    }
	    }

	return bean;
    }
}
