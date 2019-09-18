package com.riverssen.veras;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CellStream {
    private AtomicBoolean open;

    public CellStream()
    {
        this.open = new AtomicBoolean(true);
    }
    public abstract void close() throws IOException;
    protected void setClosed() { this.open.set(false); }
    public abstract void forceClose() throws IOException;
    public boolean isOpen()
    {
        return this.open.get();
    }
}
