/*
 * Distributed as part of mchange-commons-java v.0.2.3
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



