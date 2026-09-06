package com.mchange.v1.db.sql.schemarep.junit;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

import com.mchange.v1.db.sql.schemarep.ColumnRep;
import com.mchange.v1.db.sql.schemarep.ColumnRepImpl;
import com.mchange.v1.db.sql.schemarep.TableRepImpl;

/**
 *  ColumnRepImpl.hashCode moved from the deprecated com.mchange.lang.ArrayUtils.hashAll
 *  to java.util.Arrays.hashCode, and TableRepImpl moved from the deprecated
 *  com.mchange.v1.util.MapUtils to Map's own equals/hashCode.
 *
 *  Both replacements were taken to be equivalent for these types. The hash *values*
 *  changed (MapUtils xor'd entry hashes where Map.hashCode sums them); what must hold
 *  is the equals/hashCode contract and the equality semantics themselves.
 */
public class SchemaRepEqualityJUnitTestCase extends TestCase
{
    private static ColumnRep col(String name, int type, int[] size, boolean nullable, Object dflt)
    { return new ColumnRepImpl(name, type, size, nullable, dflt); }

    private static List cols(int[] bSize)
    {
	List out = new ArrayList();
	out.add( col("a", Types.INTEGER, null, true, null) );
	out.add( col("b", Types.VARCHAR, bSize, false, "x") );
	return out;
    }

    private static TableRepImpl table(List cols, Set pk)
    { return new TableRepImpl("mytable", cols, pk, null, null); }

    private static Set pk()
    {
	Set out = new HashSet();
	out.add("a");
	return out;
    }

    public void testEqualColumnRepsAgreeAndHashAlike()
    {
	ColumnRep x = col("b", Types.VARCHAR, new int[] {10}, false, "x");
	ColumnRep y = col("b", Types.VARCHAR, new int[] {10}, false, "x");

	assertEquals("equal column reps should be equal", x, y);
	assertEquals("equal column reps should hash alike", x.hashCode(), y.hashCode());
    }

    public void testColumnRepDistinguishesEveryField()
    {
	ColumnRep base = col("b", Types.VARCHAR, new int[] {10}, false, "x");

	assertFalse("name should matter",     base.equals( col("c", Types.VARCHAR, new int[] {10}, false, "x") ));
	assertFalse("type should matter",     base.equals( col("b", Types.INTEGER, new int[] {10}, false, "x") ));
	assertFalse("size should matter",     base.equals( col("b", Types.VARCHAR, new int[] {20}, false, "x") ));
	assertFalse("null size should matter",base.equals( col("b", Types.VARCHAR, null,           false, "x") ));
	assertFalse("nullability should matter", base.equals( col("b", Types.VARCHAR, new int[] {10}, true, "x") ));
	assertFalse("default should matter",  base.equals( col("b", Types.VARCHAR, new int[] {10}, false, "y") ));
    }

    /**
     *  The colSize hash specifically: Arrays.hashCode must be sensitive to contents,
     *  which is what the replaced ArrayUtils.hashAll provided.
     */
    public void testColumnRepHashReflectsColumnSize()
    {
	ColumnRep ten    = col("b", Types.VARCHAR, new int[] {10}, false, "x");
	ColumnRep twenty = col("b", Types.VARCHAR, new int[] {20}, false, "x");
	assertTrue("differing column sizes should normally hash differently",
		   ten.hashCode() != twenty.hashCode());
    }

    public void testEqualTableRepsAgreeAndHashAlike()
    {
	TableRepImpl a = table( cols(new int[] {10}), pk() );
	TableRepImpl b = table( cols(new int[] {10}), pk() );

	assertEquals("equal table reps should be equal", a, b);
	assertEquals("equal table reps should hash alike", a.hashCode(), b.hashCode());
    }

    public void testTableRepDistinguishesColumnDifferences()
    {
	TableRepImpl a = table( cols(new int[] {10}), pk() );
	TableRepImpl c = table( cols(new int[] {20}), pk() );

	assertFalse("differing column sizes should make tables unequal", a.equals(c));
	assertTrue("differing tables should normally hash differently", a.hashCode() != c.hashCode());
    }

    /**
     *  Map.equals and Set.equals disregard iteration order, which is what the replaced
     *  "DisregardingSort" helpers were named for. TableRepImpl copies its inputs into
     *  hash based collections, so a sorted input must produce an equal rep.
     */
    public void testTableRepIgnoresInputCollectionFlavor()
    {
	Set hashPk = new HashSet();
	hashPk.add("a");
	Set treePk = new TreeSet();
	treePk.add("a");

	TableRepImpl a = table( cols(new int[] {10}), hashPk );
	TableRepImpl b = table( cols(new int[] {10}), treePk );

	assertEquals("a sorted key set should yield an equal rep", a, b);
	assertEquals("a sorted key set should yield an equal hash", a.hashCode(), b.hashCode());
    }

    public void testTableRepUsableAsHashKey()
    {
	TableRepImpl a = table( cols(new int[] {10}), pk() );
	TableRepImpl b = table( cols(new int[] {10}), pk() );
	TableRepImpl c = table( cols(new int[] {20}), pk() );

	Set set = new HashSet();
	set.add(a);

	assertTrue("an equal rep should be found", set.contains(b));
	assertFalse("a differing rep should not be found", set.contains(c));
    }
}
