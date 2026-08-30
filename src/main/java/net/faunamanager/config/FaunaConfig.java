package net.faunamanager.config;

import net.faunamanager.FaunaManager;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Config TOML del usuario. En v1 usamos una lista de strings en formato
 * "grupo=modId" en vez de un mapa dinámico, porque ForgeConfigSpec no
 * soporta bien claves generadas en runtime (los grupos se descubren desde
 * los JSON de compat, no se conocen de antemano al definir el spec).
 *
 * Ejemplo en el TOML generado:
 *
 * [selections]
 *   # Formato: "grupo=modId". Usa "auto" o simplemente omite la línea
 *   # para dejar que FaunaManager elija según prioridad.
 *   entries = [
 *     "bear=alexsmobs",
 *     "snail=naturalist"
 *   ]
 *
 * TODO (v2): generar un spec dinámico por grupo tras la primera carga de
 * EquivalenceRegistry, para que cada grupo tenga su propia línea comentada
 * con las opciones válidas (ej. "bear = \"alexsmobs\" # alexsmobs | naturalist").
 */
public class FaunaConfig {

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SELECTIONS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("selections");
        SELECTIONS = builder
                .comment(
                        "Elige qué mod provee cada especie duplicada.",
                        "Formato: \"grupo=modId\", ej. \"bear=alexsmobs\".",
                        "Omitir un grupo (default, o \"grupo=todos\") = AMBOS mobs conviven sin cambios,",
                        "como si FaunaManager no existiera para ese grupo.",
                        "Recomendado: usar la pantalla de configuración in-game en vez de editar esto a mano."
                )
                .defineList("entries", List.of(), o -> o instanceof String);
        builder.pop();

        SPEC = builder.build();
    }

    private static Map<String, String> cache = new HashMap<>();

    public static void register() {
        net.minecraftforge.fml.ModLoadingContext.get()
                .registerConfig(ModConfig.Type.COMMON, SPEC, FaunaManager.MOD_ID + "-common.toml");
    }

    /** Debe llamarse en ModConfigEvent (carga o recarga) para refrescar la caché. */
    public static void refreshCache() {
        Map<String, String> map = new HashMap<>();
        for (String raw : SELECTIONS.get()) {
            String[] parts = raw.split("=", 2);
            if (parts.length == 2) {
                map.put(parts[0].trim().toLowerCase(), parts[1].trim().toLowerCase());
            } else {
                FaunaManager.LOGGER.warn("Entrada de config inválida en selections.entries: '{}'", raw);
            }
        }
        cache = map;
    }

    /** @return el modId elegido por el usuario para ese grupo, o null si no hay entrada. */
    public static String getSelection(String groupId) {
        return cache.get(groupId.toLowerCase());
    }

    /** @return copia inmutable de todas las selecciones actuales (grupo -> modId). */
    public static Map<String, String> getAllSelections() {
        return java.util.Collections.unmodifiableMap(new HashMap<>(cache));
    }

    /**
     * Reemplaza TODAS las selecciones a la vez (usado por la pantalla de
     * configuración de Cloth Config al guardar). Entradas con valor "auto"
     * se omiten del TOML (equivalente a no tener preferencia).
     */
    public static void applySelections(Map<String, String> selections) {
        List<String> entries = new java.util.ArrayList<>();
        for (Map.Entry<String, String> e : selections.entrySet()) {
            if (e.getValue() == null || e.getValue().equalsIgnoreCase("todos")) continue;
            entries.add(e.getKey() + "=" + e.getValue());
        }
        SELECTIONS.set(entries);
        refreshCache();
    }
}
