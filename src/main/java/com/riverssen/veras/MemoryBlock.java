package com.riverssen.veras;

import com.riverssen.veras.exceptions.MemoryException;

public interface MemoryBlock {
    int malloc(int size) throws MemoryException;
    int calloc(int size, int length) throws MemoryException, IndexOutOfBoundsException;
    int realloc(int uptr, int resize) throws MemoryException, IndexOutOfBoundsException;
    int reallocIfAvailable(int address, int resize);
    void delete(int uptr) throws MemoryException, IndexOutOfBoundsException;
    void memcpy(int dst, int src, int length) throws MemoryException;
    void memcom(int dst, int src0, int src1, int length) throws MemoryException, IndexOutOfBoundsException;
    void free(int ptr);
    int getAvailableBlockFollowing(int address);
    void combine();
    String getSnapshot();
    void setByte(int address, byte b) throws IndexOutOfBoundsException;
    void setShort(int address, short s) throws IndexOutOfBoundsException;
    void setInt(int address, int s) throws IndexOutOfBoundsException;
    void setLong(int address, long s) throws IndexOutOfBoundsException;
    byte getByte(int address) throws IndexOutOfBoundsException;
    short getShort(int address) throws IndexOutOfBoundsException;
    int getInt(int address) throws IndexOutOfBoundsException;
    long getLong(int address) throws IndexOutOfBoundsException;
    int sizeof(int address);

    byte[] getArray(int length, int address);
}