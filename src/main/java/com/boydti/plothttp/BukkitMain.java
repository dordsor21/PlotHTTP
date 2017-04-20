package com.boydti.plothttp;

import java.net.URISyntaxException;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitMain extends JavaPlugin {
    private Main main;

    @Override
    public void onEnable() {
        try {
            this.main = new Main();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        main.open();
    }

    @Override
    public void onDisable() {
        main.close();
    }
}
