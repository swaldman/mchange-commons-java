package com.mchange.v2.uid.junit;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;

import com.mchange.v2.uid.UidUtils;

import junit.framework.TestCase;

/**
 *  VM_ID is an opaque identifier and nothing should read structure out of it. What it does owe
 *  callers is a byte budget: it is 16 bytes -- four random, the 8-byte clock, four more random
 *  -- packed as four unsigned ints rendered in Character.MAX_RADIX. These tests hold that
 *  budget, because the generator has twice been wrong about it in ways nothing else noticed.
 *
 *  <p>The original wrote the local IP address, the clock, one random byte where four were
 *  intended -- <code>DataOutputStream.write(int)</code> writes a single byte, while
 *  <code>writeInt(int)</code> writes four -- and then padded to a multiple of four, which
 *  silently supplied the three bytes the short write had dropped. Sixteen bytes went in, so the
 *  shortfall was invisible. The replacement writes four random bytes, the clock, and four more:
 *  the same sixteen bytes, twice the entropy, and no hostname resolution. A first attempt at it
 *  emitted twelve bytes, having read the historical address write as one byte rather than four.
 *  Nothing failed.</p>
 *
 *  <p><b>Why the length cannot be the test.</b> Each int is rendered without padding and the
 *  groups are concatenated with no delimiter, so a VM_ID is not uniquely parseable and its
 *  length barely discriminates: over 20,000 samples a 16-byte packing spans 18..22 characters
 *  and a 12-byte packing spans 15..21. Any threshold in that overlap both fails on correct code
 *  and passes on the broken code most of the time.</p>
 *
 *  <p>What does discriminate is where the clock lands. In a 16-byte packing the 8-byte clock
 *  occupies the second and third int groups exactly, so the rendering contains
 *  <code>base36(millis &gt;&gt;&gt; 32) + base36(millis &amp; 0xFFFFFFFF)</code> as a contiguous
 *  substring. Shift the budget and the clock straddles the group boundaries, and that substring
 *  cannot appear. Over 3,000 samples this held 3000/3000 for sixteen bytes and 0/3000 for
 *  twelve.</p>
 */
public class UidUtilsJUnitTestCase extends TestCase
{
    /** Bound on how far back to look for the millisecond VM_ID was built from. */
    private final static long MAX_SCAN_MILLIS = 30L * 60 * 1000;

    private static String clockSignature( long millis )
    {
        long hi = (millis >>> 32) & 0xFFFFFFFFL;
        long lo = millis & 0xFFFFFFFFL;
        return Long.toString( hi, Character.MAX_RADIX ) + Long.toString( lo, Character.MAX_RADIX );
    }

    /**
     *  VM_ID is built when UidUtils initializes, which cannot precede JVM start and may already
     *  have happened in an earlier test, so the candidate window runs from JVM start to now.
     */
    public void testVmIdPacksTheClockAsTwoWholeIntGroups()
    {
        String vmid = UidUtils.VM_ID;
        assertNotNull( vmid );

        long now  = System.currentTimeMillis();
        long from = Math.max( ManagementFactory.getRuntimeMXBean().getStartTime() - 1000,
                              now - MAX_SCAN_MILLIS );
        long to   = now + 1000;

        // The clock must sit in groups two and three, which means a non-empty group one before
        // it and a non-empty group four after it. Requiring both is what distinguishes sixteen
        // bytes from a short write: 4 + 8 + 1 bytes renders as only three groups, because the
        // packing loop reads length/4 and silently discards the thirteenth byte -- leaving the
        // clock signature in place with nothing following it.
        boolean found = false;
        for ( long m = from; m <= to && ! found; ++m )
        {
            String sig = clockSignature( m );
            for ( int at = vmid.indexOf( sig ); at >= 0; at = vmid.indexOf( sig, at + 1 ) )
                if ( at > 0 && at + sig.length() < vmid.length() ) { found = true; break; }
        }

        assertTrue( "VM_ID should contain base36(millis >>> 32) + base36(millis & 0xFFFFFFFF) as a " +
                    "contiguous substring with at least one character on either side. That holds only " +
                    "if the 8-byte clock occupies groups two and three of four -- that is, only if the " +
                    "VMID is the expected 16 bytes, with four random bytes before the clock and four " +
                    "after. Searched " + ((to - from) + 1) + " candidate milliseconds. VM_ID: " + vmid,
                    found );
    }

    /** Base-36 digits only, since every group is rendered with Character.MAX_RADIX. */
    public void testVmIdRendersAsBaseThirtySixDigits()
    {
        String vmid = UidUtils.VM_ID;
        assertTrue( "VM_ID should be base-36 digits only: " + vmid, vmid.matches( "[0-9a-z]+" ) );
        assertTrue( "VM_ID should not be empty -- generateVmId returns \"\" only on an IOException " +
                    "from byte-array streams, which should not happen.", vmid.length() > 0 );
    }

    /** VM_ID is fixed for the life of the JVM; only the sequence advances. */
    public void testSequentialIdsAreUniqueAndCarryTheVmId()
    {
        String vmid = UidUtils.VM_ID;
        Set<String> seen = new HashSet<String>();
        for ( int i = 0; i < 100; ++i )
        {
            String id = UidUtils.allocateWithinVmSequential();
            assertTrue( "Every sequential id carries the VM_ID prefix: " + id,
                        id.startsWith( vmid + "#" ) );
            assertTrue( "Sequential ids must not repeat: " + id, seen.add( id ) );
        }
        assertEquals( "VM_ID must not change within a JVM.", vmid, UidUtils.VM_ID );
    }
}
