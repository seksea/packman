package me.sekc.packman.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.sekc.packman.Packman;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.util.Collections;

public class ResourcePackServer {
	HttpServer server;
	Packman plugin;

	public ResourcePackServer(Packman plugin, int port) throws IOException {
		this.plugin = plugin;

		plugin.getLogger().info("Creating HTTP server on port " + port + ".");
		server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/", new Handler());
		server.setExecutor(null); // creates a default executor
	}

	public void startServer() {
		plugin.getLogger().info("Starting HTTP server to serve the resourcepack on port " + server.getAddress().getPort() + ".");
		server.start();
	}

	static class Handler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			Packman plugin = Packman.getPlugin(Packman.class);

			byte[] bytes = Files.readAllBytes(new File(plugin.getDataPath() + "/pack.zip").toPath());

			if (bytes.length == 0) {
				throw new RemoteException("pack.zip is missing! Cannot serve to client");
			}

			t.getResponseHeaders().put("Content-Disposition", Collections.singletonList("attachment; filename=\"pack.zip\""));

			t.sendResponseHeaders(200, bytes.length);
			OutputStream os = t.getResponseBody();
			os.write(bytes);
			os.close();
		}
	}
}
