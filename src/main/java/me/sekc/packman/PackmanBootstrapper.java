package me.sekc.packman;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

public class PackmanBootstrapper implements PluginBootstrap {

	@Override
	public void bootstrap(BootstrapContext context) {
		/*context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose()
			.newHandler(event -> event.registry().register(
				DialogKeys.create(Key.key("packman:quick_actions")),
				builder -> builder
					// Build your dialog here ...
					.base(DialogBase.builder(Component.text("Title")).build())
					.type(DialogType.notice())
			)));*/
	}

	@Override
	public JavaPlugin createPlugin(PluginProviderContext context) {
		return new Packman();
	}


}
