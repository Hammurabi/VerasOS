package com.riverssen.veras;

import com.riverssen.veras.exceptions.ExecutionException;
import com.riverssen.veras.exceptions.KernelNotFoundException;
import com.riverssen.veras.exceptions.MemoryException;

////////////////////////////////////////////////////////////////
//////////////////////////VERAS OS KERNEL///////////////////////
////////////////////////////////////////////////////////////////
public abstract class Kernel {
    public Kernel(FileService entry, int ram, long storage, int maxProcesses) throws KernelNotFoundException
    {
    }
    ///// inserts the process into a SharedProcess block and executes it.
    ///// returns a new process.
    ///// returns null and throws an exception if the operation is unsuccessful.
    public abstract Process generateProcess(final Process process) throws MemoryException;
    public Process  generateProcess() throws MemoryException { return generateProcess(null); }
    public abstract void executeProcess(final Process process);
    ///// forces the process to halt
    ///// frees the processID
    ///// throws exception and blocks the Shared block if operation is unsuccessful
    public abstract void haltProcess(Process process);
    ///// forces the process to halt, recursively halts children processes.
    ///// frees the processID(s)
    ///// throws exception and blocks the Shared block if operation is unsuccessful
    public abstract void haltProcessRecursive(final int process);
    ///// fetches the internal memory block.
    ///// returns the memory block.
    ///// throws exception if operation is unsuccessful.
    public abstract MemoryBlock getMemoryBlock();
    ///// fetches the internal storage block.
    ///// returns the storage block.
    ///// throws exception if unsuccessful.
    public abstract StorageBlock getStorageBlock();
    ///// halts all SharedBlocks and shuts the vm down.
    ///// frees memory, threads, and their children.
    ///// throws exception if operation is unsuccessful.
    public abstract void haltProcesses();
    public abstract int executeProgram(Process process, Heap heap, Stack stack, int program, int steps) throws ExecutionException, ExecutionException;
    public abstract boolean getKeepAlive();
}
