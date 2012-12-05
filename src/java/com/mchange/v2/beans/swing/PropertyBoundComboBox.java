/*
 * Distributed as part of mchange-commons-java v.0.2.3.2
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
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

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import com.mchange.v2.beans.BeansUtils;

public class PropertyBoundComboBox extends JComboBox
{
    PropertyComponentBindingUtility pcbu;

    MyHbi myHbi;

    Object itemsSrc   = null;
    Object nullObject = null;

    /**
     * @param itemsSrc can be an Object[], a Collection, or a ComboBoxModel
     * @param nullObject if a non-null value is provided, this object will stand-in for
     *                   a null-value on the bound property. 
     */
    public PropertyBoundComboBox( Object bean, String propName, Object itemsSrc, Object nullObject )
	throws IntrospectionException
    {
	super();
	this.myHbi = new MyHbi();
	this.pcbu  = new PropertyComponentBindingUtility( myHbi, bean, propName, false ); 

	this.nullObject = nullObject;
	setItemsSrc( itemsSrc );
    }

    public Object getItemsSrc()
    { return itemsSrc; }

    public void setItemsSrc(Object itemsSrc)
    {
	// we added this suspend/resume logic because we were seeing spurious "selections"
	// while modifying the list
	myHbi.suspendNotifications(); 

	this.removeAllItems();
	if (itemsSrc instanceof Object[])
	    {
		Object[] oa = (Object[]) itemsSrc;
		for (int i = 0, len = oa.length; i < len; ++i)
		    this.addItem( oa[i] );
	    }
	else if (itemsSrc instanceof Collection)
	    {
		Collection c = (Collection) itemsSrc;
		for (Iterator ii = c.iterator(); ii.hasNext(); )
		    this.addItem( ii.next() );
	    }
	else if (itemsSrc instanceof ComboBoxModel)
	    { this.setModel( (ComboBoxModel) itemsSrc ); }
	else
	    throw new IllegalArgumentException("itemsSrc must be an Object[], a Collection, or a ComboBoxModel");

	this.itemsSrc = itemsSrc;

	pcbu.resync();

	myHbi.resumeNotifications();
    }

    public void setNullObject(Object o)
    { 
	this.nullObject = null; 
	pcbu.resync();
    }

    public Object getNullObject()
    { return nullObject; }

    class MyHbi implements HostBindingInterface
	{
	    boolean suspend_notice = false;

	    public void suspendNotifications()
	    { suspend_notice = true; }

	    public void resumeNotifications()
	    { suspend_notice = false; }

	    public void syncToValue( PropertyEditor editor, Object newVal )
	    {
		if (newVal == null)
		    setSelectedItem( nullObject );
		else
		    setSelectedItem( newVal ); 
	    }
	    
	    public void addUserModificationListeners()
	    {
		ItemListener isl = new ItemListener()
		    {
			public void itemStateChanged( ItemEvent evt )
			{
			    if (! suspend_notice)
				pcbu.userModification(); 
			}
		    };
		addItemListener( isl );
	    }
	    
	    public Object fetchUserModification( PropertyEditor editor, Object oldValue )
	    { 
		Object out = getSelectedItem(); 
		if (nullObject != null && nullObject.equals( out ))
		    out = null;
		return out;
	    }
	    
	    public void alertErroneousInput()
	    { getToolkit().beep(); }
	};

    public static void main( String[] argv )
    {
	try
	    {
		TestBean tb = new TestBean();
		PropertyChangeListener pcl = new PropertyChangeListener()
		    {
			public void propertyChange( PropertyChangeEvent evt )
			{ BeansUtils.debugShowPropertyChange( evt ); }
		    };
		tb.addPropertyChangeListener( pcl );
		
		JComboBox jcb1 = new PropertyBoundComboBox( tb, "theString", new String[] {"SELECT", "Frog", "Fish", "Puppy"}, "SELECT" );
		JTextField jt2 = new PropertyBoundTextField( tb, "theInt", 5);
		JTextField jt3 = new PropertyBoundTextField( tb, "theFloat", 5 );
		JFrame frame = new JFrame();
		BoxLayout bl = new BoxLayout( frame.getContentPane(), BoxLayout.Y_AXIS );
		frame.getContentPane().setLayout( bl );
		frame.getContentPane().add( jcb1 );
		frame.getContentPane().add( jt2 );
		frame.getContentPane().add( jt3 );
		frame.pack();
		frame.show();
	    }
	catch ( Exception e )
	    { e.printStackTrace(); }
    }
}
