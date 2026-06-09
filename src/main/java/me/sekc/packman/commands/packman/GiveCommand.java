package me.sekc.packman.commands.packman;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.sekc.packman.Packman;
import me.sekc.packman.commands.BaseCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class GiveCommand extends BaseCommand {
    static public void register(Packman plugin, LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("give")
			.then(Commands.argument("player", StringArgumentType.word())
				.then(Commands.argument("pack_name", StringArgumentType.word())
					.then(Commands.argument("item_name", StringArgumentType.word())
						.executes(ctx -> {
							String player_name = ctx.getArgument("player", String.class);
							String pack_name = ctx.getArgument("pack_name", String.class);
							String item_name = ctx.getArgument("item_name", String.class);

							Player player = Bukkit.getPlayer(player_name);
							if (player == null) {
								throw new RuntimeException("Player " + player_name + " not found.");
							}

							player.getInventory().addItem(plugin.getItem(pack_name, item_name));

							return Command.SINGLE_SUCCESS;
						})
					)
					.requires(sender -> {
						Entity executor = sender.getExecutor();
						if (executor == null) return false;
						return sender.getSender().hasPermission("packman.give");// not in a clan
					})
				)
			)
		);
    }
}
