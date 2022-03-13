package com.boydti.plothttp.util;

import com.boydti.plothttp.Main;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.ChunkLoc;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.util.ChunkManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WorldUtil {
    public static boolean save(final Plot plot, final String filename) throws Exception {
        final byte[] buffer = new byte[1024];

        final String output = Main.imp().DIR + File.separator + "downloads" + File.separator + filename + ".zip";
        final File outputFile = new File(output);

        // TODO FIXME calculate offset

        final FileOutputStream fos = new FileOutputStream(outputFile);
        final ZipOutputStream zos = new ZipOutputStream(fos);

        final File dat = getDat(plot.getArea().worldname);
        if (dat != null) {
            final ZipEntry ze = new ZipEntry(dat.getName());
            zos.putNextEntry(ze);
            NamedTag compound;
            try (NBTInputStream nbtIn = new NBTInputStream(new GZIPInputStream(new FileInputStream(dat)))) {
                compound = nbtIn.readNamedTag();
            }

            Location spawn = plot.getHome();
            CompoundTag tag = (CompoundTag) compound.getTag();
            Map<String, Tag> newMap  = new HashMap<>(tag.getValue());
            for (Map.Entry<String, Tag> entry : tag.getValue().entrySet()) {
                if (!entry.getKey().equals("Data")) {
                    continue;
                }
                Map<String, Tag> data = new HashMap<>(((CompoundTag) entry.getValue()).getValue());
                data.put("SpawnX", new IntTag(spawn.getX()));
                data.put("SpawnY", new IntTag(spawn.getY()));
                data.put("SpawnZ", new IntTag(spawn.getZ()));
                data.put("LevelName", new StringTag(filename));
                newMap.put("Data", new CompoundTag(data));
            }
            tag = new CompoundTag(newMap);

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
            try (NBTOutputStream out = new NBTOutputStream(new GZIPOutputStream(osWrapper))) {
                out.writeNamedTag("", tag);
            }
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
                        final ZipEntry ze = new ZipEntry("region" + File.separator + name);
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
        final File file = new File(Main.imp().DIR + File.separator + "level.dat");
        if (file.exists()) {
            return file;
        }
        return null;
    }

    public static File getMcr(final String world, final int x, final int z) {
        final File file = new File(PS.imp().getWorldContainer(), world + File.separator + "region" + File.separator + "r." + x + "." + z + ".mca");
        if (file.exists()) {
            return file;
        }
        return null;
    }

}
