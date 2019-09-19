package com.riverssen.veras;

import com.riverssen.veras.exceptions.MemoryException;

public class Stack {
    private int                             maxUsed;
    private int                             used;
    private int                             heap;
    private int                             funcheap;
    private final Kernel                    kernel;
    private int                             index;
    private int                             funcindex;

    public Stack(final Kernel kernel) throws MemoryException {
        this(kernel, 1024 * 12);
    }

    public Stack(final Kernel kernel, final int size) throws MemoryException {
        this.kernel     = kernel;
        this.heap       = kernel.getMemoryBlock().malloc(size);
        this.funcheap   = kernel.getMemoryBlock().malloc(1024);
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

    public void register(long x, int index) throws MemoryException {
        int func = peekstack();
        kernel.getMemoryBlock().setLong(func + 4 + (index * 8), x);
    }

    public void pushstack(int funcaddr) throws MemoryException {
        int function = kernel.getMemoryBlock().malloc(4 + 256);
        kernel.getMemoryBlock().setInt(function, funcaddr);
        kernel.getMemoryBlock().setLong(funcheap + funcindex, function);
        funcindex += 4;
//        if (funcindex >= (sizeOf() - 32))
//            resize(256);
    }

    public int popstack() throws MemoryException {
        funcindex -= 4;
        int function    = kernel.getMemoryBlock().getInt(funcheap + funcindex);
        int funcaddr    = kernel.getMemoryBlock().getInt(function);

        kernel.getMemoryBlock().delete(function);
        return funcaddr;
    }

    public int peekstack() throws MemoryException {
        int function    = kernel.getMemoryBlock().getInt(funcheap + funcindex);
        return function;
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
        return kernel.getMemoryBlock().getLong(heap + (index - 8));
    }

    public void delete() throws MemoryException {
        kernel.getMemoryBlock().delete(heap);
    }
}
