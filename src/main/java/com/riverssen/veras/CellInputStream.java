package com.riverssen.veras;

import java.io.IOException;
import java.io.InputStream;

public class CellInputStream extends CellStream {
    private final InputStream stream;

    public CellInputStream(InputStream stream)
    {
        super();
        this.stream = stream;
    }

    public int read(byte dst[]) throws IOException {
        return stream.read(dst);
    }

    public int read() throws IOException {
        return stream.read();
    }

    @Override
    public void close() throws IOException {
        stream.close();
        setClosed();
    }

    @Override
    public void forceClose() throws IOException {
        close();
    }
}
