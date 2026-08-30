package net.faunamanager;

import net.faunamanager.client.FaunaConfigScreen;
import net.faunamanager.command.FaunaCleanupCommand;
import net.faunamanager.config.FaunaConfig;
import net.faunamanager.core.EquivalenceRegistry;
import net.faunamanager.core.ModDetector;
import net.faunamanager.core.SelectionResolver;
import net.faunamanager.loot.LootRedirector;
import net.faunamanager.spawn.SpawnSuppressor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.faunamanager.recipe.BearFurBrewingRecipe;
import net.faunamanager.recipe.GroupActiveCondition;
import net.faunamanager.recipe.SnailSlimeInteraction;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.ModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(FaunaManager.MOD_ID)
public class FaunaManager {

    public static final String MOD_ID = "faunamanager";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Instancias únicas del núcleo del mod. Se inicializan en el constructor
    // porque ModList ya está disponible en esta fase temprana.
    public static final EquivalenceRegistry EQUIVALENCE_REGISTRY = new EquivalenceRegistry();
    public static final SelectionResolver SELECTION_RESOLVER = new SelectionResolver(EQUIVALENCE_REGISTRY);

    public FaunaManager() {
        LOGGER.info("Inicializando FaunaManager...");

        // Debe registrarse ANTES de que se parseen recetas (temprano en el
        // constructor es seguro y es la práctica estándar de Forge).
        CraftingHelper.register(GroupActiveCondition.Serializer.INSTANCE);

        FaunaConfig.register();

        // El Supplier/BiFunction de abajo solo se INVOCA cuando el jugador
        // abre el menú de mods (siempre en cliente) — la clase
        // FaunaConfigScreen (que usa Cloth Config) nunca se carga en un
        // servidor dedicado gracias a la evaluación perezosa de lambdas.
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> FaunaConfigScreen.build(parent)));

        LootRedirector.GLM_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigLoad);

        // Carga/recarga los JSON de compat/groups/*.json (datapack + config/).
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListener);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        // Eventos de juego: supresión de spawns duplicados.
        MinecraftForge.EVENT_BUS.register(new SpawnSuppressor(SELECTION_RESOLVER));
        MinecraftForge.EVENT_BUS.register(new SnailSlimeInteraction());

        LOGGER.info("Mods de fauna detectados: {}", ModDetector.installedFaunaMods());
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(EQUIVALENCE_REGISTRY);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        FaunaCleanupCommand.register(event.getDispatcher());
    }

    private void onConfigLoad(ModConfigEvent event) {
        FaunaConfig.refreshCache();
        SELECTION_RESOLVER.rebuild();
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            SELECTION_RESOLVER.rebuild();
            BrewingRecipeRegistry.addRecipe(new BearFurBrewingRecipe());
            LOGGER.info("FaunaManager listo. Grupos de equivalencia cargados: {}",
                    EQUIVALENCE_REGISTRY.getGroupIds());
        });
    }
}
