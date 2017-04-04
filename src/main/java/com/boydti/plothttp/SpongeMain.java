package com.boydti.plothttp;


import java.net.URISyntaxException;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "plothttp", name = " PlotHTTP", description = "plothttp", url = "https://github.com/boy0001/PlotHTTP", version = "development", authors = "Empire92")
public class SpongeMain {
    private Main main;

    @Listener(order = Order.PRE)
    public void onGamePreInit(GamePreInitializationEvent event) {
        try {
            this.main = new Main();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        main.open();
    }

    @Listener(order = Order.PRE)
    public void onGameStopping(GameStoppingEvent event) {
        main.close();
    }
}
