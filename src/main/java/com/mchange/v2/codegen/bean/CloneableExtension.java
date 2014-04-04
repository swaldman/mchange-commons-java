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

package com.mchange.v2.codegen.bean;

import java.util.*;
import java.io.IOException;
import com.mchange.v2.codegen.IndentedWriter;

public class CloneableExtension implements GeneratorExtension
{
    boolean export_public;
    boolean exception_swallowing;

    String mLoggerName = null;

    public boolean isExportPublic()
    { return export_public; }

    public void setExportPublic(boolean export_public)
    { this.export_public = export_public; }

    public boolean isExceptionSwallowing()
    { return exception_swallowing; }

    public void setExceptionSwallowing(boolean exception_swallowing)
    { this.exception_swallowing = exception_swallowing; }

    public String getMLoggerName()
    { return mLoggerName; }

    public void setMLoggerName( String mLoggerName )
    { this.mLoggerName = mLoggerName; }

    public CloneableExtension(boolean export_public, boolean exception_swallowing)
    { 
	this.export_public = export_public; 
	this.exception_swallowing = exception_swallowing;
    }

    public CloneableExtension()
    { this ( true, false ); }

    public Collection extraGeneralImports()
    { return (mLoggerName == null ? ((Collection) Collections.EMPTY_SET) : ((Collection) Arrays.asList( new String[] {"com.mchange.v2.log"} )) ); }

    public Collection extraSpecificImports()
    { return Collections.EMPTY_SET; }

    public Collection extraInterfaceNames()
    {
	Set set = new HashSet();
	set.add( "Cloneable" );
	return set;
    }

    public void generate(ClassInfo info, Class superclassType, Property[] props, Class[] propTypes, IndentedWriter iw)
	throws IOException
    {
	if (export_public)
	    {
		iw.print("public Object clone()");
		if ( !exception_swallowing )
		    iw.println(" throws CloneNotSupportedException");
		else
		    iw.println();
		iw.println("{");
		iw.upIndent();
		if ( exception_swallowing )
		    {
			iw.println("try");
			iw.println("{");
			iw.upIndent();
		    }
		iw.println( "return super.clone();" );
		if ( exception_swallowing )
		    {
			iw.downIndent();
			iw.println("}");
			iw.println("catch (CloneNotSupportedException e)");
			iw.println("{");
			iw.upIndent();
			if (mLoggerName == null)
			    iw.println("e.printStackTrace();");
			else
			    {
				iw.println("if ( " + mLoggerName + ".isLoggable( MLevel.FINE ) )" );
				iw.upIndent();
				iw.println( mLoggerName + ".log( MLevel.FINE, \"Inconsistent clone() definitions between subclass and superclass! \", e );");
				iw.downIndent();
			    }
			iw.println("throw new RuntimeException(\"Inconsistent clone() definitions between subclass and superclass! \" + e);" );
			iw.downIndent();
			iw.println("}");
		    }
			
		iw.downIndent();
		iw.println("}");
	    }
	//else, write nothing... just add Cloneable to interface definitions...
    }
}
