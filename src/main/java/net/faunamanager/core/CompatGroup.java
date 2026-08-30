package net.faunamanager.core;

import java.util.List;

/**
 * Un grupo de equivalencia completo, ej. "bear" con sus 2+ CompatEntry.
 *
 * @param groupId        identificador interno del grupo (ej. "bear")
 * @param entries        todas las entradas declaradas en el JSON, sin filtrar por mods instalados
 * @param canonicalDrops claves de "drops" que deben resolverse dinámicamente
 *                       (usadas por LootRedirector y RecipeRemapper)
 */
public record CompatGroup(
        String groupId,
        List<CompatEntry> entries,
        List<String> canonicalDrops
) {
}
