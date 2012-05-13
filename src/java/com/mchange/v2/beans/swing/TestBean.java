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

public class TestBean
{
    String s;
    int    i;
    float  f;

    PropertyChangeSupport pcs = new PropertyChangeSupport( this );

    public String getTheString()
    { return s; }

    public int getTheInt()
    { return i; }

    public float getTheFloat()
    { return f; }

    public void setTheString( String new_s )
    {
	if (! eqOrBothNull( new_s, s ) )
	    {
		String old_s = s;
		this.s = new_s;
		pcs.firePropertyChange( "theString", old_s, s );
	    }
    }

    public void setTheInt( int new_i )
    {
	if ( new_i != i )
	    {
		int old_i = i;
		i = new_i;
		pcs.firePropertyChange( "theInt", old_i, i );
	    }
    }

    public void setTheFloat( float new_f )
    {
	if ( new_f != f )
	    {
		float old_f = f;
		f = new_f;
		pcs.firePropertyChange( "theFloat", new Float(old_f), new Float(f) );
	    }
    }

    public void addPropertyChangeListener( PropertyChangeListener pcl )
    { pcs.addPropertyChangeListener( pcl ); }
    
    public void removePropertyChangeListener( PropertyChangeListener pcl )
    { pcs.removePropertyChangeListener( pcl ); }

    private boolean eqOrBothNull( Object a, Object b )
    {
	return
	    a == b ||
	    (a != null && a.equals(b));
    }
}

