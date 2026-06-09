package me.sekc.packman.commands.suggestions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.sekc.packman.Packman;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ItemNameSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
	Packman plugin;

	public ItemNameSuggestionProvider(Packman plugin) {
		this.plugin = plugin;
	}

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

		String packName = context.getArgument("pack_name", String.class);

        for (Map.Entry<String, String> item : plugin.packmanPackParser.allParsedItems.keySet()) {
			if (item.getKey().equals(packName))
            	builder.suggest(item.getValue());
        }

        return builder.buildFuture();
    }
}
