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
                System.out.println("Created tempFile: " + tempFile.getName());
                return tempFile;
            }

            @Override
            public void clear() {
                if (!tempFiles.isEmpty()) {
                    System.out.println("Cleaning up:");
                }
                for (TempFile file : tempFiles) {
                    try {
                        System.out.println("   "+file.getName());
                        file.delete();
                    } catch (Exception ignored) {}
                }
                tempFiles.clear();
            }

        };
    }
}