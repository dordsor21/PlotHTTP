package com.boydti.plothttp.util;

import com.boydti.plothttp.Main;
import com.google.common.io.ByteSink;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.ChunkLoc;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.util.ChunkManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.bukkit.Bukkit;

public class WorldUtil {
    public static boolean save(final Plot plot, final String filename) throws Exception {
        final byte[] buffer = new byte[1024];

        final String output = Main.imp().getDataFolder() + File.separator + "downloads" + File.separator + filename;
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
            final NbtFactory.NbtCompound compound;
            try (FileInputStream is = new FileInputStream(dat)) {
                compound = NbtFactory.fromStream(is, NbtFactory.StreamOptions.GZIP_COMPRESSION);
            }

            Location spawn = plot.getHome();
            NbtFactory.NbtCompound data = compound.getMap("Data", false);
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
            compound.saveTo(sink, NbtFactory.StreamOptions.GZIP_COMPRESSION);
        } else {
            PS.debug("NO DAT FOUND");
        }
        

        for (Plot current : plot.getConnectedPlots()) {
            final Location bot = current.getBottomAbs();
            final Location top = current.getTopAbs();
            final int brx = bot.getX() >> 9;
            final int brz = bot.getZ() >> 9;
            final int trx = top.getX() >> 9;
            final int trz = top.getZ() >> 9;
            String worldName = bot.getWorld();
            Set<ChunkLoc> files = ChunkManager.manager.getChunkChunks(worldName);
            for (ChunkLoc mca : files) {
                if (mca.x >= brx && mca.x <= trx && mca.z >= brz && mca.z <= trz) {
                    final File file = getMcr(worldName, mca.x, mca.z);
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
        zos.close();
        return true;
    }

    public static File getDat(final String world) {
        final File file = new File(Main.imp().getDataFolder() + File.separator + "level.dat");
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
