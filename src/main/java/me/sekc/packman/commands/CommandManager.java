package me.sekc.packman.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.sekc.packman.Packman;
import me.sekc.packman.commands.packman.GiveCommand;

public class CommandManager {
    static public void registerCommands(Packman plugin) {
        plugin.getLogger().info("Creating commands...");

        LiteralArgumentBuilder<CommandSourceStack> clanRoot = Commands.literal("packman");
		GiveCommand.register(plugin, clanRoot);
        LiteralCommandNode<CommandSourceStack> buildClanRoot = clanRoot.build();

        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(buildClanRoot);
        });
    }
}
