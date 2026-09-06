package com.mchange.v1.db.sql.schemarep.junit;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import junit.framework.TestCase;

import com.mchange.v1.db.sql.schemarep.ColumnRep;
import com.mchange.v1.db.sql.schemarep.ColumnRepImpl;
import com.mchange.v1.db.sql.schemarep.ForeignKeyRepImpl;
import com.mchange.v1.db.sql.schemarep.UniquenessConstraintRepImpl;
import com.mchange.v1.db.sql.schemarep.TableRepImpl;

/**
 *  These reps moved off three helpers: ColumnRepImpl.hashCode from the deprecated
 *  com.mchange.lang.ArrayUtils.hashAll to java.util.Arrays.hashCode, and TableRepImpl
 *  and UniquenessConstraintRepImpl from com.mchange.v1.util MapUtils and SetUtils to
 *  Map's and Set's own equals and hashCode.
 *
 *  Each replacement was taken to be equivalent for these types, which holds because
 *  every one of these fields is a defensive copy into a hash based collection: no
 *  caller supplied Comparator ever reaches a comparison. Hash *values* changed; what
 *  must hold is the equals/hashCode contract and the equality semantics themselves.
 *
 *  Fields compared by equals are also checked to participate in hashCode, since a
 *  dropped term breaks no contract and would otherwise go unnoticed.
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

    private static TableRepImpl table(List cols, Set pk, Set fks, Set ucs)
    { return new TableRepImpl("mytable", cols, pk, fks, ucs); }

    private static Set pk()
    {
	Set out = new HashSet();
	out.add("a");
	return out;
    }

    private static Set fks(String refTable)
    {
	List loc = new ArrayList(); loc.add("a");
	List ref = new ArrayList(); ref.add("id");
	Set out = new HashSet();
	out.add( new ForeignKeyRepImpl( loc, refTable, ref ) );
	return out;
    }

    private static Set ucs(String colName)
    {
	Set cols = new HashSet();
	cols.add( colName );
	Set out = new HashSet();
	out.add( new UniquenessConstraintRepImpl( cols ) );
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

    /**
     *  The foreign key and uniqueness constraint sets are compared the same way the
     *  primary key set is. The tests above leave both empty, so they are exercised
     *  here with contents.
     */
    public void testTableRepDistinguishesForeignKeyReps()
    {
	TableRepImpl a = table( cols(new int[] {10}), pk(), fks("other"), null );
	TableRepImpl b = table( cols(new int[] {10}), pk(), fks("other"), null );
	TableRepImpl c = table( cols(new int[] {10}), pk(), fks("different"), null );

	assertEquals( "matching foreign keys should be equal", a, b );
	assertEquals( "matching foreign keys should hash alike", a.hashCode(), b.hashCode() );
	assertFalse( "a differing foreign key should make tables unequal", a.equals(c) );
	assertTrue( "foreign keys should participate in the hash",
		    a.hashCode() != c.hashCode() );
    }

    public void testTableRepDistinguishesUniquenessConstraintReps()
    {
	TableRepImpl a = table( cols(new int[] {10}), pk(), null, ucs("b") );
	TableRepImpl b = table( cols(new int[] {10}), pk(), null, ucs("b") );
	TableRepImpl c = table( cols(new int[] {10}), pk(), null, ucs("a") );

	assertEquals( "matching uniqueness constraints should be equal", a, b );
	assertEquals( "matching uniqueness constraints should hash alike", a.hashCode(), b.hashCode() );
	assertFalse( "a differing uniqueness constraint should make tables unequal", a.equals(c) );
	assertTrue( "uniqueness constraints should participate in the hash",
		    a.hashCode() != c.hashCode() );
    }

    public void testTableRepDistinguishesPopulatedFromEmptySets()
    {
	TableRepImpl bare     = table( cols(new int[] {10}), pk() );
	TableRepImpl withFks  = table( cols(new int[] {10}), pk(), fks("other"), null );
	TableRepImpl withUcs  = table( cols(new int[] {10}), pk(), null, ucs("b") );

	assertFalse( "a table with foreign keys differs from one without", bare.equals(withFks) );
	assertFalse( "a table with uniqueness constraints differs from one without", bare.equals(withUcs) );
	assertFalse( "foreign keys and uniqueness constraints are distinct fields",
		     withFks.equals(withUcs) );
	assertTrue( "an empty and a populated foreign key set should participate in the hash",
		    bare.hashCode() != withFks.hashCode() );
	assertTrue( "an empty and a populated uniqueness constraint set should participate in the hash",
		    bare.hashCode() != withUcs.hashCode() );
    }

    public void testEqualUniquenessConstraintRepsAgreeAndHashAlike()
    {
	Set one = new HashSet(); one.add("a"); one.add("b");
	Set two = new HashSet(); two.add("b"); two.add("a");

	UniquenessConstraintRepImpl x = new UniquenessConstraintRepImpl( one );
	UniquenessConstraintRepImpl y = new UniquenessConstraintRepImpl( two );

	assertEquals( "same columns in any order should be equal", x, y );
	assertEquals( "same columns in any order should hash alike", x.hashCode(), y.hashCode() );

	Set other = new HashSet(); other.add("a"); other.add("c");
	assertFalse( "differing columns should be unequal",
		     x.equals( new UniquenessConstraintRepImpl( other ) ) );
    }

    /**
     *  UniquenessConstraintRepImpl copies its argument into a HashSet, so a sorted
     *  input -- even one ordered by a Comparator inconsistent with equals -- yields a
     *  rep equal to one built from a plain Set. This is why comparing the copies with
     *  Set.equals rather than a sort agnostic helper is safe.
     */
    public void testUniquenessConstraintRepIgnoresInputCollectionFlavor()
    {
	Set plain = new HashSet();
	plain.add("Alpha"); plain.add("BRAVO");

	Set sorted = new TreeSet( String.CASE_INSENSITIVE_ORDER );
	sorted.add("Alpha"); sorted.add("BRAVO");

	UniquenessConstraintRepImpl fromPlain  = new UniquenessConstraintRepImpl( plain );
	UniquenessConstraintRepImpl fromSorted = new UniquenessConstraintRepImpl( sorted );

	assertFalse( "the stored set must not be a SortedSet",
		     fromSorted.getUniqueColumnNames() instanceof SortedSet );
	assertEquals( "a sorted input should yield an equal rep", fromPlain, fromSorted );
	assertEquals( "a sorted input should yield an equal hash",
		      fromPlain.hashCode(), fromSorted.hashCode() );
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
