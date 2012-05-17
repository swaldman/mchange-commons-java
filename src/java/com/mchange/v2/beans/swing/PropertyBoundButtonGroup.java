/*
 * Distributed as part of mchange-commons-java v.0.2.1
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
import java.util.*;
import javax.swing.*;
import com.mchange.v2.beans.BeansUtils;

class PropertyBoundButtonGroup extends ButtonGroup
{
    PropertyComponentBindingUtility pcbu;
    HostBindingInterface myHbi;

    WeChangedListener wcl = new WeChangedListener();

    Map buttonsModelsToValues = new HashMap();
    Map valuesToButtonModels  = new HashMap();
    JButton fakeButton = new JButton();

    public PropertyBoundButtonGroup( Object bean, String propName )
	throws IntrospectionException
    {
	this.myHbi = new MyHbi();
	this.pcbu  = new PropertyComponentBindingUtility( myHbi, bean, propName, false ); 
	this.add( fakeButton, null );
	pcbu.resync();
    } 

    public void add(AbstractButton button, Object associatedValue)
    {
	super.add( button );
	buttonsModelsToValues.put( button.getModel(), associatedValue );
	valuesToButtonModels.put( associatedValue, button.getModel() );

	button.addActionListener( wcl );
	pcbu.resync();
    }

    public void add(AbstractButton button)
    {
	System.err.println( this + "Warning: The button '" + button + "' has been implicitly associated with a null value!");
	System.err.println( "To avoid this warning, please use public void add(AbstractButton button, Object associatedValue)" );
	System.err.println( "instead of the single-argument add method." );
	super.add( button );

	button.addActionListener( wcl );
	pcbu.resync();
    }

    public void remove(AbstractButton button)
    {
	button.removeActionListener( wcl );
	super.remove( button );
    }

    class MyHbi implements HostBindingInterface
	{
	    public void syncToValue( PropertyEditor editor, Object newVal )
	    {
		ButtonModel selectMe = (ButtonModel) valuesToButtonModels.get( newVal );
		if ( selectMe != null )
		    setSelected( selectMe, true );
		else
		    setSelected( fakeButton.getModel(), true );
	    }
	    
	    public void addUserModificationListeners()
	    {
		// we can not do this on initialization... we
		// add our listener to each button as it is added.
	    }
	    
	    public Object fetchUserModification( PropertyEditor editor, Object oldValue )
	    {
		ButtonModel model = getSelection();
		return buttonsModelsToValues.get( model );
	    }
	    
	    public void alertErroneousInput()
	    { Toolkit.getDefaultToolkit().beep(); }
	};

    class WeChangedListener implements ActionListener
    {
	public void actionPerformed( ActionEvent evt )
	{ pcbu.userModification(); }
    }
}
