package com.kzone.p2p;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.RandomAccessFile;


@Log4j2
public class ChunkedFile {

    private final String path;
    private final RandomAccessFile file;
    private final long chunkSize;
    private final int startOffset;
    private final long endOffset;
    private int offset;


    public ChunkedFile(RandomAccessFile file, String path, int chunkSize) throws IOException {
        this(file, path, 0, file.length(), chunkSize);
    }

    public ChunkedFile(RandomAccessFile file, String path, int offset, long length, int chunkSize) throws IOException {
        this.file = file;
        this.path = path;
        this.offset = startOffset = offset;
        this.endOffset = offset + length;
        this.chunkSize = chunkSize;

        file.seek(offset);
    }

    public boolean isEndOfInput() throws Exception {
        return !(offset < endOffset && file.getChannel().isOpen());
    }


    public void close() throws Exception {
        file.close();
    }

    public byte[] readChunk() throws Exception {
        int currentOffset = this.offset;
        if (currentOffset >= endOffset) {
            return new byte[0];
        }

        int currentChunkSize = (int) Math.min(this.chunkSize, endOffset - currentOffset);

        byte[] chunk = new byte[currentChunkSize];

        try {
            file.readFully(chunk, 0, currentChunkSize);
            this.offset = currentOffset + currentChunkSize;

            return chunk;
        } finally {

        }
    }


    public long length() {
        return endOffset - startOffset;
    }

    public int current() {
        return offset - startOffset;
    }

    public String path() {
        return path;
    }
}
