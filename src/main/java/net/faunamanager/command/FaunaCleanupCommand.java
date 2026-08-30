package net.faunamanager.command;

import com.mojang.brigadier.CommandDispatcher;
import net.faunamanager.FaunaManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;

/**
 * /faunamanager cleanup          -> vista previa (no borra nada, solo cuenta)
 * /faunamanager cleanup confirm  -> ejecuta la eliminación real
 *
 * Elimina entidades ya existentes en el mundo cuyo tipo esté actualmente
 * suprimido (perdió la selección de su grupo). NUNCA elimina:
 *  - Entidades domesticadas (TamableAnimal.isTame() == true) — cubre el
 *    sistema de tameo vanilla; mods con su propio sistema de tameo
 *    (no basado en TamableAnimal) no se detectan por esta vía, limitación
 *    conocida.
 *  - Entidades con nombre personalizado.
 *
 * LIMITACIÓN: solo actúa sobre entidades en chunks actualmente cargados
 * (no fuerza carga de chunks lejanos) — el jugador puede necesitar
 * recorrer el mundo y repetir el comando para cubrir más área.
 *
 * Requiere permiso de operador (nivel 2) por ser una acción destructiva.
 */
public class FaunaCleanupCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("faunamanager")
                .then(Commands.literal("cleanup")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> run(ctx.getSource(), false))
                        .then(Commands.literal("confirm")
                                .executes(ctx -> run(ctx.getSource(), true)))));
    }

    private static int run(CommandSourceStack source, boolean confirm) {
        int eligible = 0;
        int skippedProtected = 0;

        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getEntities().getAll()) {
                String entityId = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
                if (!FaunaManager.SELECTION_RESOLVER.isSuppressed(entityId)) continue;

                boolean tamed = entity instanceof TamableAnimal tamable && tamable.isTame();
                if (tamed || entity.hasCustomName()) {
                    skippedProtected++;
                    continue;
                }

                eligible++;
                if (confirm) {
                    entity.discard();
                }
            }
        }

        int finalEligible = eligible;
        int finalSkipped = skippedProtected;
        if (confirm) {
            source.sendSuccess(() -> Component.translatable("faunamanager.cleanup.done", finalEligible, finalSkipped), true);
        } else {
            source.sendSuccess(() -> Component.translatable("faunamanager.cleanup.preview", finalEligible, finalSkipped), false);
        }
        return eligible;
    }
}
