package com.mchange.v1.db.sql.schemarep;

import java.util.Arrays;
import com.mchange.lang.ArrayUtils;

public class ColumnRepImpl implements ColumnRep
{
    String  colName;
    int     col_type;
    int[]   colSize;
    boolean accepts_nulls;
    Object  defaultValue;

    public ColumnRepImpl(String colName, int col_type)
    { this(colName, col_type, null); }

    public ColumnRepImpl(String colName, int col_type, int[] colSize)
    { this(colName, col_type, colSize, false, null); }

    public ColumnRepImpl(String colName, int col_type, int[] colSize, 
			 boolean accepts_nulls, Object defaultValue)
    {
	this.colName = colName;
	this.col_type = col_type;
	this.colSize = colSize;
	this.accepts_nulls = accepts_nulls;
	this.defaultValue = defaultValue;
    }

    public String getColumnName()
    { return colName; }

    public int getColumnType()
    { return col_type; }

    public int[] getColumnSize()
    { return colSize; }

    public boolean acceptsNulls()
    { return accepts_nulls; }

    public Object getDefaultValue()
    { return defaultValue; }

    public boolean equals( Object o )
    {
	if (o == null || this.getClass() != o.getClass())
	    return false;

	ColumnRepImpl other = (ColumnRepImpl) o;
	if (!this.colName.equals( other.colName ) ||
	    this.col_type != other.col_type ||
	    this.accepts_nulls != other.accepts_nulls)
	    return false;
	
	if (this.colSize != other.colSize && !Arrays.equals(this.colSize, other.colSize)) 
	    return false;
	
	if (this.defaultValue != other.defaultValue &&
	    this.defaultValue != null && 
	    !this.defaultValue.equals( other.defaultValue ))
	    return false;

	return true;
    }

    public int hashCode()
    {
	int out = 
	    colName.hashCode() ^
	    col_type;

	if (! accepts_nulls) out = ~out;

	if (colSize != null)
	    out ^= ArrayUtils.hashAll(colSize);

	if (defaultValue != null)
	    out ^=defaultValue.hashCode();
 
	return out;
    }
}



