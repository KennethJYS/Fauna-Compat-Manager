package net.faunamanager.spawn;

import net.faunamanager.core.SelectionResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Cancela el spawn natural de cualquier entidad marcada como "suprimida"
 * por el SelectionResolver (es decir: perdió la selección de su grupo de
 * equivalencia frente a otra entidad del mismo grupo).
 *
 * Usamos MobSpawnEvent.SpawnPlacementCheck (no FinalizeSpawn) porque corta
 * el spawn antes de que el motor invierta trabajo posicionando la entidad —
 * es el mismo punto de intercepción que usa VMinus (mod de referencia con
 * 1M+ descargas) para su sistema de "banned entities". No afecta spawn eggs
 * ni comandos /summon: el jugador sigue pudiendo invocar manualmente lo que
 * quiera, solo se suprime el spawn *natural* del mundo.
 */
public class SpawnSuppressor {

    private final SelectionResolver resolver;

    public SpawnSuppressor(SelectionResolver resolver) {
        this.resolver = resolver;
    }

    @SubscribeEvent
    public void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        EntityType<?> entityType = event.getEntityType();
        String entityId = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));

        if (resolver.isSuppressed(entityId)) {
            if (event.isCancelable()) {
                event.setCanceled(true);
            } else if (event.hasResult()) {
                event.setResult(Event.Result.DENY);
            }
        }
    }
}
