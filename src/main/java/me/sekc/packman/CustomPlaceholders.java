package me.sekc.packman;

import java.util.Map;

public class CustomPlaceholders {
	Packman plugin;

	public CustomPlaceholders(Packman plugin) {
		this.plugin = plugin;
	}

	private String parseShifts(String input) {
		String resultString = input;

		int curIndex = 0;

		while (true) {
			String tagStart = "[[pmshift:";
			String tagEnd = "]]";
			int startIndex = resultString.indexOf(tagStart, curIndex);
			if (startIndex == -1) break;

			int endIndex = resultString.indexOf(tagEnd, startIndex);
			if (endIndex == -1) break;

			String thisTagArgString = resultString.substring(startIndex + tagStart.length(), endIndex);
			// plugin.getLogger().info(resultString + ": " + startIndex + " -> " + endIndex + " " + thisTagArgString);

			String[] thisTagArgs = thisTagArgString.split(":");
			if (thisTagArgs.length != 1) {
				throw new RuntimeException("Malformed tag, [[pmshift: takes 1 arguments: " + tagStart + thisTagArgString + tagEnd);
			}

			int shift = 0;
			try {
				shift = Integer.parseInt(thisTagArgs[0]);
			} catch (Exception e) {
				throw new RuntimeException("Shift needs to be a number: " + tagStart + thisTagArgString + tagEnd);
			}


			Character glyphChar = plugin.packmanPackParser.spaceProviderGlyphs.get(shift);
			String glyphString = glyphChar == null ? "INVALID SPACE" : String.valueOf(glyphChar);

			resultString = resultString.substring(0, startIndex) + glyphString + resultString.substring(endIndex + tagEnd.length());
		}

		return resultString;
	}

	private String parseGlyphs(String input) {
		String resultString = input;

		int curIndex = 0;

		while (true) {
			String tagStart = "[[pmglyph:";
			String tagEnd = "]]";
			int startIndex = resultString.indexOf(tagStart, curIndex);
			if (startIndex == -1) break;

			int endIndex = resultString.indexOf(tagEnd, startIndex);
			if (endIndex == -1) break;

			String thisTagArgString = resultString.substring(startIndex + tagStart.length(), endIndex);
			// plugin.getLogger().info(resultString + ": " + startIndex + " -> " + endIndex + " " + thisTagArgString);

			String[] thisTagArgs = thisTagArgString.split(":");
			if (thisTagArgs.length != 2) {
				throw new RuntimeException("Malformed tag, [[pmglyph: takes 2 arguments: " + tagStart + thisTagArgString + tagEnd);
			}

			Character glyphChar = plugin.packmanPackParser.glyphToCharMap.get(Map.entry(thisTagArgs[0], thisTagArgs[1]));
			String glyphString = glyphChar == null ? "INVALID GLYPH" : String.valueOf(glyphChar);

			resultString = resultString.substring(0, startIndex) + glyphString + resultString.substring(endIndex + tagEnd.length());
		}

		return resultString;
	}

	public String parseString(String input) {
		String resultString = input;

		resultString = parseShifts(resultString);
		resultString = parseGlyphs(resultString);

		return resultString;
	}
}
