package net.faunamanager.core;

import net.minecraftforge.fml.ModList;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Envoltorio delgado sobre {@link ModList} de Forge.
 * Toda consulta de "¿está instalado el mod X?" pasa por aquí para que el
 * resto del código nunca dependa directamente de la API de Forge.
 */
public final class ModDetector {

    private ModDetector() {
    }

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /**
     * Dado un conjunto de modIds "candidatos" (los que aparecen en algún
     * grupo de equivalencia cargado), devuelve solo los que están
     * realmente instalados en esta partida.
     */
    public static Set<String> filterInstalled(Set<String> candidateModIds) {
        return candidateModIds.stream()
                .filter(ModDetector::isLoaded)
                .collect(Collectors.toSet());
    }

    /**
     * Lista de modIds de fauna conocidos por FaunaManager, filtrada a los
     * que están efectivamente cargados. Se usa solo para logging/diagnóstico;
     * la lógica real de resolución usa los modIds declarados en cada
     * CompatEntry, no esta lista fija.
     */
    public static Set<String> installedFaunaMods() {
        return filterInstalled(Set.of(
                "alexsmobs",
                "naturalist",
                "crittersandcompanions",
                "untamedwilds",
                "aquamirae"
        ));
    }
}
