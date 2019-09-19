package com.riverssen.tests;

import com.riverssen.veras.*;
import com.riverssen.veras.Process;
import com.riverssen.veras.exceptions.KernelNotFoundException;
import com.riverssen.veras.exceptions.MemoryException;
import com.riverssen.veras.exceptions.ProcessException;

import java.io.IOException;

public class Start {
    public static void main(String args[]) throws KernelNotFoundException, MemoryException, ProcessException, IOException {
        Kernel kernel = new KernelImpl(new FileService("."), 1024*1024*512, 500, 10000);
        final Process process = kernel.generateProcess();
        byte program[]  = {KernelImpl.OP_PUSH, 0, 0, 0, 4, KernelImpl.OP_PUSH, 0, 0, 0, 8, KernelImpl.OP_MUL};
        byte bootloader[] = Bootloader.generateBootloader();
        process.setProgram(bootloader);
        kernel.executeProcess(process);
    }
}
