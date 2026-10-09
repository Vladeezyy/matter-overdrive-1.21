package matteroverdrive.compat.render;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** 1.21.1 stand-in for the 1.21.2+ EntityRenderState (the fields the renderers use). */
public class EntityRenderState {
    public EntityType<?> entityType;
    public double x, y, z;
    public float ageInTicks;
    public float boundingBoxWidth, boundingBoxHeight, eyeHeight;
    public double distanceToCameraSq;
    public boolean isInvisible, isDiscrete;
    public int lightCoords;
    public int outlineColor;
    public float partialTick;

    public void extractBase(Entity entity, float partialTick) {
        entityType = entity.getType();
        x = net.minecraft.util.Mth.lerp(partialTick, entity.xOld, entity.getX());
        y = net.minecraft.util.Mth.lerp(partialTick, entity.yOld, entity.getY());
        z = net.minecraft.util.Mth.lerp(partialTick, entity.zOld, entity.getZ());
        ageInTicks = entity.tickCount + partialTick;
        boundingBoxWidth = entity.getBbWidth();
        boundingBoxHeight = entity.getBbHeight();
        eyeHeight = entity.getEyeHeight();
        isInvisible = entity.isInvisible();
        isDiscrete = entity.isDiscrete();
        this.partialTick = partialTick;
        distanceToCameraSq = net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().distanceToSqr(entity);
    }
}
