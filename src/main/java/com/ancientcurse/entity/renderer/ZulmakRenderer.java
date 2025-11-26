package com.ancientcurse.entity.renderer;

import com.ancientcurse.entity.ZulmakEntity;
import com.ancientcurse.entity.model.ZulmakModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Zulmak Entity Renderer
 *
 * Simple GeckoLib renderer - expand with render layers for:
 * - Glowing effects
 * - Particle effects
 * - Armor overlays
 * - etc.
 */
public class ZulmakRenderer extends GeoEntityRenderer<ZulmakEntity> {

    public ZulmakRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new ZulmakModel());

        // Shadow size - adjust based on entity size
        this.shadowRadius = 0.6f;

        // Add render layers here for special effects:
        // this.addRenderLayer(new ZulmakGlowLayer(this));
    }
}
