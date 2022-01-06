package com.boydti.plothttp.command;

import com.boydti.plothttp.Main;
import com.boydti.plothttp.object.Request;
import com.boydti.plothttp.object.WebResource;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotMessage;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal2;
import com.intellectualcrafters.plot.object.RunnableVal3;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.Permissions;
import com.plotsquared.general.commands.Command;
import com.plotsquared.general.commands.CommandDeclaration;
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
            MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.upload.world");
            return;
        }
        PlotArea area = PS.get().getPlotArea("*", null);
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

        if (com.intellectualcrafters.plot.config.Settings.PLATFORM.equalsIgnoreCase("bukkit")) {
            player.sendMessage(C.PREFIX.s() + "Upload the world: " + link);
        } else {
            PlotMessage clickable = new PlotMessage().text(C.color(C.PREFIX.s() + "Upload the world: " + link)).color("$1").suggest(link);
            clickable.send(player);
        }

        final HashMap<String, String> map = new HashMap<>();
        map.put("id", id);
        final Request r = new Request("*", "*", "/web", map, 2);
        Main.imp().getWebServer().getRequestManager().addToken(r);
        return;
    }
}
