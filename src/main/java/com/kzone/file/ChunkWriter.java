package com.kzone.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class ChunkWriter {

    private final FileMetadataMaintainer mtdMaintainer;

    public void write(String fileName, byte[] chunk, int start) {

    }

}
