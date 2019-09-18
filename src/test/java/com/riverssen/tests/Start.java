package com.riverssen.tests;

import com.riverssen.veras.FileService;
import com.riverssen.veras.Kernel;
import com.riverssen.veras.KernelImpl;
import com.riverssen.veras.exceptions.KernelNotFoundException;

public class Start {
    public static void main(String args[]) throws KernelNotFoundException {
        Kernel kernel = new KernelImpl(new FileService("."), 500, 500, 10000);
    }
}
