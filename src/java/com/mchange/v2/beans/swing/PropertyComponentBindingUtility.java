/*
 * Distributed as part of mchange-commonslib v.0.2
 *
 * Copyright (C) 2010 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.v2.beans.swing;

import java.beans.*;
import java.lang.reflect.*;

import javax.swing.SwingUtilities;
import com.mchange.v2.beans.BeansUtils;


/**
 * This class ASSUMES the property at issue is a JavaBeans-bound property.
 * It does NOT rely on the introspector to verify this, on the theory that
 * often programmers fail to provide accurate BeanInfo classes.
 */
class PropertyComponentBindingUtility
{
    final static Object[] EMPTY_ARGS = {};

    HostBindingInterface hbi;

    Object   bean;
    PropertyDescriptor pd            = null;
    EventSetDescriptor propChangeEsd = null;
    Method addMethod                 = null;
    Method removeMethod              = null;
    Method propGetter                = null;
    Method propSetter                = null;
    PropertyEditor propEditor        = null;

    Object nullReplacement           = null;

    PropertyComponentBindingUtility(final HostBindingInterface hbi, Object bean, final String propName, boolean requirePropEditor)
	throws IntrospectionException
    { 
	this.hbi = hbi; 
	this.bean = bean;

	BeanInfo beanInfo = Introspector.getBeanInfo( bean.getClass() );

	PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
	for( int i = 0, len = pds.length; i < len; ++i)
	    {
		PropertyDescriptor checkPd = pds[i];
		if (propName.equals( checkPd.getName() ) )
		    {
			this.pd = checkPd;
			break;
		    }
	    }
	if (pd == null)
	    throw new IntrospectionException("Cannot find property on bean Object with name '" + propName + "'.");

	EventSetDescriptor[] esds = beanInfo.getEventSetDescriptors();
	for( int i = 0, len = esds.length; i < len; ++i)
	    {
		EventSetDescriptor checkEsd = esds[i];
		if ("propertyChange".equals( checkEsd.getName() ) )
		    {
			this.propChangeEsd = checkEsd;
			break;
		    }
	    }
	if (propChangeEsd == null)
	    throw new IntrospectionException("Cannot find PropertyChangeEvent on bean Object with name '" + propName + "'.");

	propEditor = BeansUtils.findPropertyEditor( pd );
	if (requirePropEditor && propEditor == null)
	    throw new IntrospectionException("Could not find an appropriate PropertyEditor for property: " + propName);

	//System.err.println( "propEditor -> " + propEditor );

	propGetter = pd.getReadMethod();
	propSetter = pd.getWriteMethod();

	if (propGetter == null || propSetter == null)
	    throw new IntrospectionException("The specified property '" + propName + "' must be both readdable and writable, but it is not!");

	Class propType = pd.getPropertyType();
	if (propType.isPrimitive())
	    {
		if (propType == boolean.class)
		    this.nullReplacement = Boolean.FALSE;
		if (propType == byte.class)
		    this.nullReplacement = new Byte( (byte) 0 );
		else if (propType == char.class)
		    this.nullReplacement = new Character( (char) 0 );
		else if (propType == short.class)
		    this.nullReplacement = new Short( (short) 0 );
		else if (propType == int.class)
		    this.nullReplacement = new Integer( 0 );
		else if (propType == long.class)
		    this.nullReplacement = new Long( 0 );
		else if (propType == float.class)
		    this.nullReplacement = new Float( 0 );
		else if (propType == double.class)
		    this.nullReplacement = new Double( 0 );
		else
		    throw new InternalError("What kind of primitive is " + propType.getName() + "???");
	    }

	addMethod = propChangeEsd.getAddListenerMethod();
	removeMethod = propChangeEsd.getAddListenerMethod();

	PropertyChangeListener pcl = new PropertyChangeListener()
	    {
		public void propertyChange( PropertyChangeEvent evt )
		{
		    String chkPropName = evt.getPropertyName();
		    if (chkPropName.equals( propName ))
			{ hbi.syncToValue( propEditor, evt.getNewValue()); }
		}
	    };
	
	try
	    { addMethod.invoke( bean, new Object[]{ pcl } ); }
	catch ( Exception e )
	    {
		e.printStackTrace();
		throw new IntrospectionException("The introspected PropertyChangeEvent adding method failed with an Exception.");
	    }

	hbi.addUserModificationListeners();
    }

    public void userModification()
    {
	Object oldValue = null;
	try
	    { oldValue = propGetter.invoke( bean, EMPTY_ARGS ); }
	catch (Exception e)
	    { e.printStackTrace(); }

	try
	    {
		Object newValue = hbi.fetchUserModification( propEditor, oldValue );
		if (newValue == null)
		    newValue = nullReplacement;
		propSetter.invoke( bean, new Object[] { newValue } );
	    }
	catch (Exception e)
	    {
		if (! (e instanceof PropertyVetoException))
		    e.printStackTrace();
		syncComponentToValue( true ); 
	    }
    }

    public void resync()
    { syncComponentToValue( false ); }

    private void syncComponentToValue( final boolean alert_error )
    {
	try
	    {
		final Object reversionValue = propGetter.invoke( bean, EMPTY_ARGS );
		Runnable task = new Runnable()
		    {
			public void run()
			{
			    if (alert_error) 
				hbi.alertErroneousInput();
			    hbi.syncToValue( propEditor , reversionValue );
			}
		    };
		SwingUtilities.invokeLater( task );
	    }
	catch (Exception e)
	    {
		e.printStackTrace();
	    }
    }
}
