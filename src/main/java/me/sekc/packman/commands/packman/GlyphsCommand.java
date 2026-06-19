package me.sekc.packman.commands.packman;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.sekc.packman.Packman;
import me.sekc.packman.commands.BaseCommand;
import me.sekc.packman.commands.suggestions.ItemNameSuggestionProvider;
import me.sekc.packman.commands.suggestions.PackNameSuggestionProvider;
import me.sekc.packman.commands.suggestions.PlayerSuggestionProvider;
import me.sekc.packman.parser.PackmanGlyph;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class GlyphsCommand extends BaseCommand {
	static public void glyphListPage(Packman plugin, CommandContext<CommandSourceStack> ctx, int page) {
		int PAGE_SIZE = 8;

		ctx.getSource().getSender().sendMessage(plugin.getMessage("list-glyph-header", List.of(
			Map.entry("%page%", String.valueOf(page)),
			Map.entry("%page+1%", String.valueOf(page+1)),
			Map.entry("%page-1%", String.valueOf(page-1)),
			Map.entry("%max_page%", String.valueOf((plugin.packmanPackParser.allParsedGlyphs.size() / PAGE_SIZE) + 1))
		)));

		int count = 0;
		int pageStart = (page-1) * PAGE_SIZE;
		int pageEnd = pageStart + PAGE_SIZE;
		for (Map.Entry<Map.Entry<String, String>, PackmanGlyph> glyph : plugin.packmanPackParser.allParsedGlyphs.entrySet()) {
			count++;

			if (count < pageStart) continue;

			if (count > pageEnd) break;

			ctx.getSource().getSender().sendMessage(plugin.getMessage("list-glyph", List.of(
				Map.entry("%glyph%", String.valueOf(plugin.packmanPackParser.glyphToCharMap.get(glyph.getKey()))),
				Map.entry("%pack_name%", glyph.getKey().getKey()),
				Map.entry("%item_name%", glyph.getKey().getValue())
			)));
		}

		if (count < pageEnd) {
			ctx.getSource().getSender().sendMessage(plugin.getMessage("list-glyph-no-more"));
		}
	}

    static public void register(Packman plugin, LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("glyph")
			.then(Commands.literal("list")
				.executes(ctx -> {
					glyphListPage(plugin, ctx, 1);

					return Command.SINGLE_SUCCESS;
				})
				.then(Commands.argument("page_num", IntegerArgumentType.integer(1))
					.executes(ctx -> {
						glyphListPage(plugin, ctx, ctx.getArgument("page_num", Integer.class));

						return Command.SINGLE_SUCCESS;
					})
				)
			).requires(sender -> {
				Entity executor = sender.getExecutor();
				if (executor == null) return false;
				return sender.getSender().hasPermission("packman.glyph-list");// not in a clan
			}));
    }
}
