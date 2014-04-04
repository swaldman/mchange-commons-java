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

    private synchronized static long nextWithinVmSeq()
    { return ++within_vm_seq_counter; }

    public static String allocateWithinVmSequential()
    { return VM_ID + "#" + nextWithinVmSeq(); }

    private UidUtils()
    {}
}
