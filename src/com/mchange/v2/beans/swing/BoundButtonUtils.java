package com.mchange.v2.beans.swing;

import java.beans.IntrospectionException;
import javax.swing.AbstractButton;

public final class BoundButtonUtils
{
    public static void bindToSetProperty(AbstractButton[] buttons, Object[] elements, Object bean, String setPropName) throws IntrospectionException
    { SetPropertyElementBoundButtonModel.bind(buttons, elements, bean, setPropName); }

    public static void bindAsRadioButtonsToProperty(AbstractButton[] buttons, Object[] values, Object bean, String propName) throws IntrospectionException
    {
	PropertyBoundButtonGroup daGroup = new PropertyBoundButtonGroup( bean, propName );
	for (int i = 0; i < buttons.length; ++ i)
	    daGroup.add( buttons[i], values[i] );
    }
}
