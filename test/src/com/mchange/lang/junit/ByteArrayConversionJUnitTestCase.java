package com.mchange.lang.junit;

import junit.framework.TestCase;

import com.mchange.lang.ByteUtils;
import com.mchange.lang.CharUtils;
import com.mchange.lang.IntegerUtils;
import com.mchange.lang.LongUtils;
import com.mchange.lang.ShortUtils;

/**
 *  CharUtils, ShortUtils, LongUtils and IntegerUtils all rebuild a primitive from
 *  a byte[] by promoting each byte to an unsigned value and shifting it into place.
 *  They used to call the deprecated ByteUtils.toUnsigned(byte); they now call
 *  ByteUtils.unsignedPromote(byte).
 *
 *  That substitution is only sound while the two promotions agree, so the first test
 *  here pins the equivalence directly rather than trusting it. The round trips then
 *  pin the behavior that depends on it -- a sign-extension slip in the promotion
 *  corrupts exactly the high-bit values these exercise.
 */
public class ByteArrayConversionJUnitTestCase extends TestCase
{
    /**
     *  The presumed-equivalent pair. toUnsigned is deprecated in favor of
     *  unsignedPromote; both remain, so both can be compared.
     */
    @SuppressWarnings("deprecation")
    public void testUnsignedPromoteMatchesDeprecatedToUnsigned()
    {
	for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; ++i)
	    {
		byte b = (byte) i;
		assertEquals("promotions of byte " + i + " should agree",
			     ByteUtils.toUnsigned(b), ByteUtils.unsignedPromote(b));
	    }
    }

    /**
     *  Whatever else changes, a promotion used for this purpose must yield 0..255.
     */
    public void testUnsignedPromoteRange()
    {
	for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; ++i)
	    {
		int promoted = ByteUtils.unsignedPromote((byte) i);
		assertEquals("byte " + i + " should promote to its unsigned value",
			     i & 0xFF, promoted);
	    }
    }

    public void testCharRoundTripsEveryValue()
    {
	byte[] bytes = new byte[2];
	for (int i = 0; i <= 0xFFFF; ++i)
	    {
		char c = (char) i;
		CharUtils.charIntoByteArray(c, 0, bytes);
		assertEquals("char " + i + " should round trip",
			     c, CharUtils.charFromByteArray(bytes, 0));
	    }
    }

    public void testShortRoundTripsEveryValue()
    {
	byte[] bytes = new byte[2];
	for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; ++i)
	    {
		short s = (short) i;
		ShortUtils.shortIntoByteArray(s, 0, bytes);
		assertEquals("short " + i + " should round trip",
			     s, ShortUtils.shortFromByteArray(bytes, 0));
	    }
    }

    public void testIntRoundTripsBothEndians()
    {
	byte[] bytes = new byte[4];
	int[] samples = { 0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE,
			  0x80000000, 0x00FF00FF, 0xFF00FF00, 0x01020304 };
	for (int i = 0; i < samples.length; ++i)
	    {
		IntegerUtils.intIntoByteArray(samples[i], 0, bytes);
		assertEquals("int " + samples[i] + " big endian",
			     samples[i], IntegerUtils.intFromByteArray(bytes, 0));

		IntegerUtils.intIntoByteArrayLittleEndian(samples[i], 0, bytes);
		assertEquals("int " + samples[i] + " little endian",
			     samples[i], IntegerUtils.intFromByteArrayLittleEndian(bytes, 0));
	    }
    }

    public void testLongRoundTripsBothEndians()
    {
	byte[] bytes = new byte[8];
	long[] samples = { 0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE,
			   0x8000000000000000L, 0x00FF00FF00FF00FFL,
			   0xFF00FF00FF00FF00L, 0x0102030405060708L };
	for (int i = 0; i < samples.length; ++i)
	    {
		LongUtils.longIntoByteArray(samples[i], 0, bytes);
		assertEquals("long " + samples[i] + " big endian",
			     samples[i], LongUtils.longFromByteArray(bytes, 0));

		LongUtils.longIntoByteArrayLittleEndian(samples[i], 0, bytes);
		assertEquals("long " + samples[i] + " little endian",
			     samples[i], LongUtils.longFromByteArrayLittleEndian(bytes, 0));
	    }

	java.util.Random r = new java.util.Random(42);
	for (int i = 0; i < 50000; ++i)
	    {
		long l = r.nextLong();
		LongUtils.longIntoByteArray(l, 0, bytes);
		assertEquals("random long big endian", l, LongUtils.longFromByteArray(bytes, 0));
		LongUtils.longIntoByteArrayLittleEndian(l, 0, bytes);
		assertEquals("random long little endian", l, LongUtils.longFromByteArrayLittleEndian(bytes, 0));
	    }
    }

    /**
     *  Byte sequences with the high bit set everywhere, decoded against values worked
     *  out by hand. A promotion that sign extends passes the round trips above only
     *  by cancelling out; these catch it outright.
     */
    public void testKnownHighBitSequences()
    {
	byte[] allOnes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			   (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
	assertEquals("eight 0xFF bytes should be -1L", -1L, LongUtils.longFromByteArray(allOnes, 0));
	assertEquals("four 0xFF bytes should be -1", -1, IntegerUtils.intFromByteArray(allOnes, 0));
	assertEquals("two 0xFF bytes should be -1", (short) -1, ShortUtils.shortFromByteArray(allOnes, 0));
	assertEquals("two 0xFF bytes should be 0xFFFF", (char) 0xFFFF, CharUtils.charFromByteArray(allOnes, 0));

	byte[] highOnly = { (byte) 0x80, 0, 0, 0, 0, 0, 0, 0 };
	assertEquals("0x80 then zeros should be Long.MIN_VALUE",
		     Long.MIN_VALUE, LongUtils.longFromByteArray(highOnly, 0));
	assertEquals("0x80 then zeros should be Integer.MIN_VALUE",
		     Integer.MIN_VALUE, IntegerUtils.intFromByteArray(highOnly, 0));

	byte[] mixed = { (byte) 0x80, (byte) 0x81 };
	assertEquals("0x80 0x81 should be char 0x8081", (char) 0x8081, CharUtils.charFromByteArray(mixed, 0));
	assertEquals("0x80 0x81 should be short 0x8081", (short) 0x8081, ShortUtils.shortFromByteArray(mixed, 0));
    }
}
