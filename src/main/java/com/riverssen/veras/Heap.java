package com.riverssen.veras;

import com.riverssen.veras.exceptions.MemoryException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Heap {
    private int                             maxUsed;
    private int                             used;
    private int                             heap;
    private int                             program;
    private final Kernel                    kernel;
    private final Map<Integer, Integer>     available;
    private final Map<Integer, Integer>     addresses;

    public Heap(final Kernel kernel) throws MemoryException {
        this(kernel, 1024 * 24);
    }

    public Heap(final Kernel kernel, final int size) throws MemoryException {
        this.kernel     = kernel;
        this.heap       = kernel.getMemoryBlock().malloc(size);

        this.available = new LinkedHashMap<>();
        this.addresses = new LinkedHashMap<>();
        //offset the address
        available.put(1, sizeOf() + 1);
    }

    public void resize(int size)
    {
        int available = 0;
        for (Integer address : this.available.values())
            available += address;

        if (available < size)
        {
            int oldSize = sizeOf();
            heap = kernel.getMemoryBlock().reallocIfAvailable(heap, sizeOf() + size);

            if (sizeOf() != oldSize)
                this.available.put(oldSize, oldSize + size);
        }
    }

    public int malloc(int size) throws MemoryException {
        if (size == 0)
            return 0;

        resize(size);

        int pointer = 0;
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

    public int createReadOnly(final byte data[]) throws MemoryException {
        program = kernel.getMemoryBlock().malloc(data.length);
        for (int i = 0; i < data.length; i ++) kernel.getMemoryBlock().setByte(program + i, data[i]);

        return program;
    }

    public int sizeOf()
    {
        return kernel.getMemoryBlock().sizeof(heap);
    }

    public void setByte(int address, byte b) throws IndexOutOfBoundsException{
        if (overflow(address))
            return;
        kernel.getMemoryBlock().setByte(fixAddress(address) + heap, b);
    }

    public void setShort(int address, short s) throws IndexOutOfBoundsException {
        if (overflow(address))
            return;
        kernel.getMemoryBlock().setShort(fixAddress(address) + heap, s);
    }

    public void setInt(int address, int i) throws IndexOutOfBoundsException {
        if (overflow(address))
            return;
        kernel.getMemoryBlock().setInt(fixAddress(address) + heap, i);
    }

    public void setLong(int address, long l) throws IndexOutOfBoundsException {
        if (overflow(address))
            return;
        kernel.getMemoryBlock().setLong(fixAddress(address) + heap, l);
    }

    public byte getByte(int address) throws IndexOutOfBoundsException {
        if (overflow(address))
            return 0;
        return kernel.getMemoryBlock().getByte(fixAddress(address) + heap);
    }

    public int getShort(int address) throws IndexOutOfBoundsException {
        if (overflow(address))
            return 0;
        return kernel.getMemoryBlock().getShort(fixAddress(address) + heap);
    }

    public int getInt(int address) throws IndexOutOfBoundsException {
        if (overflow(address))
            return 0;
        return kernel.getMemoryBlock().getInt(fixAddress(address) + heap);
    }

    public long getLong(int address) throws IndexOutOfBoundsException {
        if (overflow(address))
            return 0;
        return kernel.getMemoryBlock().getLong(fixAddress(address) + heap);
    }

    public int fixAddress(int adddress)
    {
        return adddress - 1;
    }

    public boolean overflow(int address)
    {
        return fixAddress(address) >= sizeOf();
    }

    public int sizeof(int address) {
        if (addresses.containsKey(address))
            return addresses.get(address);

        return 0;
    }

    public void delete() throws MemoryException {
        kernel.getMemoryBlock().delete(heap);
    }

    public void setArrayFromBlock(int addr, int array, int len) {
        for (int i = 0; i < len; i ++)
            setByte(addr + i, kernel.getMemoryBlock().getByte(array + i));
    }

    public void setArray(int addr, byte array[]) {
    }

    public byte[] getArray(int padd) throws IndexOutOfBoundsException {
        int len = sizeof(padd);
        byte a[]= new byte[len];

        for (int i = 0; i < len; i ++)
            a[i] = getByte(padd + i);

        return a;
    }
}
