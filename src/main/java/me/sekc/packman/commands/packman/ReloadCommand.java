package me.sekc.packman.commands.packman;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.sekc.packman.Packman;
import me.sekc.packman.commands.BaseCommand;
import me.sekc.packman.commands.suggestions.ItemNameSuggestionProvider;
import me.sekc.packman.commands.suggestions.PackNameSuggestionProvider;
import me.sekc.packman.commands.suggestions.PlayerSuggestionProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ReloadCommand extends BaseCommand {
    static public void register(Packman plugin, LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("reload")
			.executes(ctx -> {
				plugin.reload();
				return Command.SINGLE_SUCCESS;
			})
		)
		.requires(sender -> {
			Entity executor = sender.getExecutor();
			if (executor == null) return false;
			return sender.getSender().hasPermission("packman.reload");// not in a clan
		});
    }
}
