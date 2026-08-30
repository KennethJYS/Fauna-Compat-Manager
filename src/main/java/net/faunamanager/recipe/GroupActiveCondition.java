package net.faunamanager.recipe;

import com.google.gson.JsonObject;
import net.faunamanager.config.FaunaConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import net.minecraftforge.common.crafting.conditions.ICondition;

/**
 * Condición usada dentro de recetas "forge:conditional" (ver
 * data/alexsmobs/recipes/falconry_glove.json y similares).
 *
 * Es verdadera solo si el jugador eligió explícitamente un mod (o "auto")
 * para el grupo dado — es decir, si el grupo NO está en modo "todos"
 * (coexistencia vanilla, sin intervención de FaunaManager).
 *
 * LIMITACIÓN IMPORTANTE: esta condición se evalúa cuando el juego CARGA
 * las recetas (al unirse a un mundo o al ejecutar /reload), no en cada
 * intento de crafteo. Si el jugador cambia la selección en la GUI de
 * Cloth Config a mitad de partida, el cambio de receta no se refleja
 * hasta el próximo /reload o reingreso al mundo — a diferencia de
 * SpawnSuppressor y MissingDropsLootModifier, que sí son instantáneos.
 */
public class GroupActiveCondition implements ICondition {

    public static final ResourceLocation ID = new ResourceLocation("faunamanager", "group_active");

    private final String groupId;

    public GroupActiveCondition(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        String selection = FaunaConfig.getSelection(groupId);
        return selection != null && !selection.equalsIgnoreCase("todos");
    }

    @Override
    public String toString() {
        return "faunamanager:group_active[" + groupId + "]";
    }

    public static class Serializer implements IConditionSerializer<GroupActiveCondition> {

        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void write(JsonObject json, GroupActiveCondition value) {
            json.addProperty("group", value.groupId);
        }

        @Override
        public GroupActiveCondition read(JsonObject json) {
            return new GroupActiveCondition(json.get("group").getAsString());
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
