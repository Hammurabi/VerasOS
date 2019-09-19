package com.riverssen.veras;

import com.riverssen.veras.exceptions.MemoryException;
import com.riverssen.veras.exceptions.ProcessException;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Process implements Comparable<Process> {
    private AtomicInteger   processID;
    private final Kernel    kernel;
    private final Heap      heap;
    private final Stack     stack;
    private int             program;
    private int             index;
    private int             offset;
    private AtomicBoolean   active;
    private AtomicBoolean   halted;
    private Set<Integer>    subprocesses;
    private AtomicInteger   priority;
    private AtomicLong      cycle;
    private byte            name[];

    public Process(final Kernel kernel, final int processID) throws MemoryException {
        this.kernel         = kernel;
        this.processID      = new AtomicInteger(processID);
        this.program        = 0;
        this.heap           = new Heap(kernel);
        this.stack          = new Stack(kernel, 1024 * 12);
        this.active         = new AtomicBoolean(true);
        this.halted         = new AtomicBoolean(true);
        this.offset         = 0;
        this.subprocesses   = Collections.synchronizedSet(new LinkedHashSet<>());
        this.priority       = new AtomicInteger(10);
        this.cycle          = new AtomicLong(0);
    }

    public void setProgram(byte program[]) throws ProcessException, MemoryException {
        if (this.program != 0)
            throw new ProcessException("process already being used.");
        this.program    = heap.createReadOnly(program);
        this.index      = 0;
    }

    public void hang()
    {
        active.set(false);
    }

    public void wake()
    {
        active.set(true);
    }

    public boolean awake()
    {
        return active.get();
    }

    public void setPriority(int priority)
    {
        this.priority.set(priority);
    }

    public int getPriority()
    {
        return priority.get();
    }

    public long getCycle()
    {
        return cycle.get();
    }

    public void setName(byte name[])
    {
        this.name = name;
    }

    public byte[] getName() {
        return name;
    }

    public void halt()
    {
        this.halted.set(true);
        if (program != 0) {
            try {
                kernel.getMemoryBlock().delete(program);
                heap.delete();
                stack.delete();
            } catch (MemoryException e) {
            }
        }
    }

    public void haltChildren()
    {
        Iterator<Integer> iterator = subprocesses.iterator();
        while (iterator.hasNext())
            kernel.haltProcessRecursive(iterator.next());
    }

    public void haltAll()
    {
        halt();
        haltChildren();
    }

    public boolean isIdle()
    {
        return  (program == 0 || !active.get());
    }

    public boolean halted()
    {
        return halted.get();
    }

    public final void execute(int steps)
    {
        if (program == 0 || !active.get())
            return;

        try{
            catchUp();
            index = kernel.executeProgram(this, heap, stack, program + index, steps);
        } catch (Exception e)
        {
            haltAll();
        }
    }

    @Override
    public int compareTo(Process o) {
        //if the process is 20 cycles behind
        //it will be prioritized.
        if (o.getCycle() - getCycle() > 20)
            return 1;
        return getPriority() >= o.getPriority() ? 1 : -1;
    }

    public void catchUp() {
        cycle.incrementAndGet();
    }

    public void addChild(int pID) {
        subprocesses.add(pID);
    }

    public int getProcessID() {
        return processID.get();
    }

    public void setProgram(int program) throws ProcessException {
        if (this.program != 0)
            throw new ProcessException("process already being used.");
        this.program    = program;
        this.index      = 0;
    }

    public int getProgram() {
        return program;
    }

    public void setProgramIndex(int prccessIndex) {
        this.index = prccessIndex;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }
}
