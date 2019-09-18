package com.riverssen.veras;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

public class Utils {
    public static final byte[] deflate(byte bytes[]) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(stream);

        deflaterOutputStream.write(bytes);
        deflaterOutputStream.flush();
        deflaterOutputStream.close();

        return stream.toByteArray();
    }

    public static final byte[] inflate(byte bytes[]) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(stream);

        inflaterOutputStream.write(bytes);
        inflaterOutputStream.flush();
        inflaterOutputStream.close();

        return stream.toByteArray();
    }

    public static final byte[] concatenate(byte[]...arrays)
    {
        int size = 0;
        for(byte[] array : arrays)
            size += array.length;

        byte concatenated[] = new byte[size];

        int index = 0;

        for(byte[] array : arrays)
        {
            System.arraycopy(array, 0, concatenated, index, array.length);

            index += array.length;
        }

        return concatenated;
    }

    public static byte[] sha256(byte data[])
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256", "BC");
            byte[] hash = digest.digest(data);
            return hash;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static byte[] sha512(byte data[]) {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-512", "BC");
            byte[] hash = digest.digest(data);
            return hash;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static byte[] sha256d(byte data[])
    {
        return sha256(sha256(data));
    }
    public static byte[] sha512d(byte data[])
    {
        return sha512(sha512(data));
    }

    public static byte[] ripemd160(byte data[])
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("RIPEMD160", "BC");
            byte[] hash = digest.digest(data);
            return hash;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static short makeShort(byte b1, byte b0) {
        return (short) (((b1 & 0xff) <<  8) | ((b0 & 0xff)));
    }

    public static int makeInt(byte b3, byte b2, byte b1, byte b0) {
        return (((b3       ) << 24) |
                ((b2 & 0xff) << 16) |
                ((b1 & 0xff) <<  8) |
                ((b0 & 0xff)      ));
    }

    public static long makeLong(byte b7, byte b6, byte b5, byte b4,
                                  byte b3, byte b2, byte b1, byte b0)
    {
        return ((((long)b7       ) << 56) |
                (((long)b6 & 0xff) << 48) |
                (((long)b5 & 0xff) << 40) |
                (((long)b4 & 0xff) << 32) |
                (((long)b3 & 0xff) << 24) |
                (((long)b2 & 0xff) << 16) |
                (((long)b1 & 0xff) <<  8) |
                (((long)b0 & 0xff)      ));
    }

    public static byte[] trim(byte[] bytes, int i, int i1) {
        byte new_bytes[]    = new byte[i1 - i];

        int free            = 0;

        for(int index = i; index < i1; index ++)
            new_bytes[free ++] = bytes[index];
        return new_bytes;
    }

    public static boolean equals(byte[] trim, byte[] trim1) {
        if(trim.length != trim1.length) return false;

        for(int i = 0; i < trim1.length; i ++)
            if(trim[i] != trim1[i]) return false;

        return true;
    }

    public static byte[] int24(int i)
    {
        return new byte[] {
                (byte) ((i >> 16) & 0xFF),
                (byte) ((i >>  8) & 0xFF),
                (byte) ((i) & 0xFF)};
    }

    public static long getTimeHours(int numHours, short mins, short seconds)
    {
        return (numHours * (3600) + (mins * 60) + seconds) * 1000L;
    }

    public static long getTimeHours(int numHours)
    {
        return numHours * 3600_000L;
    }

    public static long getTimeMinutes(int numMinutes)
    {
        return numMinutes * 60_000L;
    }

    public static byte[] reverseBytes(byte[] bytes) {
        byte[] buf = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            buf[i] = bytes[bytes.length - 1 - i];
        return buf;
    }
}
