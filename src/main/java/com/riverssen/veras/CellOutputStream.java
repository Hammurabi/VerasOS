package com.riverssen.veras;

import java.io.IOException;
import java.io.OutputStream;

public class CellOutputStream extends CellStream {
    private final OutputStream stream;

    public CellOutputStream(OutputStream stream)
    {
        super();
        this.stream = stream;
    }

    public void write(int b) throws IOException {
        stream.write(b);
    }

    public void write(byte src[], int offset, int length) throws IOException {
        stream.write(src, offset, length);
    }

    public void write(byte src[]) throws IOException {
        stream.write(src);
    }

    @Override
    public void close() throws IOException {
        stream.flush();
        stream.close();
        setClosed();
    }

    @Override
    public void forceClose() throws IOException {
        close();
    }
}
