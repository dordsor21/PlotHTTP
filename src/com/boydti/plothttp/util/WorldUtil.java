package com.boydti.plothttp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.Bukkit;

import com.boydti.plothttp.Main;
import com.google.common.io.ByteSink;
import com.google.common.io.InputSupplier;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.plotsquared.bukkit.util.NbtFactory;
import com.plotsquared.bukkit.util.NbtFactory.NbtCompound;
import com.plotsquared.bukkit.util.NbtFactory.StreamOptions;

public class WorldUtil {
    public static boolean save(final Plot plot, final String filename) throws Exception {
        final byte[] buffer = new byte[1024];

        final String output = Main.plugin.getDataFolder() + File.separator + "downloads" + File.separator + filename;
        final File outputFile = new File(output);
        if (!outputFile.exists()) {
            System.out.print(outputFile.getParentFile().mkdirs());
            System.out.print(outputFile.createNewFile());
        }
        
        // TODO FIXME calculate offset

        final FileOutputStream fos = new FileOutputStream(outputFile);
        final ZipOutputStream zos = new ZipOutputStream(fos);

        final File dat = getDat(plot.getArea().worldname);
        if (dat != null) {
            final ZipEntry ze = new ZipEntry("world" + File.separator + dat.getName());
            zos.putNextEntry(ze);
            final FileInputStream in = new FileInputStream(dat);
            
            final InputSupplier<FileInputStream> is = com.google.common.io.Files.newInputStreamSupplier(dat);
            final NbtFactory.NbtCompound compound = NbtFactory.fromStream(is, NbtFactory.StreamOptions.GZIP_COMPRESSION);

            Location spawn = plot.getHome();
            NbtCompound data = compound.getMap("Data", false);
            System.out.println("DATA: " + data);
            data.put("SpawnX", spawn.getX());
            data.put("SpawnY", spawn.getY());
            data.put("SpawnZ", spawn.getZ());
            
            final OutputStream osWrapper = new OutputStream() {
                @Override
                public void write(int paramInt) throws IOException {
                    zos.write(paramInt);
                }
                @Override
                public void close() throws IOException {
                    zos.closeEntry();
                    super.close();
                }
                @Override
                public void flush() throws IOException {
                    zos.flush();
                }
            };
            ByteSink sink = new ByteSink() {
                @Override
                public OutputStream openStream() throws IOException {
                    return osWrapper;
                }
            };
            compound.saveTo(sink, StreamOptions.GZIP_COMPRESSION);
            //            int len;
            //            while ((len = in.read(buffer)) > 0) {
            //                zos.write(buffer, 0, len);
            //            }
            in.close();
        } else {
            System.out.print("NO DAT FOUND");
        }
        

        for (Plot current : plot.getConnectedPlots()) {
            final Location bot = current.getBottomAbs();
            final Location top = current.getTopAbs();
            final int brx = bot.getX() >> 9;
            final int brz = bot.getZ() >> 9;
            final int trx = top.getX() >> 9;
            final int trz = top.getZ() >> 9;
            for (int x = brx; x <= trx; x++) {
                for (int z = brz; z <= trz; z++) {
                    final File file = getMcr(plot.getArea().worldname, x, z);
                    if (file != null) {
                        //final String name = "r." + (x - cx) + "." + (z - cz) + ".mca";
                        String name = file.getName();
                        final ZipEntry ze = new ZipEntry("world" + File.separator + "region" + File.separator + name);
                        zos.putNextEntry(ze);
                        final FileInputStream in = new FileInputStream(file);
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        in.close();
                        zos.closeEntry();
                    }
                }
            }
        }

        //remember close it
        zos.close();
        System.out.println("Done");
        return true;
    }

    public static File getDat(final String world) {
        final File file = new File(Main.plugin.getDataFolder() + File.separator + "level.dat");
        if (file.exists()) {
            return file;
        }
        return null;
    }

    public static File getMcr(final String world, final int x, final int z) {
        final File file = new File(Bukkit.getWorldContainer(), world + File.separator + "region" + File.separator + "r." + x + "." + z + ".mca");
        if (file.exists()) {
            return file;
        }
        return null;
    }

}
