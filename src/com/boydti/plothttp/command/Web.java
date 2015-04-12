package com.boydti.plothttp.command;

import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;

import com.boydti.plothttp.Main;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.Permissions;

public class Web extends SubCommand {

    public Web() {
        super("web", "plots.web", "Web related commands", "", "web", CommandCategory.DEBUG, false);
    }
    
    public void noargs(PlotPlayer player) {
        ArrayList<String> args = new ArrayList<String>();
        if (Permissions.hasPermission(player, "plots.web.reload")) {
            args.add("reload");
        }
        if (Permissions.hasPermission(player, "plots.web.download")) {
            args.add("download");
        }
        if (Permissions.hasPermission(player, "plots.web.upload")) {
            args.add("upload");
        }
        if (args.size() == 0) {
            MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.<reload|download|upload>");
            return;
        }
        MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot web <" + StringUtils.join(args, "|") + ">");
    }

    @Override
    public boolean execute(PlotPlayer player, String... args) {
        if (args.length == 0) {
            noargs(player);
            return false;
        }
        switch (args[0]) {
            case "reload": {
                if (!Permissions.hasPermission(player, "plots.web.reload")) {
                    MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.reload");
                    return false;
                }
                Main.plugin.onDisable();
                Main.plugin.onEnable();
                MainUtil.sendMessage(player, "&aReloaded success!");
                return true;
            }
            case "download": {
                if (!Permissions.hasPermission(player, "plots.web.download")) {
                    MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.reload");
                    return false;
                }
                
                return true;
            }
            case "upload": {
                if (!Permissions.hasPermission(player, "plots.web.upload")) {
                    MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.reload");
                    return false;
                }
                
                return false;
            }
            default: {
                noargs(player);
                return false;
            }
        }
    }
    
}
