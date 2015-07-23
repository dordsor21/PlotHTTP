package com.boydti.plothttp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.Bukkit;

import com.boydti.plothttp.Main;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.util.MainUtil;

public class WorldUtil {
    public static boolean save(final Plot plot, final String filename) throws Exception {
        final Location bot = MainUtil.getPlotBottomLoc(plot.world, plot.id).add(1, 0, 1);
        final Location top = MainUtil.getPlotTopLoc(plot.world, plot.id);

        final int brx = bot.getX() >> 9;
        final int brz = bot.getZ() >> 9;

        final int trx = top.getX() >> 9;
        final int trz = top.getZ() >> 9;

        final byte[] buffer = new byte[1024];

        final String output = Main.plugin.getDataFolder() + File.separator + "downloads" + File.separator + filename;
        final File outputFile = new File(output);
        if (!outputFile.exists()) {
            System.out.print(outputFile.getParentFile().mkdirs());
            System.out.print(outputFile.createNewFile());
        }
        final FileOutputStream fos = new FileOutputStream(outputFile);
        final ZipOutputStream zos = new ZipOutputStream(fos);

        final File dat = getDat(plot.world);
        if (dat != null) {
            final ZipEntry ze = new ZipEntry("world" + File.separator + dat.getName());
            zos.putNextEntry(ze);
            final FileInputStream in = new FileInputStream(dat);
            int len;
            while ((len = in.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            in.close();
            zos.closeEntry();
        } else {
            System.out.print("NO DAT FOUND");
        }

        final int cx = brx;
        final int cz = brz;

        for (int x = brx; x <= trx; x++) {
            for (int z = brz; z <= trz; z++) {
                final File file = getMcr(plot.world, x, z);
                if (file != null) {
                    final String name = "r." + (x - cx) + "." + (z - cz) + ".mca";
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
