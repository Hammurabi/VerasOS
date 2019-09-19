package com.riverssen.veras;

import com.riverssen.veras.exceptions.ExecutionException;
import com.riverssen.veras.exceptions.KernelNotFoundException;
import com.riverssen.veras.exceptions.MemoryException;
import com.riverssen.veras.exceptions.ProcessException;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

//// Kernel implementation
//// This implementation does not allow floating point arithmetic
//// A rational-float (rfp_t) is in the todo list.
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
    public void executeProcess(Process process) {
        Queue<SharedProcess> processes = new PriorityQueue<>();
        for (int p = 0; p < this.processes.length; p++)
            if (this.processes[p].awake())
                processes.add(this.processes[p]);

        processes.poll().executeProcess(process);
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

    public static final byte
            //push a 32bit integer to the stack
            //
            OP_PUSH         = 0,
            //push a 64bit integer to the stack
            //
            OP_LPSH         = 1,
            //generate a new process
            OP_PROC         = 2,
            OP_PROCFUN      = 3,
            //execute process
            //pop the processID from the stack
            //lookup the process and execute it.
            OP_PRCE         = 4 ,
            OP_ADD          = 5 ,
            OP_SUB          = 6 ,
            OP_MUL          = 7 ,
            OP_DIV          = 8 ,
            OP_MOD          = 9 ,
            OP_EQUALS       = 10,
            OP_LOGAND       = 11,
            OP_LOGOR        = 12,
            OP_LOGSHFT      = 13,
            OP_AND          = 14,
            OP_OR           = 15,
            OP_XOR          = 16,
            OP_NOT          = 17,
            OP_LSHIFT       = 18,
            OP_RSHIFT       = 19,
            OP_ISTORE       = 20,
            OP_ILOAD        = 21,
            OP_JUMP         = 22,
            OP_IF           = 23,
            OP_CMPG         = 24,
            OP_CMPL         = 25,
            OP_CMPGE        = 26,
            OP_CMPLE        = 27,
            OP_APUSH        = 28,
            OP_PRINT        = 29,
            OP_POP          = 30,
            OP_DREF         = 31,
            OP_AREF         = 32,
            OP_CALL         = 33,

            OP_HALT         = 127;

    @Override
    public int executeProgram(final Process process, Heap heap, Stack stack, int program, int steps) throws ExecutionException {
        MemoryBlock block = getMemoryBlock();

        int len = block.sizeof(process.getProgram());
        int max = process.getProgram() + len;

        for (int i = 0; (i < steps) && (program < max); i ++)
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
                case OP_PROCFUN:
                    int proID = (int) stack.pop();
                    int procFunProgram = block.getInt(program);
                    program += 4;
                    final Process procfun = processMap.get(proID);

                    if (procfun == null)
                        throw new ExecutionException("could not convert to process by id '" + Long.toHexString(proID) + "'.");

                    try {
                        procfun.setProgram(process.getProgram() + procFunProgram);
//                        procfun.setOffset(procFunProgram);
                    } catch (ProcessException e) {
                        throw new ExecutionException("could not set process func by id '" + Long.toHexString(proID) + "'.");
                    }
                    break;
                case OP_PRCE:
                    int processID = (int) stack.pop();

                    final Process eProcess = processMap.get(processID);
                    if (eProcess == null)
                        throw new ExecutionException("could not execute by process id '" + Long.toHexString(processID) + "'.");

//                    Queue<SharedProcess> processes = new PriorityQueue<>();
//                    for (int p = 0; p < this.processes.length; p++)
//                        if (this.processes[i].awake())
//                            processes.add(this.processes[i]);
//                    try {
//                        Objects.requireNonNull(processes.poll()).executeProcess(eProcess);
//                    } catch (NullPointerException e)
//                    {
//                        throw new ExecutionException("could not execute by process id '" + Long.toHexString(processID) + "'.");
//                    }

                    executeProcess(eProcess);
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

                    //TODO: add implementation.
                case OP_ISTORE: break;
                case OP_ILOAD: break;

                case OP_JUMP:
                    int jumpIndex = block.getInt(program);
                    program = jumpIndex;
                    break;
                case OP_IF:
                    boolean condition = stack.pop() > 0;
                    if (condition)
                    {
                    }
                    break;
                case OP_CMPG: stack.push(stack.pop() > stack.pop() ? 1 : 0); break;
                case OP_CMPL: stack.push(stack.pop() < stack.pop() ? 1 : 0); break;
                case OP_CMPGE: stack.push(stack.pop() >= stack.pop() ? 1 : 0); break;
                case OP_CMPLE: stack.push(stack.pop() <= stack.pop() ? 1 : 0); break;
                case OP_APUSH:
                    int alen = block.getShort(program);
                    try {
                        int addr = heap.malloc(alen);
                        heap.setArrayFromBlock(addr, program + 2, alen);
                        stack.push(addr);
                    } catch (MemoryException e) {
                        throw new ExecutionException(e.getMessage());
                    }
                    program += (alen + 2);
                    break;
                case OP_PRINT:
                    int padd = (int) stack.pop();
                    System.out.println(new String(heap.getArray(padd)));
                    break;
                case OP_CALL:
                    int fadd    = (int) stack.pop();
                    byte typ    = Utils.firstByte(fadd);
                    fadd        = Utils.castint24(fadd);

                    System.out.println("call func: " + fadd + " of type: " + typ);

                    break;
            }
        }

        if (program >= max) {
            try {
                stack.popstack();
            } catch (MemoryException e) {
                throw new ExecutionException(e.toString());
            }
        }

        return program;
    }

    @Override
    public boolean getKeepAlive() {
        return keepAlive.get();
    }
}
