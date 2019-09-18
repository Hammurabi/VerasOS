package com.riverssen.veras;

import com.riverssen.veras.exceptions.ExecutionException;
import com.riverssen.veras.exceptions.KernelNotFoundException;
import com.riverssen.veras.exceptions.MemoryException;
import com.riverssen.veras.exceptions.ProcessException;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class KernelImpl extends Kernel {
    private final MemoryBlock           memoryBlock;
    private final StorageBlock          storageBlock;
    private final SharedProcess         processes[];
    private final AtomicBoolean         keepAlive;
    private final Executor              threadPool;
    private final Map<Integer, Process> processMap;

    public KernelImpl(FileService entry, int ram, long storage, int maxProcesses) throws KernelNotFoundException {
        super(entry, ram, storage, maxProcesses);

//        FileService kernel  = entry.newFile("kernel.vs");
//        if (!kernel.exists())
//            throw new KernelNotFoundException("kernel could not be found");

        final int CORE_COUNT = 8;

        this.memoryBlock    = new MemoryBlockImpl(ram);
        this.storageBlock   = new StorageBlockImpl(storage);
        this.processes      = new SharedProcess[CORE_COUNT];
        this.keepAlive      = new AtomicBoolean(true);
        this.threadPool     = Executors.newFixedThreadPool(8);
        this.processMap     = Collections.synchronizedMap(new HashMap<>());

        for (int i = 0; i < CORE_COUNT; i ++)
            processes[i] = new SharedProcess(this);

        ///1
        processes[0].setPriority(maxProcesses / CORE_COUNT);
        ///0.9921875
        processes[1].setPriority(maxProcesses / CORE_COUNT);

        processes[2].setPriority(maxProcesses / CORE_COUNT);
        processes[3].setPriority(maxProcesses / CORE_COUNT);

        processes[4].setPriority(maxProcesses / CORE_COUNT);
        processes[5].setPriority(maxProcesses / CORE_COUNT);

        processes[6].setPriority(maxProcesses / CORE_COUNT);
        processes[7].setPriority(maxProcesses / CORE_COUNT);

        for (int i = 0; i < CORE_COUNT; i ++)
            threadPool.execute(processes[i]);
    }

    @Override
    public Process generateProcess(final Process parentProcess) throws MemoryException {
        int pID = processMap.size();
        if (processMap.size() > 65535)
            throw new MemoryException("too many processes created.");
        Process process = new Process(this,  pID);
        processMap.put(pID, process);

        if (parentProcess != null)
            parentProcess.addChild(pID);
        return process;
    }

    @Override
    public void haltProcess(Process process) {
        process.halt();
        for (SharedProcess sharedProcess : processes)
            sharedProcess.haltProcess(process);

        processMap.remove(process.getProcessID());
    }

    @Override
    public void haltProcessRecursive(final int pID) {
        final Process process = processMap.get(pID);
        process.haltAll();
        for (SharedProcess sharedProcess : processes)
            sharedProcess.haltProcess(process);

        processMap.remove(pID);
    }

    @Override
    public MemoryBlock getMemoryBlock() {
        return memoryBlock;
    }

    @Override
    public StorageBlock getStorageBlock() {
        return storageBlock;
    }

    @Override
    public void haltProcesses() {
        for (SharedProcess sharedProcess : processes)
            sharedProcess.halt();

        keepAlive.set(false);
    }

    private static final byte
            //push a 32bit integer to the stack
            //
            OP_PUSH         = 0,
            //push a 64bit integer to the stack
            //
            OP_LPSH         = 1,
            //generate a new process
            OP_PROC         = 2,
            //execute process
            //pop the processID from the stack
            //lookup the process and execute it.
            OP_PRCE         = 3,
            OP_ADD          = 4,
            OP_SUB          = 5,
            OP_MUL          = 6,
            OP_DIV          = 7,
            OP_MOD          = 8,
            OP_EQUALS       = 9,
            OP_LOGAND       = 10,
            OP_LOGOR        = 11,
            OP_LOGSHFT      = 12,
            OP_AND          = 13,
            OP_OR           = 14,
            OP_XOR          = 15,
            OP_NOT          = 16,
            OP_LSHIFT       = 17,
            OP_RSHIFT       = 18,

            OP_HALT         = 127;

    @Override
    public int executeProgram(final Process process, Heap heap, Stack stack, int program, int steps) throws ExecutionException {
        MemoryBlock block = getMemoryBlock();
        for (int i = 0; i < steps; i ++)
        {
            int instruction = block.getByte(program ++);

            switch (instruction) {
                case OP_PUSH:
                    stack.push(block.getInt(program));
                    program += 4;
                    break;
                case OP_LPSH:
                    stack.push(block.getLong(program));
                    program += 8;
                    break;
                case OP_PROC:
                    int priority = block.getInt(program);
                    program += 4;
                    char length = (char) block.getShort(program);
                    program += 2;
                    byte name[] = new byte[length];

                    for (int c = 0; c < length; c++)
                        name[c] = block.getByte(program + c);

                    program += length;
                    try {
                        Process p = generateProcess();
                        p.setPriority(priority);
                        p.setName(name);

                        stack.push(p.getProcessID());
                    } catch (MemoryException e) {
                        throw new ExecutionException("could not generate new process.");
                    }
                    break;
                case OP_PRCE:
                    int procssIDX = block.getInt(program);
                    program += 4;
                    int processID = (int) stack.pop();

                    final Process eProcess = processMap.get(processID);
                    if (eProcess == null)
                        throw new ExecutionException("could not execute by process id '" + Long.toHexString(processID) + "'.");

                    try {
                        eProcess.setProgram(process.getProgram());
                        eProcess.setProgramIndex(procssIDX);
                    } catch (ProcessException e) {
                        throw new ExecutionException("could not execute by process id '" + Long.toHexString(processID) + "'.");
                    }

                    Queue<SharedProcess> processes = new PriorityQueue<>();
                    for (int p = 0; p < this.processes.length; p++)
                        if (this.processes[i].awake())
                            processes.add(this.processes[i]);

                    try {
                        Objects.requireNonNull(processes.poll()).executeProcess(eProcess);
                    } catch (NullPointerException e)
                    {
                        throw new ExecutionException("could not execute by process id '" + Long.toHexString(processID) + "'.");
                    }
                    break;
                case OP_ADD:
                    stack.push(stack.pop() + stack.pop());
                    break;
                case OP_SUB:
                    stack.push(stack.pop() - stack.pop());
                    break;
                case OP_MUL:
                    stack.push(stack.pop() * stack.pop());
                    break;
                case OP_DIV:
                    stack.push(stack.pop() / stack.pop());
                    break;
                case OP_MOD:
                    stack.push(stack.pop() % stack.pop());
                    break;
                case OP_EQUALS:
                    stack.push((stack.pop() == stack.pop()) ? 1 : 0);
                    break;
                case OP_LOGAND:
                    stack.push(((stack.pop() > 0 ? true : false) && (stack.pop() > 0 ? true : false)) ? 1 : 0);
                    break;
                case OP_LOGOR:
                    stack.push(((stack.pop() > 0 ? true : false) || (stack.pop() > 0 ? true : false)) ? 1 : 0);
                    break;
                case OP_LOGSHFT:
                    stack.push(stack.pop() >>> stack.pop());
                    break;
                case OP_AND:
                    stack.push(stack.pop() & stack.pop());
                    break;
                case OP_OR:
                    stack.push(stack.pop() | stack.pop());
                    break;
                case OP_XOR:
                    stack.push(stack.pop() ^ stack.pop());
                    break;
                case OP_NOT:
                    stack.push(~stack.pop());
                    break;
                case OP_LSHIFT:
                    stack.push(stack.pop() << stack.pop());
                    break;
                case OP_RSHIFT:
                    stack.push(stack.pop() >> stack.pop());
                    break;
                case OP_HALT:
                    process.hang();

                    if (stack.pop() > 0)
                        haltProcess(process);
                    else
                        haltProcessRecursive(process.getProcessID());
                    break;
            }
        }

        return program;
    }

    @Override
    public boolean getKeepAlive() {
        return keepAlive.get();
    }
}
