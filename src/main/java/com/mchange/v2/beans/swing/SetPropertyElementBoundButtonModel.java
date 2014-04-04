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
import javax.swing.event.*;
import com.mchange.v2.beans.BeansUtils;

class SetPropertyElementBoundButtonModel implements ButtonModel
{
    Object                          putativeElement;
    ButtonModel                     inner;
    PropertyComponentBindingUtility pcbu;

    public static void bind(AbstractButton[] buttons, Object[] elements, Object bean, String setPropName) throws IntrospectionException
    {
	for (int i = 0, len = buttons.length; i < len; ++i)
	    {
		AbstractButton doMe = buttons[i];
		doMe.setModel( new SetPropertyElementBoundButtonModel( doMe.getModel(), bean, setPropName, elements[i] ) );
	    }
    }  

    public SetPropertyElementBoundButtonModel( ButtonModel inner, Object bean, String propName, Object putativeElement )
	throws IntrospectionException
    {	
	this.inner = inner;
	this.putativeElement = putativeElement;
	this.pcbu  = new PropertyComponentBindingUtility( new MyHbi(), bean, propName, false ); 
	pcbu.resync();
    }

    public boolean isArmed()
    { return inner.isArmed(); }

    public boolean isSelected()
    { return inner.isSelected(); }

    public boolean isEnabled()
    { return inner.isEnabled(); }

    public boolean isPressed()
    { return inner.isPressed(); }

    public boolean isRollover()
    { return inner.isRollover(); }

    public void setArmed(boolean armed)
    { inner.setArmed( armed ); }

    public void setSelected(boolean selected)
    { inner.setSelected( selected ); }

    public void setEnabled(boolean enabled)
    { inner.setEnabled( enabled ); }

    public void setPressed(boolean pressed)
    { inner.setPressed( pressed ); }

    public void setRollover(boolean rollover)
    { inner.setRollover( rollover ); }

    public void setMnemonic(int mnemonic)
    { inner.setMnemonic( mnemonic ); }

    public int getMnemonic()
    { return inner.getMnemonic(); }

    public void setActionCommand(String actionCommand)
    { inner.setActionCommand( actionCommand ); }

    public String getActionCommand()
    { return inner.getActionCommand(); }

    public void setGroup(ButtonGroup group)
    { inner.setGroup( group ); }

    public Object[] getSelectedObjects()
    { return inner.getSelectedObjects(); }

    public void addActionListener(ActionListener listener)
    { inner.addActionListener( listener ); }

    public void removeActionListener(ActionListener listener)
    { inner.removeActionListener( listener ); }

    public void addItemListener(ItemListener listener)
    { inner.addItemListener( listener );}

    public void removeItemListener(ItemListener listener)
    { inner.removeItemListener( listener );}

    public void addChangeListener(ChangeListener listener)
    { inner.addChangeListener( listener );}

    public void removeChangeListener(ChangeListener listener)
    { inner.removeChangeListener( listener );}

    class MyHbi implements HostBindingInterface
	{
	    public void syncToValue( PropertyEditor editor, Object newVal )
	    {
		if (newVal == null)
		    setSelected(false);
		else
		    setSelected( ((Set) newVal).contains( putativeElement ) );
	    }
	    
	    public void addUserModificationListeners()
	    {
		ActionListener al = new ActionListener()
		    {
			public void actionPerformed( ActionEvent evt )
			{ pcbu.userModification(); }
		    };
		addActionListener( al );
	    }
	    
	    public Object fetchUserModification( PropertyEditor editor, Object oldValue )
	    {
		Set modSet;
		if (oldValue == null)
		    {
			if (! isSelected())
			    return null;
			else
			    modSet = new HashSet();
		    }
		else
		    modSet = new HashSet((Set) oldValue);

		if ( isSelected() )
		    modSet.add( putativeElement );
		else
		    modSet.remove( putativeElement );

		return modSet;
	    }
	    
	    public void alertErroneousInput()
	    { Toolkit.getDefaultToolkit().beep(); }
	};
}
