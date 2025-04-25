package com.ancientcurse.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/**
 * The Khamsin Curse - A progressive curse with 5 stages of increasing severity
 * Each stage lasts 25 minutes (30000 ticks) and applies different negative effects
 */
public class KhamsinCurseEffect extends StatusEffect {
    
    // Duration for each stage (25 minutes = 30000 ticks)
    public static final int STAGE_DURATION = 30000;
    
    // The current stage of the curse (1-5)
    private final int stage;
    
    public KhamsinCurseEffect(int stage) {
        super(StatusEffectCategory.HARMFUL, 0xD4AF37); // Gold/amber color for the effect
        this.stage = Math.max(1, Math.min(5, stage)); // Ensure stage is between 1-5
    }
    
    /**
     * Gets the current stage of the curse
     * @return The stage (1-5)
     */
    public int getStage() {
        return stage;
    }
    
    /**
     * Gets the texture for the current stage of the curse
     * @return The identifier for the texture
     */
    public Identifier getTexture() {
        return new Identifier("ancientcurse", "textures/gui/curse_phase_" + stage + "_icon.png");
    }
    
    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        // Apply the update effect every second (20 ticks)
        return duration % 20 == 0;
    }
    
    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity instanceof PlayerEntity player) {
            // Apply different effects based on the stage
            switch (stage) {
                case 1 -> {
                    // Stage 1: Mild effects - slight slowness
                    if (player.getRandom().nextFloat() < 0.1f) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 0, false, false, true));
                    }
                }
                case 2 -> {
                    // Stage 2: Moderate effects - hunger
                    if (player.getRandom().nextFloat() < 0.15f) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 200, 0, false, false, true));
                    }
                }
                case 3 -> {
                    // Stage 3: Severe effects - weakness
                    if (player.getRandom().nextFloat() < 0.2f) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 300, 1, false, false, true));
                    }
                }
                case 4 -> {
                    // Stage 4: Critical effects - mining fatigue
                    if (player.getRandom().nextFloat() < 0.25f) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 400, 1, false, false, true));
                    }
                }
                case 5 -> {
                    // Stage 5: Terminal effects - wither
                    if (player.getRandom().nextFloat() < 0.3f) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 0, false, false, true));
                    }
                }
            }
        }
    }
    
    // These methods are not overrides in the current Minecraft version
    // They were likely removed or changed in the API
    
    // Always render the effect in the player's status effect HUD
    // Always show the text in the inventory
    // Always render in the HUD
}
