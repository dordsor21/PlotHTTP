package com.boydti.plothttp.util;

import com.boydti.plothttp.Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtil {
    public static File copyFile(File jar, String resource, File output) {
        return copyFile(jar, resource, output, resource);
    }

    public static File copyFile(File jar, String resource, File output, String fileName) {
        try {
            if (output == null) {
                output = Main.imp().DIR;
            }
            if (!output.exists()) {
                output.mkdirs();
            }
            File newFile = new File(output, fileName);
            if (newFile.exists()) {
                return newFile;
            }
            try (InputStream stream = Main.class.getResourceAsStream(resource.startsWith("/") ? resource : "/" + resource)) {
                byte[] buffer = new byte[2048];
                if (stream == null) {
                    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jar))) {
                        ZipEntry ze = zis.getNextEntry();
                        while (ze != null) {
                            String name = ze.getName();
                            if (name.equals(resource)) {
                                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                                    int len;
                                    while ((len = zis.read(buffer)) > 0) {
                                        fos.write(buffer, 0, len);
                                    }
                                }
                                ze = null;
                            } else {
                                ze = zis.getNextEntry();
                            }
                        }
                        zis.closeEntry();
                    }
                    return newFile;
                }
                File parent = newFile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                newFile.createNewFile();
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = stream.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                return newFile;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("&cCould not save " + resource);
        }
        return null;
    }
}
