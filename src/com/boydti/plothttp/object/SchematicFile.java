package com.boydti.plothttp.object;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.boydti.plothttp.util.NanoHTTPD.TempFile;

public class SchematicFile implements TempFile {
    private File file;
    private OutputStream fstream;

    public SchematicFile(String tempdir) throws IOException {
        file = File.createTempFile("NanoHTTPD-", "", new File(tempdir));
        fstream = new FileOutputStream(file);
    }

    @Override
    public OutputStream open() throws Exception {
        return fstream;
    }

    @Override
    public void delete() throws Exception {
        if (fstream != null) {
            fstream.close();
        }
        file.delete();
    }

    @Override
    public String getName() {
        return file.getAbsolutePath();
    }
}
