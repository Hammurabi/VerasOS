package com.riverssen.veras;

import com.riverssen.veras.exceptions.MemoryException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class MemoryBlockImpl implements MemoryBlock {
    private final byte                      heapBuffer[];
    private final Map<Integer, Integer>     available;
    private final Map<Integer, Integer>     addresses;

    public MemoryBlockImpl(final int size) {
        this.heapBuffer = new byte[size];
        this.available = new LinkedHashMap<>();
        this.addresses = new LinkedHashMap<>();
        available.put(1, size);
    }

    public int malloc(int size) throws MemoryException {
        if (size == 0)
            return 0;

        int pointer = 0x0;
        Integer toRemove = null;

        for (Integer available : available.keySet()) {
            int src = available;
            int dst = this.available.get(available);
            int available_size = dst - src;
            if (available_size >= size) {
                pointer = available;
                addresses.put(pointer, size);

                if (available_size - size > 0)
                    this.available.put(pointer + size, dst);

                toRemove = available;

                break;
            }
        }

        if (pointer == 0)
            throw new MemoryException("cannot allocate a pointer of size '" + size + "', heap is too fragmented.");

        if (toRemove != null)
            available.remove(toRemove);

        return pointer;
    }

    public int calloc(int size, int length) throws MemoryException, IndexOutOfBoundsException {
        if (length > size)
            throw new MemoryException("calloc(a, b), b is bigger than a.");

        int pointer = malloc(size);

        for (int i = 0; i < length; i++)
            setByte(i + pointer, (byte) 0);

        return pointer;
    }

    public int realloc(int uptr, int resize) throws MemoryException, IndexOutOfBoundsException {
        if (!addresses.containsKey(uptr))
            throw new MemoryException("cannot perform realloc(" + Long.toHexString(uptr) + ", " + resize + ")");

        int nPointer = malloc(resize);

        memcpy(nPointer, uptr, addresses.get(uptr));

        return nPointer;
    }

    @Override
    public int reallocIfAvailable(int address, int resize) {
        try{
            int nAddress = realloc(address, resize);

            return nAddress;
        } catch (Exception e)
        {
            return address;
        }
    }

    public void delete(int uptr) throws MemoryException, IndexOutOfBoundsException {
        if (addresses.containsKey(uptr)) {
            free(uptr);
            return;
        }

        throw new MemoryException("address '" + Long.toString(uptr, 16) + "' is not a valid pointer.");
    }

    public void memcpy(int dst, int src, int length) throws MemoryException {
        try {
            for (int i = 0; i < length; i++)
                setByte(dst + i, getByte(src + i));
        } catch (Exception e) {
            throw new MemoryException("invalid memory access in memcpy(" + Long.toHexString(dst) + ", " + Long.toHexString(src) + ", " + length + ").");
        }
    }

    public void memcom(int dst, int src0, int src1, int length) throws MemoryException, IndexOutOfBoundsException {
        int finalAddress = dst;

        try {
            for (int i = 0; i < length; i++)
                setByte(finalAddress++, getByte(src0 + i));
            for (int i = 0; i < length; i++)
                setByte(finalAddress++, getByte(src1 + i));
        } catch (Exception e) {
            throw new MemoryException("invalid memory access in memcom(" + Long.toHexString(dst) + ", " + Long.toHexString(src0) + ", " + Long.toHexString(src1) + ", " + length + ").");
        }
    }

    public void free(int ptr) {
        int size = addresses.get(ptr);
        addresses.remove(ptr);

        available.put(ptr, ptr + size);
        combine();
    }

    public int getAvailableBlockFollowing(int address) {
        for (Integer available : available.keySet())
            if (address == available)
                return available;

        return 0;
    }

    public void combine() {
        Set<Integer> toRemove = new LinkedHashSet<>();

        for (Integer available : available.keySet()) {
            Integer followingBlock = this.available.get(this.available.get(available) + 1);//getAvailableBlockFollowing(available.getJ());
            if (followingBlock == null)
                continue;
            ;

            toRemove.add(followingBlock);
            this.available.put(available, followingBlock);

            followingBlock = this.available.get(this.available.get(available) + 1);//getAvailableBlockFollowing(available.getJ());

            while (followingBlock != null) {
                toRemove.add(followingBlock);
                this.available.put(available, followingBlock);

                followingBlock = this.available.get(this.available.get(available) + 1);
            }
        }

        for (Integer remove : toRemove)
            available.remove(remove);
    }

    public String getSnapshot() {
        String string = "-----------------HEAPBUFFER----------------\n";
        for (Integer available : available.keySet())
            string += "\t" + available + " " + this.available.get(available) + "\n";

        return string;
    }


    public void setByte(int address, byte b) throws IndexOutOfBoundsException{
        heapBuffer[address] = b;
    }

    public void setShort(int address, short s) throws IndexOutOfBoundsException {
        heapBuffer[address]       = (byte) ((s >> 8) & 0xFF);
        heapBuffer[address + 1]   = (byte) ((s) & 0xFF);
    }

    public void setInt(int address, int s) throws IndexOutOfBoundsException {
        heapBuffer[address]       = (byte) ((s >> 24) & 0xFF);
        heapBuffer[address + 1]   = (byte) ((s >> 16) & 0xFF);
        heapBuffer[address + 2]   = (byte) ((s >> 8) & 0xFF);
        heapBuffer[address + 3]   = (byte) ((s) & 0xFF);
    }

    public void setLong(int address, long s) throws IndexOutOfBoundsException {
        heapBuffer[address]       = (byte) ((s >> 56) & 0xFF);
        heapBuffer[address + 1]   = (byte) ((s >> 48) & 0xFF);
        heapBuffer[address + 2]   = (byte) ((s >> 40) & 0xFF);
        heapBuffer[address + 3]   = (byte) ((s >> 32) & 0xFF);
        heapBuffer[address + 4]   = (byte) ((s >> 24) & 0xFF);
        heapBuffer[address + 5]   = (byte) ((s >> 16) & 0xFF);
        heapBuffer[address + 6]   = (byte) ((s >> 8) & 0xFF);
        heapBuffer[address + 7]   = (byte) ((s) & 0xFF);
    }

    public byte getByte(int address) throws IndexOutOfBoundsException {
        return heapBuffer[address];
    }

    public short getShort(int address) throws IndexOutOfBoundsException {
        return Utils.makeShort(heapBuffer[address], heapBuffer[address + 1]);
    }

    public int getInt(int address) throws IndexOutOfBoundsException {
        return Utils.makeInt(heapBuffer[address], heapBuffer[address + 1], heapBuffer[address + 2], heapBuffer[address + 3]);
    }

    public long getLong(int address) throws IndexOutOfBoundsException {
        return Utils.makeLong(heapBuffer[address], heapBuffer[address + 1], heapBuffer[address + 2], heapBuffer[address + 3], heapBuffer[address + 4], heapBuffer[address + 5], heapBuffer[address + 6], heapBuffer[address + 7]);
    }

    @Override
    public int sizeof(int address) {
        if (addresses.containsKey(address))
            return addresses.get(address);
        return 0;
    }
}