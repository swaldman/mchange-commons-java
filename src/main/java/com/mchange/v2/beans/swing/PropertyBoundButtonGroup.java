/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
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
