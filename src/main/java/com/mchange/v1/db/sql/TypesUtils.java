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

package com.mchange.v1.db.sql;

import java.sql.Types;

public final class TypesUtils
{
    public static String getNameForSqlTypeCode(int type_code)
	throws UnsupportedTypeException
    {
	switch ( type_code )
	    {
	    case Types.BIT:
		return "BIT";
	    case Types.TINYINT:
		return "TINYINT";
	    case Types.SMALLINT:
		return "SMALLINT";
	    case Types.INTEGER:
		return "INTEGER";
	    case Types.BIGINT:
		return "BIGINT";
	    case Types.FLOAT:
		return "FLOAT";
	    case Types.REAL:
		return "REAL";
	    case Types.DOUBLE:
		return "DOUBLE";
	    case Types.NUMERIC:
		return "NUMERIC";
	    case Types.DECIMAL:
		return "DECIMAL";
	    case Types.CHAR:
		return "CHAR";
	    case Types.VARCHAR:
		return "VARCHAR";
	    case Types.LONGVARCHAR:
		return "LONGVARCHAR";
	    case Types.DATE:
		return "DATE";
	    case Types.TIME:
		return "TIME";
	    case Types.TIMESTAMP:
		return "TIMESTAMP";
	    case Types.BINARY:
		return "BINARY";
	    case Types.VARBINARY:
		return "VARBINARY";
	    case Types.LONGVARBINARY:
		return "LONGVARBINARY";
	    case Types.NULL:
		return "NULL";
	    case Types.OTHER:
		throw new UnsupportedTypeException("Type OTHER cannot be" + 
						   " represented as a String.");
	    case Types.JAVA_OBJECT:
		throw new UnsupportedTypeException("Type JAVA_OBJECT cannot be" +
						   " represented as a String.");
	    case Types.REF:
		return "REF";
	    case Types.STRUCT:
		return "STRUCT";
	    case Types.ARRAY:
		return "ARRAY";
	    case Types.BLOB:
		return "BLOB";
	    case Types.CLOB:
		return "CLOB";
	    default:
		throw new UnsupportedTypeException("Type code: " + type_code + " is unknown.");
	    }
    }

    private TypesUtils()
    {}
}



