package com.boydti.plothttp.command;

import com.boydti.plothttp.Main;
import com.boydti.plothttp.object.Request;
import com.boydti.plothttp.object.WebResource;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.command.Command;
import com.plotsquared.core.command.CommandCategory;
import com.plotsquared.core.command.CommandDeclaration;
import com.plotsquared.core.command.MainCommand;
import com.plotsquared.core.configuration.caption.StaticCaption;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.util.task.RunnableVal2;
import com.plotsquared.core.util.task.RunnableVal3;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<Boolean> execute(
            final PlotPlayer<?> player,
            final String[] args,
            final RunnableVal3<Command, Runnable, Runnable> confirm,
            final RunnableVal2<Command, CommandResult> whenDone
    ) throws CommandException {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (!player.hasPermission("plots.web.upload.world")) {
            player.sendMessage(
                    TranslatableCaption.of("permission.no_permission"),
                    TagResolver.resolver("node", Tag.inserting(Component.text("plots.web.upload.world")))
            );
            future.complete(false);
            return future;
        }
        PlotArea area = PlotSquared.get().getPlotAreaManager().getPlotArea("*", null);
        if (area == null) {
            player.sendMessage(StaticCaption.of("World uploads are disabled!"));
            future.complete(false);
            return future;
        }
        player.sendMessage(StaticCaption.of("Generating link..."));
        final String id = WebResource.nextId();
        final String port;
        if (Main.config().PORT != 80) {
            port = ":" + Main.config().PORT;
        } else {
            port = "";
        }
        WebResource.worldUploads.put(id, player);
        String link = Main.config().WEB_IP + port + "/web?id=" + id;

        ((Player) player.getPlatformPlayer()).sendMessage("Upload the world: " + link);

        final HashMap<String, String> map = new HashMap<>();
        map.put("id", id);
        final Request r = new Request("*", "*", "/web", map, 2);
        Main.imp().getWebServer().getRequestManager().addToken(r);
        future.complete(true);
        return future;
    }

}
