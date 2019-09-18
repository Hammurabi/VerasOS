package com.riverssen.veras;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

////// This implementation does not take size restraints into consideration.
//////
//////
public class StorageBlockImpl implements StorageBlock {
    private final Map<byte[], Cell> addressMap;

    public StorageBlockImpl(long size)
    {
        this.addressMap = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    @Override
    public boolean createBlock(byte[] key) {
        if (addressMap.containsKey(key))
        return false;

        addressMap.put(key, new Cell(Base16.encode(key), 0));

        return true;
    }

    @Override
    public boolean blockExists(byte[] key) {
        return addressMap.containsKey(key);
    }

    @Override
    public boolean deleteBlock(byte[] key) throws IOException {
        if (addressMap.containsKey(key))
            return addressMap.get(key).delete();
        return false;
    }

    @Override
    public CellInputStream openInputStream(byte[] key) throws IOException {
        if (addressMap.containsKey(key))
            return addressMap.get(key).read();
        return null;
    }

    @Override
    public CellOutputStream openOutputStream(byte[] key) throws IOException {
        if (addressMap.containsKey(key))
            return addressMap.get(key).write();
        return null;
    }

    @Override
    public boolean reachable(byte[] key) {
        if (addressMap.containsKey(key))
            return addressMap.get(key).available();
        return true;
    }

    private final static class Cell {
        private final String    address;
        private final long      size;
        private CellStream      currentStream;

        public Cell(final String address, final long size) {
            this.address = address;
            this.size = size;
            this.currentStream = null;
        }

        private boolean available()
        {
            if (currentStream == null)
                return true;
            return !currentStream.isOpen();
        }

        private CellInputStream newInStream() throws IOException {
            try{
                CellInputStream stream = new CellInputStream(new FileInputStream(new File(address)));
                currentStream = stream;

                return stream;
            } catch (Exception e)
            {
                throw new IOException("stream could not be opened.");
            }
        }

        private CellOutputStream newOutStream() throws IOException {
            try{
                CellOutputStream stream = new CellOutputStream(new FileOutputStream(new File(address)));
                currentStream = stream;

                return stream;
            } catch (Exception e)
            {
                throw new IOException("stream could not be opened.");
            }
        }

        public CellInputStream read() throws IOException {
            if (!available())
                return null;
            return newInStream();
        }

        public CellOutputStream write() throws IOException {
            if (!available())
                return null;
            return newOutStream();
        }

        public boolean delete() throws IOException {
            if (!available())
            {
                if (currentStream != null)
                    currentStream.forceClose();
            }
            if (available())
            {
                File file = new File(address);
                if (file.exists())
                    return file.delete();
            }

            return false;
        }
    }
}
