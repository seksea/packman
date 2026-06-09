package me.sekc.packman.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.sekc.packman.Packman;

public class BaseCommand {
    static public void register(Packman plugin, LiteralArgumentBuilder<CommandSourceStack> root) {
        plugin.getLogger().warning("Ran base BaseCommand.register, something has gone seriously wrong");
    }
}
