package com.ancientcurse.client.render.entity;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.client.model.SnakeHeadProjectileModel;
import com.ancientcurse.entity.SnakeHeadProjectileEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SnakeHeadProjectileRenderer extends GeoEntityRenderer<SnakeHeadProjectileEntity> {
    
    public SnakeHeadProjectileRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new SnakeHeadProjectileModel());
    }

    @Override
    public Identifier getTextureLocation(SnakeHeadProjectileEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/snake_head.png");
    }
} 