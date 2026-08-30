package net.faunamanager.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.faunamanager.FaunaManager;
import net.faunamanager.config.FaunaConfig;
import net.faunamanager.core.CompatGroup;
import net.faunamanager.core.ModDetector;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera, en runtime, un dropdown por cada grupo de compat CARGADO (ver
 * EquivalenceRegistry) que tenga 2+ mods realmente instalados — no tiene
 * sentido mostrar un selector para un grupo donde solo uno de los mods
 * está presente. Las opciones de cada dropdown son exactamente los modIds
 * instalados de ese grupo, más "auto" (deja que gane la prioridad definida
 * en el compat/).
 *
 * No conocemos los grupos de antemano (dependen de qué JSON haya en
 * compat/groups/ y qué mods estén instalados), así que esta pantalla NO
 * puede construirse con un ForgeConfigSpec estático — de ahí la necesidad
 * de Cloth Config, que sí permite screens 100% dinámicas.
 */
public class FaunaConfigScreen {

    // Selecciones pendientes de guardar (se llenan mientras el jugador
    // interactúa con los dropdowns, se aplican todas juntas al presionar
    // "Save" gracias a setSavingRunnable).
    private static final Map<String, String> pending = new HashMap<>();

    public static Screen build(Screen parent) {
        pending.clear();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("faunamanager.config.title"))
                .setSavingRunnable(FaunaConfigScreen::save);

        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("faunamanager.config.category.selection"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        Map<String, String> current = FaunaConfig.getAllSelections();

        for (CompatGroup group : FaunaManager.EQUIVALENCE_REGISTRY.getAllGroups()) {
            List<String> installedMods = group.entries().stream()
                    .map(net.faunamanager.core.CompatEntry::modId)
                    .distinct()
                    .filter(ModDetector::isLoaded)
                    .toList();

            // Sin al menos 2 mods instalados para este grupo no hay nada
            // que elegir — no mostramos el dropdown.
            if (installedMods.size() < 2) continue;

            List<String> options = new ArrayList<>();
            options.add("todos");
            options.addAll(installedMods);

            String currentValue = current.getOrDefault(group.groupId(), "todos");
            if (!options.contains(currentValue)) currentValue = "todos";

            // "faunamanager.group.<id>" se traduce vía lang/*.json si existe
            // (ver los 10 grupos actuales); si el jugador tiene un grupo sin
            // traducir (ej. de un futuro 4to mod), Minecraft muestra la
            // clave cruda como fallback — no rompe nada, solo se ve menos
            // bonito hasta que se agregue la traducción correspondiente.
            Component groupLabel = Component.translatable("faunamanager.group." + group.groupId());

            category.addEntry(entryBuilder
                    .startSelector(groupLabel, options.toArray(new String[0]), currentValue)
                    .setDefaultValue("todos")
                    .setNameProvider(value -> "todos".equals(value)
                            ? Component.translatable("faunamanager.config.option.todos")
                            : Component.literal(value))
                    .setTooltip(Component.translatable("faunamanager.config.tooltip", groupLabel))
                    .setSaveConsumer(selected -> pending.put(group.groupId(), selected))
                    .build());
        }

        ConfigCategory maintenance = builder.getOrCreateCategory(
                Component.translatable("faunamanager.config.category.maintenance"));
        maintenance.addEntry(entryBuilder
                .startTextDescription(Component.translatable("faunamanager.config.maintenance.cleanup_info"))
                .build());

        return builder.build();
    }

    private static void save() {
        // Empezamos desde las selecciones ya guardadas, para no perder
        // grupos que el jugador no tocó en esta sesión de la pantalla.
        Map<String, String> merged = new HashMap<>(FaunaConfig.getAllSelections());
        merged.putAll(pending);

        FaunaConfig.applySelections(merged);
        FaunaManager.SELECTION_RESOLVER.rebuild();

        FaunaManager.LOGGER.info("FaunaConfigScreen: selecciones guardadas -> {}", merged);
    }
}
