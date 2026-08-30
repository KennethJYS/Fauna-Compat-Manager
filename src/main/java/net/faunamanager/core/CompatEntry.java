package net.faunamanager.core;

import java.util.Map;

/**
 * Representa una única "oferta" de un mod para una especie dada.
 * Ej: dentro del grupo "bear", una CompatEntry es la oferta de Alex's Mobs
 * (entityId=alexsmobs:bear) y otra la de Naturalist (entityId=naturalist:brown_bear).
 *
 * @param modId       id del mod que provee esta entidad (usado para ModDetector)
 * @param entityId    id completo de la entidad (namespace:path)
 * @param priority    prioridad por defecto si el usuario no elige explícitamente
 * @param drops       mapa "clave canónica" -> "item id real" (ver EquivalenceRegistry)
 * @param spawnTags   tags de bioma donde esta entidad spawnea naturalmente (informativo, v1)
 */
public record CompatEntry(
        String modId,
        String entityId,
        int priority,
        java.util.Map<String, DropSpec> drops,
        Map<String, Object> extra
) {
    public String groupRelativeId() {
        return entityId;
    }
}
