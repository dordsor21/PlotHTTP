package com.boydti.plothttp;

import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal;
import com.intellectualcrafters.plot.object.worlds.PlotAreaManager;
import com.intellectualcrafters.plot.object.worlds.SinglePlotArea;
import com.intellectualcrafters.plot.object.worlds.SinglePlotAreaManager;
import com.intellectualcrafters.plot.util.TaskManager;
import java.nio.ByteBuffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MainListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (Settings.Enabled_Components.PERSISTENT_META) {
            PlotPlayer pp = PlotPlayer.wrap(event.getPlayer());
            Plot plot = pp.getCurrentPlot();
            if (plot != null) {
                if (plot.getArea() instanceof SinglePlotArea) {
                    PlotId id = plot.getId();
                    int x = id.x;
                    int z = id.y;
                    ByteBuffer buffer = ByteBuffer.allocate(13);
                    buffer.putShort((short) x);
                    buffer.putShort((short) z);
                    Location loc = pp.getLocation();
                    buffer.putInt(loc.getX());
                    buffer.put((byte) loc.getY());
                    buffer.putInt(loc.getZ());
                    pp.setPersistentMeta("quitLoc", buffer.array());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConnect(PlayerJoinEvent event) {
        if (Settings.Enabled_Components.PERSISTENT_META) {
            PlotAreaManager manager = PS.get().getPlotAreaManager();
            if (manager instanceof SinglePlotAreaManager) {
                PlotArea area = ((SinglePlotAreaManager) manager).getArea();
                PlotPlayer pp = PlotPlayer.wrap(event.getPlayer());
                TaskManager.IMP.taskLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!pp.isOnline()) {
                            return;
                        }
                        byte[] arr = pp.getPersistentMeta("quitLoc");
                        if (arr != null) {
                            ByteBuffer quitWorld = ByteBuffer.wrap(arr);
                            PlotId id = new PlotId(quitWorld.getShort(), quitWorld.getShort());
                            int x = quitWorld.getInt();
                            int y = quitWorld.get() & 0xFF;
                            int z = quitWorld.getInt();
                            Plot plot = area.getOwnedPlot(id);
                            if (plot != null) {
                                Location loc = new Location(plot.getWorldName(), x, y, z);
                                TaskManager.IMP.taskAsync(new Runnable() {
                                    @Override
                                    public void run() {
                                        Main.imp().load(plot);
                                        TaskManager.IMP.sync(new RunnableVal<Object>() {
                                            @Override
                                            public void run(Object o) {
                                                pp.teleport(loc);
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    }
                }, 30);
            }
        }
    }

}
