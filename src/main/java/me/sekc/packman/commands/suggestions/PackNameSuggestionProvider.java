package me.sekc.packman.commands.suggestions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.sekc.packman.Packman;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class PackNameSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
	Packman plugin;

	public PackNameSuggestionProvider(Packman plugin) {
		this.plugin = plugin;
	}

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        for (String packName : plugin.packmanPackParser.packNames) {
            builder.suggest(packName);
        }

        return builder.buildFuture();
    }
}
