package me.sekc.packman.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.sekc.packman.Packman;
import org.bukkit.OfflinePlayer;

import java.util.Map;

public class PlaceholderAPIPlaceholders extends PlaceholderExpansion {

	private final Packman plugin;

	public PlaceholderAPIPlaceholders(Packman plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getAuthor() {
		return String.join(", ", plugin.getPluginMeta().getAuthors());
	}

	@Override
	public String getIdentifier() {
		return "pm";
	}

	@Override
	public String getVersion() {
		return plugin.getPluginMeta().getVersion();
	}

	@Override
	public boolean persist() {
		return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
	}

	@Override
	public String onRequest(OfflinePlayer player, String params) {
		// %pm_glyph:
		if (params.startsWith("glyph:")) {
			String[] args = params.split(":");
			if (args.length != 3) {
				return "Invalid arguments, use like so: %pm_glyph:PACKNAME:GLYPHNAME%";
			}

			String packName = args[1];
			String glyphName = args[2];

			Character glyphChar = plugin.packmanPackParser.glyphToCharMap.get(Map.entry(packName, glyphName));
			return glyphChar == null ? "INVALID GLYPH" : String.valueOf(glyphChar);
		}

		// %pm_shift:
		if (params.startsWith("shift:")) {
			String[] args = params.split(":");
			if (args.length != 2) {
				return "Invalid arguments, use like so: %pm_shift:AMOUNT%";
			}

			int shift = 0;
			try {
				shift = Integer.parseInt(args[1]);
			} catch (Exception e) {
				throw new RuntimeException("Shift needs to be a number: " + params);
			}

			Character glyphChar = plugin.packmanPackParser.spaceProviderGlyphs.get(shift);
			return glyphChar == null ? "INVALID SHIFT" : String.valueOf(glyphChar);
		}

		return null; // Placeholder is unknown by the Expansion
	}
}
