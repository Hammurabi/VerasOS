package com.riverssen.veras;

import com.riverssen.veras.exceptions.MemoryException;

public class Stack {
    private int                             maxUsed;
    private int                             used;
    private int                             heap;
    private final Kernel                    kernel;
    private int                             index;

    public Stack(final Kernel kernel) throws MemoryException {
        this(kernel, 1024 * 12);
    }

    public Stack(final Kernel kernel, final int size) throws MemoryException {
        this.kernel     = kernel;
        this.heap       = kernel.getMemoryBlock().malloc(size);
    }

    public void resize(int size)
    {
        heap = kernel.getMemoryBlock().reallocIfAvailable(heap, sizeOf() + size);
    }

    public void downsize(int size)
    {
        heap = kernel.getMemoryBlock().reallocIfAvailable(heap, sizeOf() - size);
    }

    public String getSnapshot() {
        String string = "-----------------STACK----------------\n";
        for (int i = 0; i < index; i += 8)
            string += "\t" + Long.toHexString(i) + "\t" + get(i) + "\n";

        return string;
    }

    public int sizeOf()
    {
        return kernel.getMemoryBlock().sizeof(heap);
    }

    public long get(int address) throws IndexOutOfBoundsException{
        return kernel.getMemoryBlock().getLong(heap + address);
    }

    public void push(long v)
    {
        kernel.getMemoryBlock().setLong(heap + index, v);
        index += 8;

        if (index >= (sizeOf() - 32))
            resize(256);
    }

    public long pop()
    {
        index -= 8;
        if (sizeOf() > (index + 512))
            downsize(256);

        return kernel.getMemoryBlock().getLong(heap + index);
    }

    public long peek()
    {
        return kernel.getMemoryBlock().getLong(heap + (index - 4));
    }

    public void delete() throws MemoryException {
        kernel.getMemoryBlock().delete(heap);
    }
}
