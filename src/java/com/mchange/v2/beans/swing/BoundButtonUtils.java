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
