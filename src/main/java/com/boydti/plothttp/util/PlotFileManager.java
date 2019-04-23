package com.boydti.plothttp.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.boydti.plothttp.object.SchematicFile;
import com.boydti.plothttp.util.NanoHTTPD.TempFile;
import com.boydti.plothttp.util.NanoHTTPD.TempFileManager;
import com.boydti.plothttp.util.NanoHTTPD.TempFileManagerFactory;

public class PlotFileManager implements TempFileManagerFactory {
    private final String tmpdir;
    private final List<TempFile> tempFiles;

    public PlotFileManager() {
        this.tmpdir = System.getProperty("java.io.tmpdir");
        this.tempFiles = new ArrayList<TempFile>();
    }

    @Override
    public TempFileManager create() {
        return new TempFileManager() {

            @Override
            public TempFile createTempFile() throws Exception {
                final SchematicFile tempFile = new SchematicFile(PlotFileManager.this.tmpdir);
                PlotFileManager.this.tempFiles.add(tempFile);
                return tempFile;
            }

            @Override
            public void clear() {
                for (final TempFile file : PlotFileManager.this.tempFiles) {
                    try {
                        new File(file.getName()).deleteOnExit();
                    } catch (final Exception ignored) {
                    }
                }
                PlotFileManager.this.tempFiles.clear();
            }

        };
    }
}
