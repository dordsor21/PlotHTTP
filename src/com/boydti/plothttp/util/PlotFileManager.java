package com.boydti.plothttp.util;

import java.util.ArrayList;
import java.util.List;

import com.boydti.plothttp.object.SchematicFile;
import com.boydti.plothttp.util.NanoHTTPD.DefaultTempFile;
import com.boydti.plothttp.util.NanoHTTPD.TempFile;
import com.boydti.plothttp.util.NanoHTTPD.TempFileManager;
import com.boydti.plothttp.util.NanoHTTPD.TempFileManagerFactory;

public class PlotFileManager implements TempFileManagerFactory {
    private final String tmpdir;
    private final List<TempFile> tempFiles;

    public PlotFileManager() {
        tmpdir = System.getProperty("java.io.tmpdir");
        tempFiles = new ArrayList<TempFile>();
    }

    @Override
    public TempFileManager create() {
        return new TempFileManager() {
            
            @Override
            public TempFile createTempFile() throws Exception {
                SchematicFile tempFile = new SchematicFile(tmpdir);
                tempFiles.add(tempFile);
                return tempFile;
            }

            @Override
            public void clear() {
                if (!tempFiles.isEmpty()) {
                }
                for (TempFile file : tempFiles) {
                    try {
                        file.delete();
                    } catch (Exception ignored) {}
                }
                tempFiles.clear();
            }

        };
    }
}