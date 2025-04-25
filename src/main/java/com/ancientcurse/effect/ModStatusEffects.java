package com.ancientcurse.effect;

import com.ancientcurse.AncientCurse;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registry for all custom status effects in the Ancient Curse mod
 */
public class ModStatusEffects {
    
    // The five stages of the Khamsin Curse
    public static final StatusEffect KHAMSIN_CURSE_STAGE_1 = new KhamsinCurseEffect(1);
    public static final StatusEffect KHAMSIN_CURSE_STAGE_2 = new KhamsinCurseEffect(2);
    public static final StatusEffect KHAMSIN_CURSE_STAGE_3 = new KhamsinCurseEffect(3);
    public static final StatusEffect KHAMSIN_CURSE_STAGE_4 = new KhamsinCurseEffect(4);
    public static final StatusEffect KHAMSIN_CURSE_STAGE_5 = new KhamsinCurseEffect(5);
    
    /**
     * Register all status effects
     */
    public static void registerStatusEffects() {
        Registry.register(Registries.STATUS_EFFECT, new Identifier(AncientCurse.MOD_ID, "khamsin_curse_stage_1"), KHAMSIN_CURSE_STAGE_1);
        Registry.register(Registries.STATUS_EFFECT, new Identifier(AncientCurse.MOD_ID, "khamsin_curse_stage_2"), KHAMSIN_CURSE_STAGE_2);
        Registry.register(Registries.STATUS_EFFECT, new Identifier(AncientCurse.MOD_ID, "khamsin_curse_stage_3"), KHAMSIN_CURSE_STAGE_3);
        Registry.register(Registries.STATUS_EFFECT, new Identifier(AncientCurse.MOD_ID, "khamsin_curse_stage_4"), KHAMSIN_CURSE_STAGE_4);
        Registry.register(Registries.STATUS_EFFECT, new Identifier(AncientCurse.MOD_ID, "khamsin_curse_stage_5"), KHAMSIN_CURSE_STAGE_5);
        
        AncientCurse.LOGGER.info("Registering ModStatusEffects for " + AncientCurse.MOD_ID);
    }
}
