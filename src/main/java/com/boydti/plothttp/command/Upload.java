package com.boydti.plothttp.command;

import com.boydti.plothttp.Main;
import com.boydti.plothttp.object.Request;
import com.boydti.plothttp.object.WebResource;
import com.github.intellectualsites.plotsquared.commands.Command;
import com.github.intellectualsites.plotsquared.commands.CommandDeclaration;
import com.github.intellectualsites.plotsquared.plot.PlotSquared;
import com.github.intellectualsites.plotsquared.plot.commands.CommandCategory;
import com.github.intellectualsites.plotsquared.plot.commands.MainCommand;
import com.github.intellectualsites.plotsquared.plot.config.Captions;
import com.github.intellectualsites.plotsquared.plot.config.Settings;
import com.github.intellectualsites.plotsquared.plot.object.PlotArea;
import com.github.intellectualsites.plotsquared.plot.object.PlotMessage;
import com.github.intellectualsites.plotsquared.plot.object.PlotPlayer;
import com.github.intellectualsites.plotsquared.plot.object.RunnableVal2;
import com.github.intellectualsites.plotsquared.plot.object.RunnableVal3;
import com.github.intellectualsites.plotsquared.plot.util.MainUtil;
import com.github.intellectualsites.plotsquared.plot.util.Permissions;

import java.util.HashMap;

@CommandDeclaration(
        command = "upload",
        aliases = {"upload"},
        description = "Upload a world to the server",
        permission = "plots.upload",
        category = CommandCategory.SCHEMATIC,
        usage = "/plot upload")
public class Upload extends Command {
    public Upload() {
        super(MainCommand.getInstance(), true);
    }

    @Override
    public void execute(PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) throws CommandException {
        if (!Permissions.hasPermission(player, "plots.web.upload.world")) {
            MainUtil.sendMessage(player, Captions.NO_PERMISSION, "plots.web.upload.world");
            return;
        }
        PlotArea area = PlotSquared.get().getPlotArea("*", null);
        if (area == null) {
            MainUtil.sendMessage(player, "&6World uploads are disabled!");
            return;
        }
        MainUtil.sendMessage(player, "&6Generating link...");
        final String id = WebResource.nextId();
        final String port;
        if (Main.config().PORT != 80) {
            port = ":" + Main.config().PORT;
        } else {
            port = "";
        }
        WebResource.worldUploads.put(id, player);
        String link = Main.config().WEB_IP + port + "/web?id=" + id;

        if (Settings.PLATFORM.equalsIgnoreCase("bukkit")) {
            player.sendMessage(Captions.PREFIX.s() + "Upload the world: " + link);
        } else {
            PlotMessage clickable = new PlotMessage().text(Captions.color(Captions.PREFIX.s() + "Upload the world: " + link)).color("$1").suggest(link);
            clickable.send(player);
        }

        final HashMap<String, String> map = new HashMap<>();
        map.put("id", id);
        final Request r = new Request("*", "*", "/web", map, 2);
        Main.imp().getWebServer().getRequestManager().addToken(r);
        return;
    }
}
