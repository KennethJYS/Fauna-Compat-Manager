package net.faunamanager.core;

import net.faunamanager.config.FaunaConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Única fuente de verdad sobre "qué mob gana" por grupo de equivalencia.
 * Tanto SpawnSuppressor como (más adelante) LootRedirector y RecipeRemapper
 * consultan esta clase — nunca deben reimplementar esta lógica.
 */
public class SelectionResolver {

    private final EquivalenceRegistry registry;

    // Cache: entityId (namespace:path) -> true si debe suprimirse.
    // Se reconstruye cada vez que cambian los datos (ver rebuild()).
    private Set<String> suppressedEntityIds = Set.of();
    private Map<String, CompatEntry> winnerByGroup = Map.of();

    public SelectionResolver(EquivalenceRegistry registry) {
        this.registry = registry;
    }

    // entityId del ganador -> lista de DropSpec (item + rango) que le
    // faltan (roles que no dropea nativamente pero sí algún perdedor).
    private Map<String, List<DropSpec>> missingDropsByWinner = Map.of();

    /** Debe llamarse tras cada recarga de datapacks y tras cargar la config. */
    public void rebuild() {
        Map<String, CompatEntry> winners = new HashMap<>();
        Set<String> suppressed = new HashSet<>();
        Map<String, List<DropSpec>> missingDrops = new HashMap<>();

        for (CompatGroup group : registry.getAllGroups()) {
            List<CompatEntry> installedEntries = group.entries().stream()
                    .filter(e -> ModDetector.isLoaded(e.modId()))
                    .collect(Collectors.toList());

            if (installedEntries.isEmpty()) continue; // ningún mod de este grupo está instalado

            String userChoice = FaunaConfig.getSelection(group.groupId());

            // Modo "todos" (default, o explícito): el grupo queda inactivo
            // por completo. Todos los mobs instalados spawnean normal, sin
            // supresión ni inyección de drops — exactamente como si
            // FaunaManager no existiera para este grupo en particular.
            if (userChoice == null || userChoice.equalsIgnoreCase("todos")) {
                continue;
            }

            CompatEntry winner = resolveWinner(userChoice, installedEntries);
            winners.put(group.groupId(), winner);

            for (CompatEntry entry : installedEntries) {
                if (!entry.entityId().equals(winner.entityId())) {
                    suppressed.add(entry.entityId());
                }
            }

            // Por cada rol declarado en CUALQUIER entrada del grupo:
            //  - si el ganador ya lo dropea nativamente -> no hacer nada
            //    (la robustez de recetas se resuelve aparte, vía tags).
            //  - si el ganador NO lo dropea -> tomar el ítem de la entrada
            //    perdedora de mayor prioridad que sí lo tenga, e inyectarlo.
            Set<String> allRoles = installedEntries.stream()
                    .flatMap(e -> e.drops().keySet().stream())
                    .collect(Collectors.toSet());

            List<DropSpec> toInject = new ArrayList<>();
            for (String role : allRoles) {
                if (winner.drops().containsKey(role)) continue; // ya nativo, nada que inyectar

                installedEntries.stream()
                        .filter(e -> !e.entityId().equals(winner.entityId()))
                        .filter(e -> e.drops().containsKey(role))
                        .max(Comparator.comparingInt(CompatEntry::priority))
                        .ifPresent(donor -> toInject.add(donor.drops().get(role)));
            }
            if (!toInject.isEmpty()) {
                missingDrops.put(winner.entityId(), toInject);
            }
        }

        this.winnerByGroup = winners;
        this.suppressedEntityIds = suppressed;
        this.missingDropsByWinner = missingDrops;

        for (Map.Entry<String, CompatEntry> entry : winners.entrySet()) {
            net.faunamanager.FaunaManager.LOGGER.info("SelectionResolver: grupo '{}' -> gana '{}' (mod {})",
                    entry.getKey(), entry.getValue().entityId(), entry.getValue().modId());
        }
        if (!suppressed.isEmpty()) {
            net.faunamanager.FaunaManager.LOGGER.info("SelectionResolver: entidades suprimidas: {}", suppressed);
        }
        if (!missingDrops.isEmpty()) {
            net.faunamanager.FaunaManager.LOGGER.info("SelectionResolver: drops inyectados por entidad: {}", missingDrops);
        }
    }

    /**
     * @param userChoice ya no puede ser null ni "todos" aquí (ver rebuild());
     *                   es "auto" o un modId explícito.
     */
    private CompatEntry resolveWinner(String userChoice, List<CompatEntry> installedEntries) {
        if (!userChoice.equalsIgnoreCase("auto")) {
            for (CompatEntry entry : installedEntries) {
                if (entry.modId().equalsIgnoreCase(userChoice)) {
                    return entry;
                }
            }
            // El usuario eligió un mod que no está instalado / no existe en
            // el grupo: caemos a la resolución automática en vez de fallar.
        }

        // "auto" (o fallback del caso anterior): mayor prioridad gana.
        return installedEntries.stream()
                .max(Comparator.comparingInt(CompatEntry::priority))
                .orElseThrow();
    }

    public boolean isSuppressed(String entityId) {
        return suppressedEntityIds.contains(entityId);
    }

    /**
     * Ítems (itemId como String "namespace:path") que deben inyectarse al
     * loot de esta entidad porque ganó su grupo pero no cubre nativamente
     * algún rol que sí cubría un mob perdedor del mismo grupo.
     * Devuelve lista vacía si no aplica (caso normal: la mayoría de entidades).
     */
    public List<DropSpec> getMissingDrops(String entityId) {
        return missingDropsByWinner.getOrDefault(entityId, List.of());
    }

    public Optional<CompatEntry> getWinner(String groupId) {
        return Optional.ofNullable(winnerByGroup.get(groupId));
    }

    public Map<String, CompatEntry> getAllWinners() {
        return Collections.unmodifiableMap(winnerByGroup);
    }
}
