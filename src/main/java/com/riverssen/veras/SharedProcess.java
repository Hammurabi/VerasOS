package com.riverssen.veras;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SharedProcess implements Runnable, Comparable<SharedProcess> {
    private final Kernel        kernel;
    private AtomicInteger       priority;
    private final Set<Process>  processes;
    private AtomicBoolean       active;

    public SharedProcess(final Kernel kernel)
    {
        this.kernel     = kernel;
        this.priority   = new AtomicInteger(8);
        this.active     = new AtomicBoolean(true);
        this.processes  = Collections.synchronizedSet(new LinkedHashSet<>());
    }

    protected void setPriority(int priority)
    {
        this.priority.set(priority);
    }

    protected void hang()
    {
        active.set(false);
    }

    protected void wake()
    {
        active.set(true);
    }

    protected boolean awake()
    {
        return active.get();
    }

    protected int getActiveProcesses()
    {
        int numActive = 0;

        Iterator<Process> iterator = processes.iterator();

        while (iterator.hasNext())
            numActive += iterator.next().isIdle() ? 0 : 1;

        return numActive;
    }

    protected boolean isIdle()
    {
        return getActiveProcesses() == 0;
    }

    protected void haltProcess(final Process process)
    {
        this.processes.remove(process);
    }

    public void run()
    {
        while (kernel.getKeepAlive())
        {
            if (active.get() && processes.size() > 0)
            {
                Iterator<Process> iterator = processes.iterator();

                Queue<Process> prioritized = new PriorityQueue<>();

                while (iterator.hasNext())
                {
                    final Process process = iterator.next();
                    if (process.isIdle())
                        process.catchUp();
                    else
                        prioritized.add(process);
                }

                int maxIterations   = priority.get();

                iterator = prioritized.iterator();
                int iterations      = maxIterations;

                while (iterator.hasNext() && iterations > 0)
                {
                    final Process process = iterator.next();
                    int cycles = Math.min(((maxIterations * process.getPriority()) / 100), iterations);

                    process.execute(cycles);

                    iterations -= cycles;
                }
            }
        }
    }

    public void halt() {
        hang();
        Iterator<Process> iterator = processes.iterator();
        while (iterator.hasNext())
            iterator.next().haltAll();
        processes.clear();
    }

    @Override
    public int compareTo(SharedProcess o) {
        if (isIdle())
            return 1;
        return getActiveProcesses() > o.getActiveProcesses() ? -1 : 1;
    }

    public void executeProcess(final Process process)
    {
        this.processes.add(process);
    }
}
