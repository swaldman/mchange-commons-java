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

    @Override
    public boolean isArmed()
    { return inner.isArmed(); }

    @Override
    public boolean isSelected()
    { return inner.isSelected(); }

    @Override
    public boolean isEnabled()
    { return inner.isEnabled(); }

    @Override
    public boolean isPressed()
    { return inner.isPressed(); }

    @Override
    public boolean isRollover()
    { return inner.isRollover(); }

    @Override
    public void setArmed(boolean armed)
    { inner.setArmed( armed ); }

    @Override
    public void setSelected(boolean selected)
    { inner.setSelected( selected ); }

    @Override
    public void setEnabled(boolean enabled)
    { inner.setEnabled( enabled ); }

    @Override
    public void setPressed(boolean pressed)
    { inner.setPressed( pressed ); }

    @Override
    public void setRollover(boolean rollover)
    { inner.setRollover( rollover ); }

    @Override
    public void setMnemonic(int mnemonic)
    { inner.setMnemonic( mnemonic ); }

    @Override
    public int getMnemonic()
    { return inner.getMnemonic(); }

    @Override
    public void setActionCommand(String actionCommand)
    { inner.setActionCommand( actionCommand ); }

    @Override
    public String getActionCommand()
    { return inner.getActionCommand(); }

    @Override
    public void setGroup(ButtonGroup group)
    { inner.setGroup( group ); }

    @Override
    public Object[] getSelectedObjects()
    { return inner.getSelectedObjects(); }

    @Override
    public void addActionListener(ActionListener listener)
    { inner.addActionListener( listener ); }

    @Override
    public void removeActionListener(ActionListener listener)
    { inner.removeActionListener( listener ); }

    @Override
    public void addItemListener(ItemListener listener)
    { inner.addItemListener( listener );}

    @Override
    public void removeItemListener(ItemListener listener)
    { inner.removeItemListener( listener );}

    @Override
    public void addChangeListener(ChangeListener listener)
    { inner.addChangeListener( listener );}

    @Override
    public void removeChangeListener(ChangeListener listener)
    { inner.removeChangeListener( listener );}

    class MyHbi implements HostBindingInterface
	{
	    @Override
	    public void syncToValue( PropertyEditor editor, Object newVal )
	    {
		if (newVal == null)
		    setSelected(false);
		else
		    setSelected( ((Set) newVal).contains( putativeElement ) );
	    }
	    
	    @Override
	    public void addUserModificationListeners()
	    {
		ActionListener al = new ActionListener()
		    {
			@Override
			public void actionPerformed( ActionEvent evt )
			{ pcbu.userModification(); }
		    };
		addActionListener( al );
	    }
	    
	    @Override
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
	    
	    @Override
	    public void alertErroneousInput()
	    { Toolkit.getDefaultToolkit().beep(); }
	};
}
