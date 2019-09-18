package com.riverssen.veras;

import java.io.IOException;

public interface StorageBlock {
    boolean             createBlock(byte key[]);
    boolean             blockExists(byte key[]);
    boolean             deleteBlock(byte key[]) throws IOException;
    CellInputStream     openInputStream(byte key[]) throws IOException;
    CellOutputStream    openOutputStream(byte key[]) throws IOException;
    boolean             reachable(byte key[]);
}
