package com.mchange.v2.uid;

import java.net.InetAddress;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

import com.mchange.v2.log.MLevel;
import com.mchange.v2.log.MLog;
import com.mchange.v2.log.MLogger;

public final class UidUtils
{
    final static MLogger logger = MLog.getLogger( UidUtils.class );

    public final static String VM_ID = generateVmId();

    //MT: protected by class lock
    private static long within_vm_seq_counter = 0;

    private static String generateVmId()
    {
        DataOutputStream dos = null;
        DataInputStream  dis = null;
        try
        {
            SecureRandom srand = new SecureRandom();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            dos = new DataOutputStream( baos );

            // we used to have buggy code that intended to write a four-byte
            // local IP address, then the long currentTimeMillis, then pad
            // to a multiple of four bytes if somehow we weren't a multiple
            // of four bytes, as we expected to be.
            //
            // in fact, what we historically did by virtue of erroneously
            // using dos.write(int b) rather than dos.writeInt(int i),
            // was write four bytes of localhost's IP address (or one random
            // byte if the lookup of localhost failed), then an 8-byte long,
            // then one random byte, then three more random bytes as the padding.
            //
            // no one should be relying on any components of the ID produced
            // here. it was intended as an opaque unique identifier, that used
            // my younger self's weak intuition for how best to guarantee
            // uniqueness. randomness is probably better for that than IP
            // addresses anyway, which might be commonly used 127.0.0.1
            // addresses or 192.168.x.x
            //
            // in any case, we now keep the inadvertent format we introduced
            // decades ago, which in the typical case (where the lookup of
            // localhost succeeded) was four bytes (then an IP address, now
            // random), then the 8-byte-UNIX-time-in-millis, then four random
            // bytes more, one where four were intended from dos.write(int b),
            // then three added as padding for a total of 16 bytes.

            /*
            try
            {
                dos.write( InetAddress.getLocalHost().getAddress() );
            }
            catch (Exception e)
            {
                if (logger.isLoggable(MLevel.INFO))
                    logger.log(MLevel.INFO, "Failed to get local InetAddress for VMID. This is unlikely to matter. At all. We'll add some extra randomness", e);
                dos.write( srand.nextInt() );
            }
            dos.writeLong(System.currentTimeMillis());
            dos.write( srand.nextInt() );

            int remainder = baos.size() % 4; //if it wasn't a 4 byte inet address
            if (remainder > 0)
            {
                int pad = 4 - remainder;
                byte[] pad_bytes = new byte[pad];
                srand.nextBytes(pad_bytes);
                dos.write(pad_bytes);
            }
            */

            dos.writeInt( srand.nextInt() );           // write four random bytes
            dos.writeLong(System.currentTimeMillis()); // write eight bytes, unix time in millis
            dos.writeInt( srand.nextInt() );           // write four random bytes

            // but we won't check the length and pad our own bugs,
            // instead we'll test that our string resulted from the expected 16 bytes

            StringBuffer sb = new StringBuffer(32);
            byte[] vmid_bytes = baos.toByteArray();
            dis = new DataInputStream(new ByteArrayInputStream( vmid_bytes ) );
            for (int i = 0, num_ints = vmid_bytes.length / 4; i < num_ints; ++i)
            {
                int signed = dis.readInt();
                long unsigned = ((long) signed) & 0x00000000FFFFFFFFL; 
                sb.append(Long.toString(unsigned, Character.MAX_RADIX));
            }
            return sb.toString();
        }
        catch (IOException e)
        {
            if (logger.isLoggable(MLevel.WARNING))
                logger.log(MLevel.WARNING, 
                           "Bizarro! IOException while reading/writing from ByteArray-based streams? " +
                           "We're skipping the VMID thing. It almost certainly doesn't matter, " +
                           "but please report the error.", 
                           e);
            return "";
        }
        finally
        {
            // this is like total overkill for byte-array based streams,
            // but it's a good habit
            try { if (dos != null) dos.close(); }
            catch ( IOException e )
            { logger.log(MLevel.WARNING, "Huh? Exception close()ing a byte-array bound OutputStream.", e); }
            try { if (dis != null) dis.close(); }
            catch ( IOException e )
            { logger.log(MLevel.WARNING, "Huh? Exception close()ing a byte-array bound IntputStream.", e); }
        }
    }

    private synchronized static long nextWithinVmSeq()
    { return ++within_vm_seq_counter; }

    public static String allocateWithinVmSequential()
    { return VM_ID + "#" + nextWithinVmSeq(); }

    private UidUtils()
    {}
}
