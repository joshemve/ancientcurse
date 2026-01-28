package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.BindingBoltEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.util.Identifier;

public class BindingBoltRenderer extends ProjectileEntityRenderer<BindingBoltEntity> {
    private static final Identifier TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/binding_bolt.png");

    public BindingBoltRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(BindingBoltEntity entity) {
        return TEXTURE;
    }
}
