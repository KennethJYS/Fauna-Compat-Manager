package net.faunamanager.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.faunamanager.FaunaManager;
import net.faunamanager.core.DropSpec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

import java.util.List;

/**
 * Implementa la regla de "no perder progresión al elegir un mob".
 *
 * Para el mob ganador de cada grupo (ver SelectionResolver), si le falta
 * algún rol de drop que sí cubría un mob perdedor del mismo grupo (ej.
 * alexsmobs:elephant no dropea nada, pero naturalist:elephant sí dropea
 * leather + bushmeat), este modifier le AÑADE esos ítems a su loot, sin
 * tocar ni reemplazar lo que el mob ya dropeaba nativamente.
 *
 * Caso que NO debe activar esto (y no lo hace, por diseño de
 * SelectionResolver.rebuild()): cuando el rol ya está cubierto por el
 * propio ganador (ej. el oso, que siempre dropea su propia piel) — ahí la
 * robustez de recetas se resuelve con tags, no inyectando un segundo drop
 * redundante.
 */
public class MissingDropsLootModifier extends LootModifier {

    public static final Codec<MissingDropsLootModifier> CODEC = RecordCodecBuilder.create(inst ->
            codecStart(inst).apply(inst, MissingDropsLootModifier::new));

    protected MissingDropsLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (entity == null) return generatedLoot;

        String entityId = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        List<DropSpec> missing = FaunaManager.SELECTION_RESOLVER.getMissingDrops(entityId);
        if (missing.isEmpty()) return generatedLoot;

        for (DropSpec spec : missing) {
            if (spec.chance() < 1.0 && context.getRandom().nextFloat() >= spec.chance()) {
                continue; // no pasó la tirada de probabilidad, sin drop esta vez
            }

            if (spec.requiresSpecificKiller() && !killedBy(context, spec.requiresKillerType())) {
                continue; // ej. froglass: solo si lo mató una rana, no cualquier muerte
            }

            String resolvedItemId = spec.hasVariants()
                    ? spec.variants().get(context.getRandom().nextInt(spec.variants().size()))
                    : spec.itemId();

            ResourceLocation itemId = new ResourceLocation(resolvedItemId);
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null) {
                FaunaManager.LOGGER.warn("MissingDropsLootModifier: item '{}' no encontrado, se omite", resolvedItemId);
                continue;
            }
            int count = context.getRandom().nextIntBetweenInclusive(spec.min(), spec.max());
            if (count > 0) {
                generatedLoot.add(new ItemStack(item, count));
            }
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    private boolean killedBy(LootContext context, String requiredKillerEntityTypeId) {
        Entity killer = context.getParamOrNull(LootContextParams.KILLER_ENTITY);
        if (!(killer instanceof LivingEntity)) return false;

        String killerId = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(killer.getType()));
        return requiredKillerEntityTypeId.equals(killerId);
    }
}
