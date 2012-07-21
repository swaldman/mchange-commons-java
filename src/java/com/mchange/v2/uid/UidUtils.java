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

    private static String generateVmId()
    {
        DataOutputStream dos = null;
        DataInputStream  dis = null;
        try
        {
            SecureRandom srand = new SecureRandom();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            dos = new DataOutputStream( baos );
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

    private UidUtils()
    {}
}