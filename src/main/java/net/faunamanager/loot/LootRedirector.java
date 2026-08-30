package net.faunamanager.loot;

import com.mojang.serialization.Codec;
import net.faunamanager.FaunaManager;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class LootRedirector {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> GLM_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, FaunaManager.MOD_ID);

    public static final RegistryObject<Codec<MissingDropsLootModifier>> MISSING_DROPS =
            GLM_SERIALIZERS.register("missing_drops", () -> MissingDropsLootModifier.CODEC);
}
