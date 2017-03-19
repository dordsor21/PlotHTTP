package com.boydti.plothttp.object;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.boydti.plothttp.util.NanoHTTPD.TempFile;

public class SchematicFile implements TempFile {
    private final File file;
    private final OutputStream fstream;

    public SchematicFile(final String tempdir) throws IOException {
        this.file = File.createTempFile("NanoHTTPD-", "", new File(tempdir));
        this.fstream = new FileOutputStream(this.file);
    }

    @Override
    public OutputStream open() throws Exception {
        return this.fstream;
    }

    @Override
    public void delete() throws Exception {
        if (this.fstream != null) {
            this.fstream.close();
        }
        this.file.delete();
    }

    @Override
    public String getName() {
        return this.file.getAbsolutePath();
    }
}
