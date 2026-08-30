package net.faunamanager.core;

import com.google.gson.*;
import net.faunamanager.FaunaManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.*;

/**
 * Carga los archivos de compat desde dos fuentes, igual que hace
 * BannedRecipeManager en VMinus (patrón confirmado como el correcto para
 * permitir tanto datapacks como edición manual):
 *
 *   1. data/faunamanager/compat/groups/*.json   (datapacks / dentro del mod)
 *   2. config/faunamanager/compat/*.json         (el usuario puede añadir/editar)
 *
 * Cada archivo describe UN grupo de equivalencia (una especie).
 * Formato esperado, ver compat/groups/bear.json de ejemplo:
 *
 * {
 *   "group_id": "bear",
 *   "entries": [
 *     { "mod_id": "alexsmobs", "entity_id": "alexsmobs:bear", "priority": 10,
 *       "drops": { "bear_fur": "alexsmobs:bear_fur" } },
 *     { "mod_id": "naturalist", "entity_id": "naturalist:brown_bear", "priority": 5,
 *       "drops": { "bear_fur": "naturalist:bear_fur" } }
 *   ],
 *   "canonical_drops": ["bear_fur"]
 * }
 */
public class EquivalenceRegistry extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, CompatGroup> groups = new LinkedHashMap<>();

    public EquivalenceRegistry() {
        // "compat/groups" -> data/<namespace>/compat/groups/*.json en cualquier datapack activo
        super(GSON, "compat/groups");
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> objects,
                          ResourceManager resourceManager,
                          ProfilerFiller profiler) {
        groups.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                parseAndRegister(entry.getValue().getAsJsonObject(), entry.getKey().toString());
            } catch (Exception e) {
                FaunaManager.LOGGER.error("Error cargando grupo de compat '{}':", entry.getKey(), e);
            }
        }

        loadFromConfigDirectory();

        FaunaManager.LOGGER.info("EquivalenceRegistry: {} grupos cargados ({})",
                groups.size(), groups.keySet());

        // Cada vez que cambian los datos (mundo cargado, /reload, etc.) hay
        // que recalcular quién gana cada grupo.
        FaunaManager.SELECTION_RESOLVER.rebuild();
    }

    private void loadFromConfigDirectory() {
        File configDir = new File(FMLPaths.CONFIGDIR.get().toFile(), "faunamanager/compat");
        if (!configDir.exists() || !configDir.isDirectory()) return;

        File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                parseAndRegister(json, "config/" + file.getName());
            } catch (Exception e) {
                FaunaManager.LOGGER.error("Error leyendo archivo de compat de config '{}':", file.getName(), e);
            }
        }
    }

    private void parseAndRegister(JsonObject obj, String sourceForLogging) {
        String groupId = obj.get("group_id").getAsString();

        List<CompatEntry> entries = new ArrayList<>();
        for (JsonElement el : obj.getAsJsonArray("entries")) {
            JsonObject entryObj = el.getAsJsonObject();

            Map<String, DropSpec> drops = new LinkedHashMap<>();
            if (entryObj.has("drops")) {
                for (Map.Entry<String, JsonElement> d : entryObj.getAsJsonObject("drops").entrySet()) {
                    JsonElement value = d.getValue();
                    if (value.isJsonPrimitive()) {
                        // Formato corto: "role": "item:id" -> siempre count=1.
                        // Válido para roles que solo se usan para generar
                        // tags (nunca se inyectan vía LootRedirector), como
                        // bear_fur o catfish.
                        drops.put(d.getKey(), DropSpec.exactlyOne(value.getAsString()));
                    } else {
                        // Formato completo: { "item": "...", "min": X, "max": Y }
                        // Usar SIEMPRE que el rol pueda terminar inyectándose
                        // (ver MissingDropsLootModifier), para replicar la
                        // probabilidad real de la loot table original.
                        JsonObject dropObj = value.getAsJsonObject();
                        String itemId = dropObj.has("item") ? dropObj.get("item").getAsString() : null;
                        int min = dropObj.has("min") ? dropObj.get("min").getAsInt() : 1;
                        int max = dropObj.has("max") ? dropObj.get("max").getAsInt() : min;
                        double chance = dropObj.has("chance") ? dropObj.get("chance").getAsDouble() : 1.0;

                        List<String> variants = new ArrayList<>();
                        if (dropObj.has("variants")) {
                            for (JsonElement v : dropObj.getAsJsonArray("variants")) {
                                variants.add(v.getAsString());
                            }
                        }

                        String requiresKiller = dropObj.has("requires_killer")
                                ? dropObj.get("requires_killer").getAsString() : null;

                        drops.put(d.getKey(), new DropSpec(itemId, min, max, chance, variants, requiresKiller));
                    }
                }
            }

            entries.add(new CompatEntry(
                    entryObj.get("mod_id").getAsString(),
                    entryObj.get("entity_id").getAsString(),
                    entryObj.has("priority") ? entryObj.get("priority").getAsInt() : 0,
                    drops,
                    Map.of()
            ));
        }

        List<String> canonicalDrops = new ArrayList<>();
        if (obj.has("canonical_drops")) {
            for (JsonElement el : obj.getAsJsonArray("canonical_drops")) {
                canonicalDrops.add(el.getAsString());
            }
        }

        if (groups.containsKey(groupId)) {
            FaunaManager.LOGGER.warn("Grupo de compat duplicado '{}' en {} — se sobreescribe el anterior.",
                    groupId, sourceForLogging);
        }
        groups.put(groupId, new CompatGroup(groupId, entries, canonicalDrops));
    }

    public Optional<CompatGroup> getGroup(String groupId) {
        return Optional.ofNullable(groups.get(groupId));
    }

    public Set<String> getGroupIds() {
        return Collections.unmodifiableSet(groups.keySet());
    }

    public Collection<CompatGroup> getAllGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }
}
