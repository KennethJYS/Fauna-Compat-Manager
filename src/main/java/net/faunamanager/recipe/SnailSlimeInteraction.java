package net.faunamanager.recipe;

import net.faunamanager.FaunaManager;
import net.faunamanager.core.CompatEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Critters and Companions le da a su crittersandcompanions:snail la
 * habilidad de producir "snail_slime_bottle" al interactuar con una
 * botella de vidrio — mecánica de código, no datapack, así que no se puede
 * portar vía loot/recipe. Esta clase le da la MISMA habilidad a
 * naturalist:snail, pero SOLO cuando ese es el mob que efectivamente ganó
 * el grupo 'snail' (es decir, cuando crittersandcompanions:snail está
 * suprimido y el jugador perdería esta capacidad por completo si no
 * hiciéramos nada).
 *
 * A diferencia de las recetas de crafteo (evaluadas al cargar el mundo),
 * esto se evalúa en cada interacción — instantáneo, sin necesitar /reload,
 * igual que BearFurBrewingRecipe.
 */
public class SnailSlimeInteraction {

    private static final ResourceLocation SLIME_BOTTLE_ID =
            new ResourceLocation("crittersandcompanions", "snail_slime_bottle");
    private static final String SNAIL_GROUP_ID = "snail";
    private static final String NATURALIST_SNAIL_ENTITY_ID = "naturalist:snail";

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        String entityId = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(event.getTarget().getType()));
        if (!NATURALIST_SNAIL_ENTITY_ID.equals(entityId)) return;

        // Solo si naturalist:snail es de verdad el ganador del grupo ahora
        // mismo (no en modo "todos", y no si crittersandcompanions ganó).
        boolean naturalistIsWinner = FaunaManager.SELECTION_RESOLVER.getWinner(SNAIL_GROUP_ID)
                .map(CompatEntry::entityId)
                .map(NATURALIST_SNAIL_ENTITY_ID::equals)
                .orElse(false);
        if (!naturalistIsWinner) return;

        InteractionHand hand = event.getHand();
        ItemStack held = event.getEntity().getItemInHand(hand);
        if (held.getItem() != Items.GLASS_BOTTLE) return;

        Item slimeBottle = BuiltInRegistries.ITEM.get(SLIME_BOTTLE_ID);
        if (slimeBottle == null) return; // crittersandcompanions no instalado, por seguridad

        if (!event.getLevel().isClientSide()) {
            held.shrink(1);
            ItemStack result = new ItemStack(slimeBottle);
            if (!event.getEntity().getInventory().add(result)) {
                event.getEntity().drop(result, false);
            }
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        event.setCanceled(true);
    }
}
