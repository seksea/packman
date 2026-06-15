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
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class GiveCommand extends BaseCommand {
	static private void giveCommand(Packman plugin, CommandContext<CommandSourceStack> ctx, int amount) {
		String player_name = ctx.getArgument("player", String.class);
		String pack_name = ctx.getArgument("pack_name", String.class);
		String item_name = ctx.getArgument("item_name", String.class);

		Player player = Bukkit.getPlayer(player_name);
		if (player == null) {
			throw new RuntimeException("Player " + player_name + " not found.");
		}

		ItemStack stack = plugin.getCustomItemStack(pack_name, item_name);
		stack.setAmount(amount);

		player.getInventory().addItem(stack);

		ctx.getSource().getSender().sendMessage(plugin.getMessage("gave-item", List.of(
			Map.entry("%amount%", String.valueOf(amount)),
			Map.entry("%pack_name%", pack_name),
			Map.entry("%item_name%", item_name),
			Map.entry("%player_name%", player_name)
		)));
	}

    static public void register(Packman plugin, LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("give")
			.then(Commands.argument("player", StringArgumentType.word())
				.suggests(new PlayerSuggestionProvider())
				.then(Commands.argument("pack_name", StringArgumentType.word())
					.suggests(new PackNameSuggestionProvider(plugin))
					.then(Commands.argument("item_name", StringArgumentType.word())
						.suggests(new ItemNameSuggestionProvider(plugin))
						.executes(ctx -> {
							giveCommand(plugin, ctx, 1);

							return Command.SINGLE_SUCCESS;
						})
						.then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
							.executes(ctx -> {
								giveCommand(plugin, ctx, ctx.getArgument("amount", Integer.class));

								return Command.SINGLE_SUCCESS;
							})
						)
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
