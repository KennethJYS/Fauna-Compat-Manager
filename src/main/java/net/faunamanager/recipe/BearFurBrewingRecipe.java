package net.faunamanager.recipe;

import net.faunamanager.config.FaunaConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Alex's Mobs registra, por código (no datapack — el sistema de brewing de
 * Forge/vanilla no es data-driven), la receta:
 *   Potion of Strength + Hair of Bear (alexsmobs:bear_fur) -> Potion of
 *   Knockback Resistance (alexsmobs:knockback_resistance).
 *
 * Esta clase AÑADE una segunda receta equivalente usando naturalist:fur
 * como ingrediente, para que el jugador no pierda acceso a esta poción si
 * el grupo 'bear' termina resolviéndose a favor de Naturalist.
 *
 * A diferencia de las recetas de crafteo (data-driven, evaluadas al
 * cargar el mundo), el brewing se evalúa en CADA intento, así que esto
 * respeta el modo "todos" de forma instantánea, sin necesitar /reload.
 */
public class BearFurBrewingRecipe implements IBrewingRecipe {

    private static final ResourceLocation OUTPUT_POTION_ID = new ResourceLocation("alexsmobs", "knockback_resistance");
    private static final ResourceLocation INGREDIENT_ID = new ResourceLocation("naturalist", "fur");

    @Override
    public boolean isInput(ItemStack input) {
        return isPotionItem(input) && PotionUtils.getPotion(input) == Potions.STRENGTH;
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        // En modo "todos" (default) FaunaManager no interviene en NADA,
        // ni siquiera en esta receta añadida — comportamiento 100% vanilla
        // de ambos mods por separado, como pidió el usuario.
        String selection = FaunaConfig.getSelection("bear");
        if (selection == null || selection.equalsIgnoreCase("todos")) return false;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(ingredient.getItem());
        return INGREDIENT_ID.equals(itemId);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isInput(input) || !isIngredient(ingredient)) return ItemStack.EMPTY;

        Potion output = ForgeRegistries.POTIONS.getValue(OUTPUT_POTION_ID);
        if (output == null) return ItemStack.EMPTY; // alexsmobs no instalado, por seguridad

        return PotionUtils.setPotion(new ItemStack(input.getItem()), output);
    }

    private boolean isPotionItem(ItemStack stack) {
        return stack.getItem() == Items.POTION
                || stack.getItem() == Items.SPLASH_POTION
                || stack.getItem() == Items.LINGERING_POTION;
    }
}
