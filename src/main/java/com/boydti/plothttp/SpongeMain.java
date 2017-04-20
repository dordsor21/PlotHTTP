package com.boydti.plothttp;


import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "plothttp", name = " PlotHTTP", description = "plothttp", url = "https://github.com/boy0001/PlotHTTP", version = "development", authors = "Empire92", dependencies = @Dependency(id = "plotsquared", optional = true))
public class SpongeMain {
    private Main main;

    @Listener(order = Order.POST)
    public void onGameCreate(GameStartedServerEvent event) {
        try {
            this.main = new Main();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        main.open();
    }

    @Listener(order = Order.PRE)
    public void onGameStopping(GameStoppingEvent event) {
        main.close();
    }
}
