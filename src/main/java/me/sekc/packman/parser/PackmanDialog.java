package me.sekc.packman.parser;

import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import me.sekc.packman.Packman;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ServerLinks;
import org.bukkit.configuration.file.YamlConfiguration;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackmanDialog {
	static public void parseDialogsYml(Packman plugin, PackmanPackParser parser, String packName, File pathToPack) {
		File dialogsFolder = new File(pathToPack + "/dialogs/");

		if (!dialogsFolder.isDirectory()) return;

		for (File dialogFile : dialogsFolder.listFiles()) {
			plugin.getLogger().info("Parsing " + dialogFile.getPath());
			YamlConfiguration dialogYml = YamlConfiguration.loadConfiguration(dialogFile);

			addDialog(packName, dialogFile.getName().split("\\.")[0], dialogYml);
		}
	}

	static public void addDialog(String packName, String dialogName, YamlConfiguration file) {
		if (dialogName.equals("serverlinks")) {
			for (Object linkConfigObj : file.getList("links")) {
				if ((linkConfigObj instanceof HashMap<?, ?>)) {
					HashMap<String,?> linkConfig = (HashMap<String,?>)linkConfigObj;
					Bukkit.getServerLinks().addLink(MiniMessage.miniMessage().deserialize(
						(String)(linkConfig.get("title"))),
						URI.create((String)(linkConfig.get("url"))
					));
				}
			}
		}
	}

	static public void unregisterAllDialogs() { // for reloading
		for (ServerLinks.ServerLink serverLink : Bukkit.getServerLinks().getLinks()) {
			Bukkit.getServerLinks().removeLink(serverLink);
		}
	}
}
