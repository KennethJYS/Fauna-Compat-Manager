package net.faunamanager.core;

/**
 * Especificación de un drop: qué ítem y en qué cantidad (rango, igual que
 * una loot table normal). Usado tanto para generar tags (donde min/max no
 * importa) como para MissingDropsLootModifier (donde sí, para replicar
 * fielmente la probabilidad original en vez de forzar count=1).
 */
public record DropSpec(String itemId, int min, int max, double chance, java.util.List<String> variants,
                        String requiresKillerType) {

    public DropSpec {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("Rango de drop inválido: min=" + min + " max=" + max);
        }
        if (chance < 0.0 || chance > 1.0) {
            throw new IllegalArgumentException("chance debe estar entre 0.0 y 1.0, fue: " + chance);
        }
    }

    public static DropSpec exactlyOne(String itemId) {
        return new DropSpec(itemId, 1, 1, 1.0, java.util.List.of(), null);
    }

    public boolean hasVariants() {
        return variants != null && !variants.isEmpty();
    }

    public boolean requiresSpecificKiller() {
        return requiresKillerType != null && !requiresKillerType.isBlank();
    }
}
